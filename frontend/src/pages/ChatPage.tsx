import { useState, useEffect, useRef, FormEvent } from 'react';
import {
    Container, Box, TextField, Button, Typography,
    Paper, List, ListItem, ListItemText, Avatar,
    Chip, Divider
} from '@mui/material';
import { User, ChatMessage } from '../types';
import { registerUser, getAllUsers, getRoomMessages } from '../api';
import websocketService from '../api/websocket';

const ROOM_ID = 'general';

const ChatPage = () => {
    const [username, setUsername] = useState('');
    const [currentUser, setCurrentUser] = useState<User | null>(null);
    const [users, setUsers] = useState<User[]>([]);
    const [messages, setMessages] = useState<ChatMessage[]>([]);
    const [input, setInput] = useState('');
    const messagesEndRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        if (currentUser) {
            fetchUsers();
            fetchHistory();

            websocketService.connect(
                currentUser.id,
                () => {},
                (presenceUpdate) => {
                    setUsers(prev => prev.map(u =>
                        u.id === presenceUpdate.userId ? { ...u, online: presenceUpdate.online } : u
                    ));
                }
            );

            websocketService.subscribeToRoom(ROOM_ID, (msg) => {
                setMessages(prev => [...prev, msg]);
            });
        }

        return () => {
            websocketService.disconnect();
        };
    }, [currentUser]);

    useEffect(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, [messages]);

    const fetchUsers = async () => {
        const data = await getAllUsers();
        setUsers(data);
    };

    const fetchHistory = async () => {
        const history = await getRoomMessages(ROOM_ID);
        setMessages(history.map(m => ({
            senderId: m.sender.id,
            senderUsername: m.sender.username,
            content: m.content,
            roomId: m.roomId
        })));
    };

    const handleLogin = async (e: FormEvent) => {
        e.preventDefault();
        if (!username.trim()) return;
        const user = await registerUser(username);
        setCurrentUser(user);
    };

    const handleSend = (e: FormEvent) => {
        e.preventDefault();
        if (!input.trim() || !currentUser) return;
        websocketService.sendMessage(currentUser.id, input, ROOM_ID);
        setInput('');
    };

    if (!currentUser) {
        return (
            <Container maxWidth="sm" sx={{ mt: 8 }}>
                <Paper elevation={3} sx={{ p: 4 }}>
                    <Typography variant="h4" sx={{ mb: 3, textAlign: 'center', fontWeight: 'bold' }}>
                        Wisp
                    </Typography>
                    <Box component="form" onSubmit={handleLogin}>
                        <TextField
                            fullWidth
                            label="Username"
                            value={username}
                            onChange={(e) => setUsername(e.target.value)}
                            margin="normal"
                        />
                        <Button fullWidth type="submit" variant="contained" sx={{ mt: 2 }}>
                            Join Chat
                        </Button>
                    </Box>
                </Paper>
            </Container>
        );
    }

    return (
        <Container maxWidth="md" sx={{ mt: 4, mb: 4 }}>
            <Box sx={{ display: 'flex', gap: 2 }}>
                {/* Sidebar - online users */}
                <Paper elevation={2} sx={{ width: 200, p: 2 }}>
                    <Typography variant="h6" sx={{ mb: 2 }}>Users</Typography>
                    <List dense>
                        {users.map(user => (
                            <ListItem key={user.id}>
                                <ListItemText
                                    primary={user.username}
                                />
                                <Chip
                                    label={user.online ? 'online' : 'offline'}
                                    color={user.online ? 'success' : 'default'}
                                    size="small"
                                />
                            </ListItem>
                        ))}
                    </List>
                </Paper>

                {/* Chat area */}
                <Paper elevation={2} sx={{ flexGrow: 1, p: 2, display: 'flex', flexDirection: 'column', height: 500 }}>
                    <Typography variant="h6" sx={{ mb: 1 }}>#{ROOM_ID}</Typography>
                    <Divider sx={{ mb: 2 }} />
                    <Box sx={{ flexGrow: 1, overflowY: 'auto', mb: 2 }}>
                        {messages.map((msg, i) => (
                            <Box key={i} sx={{ display: 'flex', gap: 1, mb: 1 }}>
                                <Avatar sx={{ width: 28, height: 28 }}>
                                    {msg.senderUsername?.[0]?.toUpperCase()}
                                </Avatar>
                                <Box>
                                    <Typography variant="body2" sx={{ fontWeight: 'bold' }}>
                                        {msg.senderUsername || `User ${msg.senderId}`}
                                    </Typography>
                                    <Typography variant="body2">{msg.content}</Typography>
                                </Box>
                            </Box>
                        ))}
                        <div ref={messagesEndRef} />
                    </Box>
                    <Box component="form" onSubmit={handleSend} sx={{ display: 'flex', gap: 1 }}>
                        <TextField
                            fullWidth
                            size="small"
                            placeholder="Type a message..."
                            value={input}
                            onChange={(e) => setInput(e.target.value)}
                        />
                        <Button type="submit" variant="contained">Send</Button>
                    </Box>
                </Paper>
            </Box>
        </Container>
    );
};

export default ChatPage;