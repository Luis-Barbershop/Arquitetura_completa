import axios from 'axios';

const api = axios.create({
    // O IMPORTANTE É O "https://" NO COMEÇO
    baseURL: 'http://localhost:8080/api', 
});

api.interceptors.request.use(async (config) => {
    const token = localStorage.getItem('token');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

export default api;