import { beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('./api', () => ({
  default: {
    post: vi.fn(),
    get: vi.fn(),
    delete: vi.fn(),
  },
}));

vi.mock('./firebase', () => ({
  auth: {},
  googleProvider: {},
}));

vi.mock('firebase/auth', () => ({
  signInWithPopup: vi.fn(),
}));

vi.mock('./pushNotificationService', () => ({
  registerPushNotificationsIfPossible: vi.fn(),
  unregisterPushNotificationsIfPossible: vi.fn(),
}));

import api from './api';
import { signInWithPopup } from 'firebase/auth';
import {
  changePassword,
  checkEmailExists,
  completeProfileBarber,
  completeProfileCustomer,
  forgotPassword,
  loginUser,
  loginWithGoogle,
  logoutUser,
  refreshSession,
  registerBarber,
  registerCustomer,
  resendForgotPassword,
  resendVerificationEmail,
  translateFirebaseError,
} from './authService';
import {
  registerPushNotificationsIfPossible,
  unregisterPushNotificationsIfPossible,
} from './pushNotificationService';

describe('authService', () => {
  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    vi.mocked(api.post).mockReset();
    vi.mocked(api.get).mockReset();
    vi.mocked(api.delete).mockReset();
    vi.mocked(signInWithPopup).mockReset();
    vi.mocked(registerPushNotificationsIfPossible).mockReset();
    vi.mocked(unregisterPushNotificationsIfPossible).mockReset();
  });

  it('translates the main Firebase errors to pt-BR', () => {
    expect(translateFirebaseError('EMAIL_NOT_FOUND')).toBe('Nenhuma conta encontrada com este e-mail.');
    expect(translateFirebaseError('TOO_MANY_REQUESTS')).toBe(
      'Muitas tentativas. Aguarde alguns minutos antes de tentar novamente.',
    );
    expect(translateFirebaseError('unknown', 'Fallback')).toBe('Fallback');
  });

  it('registers a customer with cleaned document data', async () => {
    vi.mocked(api.post).mockResolvedValueOnce({ data: { ok: true } });

    await expect(
      registerCustomer({
        email: 'cliente@teste.com',
        password: '123456',
        name: 'Cliente',
        tell: '(11) 99999-8888',
        documentCPF: '123.456.789-01',
        birthDate: '2000-01-01',
      }),
    ).resolves.toEqual({ ok: true });

    expect(api.post).toHaveBeenCalledWith('/auth/email/register', {
      email: 'cliente@teste.com',
      password: '123456',
      userType: 'CUSTOMER',
      name: 'Cliente',
      tell: '11999998888',
      documentCPF: '12345678901',
      birthDate: '2000-01-01',
    });
  });

  it('registers a barber with cleaned document data and owner flag', async () => {
    vi.mocked(api.post).mockResolvedValueOnce({ data: { ok: true } });

    await expect(
      registerBarber({
        email: 'barbeiro@teste.com',
        password: '123456',
        name: 'Barbeiro',
        tell: '(11) 98888-7777',
        documentCPF: '123.456.789-01',
        birthDate: '1995-01-01',
        isOwner: true,
      }),
    ).resolves.toEqual({ ok: true });

    expect(api.post).toHaveBeenCalledWith('/auth/email/register', {
      email: 'barbeiro@teste.com',
      password: '123456',
      userType: 'BARBER',
      name: 'Barbeiro',
      tell: '11988887777',
      documentCPF: '12345678901',
      birthDate: '1995-01-01',
      isOwner: true,
    });
  });

  it('performs the basic auth wrappers', async () => {
    vi.mocked(api.post).mockResolvedValue({ data: { ok: true } });
    vi.mocked(api.get).mockResolvedValue({ data: { exists: true } });

    await forgotPassword('email@teste.com');
    await resendForgotPassword('email@teste.com');
    await resendVerificationEmail('email@teste.com', 'senha');
    await changePassword('token', 'nova-senha');
    await checkEmailExists('email@teste.com');
    await completeProfileCustomer({
      tell: '(11) 99999-1111',
      documentCPF: '123.456.789-01',
      name: 'Cliente',
      birthDate: '2000-01-01',
    });
    await completeProfileBarber({
      tell: '(11) 99999-2222',
      documentCPF: '123.456.789-01',
      name: 'Barbeiro',
      birthDate: '1990-01-01',
      isOwner: false,
    });

    expect(api.post).toHaveBeenCalledWith('/auth/email/forgot-password', { email: 'email@teste.com' });
    expect(api.post).toHaveBeenCalledWith('/auth/email/resend-forgot-password', { email: 'email@teste.com' });
    expect(api.post).toHaveBeenCalledWith('/auth/email/resend-verification', {
      email: 'email@teste.com',
      password: 'senha',
    });
    expect(api.post).toHaveBeenCalledWith('/auth/email/change-password', {
      idToken: 'token',
      newPassword: 'nova-senha',
    });
    expect(api.get).toHaveBeenCalledWith('/auth/email/exists', { params: { email: 'email@teste.com' } });
    expect(api.post).toHaveBeenCalledWith('/auth/customers/complete-profile', {
      tell: '11999991111',
      documentCPF: '12345678901',
      name: 'Cliente',
      birthDate: '2000-01-01',
    });
    expect(api.post).toHaveBeenCalledWith('/auth/barbers/complete-profile', {
      tell: '11999992222',
      documentCPF: '12345678901',
      name: 'Barbeiro',
      birthDate: '1990-01-01',
      isOwner: false,
    });
  });

  it('refreshes the session using the stored role', async () => {
    localStorage.setItem('token', 'token-123');
    localStorage.setItem('userRole', 'ROLE_BARBER');

    vi.mocked(api.post).mockResolvedValueOnce({
      data: {
        role: 'ROLE_BARBER',
        isOwner: true,
        id: 77,
        barbershopId: 19,
        barbershopName: 'Barbearia Central',
        name: 'João',
        authProvider: 'firebase',
      },
    });

    await refreshSession();

    expect(api.post).toHaveBeenCalledWith('/auth/verify', {
      idToken: 'token-123',
      userType: 'BARBER',
    });
    expect(localStorage.getItem('userRole')).toBe('ROLE_BARBER');
    expect(localStorage.getItem('isOwner')).toBe('true');
    expect(localStorage.getItem('internalUserId')).toBe('77');
    expect(localStorage.getItem('barbershopId')).toBe('19');
    expect(localStorage.getItem('barbershopName')).toBe('Barbearia Central');
    expect(localStorage.getItem('userName')).toBe('João');
    expect(localStorage.getItem('authProvider')).toBe('firebase');
  });

  it('logs out and clears the local session', () => {
    localStorage.setItem('token', 'token');
    localStorage.setItem('userId', '1');
    vi.mocked(api.delete).mockResolvedValueOnce({});

    logoutUser();

    expect(unregisterPushNotificationsIfPossible).toHaveBeenCalledTimes(1);
    expect(api.delete).toHaveBeenCalledWith('/schedule/ai/chat/history');
    expect(localStorage.length).toBe(0);
  });

  it('logs in with email and password using the verified profile', async () => {
    sessionStorage.setItem('user_intent', 'barber');

    vi.mocked(api.post)
      .mockResolvedValueOnce({
        data: {
          idToken: 'token-abc',
          localId: 'user-1',
          email: 'barbeiro@teste.com',
        },
      })
      .mockResolvedValueOnce({
        data: {
          role: 'ROLE_BARBER',
          isOwner: true,
          profileComplete: true,
          emailVerified: true,
          id: 99,
          barbershopId: 55,
          barbershopName: 'Barbearia Prime',
          name: 'Barbeiro Prime',
          authProvider: 'password',
        },
      });

    const result = await loginUser('barbeiro@teste.com', 'senha123');

    expect(result.profile.role).toBe('ROLE_BARBER');
    expect(localStorage.getItem('token')).toBe('token-abc');
    expect(localStorage.getItem('userId')).toBe('user-1');
    expect(localStorage.getItem('userEmail')).toBe('barbeiro@teste.com');
    expect(sessionStorage.getItem('user_intent')).toBeNull();
    expect(registerPushNotificationsIfPossible).toHaveBeenCalledTimes(1);
  });

  it('blocks login when the verified profile is incomplete', async () => {
    sessionStorage.setItem('user_intent', 'customer');

    vi.mocked(api.post)
      .mockResolvedValueOnce({
        data: {
          idToken: 'token-incompleto',
          localId: 'user-2',
          email: 'cliente@teste.com',
        },
      })
      .mockResolvedValueOnce({
        data: {
          role: 'ROLE_CUSTOMER',
          isOwner: false,
          profileComplete: false,
          emailVerified: true,
        },
      });

    await expect(loginUser('cliente@teste.com', 'senha123')).rejects.toMatchObject({
      code: 'PROFILE_INCOMPLETE',
    });

    expect(localStorage.getItem('token')).toBe('token-incompleto');
    expect(sessionStorage.getItem('user_intent')).toBeNull();
  });

  it('completes the Google login flow', async () => {
    sessionStorage.setItem('user_intent', 'customer');

    vi.mocked(signInWithPopup).mockResolvedValueOnce({
      user: {
        uid: 'google-uid',
        email: 'google@teste.com',
        displayName: 'Google User',
        photoURL: 'https://example.com/photo.jpg',
        getIdToken: vi.fn().mockResolvedValue('google-token'),
      },
    });

    vi.mocked(api.post).mockResolvedValueOnce({
      data: {
        role: 'ROLE_CUSTOMER',
        isOwner: false,
        profileComplete: true,
        emailVerified: true,
        id: 101,
        authProvider: 'google',
      },
    });

    const result = await loginWithGoogle();

    expect(result.email).toBe('google@teste.com');
    expect(result.profile.role).toBe('ROLE_CUSTOMER');
    expect(localStorage.getItem('token')).toBe('google-token');
    expect(registerPushNotificationsIfPossible).toHaveBeenCalledTimes(1);
  });
});