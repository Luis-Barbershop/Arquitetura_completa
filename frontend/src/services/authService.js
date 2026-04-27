import api from './api';
import { auth, googleProvider } from './firebase';
import { signInWithPopup } from 'firebase/auth';
import { registerPushNotificationsIfPossible, unregisterPushNotificationsIfPossible } from './pushNotificationService';

const AUTH_ENDPOINTS = {
    login: '/auth/email/login',
    register: '/auth/email/register',
    verify: '/auth/verify',
    forgotPassword: '/auth/email/forgot-password',
    resendForgotPassword: '/auth/email/resend-forgot-password',
    changePassword: '/auth/email/change-password',
    emailExists: '/auth/email/exists',
    resendVerification: '/auth/email/resend-verification',
    completeProfileCustomer: '/auth/customers/complete-profile',
    completeProfileBarber: '/auth/barbers/complete-profile',
};

// ─── TRADUÇÃO DE ERROS FIREBASE → PT-BR ──────────────────────────────────────
// Converte códigos e mensagens brutas do Firebase (vindos do backend) para
// mensagens amigáveis em português.
export const translateFirebaseError = (rawMsg = '', fallback = 'Ocorreu um erro. Tente novamente.') => {
    const msg = rawMsg.toUpperCase();
    if (msg.includes('EMAIL_NOT_FOUND'))               return 'Nenhuma conta encontrada com este e-mail.';
    if (msg.includes('INVALID_PASSWORD'))              return 'Senha incorreta. Tente novamente.';
    if (msg.includes('INVALID_LOGIN_CREDENTIALS'))     return 'E-mail ou senha inválidos.';
    if (msg.includes('USER_DISABLED'))                 return 'Esta conta foi desativada. Entre em contato com o suporte.';
    if (msg.includes('TOO_MANY_ATTEMPTS_TRY_LATER') || msg.includes('TOO_MANY_REQUESTS')) return 'Muitas tentativas. Aguarde alguns minutos antes de tentar novamente.';
    if (msg.includes('WEAK_PASSWORD'))                 return 'A senha é muito fraca. Use pelo menos 6 caracteres.';
    if (msg.includes('EMAIL_EXISTS') || msg.includes('EMAIL_ALREADY_IN_USE')) return 'Este e-mail já está cadastrado.';
    if (msg.includes('INVALID_EMAIL'))                 return 'Endereço de e-mail inválido.';
    if (msg.includes('OPERATION_NOT_ALLOWED'))         return 'Operação não permitida. Contate o suporte.';
    if (msg.includes('EXPIRED_OOB_CODE'))              return 'O link expirou. Solicite um novo.';
    if (msg.includes('INVALID_OOB_CODE'))              return 'Link inválido ou já utilizado.';
    if (msg.includes('CREDENTIAL_TOO_OLD_LOGIN_AGAIN')) return 'Sessão expirada. Faça login novamente.';
    if (msg.includes('auth/wrong-password'))           return 'Senha incorreta. Tente novamente.';
    if (msg.includes('auth/user-not-found'))           return 'Nenhuma conta encontrada com este e-mail.';
    if (msg.includes('auth/invalid-credential'))       return 'Credenciais inválidas. Verifique e-mail e senha.';
    if (msg.includes('auth/email-already-in-use'))     return 'Este e-mail já está cadastrado.';
    if (msg.includes('auth/weak-password'))            return 'A senha é muito fraca. Use pelo menos 6 caracteres.';
    if (msg.includes('auth/too-many-requests'))        return 'Muitas tentativas. Aguarde alguns minutos antes de tentar novamente.';
    if (msg.includes('auth/popup-closed-by-user'))     return 'Login cancelado. Feche a janela e tente novamente.';
    if (msg.includes('auth/popup-blocked'))            return 'Pop-up bloqueado pelo navegador. Permita pop-ups e tente novamente.';
    if (msg.includes('auth/network-request-failed'))   return 'Sem conexão com a internet. Verifique sua rede.';
    if (msg.includes('E-MAIL JÁ CADASTRADO') || msg.includes('JA CADASTRADO')) return 'Este e-mail já está cadastrado.';
    if (msg.includes('VERIFICADO'))                    return 'E-mail ainda não verificado. Verifique sua caixa de entrada.';
    return fallback;
};

// ─── REFRESH DE SESSÃO ───────────────────────────────────────────────────────
// Após operações que mudam o role do usuário no backend (ex: criar barbearia),
// re-verifica o token para atualizar userRole e isOwner no localStorage.
export const refreshSession = async () => {
    const idToken = localStorage.getItem('token');
    if (!idToken) return;
    try {
        // Detecta o tipo de conta para evitar 403 "Acesse o portal correto"
        const storedRole = (localStorage.getItem('userRole') || '').toUpperCase();
        const userType = (storedRole.includes('BARBER') || storedRole.includes('OWNER'))
            ? 'BARBER'
            : 'CUSTOMER';
        const verifyResponse = await api.post(AUTH_ENDPOINTS.verify, { idToken, userType });
        const role = verifyResponse.data?.role || localStorage.getItem('userRole');
        const isOwner = verifyResponse.data?.isOwner || false;
        localStorage.setItem('userRole', role);
        localStorage.setItem('isOwner', String(isOwner));
        if (verifyResponse.data?.id)
            localStorage.setItem('internalUserId', String(verifyResponse.data.id));
        if (verifyResponse.data?.barbershopId)
            localStorage.setItem('barbershopId', String(verifyResponse.data.barbershopId));
        if (verifyResponse.data?.name)
            localStorage.setItem('userName', verifyResponse.data.name);
        if (verifyResponse.data?.authProvider)
            localStorage.setItem('authProvider', verifyResponse.data.authProvider);
    } catch {
        // Se falhar o re-verify, mantém os valores atuais silenciosamente
    }
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

    // Lê a intenção do usuário (BARBER ou CUSTOMER) para cross-validation no backend
    const userIntent = sessionStorage.getItem('user_intent')?.toUpperCase() || null;

    let verifyResponse;
    try {
        verifyResponse = await api.post(AUTH_ENDPOINTS.verify, { idToken, userType: userIntent });
    } catch (verifyErr) {
        localStorage.removeItem('token');
        localStorage.removeItem('userId');
        localStorage.removeItem('userEmail');
        // 403 = conflito de perfil (barber tentando entrar como customer, ou vice-versa)
        if (verifyErr?.response?.status === 403) {
            const serverMsg = verifyErr.response?.data?.message || '';
            const conflictError = new Error(serverMsg || 'Perfil incompatível. Acesse o portal correto.');
            conflictError.code = 'ROLE_CONFLICT';
            conflictError.serverMessage = serverMsg;
            throw conflictError;
        }
        throw verifyErr;
    }

    if (verifyResponse?.data?.verificationRequired) {
        localStorage.removeItem('token');
        localStorage.removeItem('userId');
        localStorage.removeItem('userEmail');
        const error = new Error('E-mail ainda não verificado. Verifique sua caixa de entrada e tente novamente.');
        error.code = 'VERIFICATION_REQUIRED';
        error.verificationEmail = userEmail;
        error.response = { data: { message: error.message } };
        throw error;
    }

    // Salva role e isOwner para uso no redirecionamento e em guards de rota
    const role = verifyResponse.data?.role || 'ROLE_CUSTOMER';
    const isOwner = verifyResponse.data?.isOwner || false;
    localStorage.setItem('userRole', role);
    localStorage.setItem('isOwner', String(isOwner));
    if (verifyResponse.data?.id)
        localStorage.setItem('internalUserId', String(verifyResponse.data.id));
    if (verifyResponse.data?.barbershopId)
        localStorage.setItem('barbershopId', String(verifyResponse.data.barbershopId));
    if (verifyResponse.data?.name)
        localStorage.setItem('userName', verifyResponse.data.name);
    if (verifyResponse.data?.authProvider)
        localStorage.setItem('authProvider', verifyResponse.data.authProvider);
    // Limpa a intenção após login bem-sucedido
    sessionStorage.removeItem('user_intent');

    // ── Guarda de perfil incompleto ───────────────────────────────────────────
    // Salvaguarda extra: se o e-mail ainda não foi verificado (ex: estado inconsistente
    // onde verificationRequired veio false mas emailVerified também é false),
    // bloqueia com VERIFICATION_REQUIRED em vez de permitir completar perfil.
    if (verifyResponse.data?.profileComplete === false) {
        if (verifyResponse.data?.emailVerified === false) {
            localStorage.removeItem('token');
            localStorage.removeItem('userId');
            localStorage.removeItem('userEmail');
            const verifyError = new Error('E-mail ainda não verificado. Verifique sua caixa de entrada e tente novamente.');
            verifyError.code = 'VERIFICATION_REQUIRED';
            verifyError.verificationEmail = userEmail;
            verifyError.response = { data: { message: verifyError.message } };
            throw verifyError;
        }
        const incompleteError = new Error('Cadastro incompleto. Complete seu perfil para continuar.');
        incompleteError.code = 'PROFILE_INCOMPLETE';
        incompleteError.profileData = verifyResponse.data;
        throw incompleteError;
    }

    void registerPushNotificationsIfPossible();
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
        birthDate:   userData.birthDate || null,
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
        birthDate:     barberData.birthDate || null,
        isOwner:       barberData.isOwner ?? false,
    });

    return response.data;
};

// ─── LOGOUT ───────────────────────────────────────────────────────────────────
export const logoutUser = () => {
    void unregisterPushNotificationsIfPossible();
    localStorage.clear();
};

// ─── RECUPERAR SENHA ─────────────────────────────────────────────────────────
// Usa: POST /api/auth/email/forgot-password (público, sem auth)
// Firebase envia e-mail com link de redefinição. Resposta 204 = sucesso.
export const forgotPassword = async (email) => {
    await api.post(AUTH_ENDPOINTS.forgotPassword, { email });
};

export const resendForgotPassword = async (email) => {
    await api.post(AUTH_ENDPOINTS.resendForgotPassword, { email });
};

// ─── ALTERAR SENHA ────────────────────────────────────────────────────────────
// Usa: POST /api/auth/email/change-password (público, idToken no body)
// Retorna { idToken, refreshToken } — o cliente substitui o token sem deslogar.
export const changePassword = async (idToken, newPassword) => {
    return api.post(AUTH_ENDPOINTS.changePassword, { idToken, newPassword });
};

// ─── VERIFICAR SE E-MAIL EXISTE ───────────────────────────────────────────────
// Usa: GET /api/auth/email/exists?email=...
// Retorna { exists: boolean, userType: "CUSTOMER"|"BARBER"|null }
// Usado pelo redirecionamento inteligente no login.
export const checkEmailExists = async (email) => {
    const response = await api.get(AUTH_ENDPOINTS.emailExists, { params: { email } });
    return response.data;
};

// ─── COMPLETAR PERFIL (CUSTOMER) — pós-login Google ─────────────────────────
// Usa: POST /api/auth/customers/complete-profile (exige Bearer token no header)
// O idToken do Google deve estar em localStorage.token antes de chamar.
export const completeProfileCustomer = async ({ tell, documentCPF, name, birthDate }) => {
    const cleanCPF  = documentCPF ? documentCPF.replace(/\D/g, '') : '';
    const cleanTell = tell        ? tell.replace(/\D/g, '')         : '';
    const response = await api.post(AUTH_ENDPOINTS.completeProfileCustomer, {
        tell: cleanTell,
        documentCPF: cleanCPF,
        name,
        birthDate: birthDate || null,
    });
    return response.data;
};

// ─── COMPLETAR PERFIL (BARBER) — pós-login Google ────────────────────────────
// Usa: POST /api/auth/barbers/complete-profile (exige Bearer token no header)
export const completeProfileBarber = async ({ tell, documentCPF, name, birthDate, isOwner }) => {
    const cleanCPF  = documentCPF ? documentCPF.replace(/\D/g, '') : '';
    const cleanTell = tell        ? tell.replace(/\D/g, '')         : '';
    const response = await api.post(AUTH_ENDPOINTS.completeProfileBarber, {
        tell: cleanTell,
        documentCPF: cleanCPF,
        name,
        birthDate: birthDate || null,
        isOwner: isOwner ?? false,
    });
    return response.data;
};

// ─── REENVIAR E-MAIL DE VERIFICAÇÃO ──────────────────────────────────────────
// Usa: POST /api/auth/email/resend-verification (público, sem auth)
// O backend faz sign-in silencioso para obter idToken e reenvia o link.
// Parâmetro `password` é necessário pois o Firebase exige autenticação para enviar o link.
export const resendVerificationEmail = async (email, password) => {
    await api.post(AUTH_ENDPOINTS.resendVerification, { email, password });
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

    // Lê a intenção do usuário (BARBER ou CUSTOMER) para cross-validation no backend
    const userIntent = sessionStorage.getItem('user_intent')?.toUpperCase() || null;

    try {
        const verifyResponse = await api.post(AUTH_ENDPOINTS.verify, { idToken, userType: userIntent });

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
        if (verifyResponse.data?.id)
            localStorage.setItem('internalUserId', String(verifyResponse.data.id));
        if (verifyResponse.data?.barbershopId)
            localStorage.setItem('barbershopId', String(verifyResponse.data.barbershopId));
        if (verifyResponse.data?.name)
            localStorage.setItem('userName', verifyResponse.data.name);
        if (verifyResponse.data?.authProvider)
            localStorage.setItem('authProvider', verifyResponse.data.authProvider);
        // Limpa a intenção após login bem-sucedido
        sessionStorage.removeItem('user_intent');

        // ── Guarda de perfil incompleto ───────────────────────────────────────
        // Se o cadastro não foi finalizado (sem CPF/telefone), bloqueia o acesso
        // e redireciona para completar o perfil.
        if (verifyResponse.data?.profileComplete === false) {
            const incompleteError = new Error('Cadastro incompleto. Complete seu perfil para continuar.');
            incompleteError.code = 'PROFILE_INCOMPLETE';
            incompleteError.profileData = verifyResponse.data;
            throw incompleteError;
        }

        void registerPushNotificationsIfPossible();
        return {
            idToken,
            localId: result.user.uid,
            email: result.user.email,
            displayName: result.user.displayName,
            photoURL: result.user.photoURL,
            profile: verifyResponse.data,
        };
    } catch (err) {
        // 403 = conflito de perfil (barber tentando entrar como customer, ou vice-versa)
        if (err?.response?.status === 403) {
            localStorage.removeItem('token');
            localStorage.removeItem('userId');
            localStorage.removeItem('userEmail');
            const serverMsg = err.response?.data?.message || '';
            const conflictError = new Error(serverMsg || 'Perfil incompatível. Acesse o portal correto.');
            conflictError.code = 'ROLE_CONFLICT';
            conflictError.serverMessage = serverMsg;
            throw conflictError;
        }
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