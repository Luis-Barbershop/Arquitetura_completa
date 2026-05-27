import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const navigate = vi.fn();
let search = '';

vi.mock('react-router-dom', () => ({
  Link: ({ children, to, ...props }) => <a href={to} {...props}>{children}</a>,
  useLocation: () => ({ search }),
  useNavigate: () => navigate,
}));

vi.mock('firebase/auth', () => ({
  confirmPasswordReset: vi.fn(),
  onAuthStateChanged: vi.fn(),
  signInWithEmailAndPassword: vi.fn(),
  verifyPasswordResetCode: vi.fn(),
}));

vi.mock('../../services/firebase', () => ({
  auth: {},
}));

vi.mock('../../services/authService', () => ({
  changePassword: vi.fn(),
}));

import {
  confirmPasswordReset,
  onAuthStateChanged,
  signInWithEmailAndPassword,
  verifyPasswordResetCode,
} from 'firebase/auth';
import { changePassword } from '../../services/authService';
import ChangePasswordPage from '../ChangePasswordPage';

describe('ChangePasswordPage', () => {
  beforeEach(() => {
    navigate.mockReset();
    search = '';
    localStorage.clear();
    vi.mocked(changePassword).mockReset();
    vi.mocked(confirmPasswordReset).mockReset();
    vi.mocked(onAuthStateChanged).mockReset();
    vi.mocked(signInWithEmailAndPassword).mockReset();
    vi.mocked(verifyPasswordResetCode).mockReset();
  });

  it('blocks social login users from changing password', async () => {
    localStorage.setItem('authProvider', 'GOOGLE');

    render(<ChangePasswordPage />);

    expect(await screen.findByText(/conta vinculada ao google/i)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /abrir segurança/i })).toHaveAttribute(
      'href',
      'https://myaccount.google.com/security',
    );
  });

  it('validates password strength and mismatch before submitting', async () => {
    localStorage.setItem('token', 'token-123');
    localStorage.setItem('authProvider', 'EMAIL');

    render(<ChangePasswordPage />);

    fireEvent.change(await screen.findByPlaceholderText(/mín. 8 caracteres/i), {
      target: { value: 'fraca' },
    });
    fireEvent.change(screen.getByPlaceholderText(/repita a nova senha/i), {
      target: { value: 'fraca' },
    });
    fireEvent.click(screen.getByRole('button', { name: /alterar senha/i }));

    expect(screen.getByText(/senha não é forte/i)).toBeInTheDocument();

    fireEvent.change(screen.getByPlaceholderText(/mín. 8 caracteres/i), {
      target: { value: 'Senha@123' },
    });
    fireEvent.change(screen.getByPlaceholderText(/repita a nova senha/i), {
      target: { value: 'Outra@123' },
    });
    fireEvent.click(screen.getByRole('button', { name: /alterar senha/i }));

    expect(screen.getAllByText(/senhas não coincidem/i)).toHaveLength(2);
    expect(changePassword).not.toHaveBeenCalled();
  });

  it('changes password for authenticated email users and refreshes token', async () => {
    localStorage.setItem('token', 'token-123');
    localStorage.setItem('userEmail', 'cliente@teste.com');
    localStorage.setItem('authProvider', 'EMAIL');
    vi.mocked(changePassword).mockResolvedValueOnce({ data: { idToken: 'backend-token' } });
    vi.mocked(signInWithEmailAndPassword).mockResolvedValueOnce({
      user: { getIdToken: vi.fn().mockResolvedValue('fresh-token') },
    });

    render(<ChangePasswordPage />);

    fireEvent.change(await screen.findByPlaceholderText(/mín. 8 caracteres/i), {
      target: { value: 'Senha@123' },
    });
    fireEvent.change(screen.getByPlaceholderText(/repita a nova senha/i), {
      target: { value: 'Senha@123' },
    });
    fireEvent.click(screen.getByRole('button', { name: /alterar senha/i }));

    await waitFor(() => expect(changePassword).toHaveBeenCalledWith('token-123', 'Senha@123'));
    expect(signInWithEmailAndPassword).toHaveBeenCalledWith({}, 'cliente@teste.com', 'Senha@123');
    expect(localStorage.getItem('token')).toBe('fresh-token');
    expect(screen.getByText(/senha alterada/i)).toBeInTheDocument();
  });

  it('uses Firebase reset flow when reset code is present', async () => {
    search = '?mode=resetPassword&oobCode=abc';
    vi.mocked(verifyPasswordResetCode).mockResolvedValueOnce('cliente@teste.com');
    vi.mocked(confirmPasswordReset).mockResolvedValueOnce();

    render(<ChangePasswordPage />);

    expect(await screen.findByText(/cliente@teste.com/i)).toBeInTheDocument();
    fireEvent.change(screen.getByPlaceholderText(/mín. 8 caracteres/i), {
      target: { value: 'Senha@123' },
    });
    fireEvent.change(screen.getByPlaceholderText(/repita a nova senha/i), {
      target: { value: 'Senha@123' },
    });
    fireEvent.click(screen.getByRole('button', { name: /alterar senha/i }));

    await waitFor(() => expect(confirmPasswordReset).toHaveBeenCalledWith({}, 'abc', 'Senha@123'));
    expect(screen.getByText(/senha alterada/i)).toBeInTheDocument();
  });
});
