import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { ChatMessage } from '../types';

class WebSocketService {
    private client: Client | null = null;
    private heartbeatInterval: ReturnType<typeof setInterval> | null = null;
    private pendingRoomSubscriptions: { roomId: string; onMessage: (msg: ChatMessage) => void }[] = [];

    connect(userId: number, onMessage: (msg: ChatMessage) => void, onPresence: (data: any) => void) {
        this.client = new Client({
            webSocketFactory: () => new SockJS('http://localhost:8081/ws'),
            reconnectDelay: 5000,
            onConnect: () => {
                console.log('Connected to WebSocket');

                // Register this session so the backend can track presence
                this.client?.publish({
                    destination: '/app/chat.register',
                    body: JSON.stringify({ senderId: userId, content: '', roomId: '' }),
                });

                // Subscribe to presence updates
                this.client?.subscribe('/topic/presence', (message: IMessage) => {
                    onPresence(JSON.parse(message.body));
                });

                // Now that we're connected, subscribe to any rooms that were requested early
                this.pendingRoomSubscriptions.forEach(({ roomId, onMessage }) => {
                    this.client?.subscribe(`/topic/room/${roomId}`, (message: IMessage) => {
                        onMessage(JSON.parse(message.body));
                    });
                });
                this.pendingRoomSubscriptions = [];

                // Start sending heartbeats every 15 seconds
                this.heartbeatInterval = setInterval(() => {
                    this.client?.publish({
                        destination: '/app/chat.heartbeat',
                        body: '',
                    });
                }, 15000);
            },
        });

        this.client.activate();
    }

    subscribeToRoom(roomId: string, onMessage: (msg: ChatMessage) => void) {
        if (this.client?.connected) {
            this.client.subscribe(`/topic/room/${roomId}`, (message: IMessage) => {
                onMessage(JSON.parse(message.body));
            });
        } else {
            // Not connected yet — queue it and subscribe once onConnect fires
            this.pendingRoomSubscriptions.push({ roomId, onMessage });
        }
    }

    sendMessage(senderId: number, content: string, roomId: string) {
        this.client?.publish({
            destination: '/app/chat.sendMessage',
            body: JSON.stringify({ senderId, content, roomId }),
        });
    }

    disconnect() {
        if (this.heartbeatInterval) {
            clearInterval(this.heartbeatInterval);
        }
        this.client?.deactivate();
    }
}

export default new WebSocketService();