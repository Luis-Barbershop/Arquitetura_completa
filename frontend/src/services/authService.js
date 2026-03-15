import api from './api';
import { auth } from './firebase';
import {
    signInWithEmailAndPassword,
    createUserWithEmailAndPassword,
    signInWithPopup,
    GoogleAuthProvider,
    signOut,
} from 'firebase/auth';

// Login Google (retorna objeto completo do backend)
export const signInWithGoogle = async () => {
    const provider = new GoogleAuthProvider();
    try {
        const result = await signInWithPopup(auth, provider);
        const idToken = await result.user.getIdToken();
        // Envia o token para o backend verificar/provisionar
        const response = await api.post('/api/auth/verify', { token: idToken });
        // Retorna o objeto completo do backend (contendo isProfileComplete, userType, etc)
        return response.data;
    } catch (error) {
        console.error("Erro no login social:", error);
        throw error;
    }
};

// Completar perfil após login social
export const completeProfile = async (type, data) => {
    const endpoint = type === 'BARBER' ? '/api/auth/complete-profile/barber' : '/api/auth/complete-profile/customer';
    return await api.post(endpoint, data);
};
import api from './api';
import { auth } from './firebase';
import {
    signInWithEmailAndPassword,
    createUserWithEmailAndPassword,
    signInWithPopup,
    GoogleAuthProvider,
    signOut,
} from 'firebase/auth';

// ─────────────────────────────────────────────────────────────
// Helpers internos
// ─────────────────────────────────────────────────────────────

/**
 * Após autenticar no Firebase (email/senha, Google, etc.),
 * envia o idToken para o backend verificar e auto-provisionar.
 *
 * Backend: POST /api/auth/verify  { idToken, userType }
 * Resposta: AuthResponseDTO { id, name, email, phone, photoUrl, userType, authProvider, profileComplete, role }
 */
const verifyWithBackend = async (firebaseUser, userType) => {
    const idToken = await firebaseUser.getIdToken();
    const response = await api.post('/auth/verify', { idToken, userType });
    const data = response.data;

    // Persiste dados essenciais para componentes que ainda leem do localStorage
    localStorage.setItem('userId', data.id);
    localStorage.setItem('userName', data.name || '');
    localStorage.setItem('role', data.role);          // ROLE_CUSTOMER | ROLE_BARBER | ROLE_OWNER
    localStorage.setItem('userType', data.userType);  // CUSTOMER | BARBER
    localStorage.setItem('user', JSON.stringify(data));

    return data;
};

// ─────────────────────────────────────────────────────────────
// Login com e-mail e senha
// ─────────────────────────────────────────────────────────────

export const loginUser = async (email, password, userType = 'CUSTOMER') => {
    // 1. Autentica no Firebase
    const credential = await signInWithEmailAndPassword(auth, email, password);

    // 2. Verifica no backend e auto-provisiona
    const data = await verifyWithBackend(credential.user, userType.toUpperCase());

    return data; // { id, name, email, role, profileComplete, ... }
};

// ─────────────────────────────────────────────────────────────
// Login com Google
// ─────────────────────────────────────────────────────────────

export const loginWithGoogle = async (userType = 'CUSTOMER') => {
    const provider = new GoogleAuthProvider();
    const credential = await signInWithPopup(auth, provider);

    const data = await verifyWithBackend(credential.user, userType.toUpperCase());
    return data;
};

// ─────────────────────────────────────────────────────────────
// Registro de Cliente
// ─────────────────────────────────────────────────────────────

export const registerCustomer = async (userData) => {
    // 1. Cria conta no Firebase
    const credential = await createUserWithEmailAndPassword(auth, userData.email, userData.password);

    // 2. Verifica no backend (auto-provisiona como CUSTOMER)
    const data = await verifyWithBackend(credential.user, 'CUSTOMER');

    // 3. Completa perfil com CPF e telefone
    const cleanCPF = (userData.documentCPF || '').replace(/\D/g, '');
    const cleanTell = (userData.tell || '').replace(/\D/g, '');

    const completeResponse = await api.post('/auth/customers/complete-profile', {
        tell: cleanTell,
        documentCPF: cleanCPF,
        name: userData.name,
    });

    return completeResponse.data;
};

// ─────────────────────────────────────────────────────────────
// Registro de Barbeiro
// ─────────────────────────────────────────────────────────────

export const registerBarber = async (barberData) => {
    // 1. Cria conta no Firebase
    const credential = await createUserWithEmailAndPassword(auth, barberData.email, barberData.password);

    // 2. Verifica no backend (auto-provisiona como BARBER)
    const data = await verifyWithBackend(credential.user, 'BARBER');

    // 3. Completa perfil com CPF, telefone e horários
    const cleanCPF = (barberData.documentCPF || '').replace(/\D/g, '');
    const cleanTell = (barberData.tell || '').replace(/\D/g, '');

    const completeResponse = await api.post('/auth/barbers/complete-profile', {
        tell: cleanTell,
        documentCPF: cleanCPF,
        name: barberData.name,
        workStartTime: barberData.workStartTime,
        workEndTime: barberData.workEndTime,
        isOwner: barberData.isOwner || false,
    });

    return completeResponse.data;
};

// ─────────────────────────────────────────────────────────────
// Buscar dados do usuário logado
// ─────────────────────────────────────────────────────────────

export const getMe = async () => {
    const response = await api.get('/auth/me');
    const data = response.data;

    localStorage.setItem('userId', data.id);
    localStorage.setItem('userName', data.name || '');
    localStorage.setItem('role', data.role);
    localStorage.setItem('userType', data.userType);
    localStorage.setItem('user', JSON.stringify(data));

    return data;
};

// ─────────────────────────────────────────────────────────────
// Logout
// ─────────────────────────────────────────────────────────────

export const logoutUser = async () => {
    await signOut(auth);
    localStorage.clear();
    window.location.href = '/login';
};