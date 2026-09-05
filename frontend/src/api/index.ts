import axios from 'axios';
import { User, Message } from '../types';

const api = axios.create({
    baseURL: 'http://localhost:8081',
    headers: {
        'Content-Type': 'application/json',
    },
});

export const registerUser = async (username: string): Promise<User> => {
    const response = await api.post('/api/users/register', { username });
    return response.data;
};

export const getAllUsers = async (): Promise<User[]> => {
    const response = await api.get('/api/users');
    return response.data;
};

export const getRoomMessages = async (roomId: string): Promise<Message[]> => {
    const response = await api.get(`/api/messages/${roomId}`);
    return response.data;
};

export default api;