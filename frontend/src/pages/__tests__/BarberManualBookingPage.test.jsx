import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const navigate = vi.fn();
const toastError = vi.fn();
const toastSuccess = vi.fn();
const toastWarn = vi.fn();

vi.mock('react-router-dom', () => ({
  useNavigate: () => navigate,
}));

vi.mock('react-toastify', () => ({
  toast: {
    error: (...args) => toastError(...args),
    success: (...args) => toastSuccess(...args),
    warn: (...args) => toastWarn(...args),
  },
}));

vi.mock('react-icons/fi', () => ({
  FiChevronUp: () => <span data-testid="icon-up" />,
  FiChevronDown: () => <span data-testid="icon-down" />,
}));

vi.mock('../../hooks/useAuthGuard', () => ({
  useAuthGuard: vi.fn(),
}));

vi.mock('../../services/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

vi.mock('../../services/authService', () => ({
  logoutUser: vi.fn(),
}));

vi.mock('../../services/userContext', () => ({
  isOwnerUser: vi.fn(),
}));

vi.mock('../../services/navigationService', () => ({
  navigateToBarberTab: vi.fn(),
}));

vi.mock('../../services/barbershopService', () => ({
  getMyAssignedActivities: vi.fn(),
}));

vi.mock('../../services/appointmentAvailabilityService', () => ({
  formatDateToApi: vi.fn((date) => date.toLocaleDateString('en-CA')),
  formatCompactDate: vi.fn((date) => date.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit' })),
  getRelativeDateLabel: vi.fn((date, index) => (index === 0 ? 'Hoje' : `Dia ${index}`)),
  createDateOptionsBase: vi.fn(() => [
    { key: '2030-01-05', date: new Date('2030-01-05T00:00:00'), isAvailable: false, slots: [] },
    { key: '2030-01-06', date: new Date('2030-01-06T00:00:00'), isAvailable: false, slots: [] },
  ]),
  hydrateDateOptionsWithAvailability: vi.fn(),
}));

vi.mock('../../services/offlineTransactionalService', () => ({
  isOfflineTransactionalError: vi.fn(),
  getOfflineTransactionalMessage: vi.fn(() => 'Agendamento salvo para sincronizar depois.'),
}));

vi.mock('../../components/BarberPage/BarberHeader', () => ({
  default: ({ onLogout, onTabChange }) => (
    <div>
      <button type="button" onClick={onLogout}>Header logout</button>
      <button type="button" onClick={() => onTabChange('estoque')}>Header estoque</button>
    </div>
  ),
}));

vi.mock('../../components/BarberPage/BarberNavbar', () => ({
  default: ({ onTabChange }) => (
    <button type="button" onClick={() => onTabChange('agenda')}>Navbar agenda</button>
  ),
}));

import api from '../../services/api';
import { logoutUser } from '../../services/authService';
import { useAuthGuard } from '../../hooks/useAuthGuard';
import { navigateToBarberTab } from '../../services/navigationService';
import { getMyAssignedActivities } from '../../services/barbershopService';
import { hydrateDateOptionsWithAvailability } from '../../services/appointmentAvailabilityService';
import {
  getOfflineTransactionalMessage,
  isOfflineTransactionalError,
} from '../../services/offlineTransactionalService';
import BarberManualBookingPage from '../BarberManualBookingPage';

describe('BarberManualBookingPage', () => {
  beforeEach(() => {
    navigate.mockReset();
    toastError.mockReset();
    toastSuccess.mockReset();
    toastWarn.mockReset();
    localStorage.clear();
    vi.mocked(api.get).mockReset();
    vi.mocked(api.post).mockReset();
    vi.mocked(logoutUser).mockReset();
    vi.mocked(useAuthGuard).mockReset();
    vi.mocked(navigateToBarberTab).mockReset();
    vi.mocked(getMyAssignedActivities).mockReset();
    vi.mocked(hydrateDateOptionsWithAvailability).mockReset();
    vi.mocked(isOfflineTransactionalError).mockReset();
    vi.mocked(getOfflineTransactionalMessage).mockClear();
    vi.mocked(useAuthGuard).mockReturnValue({ isAuthorized: true });
    vi.mocked(isOfflineTransactionalError).mockReturnValue(false);
    vi.mocked(api.get).mockImplementation((url) => {
      if (url === '/auth/me') {
        return Promise.resolve({
          data: {
            id: 'barber-1',
            name: 'Barbeiro Encaixe',
            isOwner: true,
            barbershopId: 'shop-1',
          },
        });
      }
      if (url === '/barbershops/shop-1/activities') {
        return Promise.resolve({
          data: [
            { id: 'svc-1', activityName: 'Corte', price: 50, durationMinutes: 30 },
            { id: 'svc-2', activityName: 'Barba', price: 30, durationMinutes: 20 },
            { id: 'svc-3', activityName: 'Servico nao atribuido', price: 10, durationMinutes: 10 },
          ],
        });
      }
      return Promise.reject(new Error(`unexpected ${url}`));
    });
    vi.mocked(getMyAssignedActivities).mockResolvedValue(['svc-1', 'svc-2']);
    vi.mocked(hydrateDateOptionsWithAvailability).mockResolvedValue([
      {
        key: '2030-01-05',
        date: new Date('2030-01-05T00:00:00'),
        label: 'Hoje',
        compact: '05/01',
        isAvailable: true,
        slots: ['09:00', '14:30'],
      },
      {
        key: '2030-01-06',
        date: new Date('2030-01-06T00:00:00'),
        label: 'Dia 1',
        compact: '06/01',
        isAvailable: false,
        slots: [],
      },
    ]);
  });

  it('loads assigned services, selects availability and submits a manual booking payload', async () => {
    vi.mocked(api.post).mockResolvedValueOnce({});

    const { container } = render(<BarberManualBookingPage />);

    expect(await screen.findByText('Corte')).toBeInTheDocument();
    expect(screen.getByText('Barba')).toBeInTheDocument();
    expect(screen.queryByText('Servico nao atribuido')).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole('checkbox', { name: /corte/i }));
    expect(await screen.findByText(/total: r\$ 50\.00/i)).toBeInTheDocument();
    expect(await screen.findByRole('button', { name: /09:00/i })).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /14:30/i }));
    fireEvent.change(screen.getByPlaceholderText(/joão silva/i), {
      target: { value: ' Cliente Balcao ' },
    });
    fireEvent.change(screen.getByPlaceholderText(/\(11\) 99999-9999/i), {
      target: { value: '11988887777' },
    });

    fireEvent.submit(container.querySelector('form'));

    await waitFor(() => expect(api.post).toHaveBeenCalledWith('/appointments/barber-booking', {
      barbershopId: 'shop-1',
      activityIds: ['svc-1'],
      startTime: '2030-01-05T14:30:00',
      clientName: 'Cliente Balcao',
      clientPhone: '11988887777',
    }));
    expect(toastSuccess).toHaveBeenCalledWith('Agendamento registrado com sucesso!');
    expect(screen.getByPlaceholderText(/joão silva/i)).toHaveValue('');
  });

  it('validates required fields, phone length and unlinked barber', async () => {
    const { container } = render(<BarberManualBookingPage />);

    await screen.findByText('Corte');
    fireEvent.submit(container.querySelector('form'));
    expect(toastWarn).toHaveBeenCalledWith('Informe o nome do cliente.');

    fireEvent.change(screen.getByPlaceholderText(/joão silva/i), {
      target: { value: 'Cliente' },
    });
    fireEvent.submit(container.querySelector('form'));
    expect(toastWarn).toHaveBeenCalledWith('Selecione ao menos um serviço.');

    fireEvent.click(screen.getByRole('checkbox', { name: /corte/i }));
    await screen.findByRole('button', { name: /09:00/i });
    fireEvent.click(screen.getByRole('button', { name: /09:00/i }));
    fireEvent.submit(container.querySelector('form'));
    expect(toastWarn).toHaveBeenCalledWith('Selecione a data e horário do atendimento.');

    fireEvent.click(screen.getByRole('button', { name: /09:00/i }));
    fireEvent.change(screen.getByPlaceholderText(/\(11\) 99999-9999/i), {
      target: { value: '11999' },
    });
    fireEvent.submit(container.querySelector('form'));
    expect(toastWarn).toHaveBeenCalledWith('Telefone deve conter exatamente 11 dígitos.');
  });

  it('shows offline notice on availability or submit failures and handles navigation/logout', async () => {
    vi.mocked(hydrateDateOptionsWithAvailability).mockRejectedValueOnce(new Error('offline'));
    vi.mocked(isOfflineTransactionalError).mockReturnValue(true);

    const { unmount } = render(<BarberManualBookingPage />);

    expect(await screen.findByText('Corte')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('checkbox', { name: /corte/i }));
    await waitFor(() => expect(screen.getByText('Agendamento salvo para sincronizar depois.')).toBeInTheDocument());

    unmount();
    vi.mocked(hydrateDateOptionsWithAvailability).mockResolvedValueOnce([
      {
        key: '2030-01-05',
        date: new Date('2030-01-05T00:00:00'),
        label: 'Hoje',
        compact: '05/01',
        isAvailable: true,
        slots: ['09:00'],
      },
    ]);
    vi.mocked(api.post).mockRejectedValueOnce({ response: { status: 409, data: { message: 'Horario ocupado' } } });
    vi.mocked(isOfflineTransactionalError).mockReturnValue(false);

    render(<BarberManualBookingPage />);
    fireEvent.click(await screen.findByRole('checkbox', { name: /corte/i }));
    await screen.findByRole('button', { name: /09:00/i });
    fireEvent.change(screen.getByPlaceholderText(/joão silva/i), { target: { value: 'Cliente' } });
    fireEvent.submit(document.querySelector('form'));

    await waitFor(() => expect(toastWarn).toHaveBeenCalledWith('Horario ocupado'));

    fireEvent.click(screen.getByText('Header estoque'));
    expect(navigateToBarberTab).toHaveBeenCalledWith('estoque', navigate, {
      isOwner: true,
      currentPath: '/barberHome/novo-agendamento',
    });

    fireEvent.click(screen.getByText('Header logout'));
    await waitFor(() => expect(logoutUser).toHaveBeenCalled());
    expect(navigate).toHaveBeenCalledWith('/');
  });
});
