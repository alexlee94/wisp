export interface User {
    id: number;
    username: string;
    online: boolean;
    lastSeen: string | null;
}

export interface ChatMessage {
    senderId: number;
    senderUsername: string | null;
    content: string;
    roomId: string;
}

export interface Message {
    id: number;
    sender: User;
    content: string;
    roomId: string;
    sentAt: string;
}