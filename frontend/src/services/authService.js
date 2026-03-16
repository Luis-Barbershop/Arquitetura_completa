import api from './api';
import { auth } from './firebase';
import {
    signInWithEmailAndPassword,
    createUserWithEmailAndPassword,
    signInWithPopup,
    GoogleAuthProvider,
    signOut,
} from 'firebase/auth';

// LOGIN COM GOOGLE (ATUALIZADO PARA O NOVO FLUXO)
export const signInWithGoogle = async () => {
    const provider = new GoogleAuthProvider();
    try {
        const result = await signInWithPopup(auth, provider);
        const idToken = await result.user.getIdToken();

        // Backend espera { idToken, userType } conforme FirebaseAuthRequestDTO
        const response = await api.post('/auth/verify', { idToken, userType: null });
        
        // AuthResponseDTO retorna: { id, name, email, phone, photoUrl, userType, authProvider, profileComplete, role }
        // Normalizamos para o formato que os componentes esperam
        const data = response.data;
        return {
            user: {
                id: data.id,
                name: data.name,
                email: data.email,
                phone: data.phone,
                photoUrl: data.photoUrl,
                firebaseUid: result.user.uid,
            },
            userType: data.userType,
            profileComplete: data.profileComplete,
            role: data.role,
            authProvider: data.authProvider,
        };
    } catch (error) {
        console.error("Erro no login com Google:", error);
        throw error;
    }
};

// NOVA FUNÇÃO PARA COMPLETAR O PERFIL (FALTAVA ESTA EXPORTAÇÃO!)
export const completeProfileApi = async (type, data, firebaseUserInfo = {}) => {
    try {
        // Backend: /api/auth/customers/complete-profile ou /api/auth/barbers/complete-profile
        // baseURL já inclui /api, então usamos apenas /auth/...
        const endpoint = type === 'BARBER' 
            ? '/auth/barbers/complete-profile' 
            : '/auth/customers/complete-profile';
        
        // Envia também o nome do Firebase para o backend salvar junto
        const payload = { ...data };
        if (firebaseUserInfo.name && !payload.name) {
            payload.name = firebaseUserInfo.name;
        }
            
        const response = await api.post(endpoint, payload);
        return response.data;
    } catch (error) {
        console.error("Erro ao completar o perfil:", error);
        throw error;
    }
};

// Login Padrão (Email e Senha)
export const login = async (email, password) => {
    try {
        const userCredential = await signInWithEmailAndPassword(auth, email, password);
        const idToken = await userCredential.user.getIdToken();
        
        // Backend espera { idToken, userType } conforme FirebaseAuthRequestDTO
        const response = await api.post('/auth/verify', { idToken, userType: null });
        
        // Normaliza a resposta do AuthResponseDTO
        const data = response.data;
        return {
            user: {
                id: data.id,
                name: data.name,
                email: data.email,
                phone: data.phone,
                photoUrl: data.photoUrl,
                firebaseUid: userCredential.user.uid,
            },
            userType: data.userType,
            profileComplete: data.profileComplete,
            role: data.role,
            authProvider: data.authProvider,
        };
    } catch (error) {
        console.error("Erro no login:", error);
        throw error;
    }
};

// Registo Padrão (Email e Senha)
export const register = async (email, password, userData, type) => {
    try {
        const userCredential = await createUserWithEmailAndPassword(auth, email, password);
        const idToken = await userCredential.user.getIdToken(); // Token de verificação

        // Consoante o tipo, envia para a rota certa do backend
        // baseURL já inclui /api, então usamos apenas /barbers/register ou /customers/register
        const endpoint = type === 'BARBER' ? '/barbers/register' : '/customers/register';
        
        // Passa o UID do Firebase e os dados digitados para o Java
        const response = await api.post(endpoint, { 
            ...userData, 
            firebaseUid: userCredential.user.uid 
        });
        
        return response.data;
    } catch (error) {
        console.error("Erro no registo:", error);
        throw error;
    }
};

// Logout
export const logoutUser = async () => {
    try {
        await signOut(auth);
        localStorage.removeItem('token');
        localStorage.removeItem('user');
    } catch (error) {
        console.error("Erro no logout:", error);
        throw error;
    }
};