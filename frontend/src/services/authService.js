import api from './api';
import { auth } from './firebase';
import {
    signInWithEmailAndPassword,
    createUserWithEmailAndPassword,
    signInWithPopup,
    GoogleAuthProvider,
    signOut,
    sendEmailVerification,
    reload,
} from 'firebase/auth';

const normalizeAuthPayload = (backendData, firebaseUid) => ({
    user: {
        id: backendData.id,
        name: backendData.name,
        email: backendData.email,
        phone: backendData.phone,
        photoUrl: backendData.photoUrl,
        firebaseUid,
    },
    userType: backendData.userType,
    profileComplete: backendData.profileComplete,
    role: backendData.role,
    authProvider: backendData.authProvider,
    emailVerified: backendData.emailVerified,
    verificationRequired: backendData.verificationRequired,
});

// LOGIN COM GOOGLE (ATUALIZADO PARA O NOVO FLUXO)
export const signInWithGoogle = async (userType = null) => {
    const provider = new GoogleAuthProvider();
    try {
        const result = await signInWithPopup(auth, provider);
        const idToken = await result.user.getIdToken();

        // Backend espera { idToken, userType } conforme FirebaseAuthRequestDTO
        const response = await api.post('/auth/verify', { idToken, userType });
        
        
        // AuthResponseDTO retorna: { id, name, email, phone, photoUrl, userType, authProvider, profileComplete, role }
        // Normalizamos para o formato que os componentes esperam
        return normalizeAuthPayload(response.data, result.user.uid);
    } catch (error) {
        console.error("Erro no login com Google:", error);
        if (error.response) {
            console.error("Status:", error.response.status, "Body:", JSON.stringify(error.response.data));
        }
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

        if (auth.currentUser) {
            await auth.currentUser.getIdToken(true);
        }

        
        return response.data;
    } catch (error) {
        console.error("Erro ao completar o perfil:", error);
        if (error.response) {
            console.error("Status:", error.response.status, "Body:", JSON.stringify(error.response.data));
        }
        throw error;
    }
};

// Login Padrão (Email e Senha)
export const login = async (email, password) => {
    try {
        const userCredential = await signInWithEmailAndPassword(auth, email, password);
        await reload(userCredential.user);

        if (!userCredential.user.emailVerified) {
            await sendEmailVerification(userCredential.user);
            await signOut(auth);
            const error = new Error('Email nao verificado.');
            error.code = 'auth/email-not-verified';
            throw error;
        }

        const idToken = await userCredential.user.getIdToken();
        
        // Backend espera { idToken, userType } conforme FirebaseAuthRequestDTO
        const response = await api.post('/auth/verify', { idToken, userType: null });
        if (response.data?.verificationRequired) {
            await signOut(auth);
            const error = new Error('Email nao verificado.');
            error.code = 'auth/email-not-verified';
            throw error;
        }
        
        // Normaliza a resposta do AuthResponseDTO
        return normalizeAuthPayload(response.data, userCredential.user.uid);
    } catch (error) {
        console.error("Erro no login:", error);
        throw error;
    }
};

// Registo Padrão (Email e Senha)
// Fluxo: Firebase createUser → /auth/verify → /auth/{type}/complete-profile
export const register = async (email, password, userData, type) => {
    try {
        // 1. Cria o user no Firebase Authentication
        const userCredential = await createUserWithEmailAndPassword(auth, email, password);
        await sendEmailVerification(userCredential.user);
        await signOut(auth);

        return {
            user: {
                id: null,
                name: userData.name || userCredential.user.displayName || 'Usuario',
                email,
                phone: userData.tell || null,
                photoUrl: null,
                firebaseUid: userCredential.user.uid,
            },
            userType: type,
            profileComplete: false,
            role: type === 'BARBER' ? 'ROLE_BARBER' : 'ROLE_CUSTOMER',
            authProvider: 'EMAIL',
            emailVerified: false,
            verificationRequired: true,
        };
    } catch (error) {
        console.error("Erro no registo:", error);
        if (error.response) {
            console.error("Status:", error.response.status, "Body:", JSON.stringify(error.response.data));
        }
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