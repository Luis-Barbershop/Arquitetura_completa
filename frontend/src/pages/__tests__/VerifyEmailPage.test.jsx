import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const navigate = vi.fn();
let locationState = {};
let searchParams = new URLSearchParams();

vi.mock('react-router-dom', () => ({
  Link: ({ children, to, ...props }) => <a href={to} {...props}>{children}</a>,
  useLocation: () => ({ state: locationState }),
  useNavigate: () => navigate,
  useSearchParams: () => [searchParams],
}));

vi.mock('firebase/auth', () => ({
  applyActionCode: vi.fn(),
}));

vi.mock('../../services/firebase', () => ({
  auth: {},
}));

vi.mock('../../services/authService', () => ({
  resendVerificationEmail: vi.fn(),
}));

import { applyActionCode } from 'firebase/auth';
import { resendVerificationEmail } from '../../services/authService';
import VerifyEmailPage from '../VerifyEmailPage';

describe('VerifyEmailPage', () => {
  beforeEach(() => {
    navigate.mockReset();
    locationState = {};
    searchParams = new URLSearchParams();
    vi.mocked(applyActionCode).mockReset();
    vi.mocked(resendVerificationEmail).mockReset();
  });

  it('renders waiting mode, requests password and resends verification link', async () => {
    locationState = { mode: 'waiting', email: 'cliente@teste.com', role: 'customer' };
    vi.mocked(resendVerificationEmail).mockResolvedValueOnce();

    render(<VerifyEmailPage />);

    expect(screen.getByText(/verifique seu e-mail/i)).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /reenviar link/i }));
    expect(screen.getByPlaceholderText(/sua senha/i)).toBeInTheDocument();

    fireEvent.change(screen.getByPlaceholderText(/sua senha/i), {
      target: { value: 'senha123' },
    });
    fireEvent.click(screen.getByRole('button', { name: /^reenviar$/i }));

    await waitFor(() => expect(resendVerificationEmail).toHaveBeenCalledWith('cliente@teste.com', 'senha123'));
    expect(screen.getByText(/link reenviado/i)).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /já verifiquei/i }));
    expect(navigate).toHaveBeenCalledWith('/login', { state: { role: 'customer' } });
  });

  it('allows editing email and shows resend errors', async () => {
    locationState = { mode: 'waiting', email: 'antigo@teste.com', password: 'senha', role: 'barber' };
    vi.mocked(resendVerificationEmail).mockRejectedValueOnce({
      response: { data: { message: 'Falha no reenvio' } },
    });

    render(<VerifyEmailPage />);

    fireEvent.click(screen.getByRole('button', { name: /alterar e-mail/i }));
    fireEvent.change(screen.getByPlaceholderText(/novo e-mail/i), {
      target: { value: 'novo@teste.com' },
    });
    fireEvent.click(screen.getByRole('button', { name: /enviar para este e-mail/i }));

    expect(await screen.findByText('Falha no reenvio')).toBeInTheDocument();
  });

  it('applies Firebase action code and navigates after successful verification', async () => {
    searchParams = new URLSearchParams('mode=verifyEmail&oobCode=abc');
    vi.mocked(applyActionCode).mockResolvedValue();

    render(<VerifyEmailPage />);

    await waitFor(() => expect(applyActionCode).toHaveBeenCalledWith({}, 'abc'));
    expect(screen.getByText(/e-mail verificado/i)).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /fazer login agora/i }));
    expect(navigate).toHaveBeenCalledWith('/login', { state: { role: 'customer' } });
  });

  it('shows invalid link state when verification fails', async () => {
    searchParams = new URLSearchParams('mode=verifyEmail&oobCode=abc');
    vi.mocked(applyActionCode).mockRejectedValue(new Error('invalid'));

    render(<VerifyEmailPage />);

    expect(await screen.findByText(/ops/i)).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /voltar para o login/i }));
    expect(navigate).toHaveBeenCalledWith('/identificacao', { state: { mode: 'login' } });
  });
});
