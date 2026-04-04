import api from './api';
import { auth, googleProvider } from './firebase';
import { signInWithPopup } from 'firebase/auth';

const AUTH_ENDPOINTS = {
    login: '/auth/email/login',
    register: '/auth/email/register',
    verify: '/auth/verify',
    forgotPassword: '/auth/email/forgot-password',
    changePassword: '/auth/email/change-password',
    emailExists: '/auth/email/exists',
};

// ─── LOGIN ────────────────────────────────────────────────────────────────────
// Usa o Firebase Identity Toolkit via backend: POST /api/auth/email/login
// Retorna { idToken, refreshToken, expiresIn, localId, email, registered }
export const loginUser = async (email, password) => {
    const response = await api.post(AUTH_ENDPOINTS.login, { email, password });

    const { idToken, localId, email: userEmail } = response.data;

    // Salva o Firebase idToken — o interceptor do api.js vai colocá-lo no Authorization
    localStorage.setItem('token', idToken);
    localStorage.setItem('userId', localId);
    localStorage.setItem('userEmail', userEmail);

    // Verifica o estado no backend para bloquear acesso quando emailVerified=false.
    const verifyResponse = await api.post(AUTH_ENDPOINTS.verify, { idToken, userType: null });
    if (verifyResponse?.data?.verificationRequired) {
        localStorage.removeItem('token');
        localStorage.removeItem('userId');
        localStorage.removeItem('userEmail');
        const error = new Error('E-mail ainda não verificado. Verifique sua caixa de entrada e tente novamente.');
        error.response = { data: { message: error.message } };
        throw error;
    }

    // Salva role e isOwner para uso no redirecionamento e em guards de rota
    const role = verifyResponse.data?.role || 'ROLE_CUSTOMER';
    const isOwner = verifyResponse.data?.isOwner || false;
    localStorage.setItem('userRole', role);
    localStorage.setItem('isOwner', String(isOwner));

    return { ...response.data, profile: verifyResponse.data };
};

// ─── CADASTRO (CUSTOMER) ──────────────────────────────────────────────────────
// Usa: POST /api/auth/email/register (público, sem auth)
// Retorna { idToken, refreshToken, expiresIn, localId, profile }
export const registerCustomer = async (userData) => {
    const cleanCPF  = userData.documentCPF ? userData.documentCPF.replace(/\D/g, '') : '';
    const cleanTell = userData.tell        ? userData.tell.replace(/\D/g, '')         : '';

    const response = await api.post(AUTH_ENDPOINTS.register, {
        email:       userData.email,
        password:    userData.password,
        userType:    'CUSTOMER',
        name:        userData.name,
        tell:        cleanTell,
        documentCPF: cleanCPF,
    });

    return response.data;
};

// ─── CADASTRO (BARBER) ────────────────────────────────────────────────────────
// Usa: POST /api/auth/email/register (público, sem auth)
export const registerBarber = async (barberData) => {
    const cleanCPF  = barberData.documentCPF ? barberData.documentCPF.replace(/\D/g, '') : '';
    const cleanTell = barberData.tell        ? barberData.tell.replace(/\D/g, '')         : '';

    const response = await api.post(AUTH_ENDPOINTS.register, {
        email:         barberData.email,
        password:      barberData.password,
        userType:      'BARBER',
        name:          barberData.name,
        tell:          cleanTell,
        documentCPF:   cleanCPF,
        workStartTime: barberData.workStartTime, // "09:00"
        workEndTime:   barberData.workEndTime,   // "18:00"
        isOwner:       barberData.isOwner ?? false,
    });

    return response.data;
};

// ─── LOGOUT ───────────────────────────────────────────────────────────────────
export const logoutUser = () => {
    localStorage.clear();
};

// ─── RECUPERAR SENHA ─────────────────────────────────────────────────────────
// Usa: POST /api/auth/email/forgot-password (público, sem auth)
// Firebase envia e-mail com link de redefinição. Resposta 204 = sucesso.
export const forgotPassword = async (email) => {
    await api.post(AUTH_ENDPOINTS.forgotPassword, { email });
};

// ─── ALTERAR SENHA ────────────────────────────────────────────────────────────
// Usa: POST /api/auth/email/change-password (público, idToken no body)
// Após alterar, Firebase invalida todas as sessões. Usuário precisa fazer login novamente.
export const changePassword = async (idToken, newPassword) => {
    await api.post(AUTH_ENDPOINTS.changePassword, { idToken, newPassword });
};

// ─── VERIFICAR SE E-MAIL EXISTE ───────────────────────────────────────────────
// Usa: GET /api/auth/email/exists?email=...
// Retorna { exists: boolean, userType: "CUSTOMER"|"BARBER"|null }
// Usado pelo redirecionamento inteligente no login.
export const checkEmailExists = async (email) => {
    const response = await api.get(AUTH_ENDPOINTS.emailExists, { params: { email } });
    return response.data;
};

// ─── LOGIN COM GOOGLE ─────────────────────────────────────────────────────────
// 1. Abre popup do Google (Firebase SDK)
// 2. Pega o idToken e envia para o backend via /api/auth/verify
// 3. Se o backend retornar 404/USER_NOT_FOUND, lança erro especial
//    para que o front redirecione para o cadastro com os dados do Google.
export const loginWithGoogle = async () => {
    const result = await signInWithPopup(auth, googleProvider);
    const idToken = await result.user.getIdToken();

    // Salva o token temporariamente para o verify
    localStorage.setItem('token', idToken);
    localStorage.setItem('userId', result.user.uid);
    localStorage.setItem('userEmail', result.user.email);

    try {
        const verifyResponse = await api.post(AUTH_ENDPOINTS.verify, { idToken, userType: null });

        if (verifyResponse?.data?.verificationRequired) {
            localStorage.removeItem('token');
            localStorage.removeItem('userId');
            localStorage.removeItem('userEmail');
            const error = new Error('E-mail ainda não verificado.');
            error.response = { data: { message: error.message } };
            throw error;
        }

        // Salva role e isOwner para uso no redirecionamento e em guards de rota
        const role = verifyResponse.data?.role || 'ROLE_CUSTOMER';
        const isOwner = verifyResponse.data?.isOwner || false;
        localStorage.setItem('userRole', role);
        localStorage.setItem('isOwner', String(isOwner));

        return {
            idToken,
            localId: result.user.uid,
            email: result.user.email,
            displayName: result.user.displayName,
            photoURL: result.user.photoURL,
            profile: verifyResponse.data,
        };
    } catch (err) {
        // Se o backend retornar 404, o usuário não existe — sinaliza redirecionamento para cadastro
        if (err?.response?.status === 404) {
            localStorage.removeItem('token');
            localStorage.removeItem('userId');
            localStorage.removeItem('userEmail');
            const redirectError = new Error('USER_NOT_FOUND');
            redirectError.code = 'USER_NOT_FOUND';
            redirectError.googleData = {
                idToken,
                email: result.user.email,
                displayName: result.user.displayName,
                photoURL: result.user.photoURL,
                uid: result.user.uid,
            };
            throw redirectError;
        }
        throw err;
    }
};