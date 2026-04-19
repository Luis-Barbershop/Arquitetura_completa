import axios from 'axios';
import { toast } from 'react-toastify';

const rawBaseUrl = import.meta.env.VITE_API_BASE_URL || 'https://api.cortaai.shop/api';
const normalizedBaseUrl = rawBaseUrl.endsWith('/') ? rawBaseUrl.slice(0, -1) : rawBaseUrl;

const SESSION_COOKIE_MODE = import.meta.env.VITE_SESSION_COOKIE_MODE === 'true';
const SESSION_BEARER_FALLBACK = import.meta.env.VITE_SESSION_BEARER_FALLBACK !== 'false';
const SESSION_COOKIE_CANARY_PERCENT = Math.max(
    0,
    Math.min(100, Number.parseInt(import.meta.env.VITE_SESSION_COOKIE_CANARY_PERCENT || '0', 10) || 0),
);

const getSessionIdentity = () =>
    localStorage.getItem('userId') || localStorage.getItem('userEmail') || localStorage.getItem('userRole') || '';

const hashToPercent = (value) => {
    if (!value) return 100;

    let hash = 0;
    for (let i = 0; i < value.length; i += 1) {
        hash = (hash * 31 + value.charCodeAt(i)) >>> 0;
    }

    return hash % 100;
};

const shouldUseCookiePilotForCurrentUser = () => {
    if (!SESSION_COOKIE_MODE) return false;
    if (SESSION_COOKIE_CANARY_PERCENT >= 100) return true;
    if (SESSION_COOKIE_CANARY_PERCENT <= 0) return false;

    return hashToPercent(getSessionIdentity()) < SESSION_COOKIE_CANARY_PERCENT;
};

const api = axios.create({
    baseURL: normalizedBaseUrl,
    withCredentials: SESSION_COOKIE_MODE,
});

api.interceptors.request.use(async (config) => {
    const useCookiePilot = shouldUseCookiePilotForCurrentUser();
    config.withCredentials = useCookiePilot;

    const token = localStorage.getItem('token');
    if (token && (!useCookiePilot || SESSION_BEARER_FALLBACK)) {
        config.headers.Authorization = `Bearer ${token}`;
    } else if (config.headers?.Authorization) {
        delete config.headers.Authorization;
    }

    return config;
});

// --- Interceptor de respostas: traducao global de erros HTTP -> PT-BR --------
// Rotas de auth tratam os proprios erros localmente; silencia toast para elas.
const SILENT_URLS = ['/auth/email/login', '/auth/email/register', '/auth/verify', '/auth/email/exists', '/barbershops/my-invites'];
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
                if (localStorage.getItem('token') || localStorage.getItem('userId') || localStorage.getItem('userEmail')) {
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
