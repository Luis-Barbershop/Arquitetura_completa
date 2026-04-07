import axios from 'axios';
import { toast } from 'react-toastify';

const rawBaseUrl = import.meta.env.VITE_API_BASE_URL || 'https://api.cortaai.shop/api';
const normalizedBaseUrl = rawBaseUrl.endsWith('/') ? rawBaseUrl.slice(0, -1) : rawBaseUrl;

const api = axios.create({
    baseURL: normalizedBaseUrl,
});

api.interceptors.request.use(async (config) => {
    const token = localStorage.getItem('token');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

// --- Interceptor de respostas: traducao global de erros HTTP -> PT-BR --------
// Rotas de auth tratam os proprios erros localmente; silencia toast para elas.
const SILENT_URLS = ['/auth/email/login', '/auth/email/register', '/auth/verify', '/auth/email/exists'];
const isSilentUrl = (url = '') => SILENT_URLS.some(u => url.includes(u));

api.interceptors.response.use(
    (response) => response,
    (error) => {
        const status = error?.response?.status;
        const url    = error?.config?.url || '';

        if (isSilentUrl(url)) return Promise.reject(error);
        if (axios.isCancel(error)) return Promise.reject(error);

        if (!status) {
            toast.error('Sem conexao com o servidor. Verifique sua internet.');
            return Promise.reject(error);
        }

        switch (status) {
            case 401:
                toast.error('Sessao expirada. Faca login novamente.');
                if (localStorage.getItem('token')) {
                    localStorage.clear();
                    setTimeout(() => { window.location.href = '/login'; }, 1500);
                }
                break;
            case 403:
                if (!url.includes('/auth/')) {
                    toast.error('Acesso nao autorizado.');
                }
                break;
            case 400: {
                const backendMsg = error?.response?.data?.message || 'Dados invalidos. Verifique os campos.';
                toast.error(backendMsg);
                break;
            }
            case 404:
                break;
            case 502:
            case 503:
                toast.error('Servico temporariamente indisponivel. Tente novamente em instantes.');
                break;
            case 500:
            default:
                toast.error('Erro interno do servidor. Tente novamente mais tarde.');
                break;
        }

        return Promise.reject(error);
    }
);

export default api;
