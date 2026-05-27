import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const navigate = vi.fn();
const toastError = vi.fn();
const toastSuccess = vi.fn();

vi.mock('react-router-dom', () => ({
  useNavigate: () => navigate,
}));

vi.mock('react-toastify', () => ({
  toast: {
    error: (...args) => toastError(...args),
    success: (...args) => toastSuccess(...args),
  },
}));

vi.mock('../../components/HomePage/CustomerHeader', () => ({
  default: ({ onLogout }) => <button onClick={onLogout}>Header logout</button>,
}));

vi.mock('../../components/HomePage/CustomerNavbar', () => ({
  default: ({ onLogout }) => <button onClick={onLogout}>Navbar logout</button>,
}));

vi.mock('../../components/PushNotificationToggle/PushNotificationToggle', () => ({
  default: () => <div>Push toggle</div>,
}));

vi.mock('../../services/authService', () => ({
  logoutUser: vi.fn(),
}));

vi.mock('../../services/userContext', () => ({
  isBarber: vi.fn(),
}));

vi.mock('../../services/userProfileService', () => ({
  getMyProfile: vi.fn(),
  updateCustomerProfile: vi.fn(),
  uploadCustomerProfilePhoto: vi.fn(),
}));

import { logoutUser } from '../../services/authService';
import { isBarber } from '../../services/userContext';
import {
  getMyProfile,
  updateCustomerProfile,
  uploadCustomerProfilePhoto,
} from '../../services/userProfileService';
import CustomerProfilePage from '../CustomerProfilePage';

describe('CustomerProfilePage', () => {
  beforeEach(() => {
    navigate.mockReset();
    toastError.mockReset();
    toastSuccess.mockReset();
    localStorage.clear();
    vi.mocked(logoutUser).mockReset();
    vi.mocked(isBarber).mockReset();
    vi.mocked(getMyProfile).mockReset();
    vi.mocked(updateCustomerProfile).mockReset();
    vi.mocked(uploadCustomerProfilePhoto).mockReset();
    vi.mocked(isBarber).mockReturnValue(false);
  });

  it('loads profile, updates fields and uploads photo', async () => {
    vi.mocked(getMyProfile).mockResolvedValueOnce({
      name: 'Cliente Teste',
      tell: '11999998888',
      email: 'cliente@teste.com',
    });
    vi.mocked(updateCustomerProfile).mockResolvedValueOnce({});
    vi.mocked(uploadCustomerProfilePhoto).mockResolvedValueOnce({ imageUrl: 'foto-nova.png' });

    const { container } = render(<CustomerProfilePage />);

    expect(screen.getByText(/carregando perfil/i)).toBeInTheDocument();
    expect(await screen.findByDisplayValue('Cliente Teste')).toBeInTheDocument();
    expect(screen.getByDisplayValue('(11) 99999-8888')).toBeInTheDocument();

    fireEvent.change(screen.getByDisplayValue('Cliente Teste'), {
      target: { name: 'name', value: ' Cliente Novo ' },
    });
    fireEvent.change(screen.getByDisplayValue('(11) 99999-8888'), {
      target: { name: 'tell', value: '(11) 98888-7777' },
    });
    fireEvent.click(screen.getByRole('button', { name: /salvar alterações/i }));

    await waitFor(() => expect(updateCustomerProfile).toHaveBeenCalledWith({
      name: 'Cliente Novo',
      tell: '11988887777',
    }));
    expect(toastSuccess).toHaveBeenCalledWith('Perfil atualizado com sucesso!');

    const fileInput = container.querySelector('input[type="file"]');
    fireEvent.change(fileInput, {
      target: { files: [new File(['avatar'], 'avatar.png', { type: 'image/png' })] },
    });

    await waitFor(() => expect(uploadCustomerProfilePhoto).toHaveBeenCalled());
    expect(localStorage.getItem('userProfileImage')).toBe('foto-nova.png');
  });

  it('redirects barber users and handles logout', async () => {
    vi.mocked(isBarber).mockReturnValueOnce(true);

    render(<CustomerProfilePage />);

    expect(navigate).toHaveBeenCalledWith('/barberHome', { replace: true });
    expect(getMyProfile).not.toHaveBeenCalled();
  });

  it('shows errors when profile load or save fails', async () => {
    vi.mocked(getMyProfile).mockResolvedValueOnce({ name: 'Cliente', email: 'cliente@teste.com' });
    vi.mocked(updateCustomerProfile).mockRejectedValueOnce({
      response: { data: { message: 'Nome invalido' } },
    });

    render(<CustomerProfilePage />);

    fireEvent.click(await screen.findByRole('button', { name: /salvar alterações/i }));

    await waitFor(() => expect(toastError).toHaveBeenCalledWith('Nome invalido'));
  });

  it('logs out from profile header', async () => {
    vi.mocked(getMyProfile).mockResolvedValueOnce({ name: 'Cliente' });

    render(<CustomerProfilePage />);

    fireEvent.click(await screen.findByRole('button', { name: /header logout/i }));
    expect(logoutUser).toHaveBeenCalled();
    expect(navigate).toHaveBeenCalledWith('/');
  });
});
