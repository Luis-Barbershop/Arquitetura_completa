import axios from 'axios';

const api = axios.create({
    // Em dev: http://localhost:8080/api
    // Em produção (Docker): usa o IP/domínio do servidor na porta do gateway
    // A variável VITE_API_URL pode ser definida em .env ou .env.production
    baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api', 
});

api.interceptors.request.use(async (config) => {
    const token = localStorage.getItem('token');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

export default api;