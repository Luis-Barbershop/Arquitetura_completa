import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('react-router-dom', () => ({
  Link: ({ children, to, ...props }) => <a href={to} {...props}>{children}</a>,
}));

vi.mock('../../services/authService', () => ({
  forgotPassword: vi.fn(),
  resendForgotPassword: vi.fn(),
}));

import { forgotPassword, resendForgotPassword } from '../../services/authService';
import ForgotPasswordPage from '../ForgotPasswordPage';

describe('ForgotPasswordPage', () => {
  beforeEach(() => {
    vi.mocked(forgotPassword).mockReset();
    vi.mocked(resendForgotPassword).mockReset();
  });

  it('sends password recovery email and shows success state', async () => {
    vi.mocked(forgotPassword).mockResolvedValueOnce();

    render(<ForgotPasswordPage />);

    fireEvent.change(screen.getByPlaceholderText(/seu@email.com/i), {
      target: { value: 'cliente@teste.com' },
    });
    fireEvent.click(screen.getByRole('button', { name: /enviar link/i }));

    await waitFor(() => expect(forgotPassword).toHaveBeenCalledWith('cliente@teste.com'));
    expect(screen.getByText(/e-mail enviado/i)).toBeInTheDocument();
    expect(screen.getByText(/cliente@teste.com/i)).toBeInTheDocument();
  });

  it('shows backend error when recovery request fails', async () => {
    vi.mocked(forgotPassword).mockRejectedValueOnce({
      response: { data: { message: 'E-mail nao encontrado' } },
    });

    render(<ForgotPasswordPage />);

    fireEvent.change(screen.getByPlaceholderText(/seu@email.com/i), {
      target: { value: 'missing@teste.com' },
    });
    fireEvent.click(screen.getByRole('button', { name: /enviar link/i }));

    expect(await screen.findByText('E-mail nao encontrado')).toBeInTheDocument();
  });

  it('does not resend while cooldown is active', async () => {
    vi.mocked(forgotPassword).mockResolvedValueOnce();

    render(<ForgotPasswordPage />);

    fireEvent.change(screen.getByPlaceholderText(/seu@email.com/i), {
      target: { value: 'cliente@teste.com' },
    });
    fireEvent.click(screen.getByRole('button', { name: /enviar link/i }));

    const resendButton = await screen.findByRole('button', { name: /reenviar em/i });
    fireEvent.click(resendButton);

    expect(resendForgotPassword).not.toHaveBeenCalled();
  });
});
