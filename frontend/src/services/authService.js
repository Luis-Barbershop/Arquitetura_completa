import api from './api';

// ─── LOGIN ────────────────────────────────────────────────────────────────────
// Usa o Firebase Identity Toolkit via backend: POST /api/auth/firebase-test/sign-in-email
// Retorna { idToken, refreshToken, expiresIn, localId, email, registered }
export const loginUser = async (email, password) => {
    const response = await api.post('/auth/firebase-test/sign-in-email', { email, password });

    const { idToken, localId, email: userEmail } = response.data;

    // Salva o Firebase idToken — o interceptor do api.js vai colocá-lo no Authorization
    localStorage.setItem('token', idToken);
    localStorage.setItem('userId', localId);
    localStorage.setItem('userEmail', userEmail);

    return response.data;
};

// ─── CADASTRO (CUSTOMER) ──────────────────────────────────────────────────────
// Usa: POST /api/auth/firebase-test/register-email (público, sem auth)
// Retorna { idToken, refreshToken, expiresIn, localId, profile }
export const registerCustomer = async (userData) => {
    const cleanCPF  = userData.documentCPF ? userData.documentCPF.replace(/\D/g, '') : '';
    const cleanTell = userData.tell        ? userData.tell.replace(/\D/g, '')         : '';

    const response = await api.post('/auth/firebase-test/register-email', {
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
// Usa: POST /api/auth/firebase-test/register-email (público, sem auth)
export const registerBarber = async (barberData) => {
    const cleanCPF  = barberData.documentCPF ? barberData.documentCPF.replace(/\D/g, '') : '';
    const cleanTell = barberData.tell        ? barberData.tell.replace(/\D/g, '')         : '';

    const response = await api.post('/auth/firebase-test/register-email', {
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