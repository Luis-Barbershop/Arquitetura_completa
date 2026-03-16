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

        // Envia o token para o Backend verificar e provisionar o utilizador
        const response = await api.post('/api/auth/verify', { token: idToken });
        
        // Retorna o objeto completo { token, user, profileComplete, userType }
        return response.data;
    } catch (error) {
        console.error("Erro no login com Google:", error);
        throw error;
    }
};

// NOVA FUNÇÃO PARA COMPLETAR O PERFIL (FALTAVA ESTA EXPORTAÇÃO!)
export const completeProfileApi = async (type, data) => {
    try {
        const endpoint = type === 'BARBER' 
            ? '/api/auth/complete-profile/barber' 
            : '/api/auth/complete-profile/customer';
            
        const response = await api.post(endpoint, data);
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
        
        const response = await api.post('/api/auth/verify', { token: idToken });
        return response.data; 
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
        const endpoint = type === 'BARBER' ? '/api/barbers/register' : '/api/customers/register';
        
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
export const logout = async () => {
    try {
        await signOut(auth);
        localStorage.removeItem('token');
        localStorage.removeItem('user');
    } catch (error) {
        console.error("Erro no logout:", error);
        throw error;
    }
};