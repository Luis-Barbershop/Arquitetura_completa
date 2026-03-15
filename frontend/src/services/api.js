import axios from 'axios';
import { auth } from './firebase';

const api = axios.create({
    baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api',
});

// Interceptor: injeta o Firebase ID Token atualizado em cada requisição
api.interceptors.request.use(async (config) => {
    const user = auth.currentUser;
    if (user) {
        const idToken = await user.getIdToken();
        config.headers.Authorization = `Bearer ${idToken}`;
    }
    return config;
});

export default api;