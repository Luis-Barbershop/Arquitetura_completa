import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const navigate = vi.fn();
const toastError = vi.fn();
const toastInfo = vi.fn();
const toastSuccess = vi.fn();
const toastWarn = vi.fn();

vi.mock('react-router-dom', () => ({
  useNavigate: () => navigate,
}));

vi.mock('react-toastify', () => ({
  toast: {
    error: (...args) => toastError(...args),
    info: (...args) => toastInfo(...args),
    success: (...args) => toastSuccess(...args),
    warn: (...args) => toastWarn(...args),
  },
}));

vi.mock('../../services/api', () => ({
  default: {
    get: vi.fn(),
    put: vi.fn(),
  },
}));

vi.mock('../../services/authService', () => ({
  logoutUser: vi.fn(),
}));

vi.mock('../../services/userContext', () => ({
  isCustomer: vi.fn(),
}));

vi.mock('../../services/barbershopService', () => ({
  getMyInvites: vi.fn(),
  acceptInvite: vi.fn(),
  rejectInvite: vi.fn(),
  leaveShop: vi.fn(),
  getMyWorkSchedule: vi.fn(),
  saveMyWorkSchedule: vi.fn(),
}));

vi.mock('../../services/userProfileService', () => ({
  uploadBarberProfilePhoto: vi.fn(),
}));

vi.mock('../../components/BarberPage/BarberHeader', () => ({
  default: ({ onLogout, onTabChange }) => (
    <div>
      <button type="button" onClick={onLogout}>Header logout</button>
      <button type="button" onClick={() => onTabChange('time')}>Header time</button>
      <button type="button" onClick={() => onTabChange('gerenciar-barbearia')}>Header gerenciar</button>
    </div>
  ),
}));

vi.mock('../../components/BarberPage/BarberNavbar', () => ({
  default: ({ onTabChange }) => (
    <button type="button" onClick={() => onTabChange('agenda-equipe')}>Navbar agenda equipe</button>
  ),
}));

vi.mock('../../components/PushNotificationToggle/PushNotificationToggle', () => ({
  default: () => <div>Push notification toggle</div>,
}));

vi.mock('../../components/CropImageModal/CropImageModal', () => ({
  default: ({ title, onConfirm, onCancel }) => (
    <div role="dialog" aria-label={title}>
      <button type="button" onClick={() => onConfirm(new Blob(['crop'], { type: 'image/jpeg' }))}>
        Confirmar crop
      </button>
      <button type="button" onClick={onCancel}>Cancelar crop</button>
    </div>
  ),
}));

import api from '../../services/api';
import { logoutUser } from '../../services/authService';
import {
  getMyInvites,
  getMyWorkSchedule,
  leaveShop,
  rejectInvite,
  saveMyWorkSchedule,
} from '../../services/barbershopService';
import { isCustomer } from '../../services/userContext';
import { uploadBarberProfilePhoto } from '../../services/userProfileService';
import BarberProfilePage from '../BarberProfilePage';

describe('BarberProfilePage', () => {
  beforeEach(() => {
    navigate.mockReset();
    toastError.mockReset();
    toastInfo.mockReset();
    toastSuccess.mockReset();
    toastWarn.mockReset();
    localStorage.clear();
    localStorage.setItem('token', 'token');
    vi.mocked(api.get).mockReset();
    vi.mocked(api.put).mockReset();
    vi.mocked(logoutUser).mockReset();
    vi.mocked(isCustomer).mockReset();
    vi.mocked(getMyInvites).mockReset();
    vi.mocked(getMyWorkSchedule).mockReset();
    vi.mocked(leaveShop).mockReset();
    vi.mocked(rejectInvite).mockReset();
    vi.mocked(saveMyWorkSchedule).mockReset();
    vi.mocked(uploadBarberProfilePhoto).mockReset();
    vi.mocked(isCustomer).mockReturnValue(false);
    vi.mocked(getMyInvites).mockResolvedValue([]);
    vi.mocked(getMyWorkSchedule).mockResolvedValue([
      {
        dayOfWeek: 'MONDAY',
        blocks: [{ startTime: '09:00:00', endTime: '18:00:00' }],
      },
    ]);
    vi.stubGlobal('URL', {
      createObjectURL: vi.fn(() => 'blob:profile'),
      revokeObjectURL: vi.fn(),
    });
  });

  it('loads owner data, saves schedule, toggles owner availability and uploads photo', async () => {
    vi.mocked(api.get).mockResolvedValueOnce({
      data: {
        id: 'barber-1',
        name: 'Dono Teste',
        email: 'dono@teste.com',
        tell: '11999998888',
        documentCPF: '12345678901',
        barbershopId: 'shop-1',
        barbershopName: 'Barbearia Central',
        isOwner: true,
        actAsBarber: true,
      },
    });
    vi.mocked(api.put).mockResolvedValueOnce({});
    vi.mocked(saveMyWorkSchedule).mockResolvedValueOnce({});
    vi.mocked(uploadBarberProfilePhoto).mockResolvedValueOnce({ imageUrl: 'foto-nova.png' });

    const { container } = render(<BarberProfilePage />);

    expect(await screen.findByText(/dono teste/i)).toBeInTheDocument();
    expect(screen.getByText(/dono@teste.com/i)).toBeInTheDocument();
    expect(screen.getByText(/\(11\) 99999-8888/)).toBeInTheDocument();
    expect(screen.getByText(/123\.456\.789-01/)).toBeInTheDocument();
    expect(await screen.findByText('09:00–18:00')).toBeInTheDocument();
    expect(screen.getByText('Push notification toggle')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /salvar horário/i }));
    await waitFor(() => expect(saveMyWorkSchedule).toHaveBeenCalledWith({
      schedule: [{
        dayOfWeek: 'MONDAY',
        blocks: [{ startTime: '09:00', endTime: '18:00' }],
      }],
    }));
    expect(toastSuccess).toHaveBeenCalledWith('Horário de trabalho salvo com sucesso!');

    fireEvent.click(screen.getByLabelText(/atuar como barbeiro/i));
    await waitFor(() => expect(api.put).toHaveBeenCalledWith('/barbers/barber-1', { actAsBarber: false }));
    expect(toastSuccess).toHaveBeenCalledWith('Você não aparecerá como barbeiro para novos agendamentos.');

    fireEvent.change(container.querySelector('input[type="file"]'), {
      target: { files: [new File(['avatar'], 'avatar.png', { type: 'image/png' })] },
    });
    fireEvent.click(screen.getByRole('button', { name: /confirmar crop/i }));

    await waitFor(() => expect(uploadBarberProfilePhoto).toHaveBeenCalled());
    expect(localStorage.getItem('userProfileImage')).toBe('foto-nova.png');
    expect(await screen.findByAltText(/foto de perfil/i)).toHaveAttribute('src', 'foto-nova.png');

    fireEvent.click(screen.getByText('Header time'));
    expect(navigate).toHaveBeenCalledWith('/barberHome/time');
    fireEvent.click(screen.getByText('Navbar agenda equipe'));
    expect(navigate).toHaveBeenCalledWith('/meus-agendamentos?view=team');

    fireEvent.click(screen.getByText('Header gerenciar'));
    expect(navigate).toHaveBeenCalledWith('/barberHome/gerenciar-barbearia');
  });

  it('shows pending invites for barbers without shop and rejects an invite', async () => {
    vi.mocked(api.get).mockResolvedValueOnce({
      data: {
        id: 'barber-2',
        name: 'Barbeiro Livre',
        email: 'livre@teste.com',
        isOwner: false,
        barbershopId: null,
      },
    });
    vi.mocked(getMyInvites).mockResolvedValueOnce([
      { requestId: 'invite-1', barbershopName: 'Barbearia Convite' },
    ]);
    vi.mocked(rejectInvite).mockResolvedValueOnce({});

    render(<BarberProfilePage />);

    expect(await screen.findByText('Barbearia Convite')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /recusar/i }));

    await waitFor(() => expect(rejectInvite).toHaveBeenCalledWith('invite-1'));
    expect(toastInfo).toHaveBeenCalledWith('Convite recusado.');
    expect(screen.getByText(/nenhum convite pendente/i)).toBeInTheDocument();
  });

  it('lets a collaborator leave the linked shop and handles auth redirects', async () => {
    vi.stubGlobal('confirm', vi.fn(() => true));
    vi.mocked(api.get)
      .mockResolvedValueOnce({
        data: {
          id: 'barber-3',
          name: 'Colaborador',
          email: 'colab@teste.com',
          isOwner: false,
          barbershopId: 'shop-1',
          barbershopName: 'Barbearia Vinculada',
        },
      })
      .mockResolvedValueOnce({
        data: {
          id: 'barber-3',
          name: 'Colaborador',
          email: 'colab@teste.com',
          isOwner: false,
          barbershopId: null,
        },
      });
    vi.mocked(leaveShop).mockResolvedValueOnce({});
    vi.mocked(getMyInvites).mockResolvedValueOnce([]);

    render(<BarberProfilePage />);

    fireEvent.click(await screen.findByRole('button', { name: /sair da barbearia/i }));

    await waitFor(() => expect(leaveShop).toHaveBeenCalled());
    expect(localStorage.getItem('barbershopId')).toBeNull();
    expect(toastSuccess).toHaveBeenCalledWith('Você saiu da barbearia com sucesso.');

    fireEvent.click(screen.getByText('Header logout'));
    await waitFor(() => expect(logoutUser).toHaveBeenCalled());
    expect(navigate).toHaveBeenCalledWith('/');
  });

  it('redirects customers before loading the barber profile', () => {
    vi.mocked(isCustomer).mockReturnValueOnce(true);

    render(<BarberProfilePage />);

    expect(navigate).toHaveBeenCalledWith('/homepage', { replace: true });
    expect(api.get).not.toHaveBeenCalled();
  });
});
