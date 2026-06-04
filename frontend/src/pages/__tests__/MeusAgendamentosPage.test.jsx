import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const navigate = vi.fn();
let locationSearch = '';
const toastError = vi.fn();
const toastSuccess = vi.fn();
const toastWarn = vi.fn();

vi.mock('react-router-dom', () => ({
  useNavigate: () => navigate,
  useLocation: () => ({ search: locationSearch }),
}));

vi.mock('react-toastify', () => ({
  toast: {
    error: (...args) => toastError(...args),
    success: (...args) => toastSuccess(...args),
    warn: (...args) => toastWarn(...args),
  },
}));

vi.mock('react-icons/fi', () => ({
  FiBarChart2: () => <span data-testid="icon-chart" />,
  FiCalendar: () => <span data-testid="icon-calendar" />,
  FiCheckCircle: () => <span data-testid="icon-check" />,
  FiChevronLeft: () => <span data-testid="icon-left" />,
  FiChevronRight: () => <span data-testid="icon-right" />,
  FiClock: () => <span data-testid="icon-clock" />,
  FiList: () => <span data-testid="icon-list" />,
  FiRefreshCw: () => <span data-testid="icon-refresh" />,
  FiScissors: () => <span data-testid="icon-scissors" />,
  FiStar: () => <span data-testid="icon-star" />,
  FiUsers: () => <span data-testid="icon-users" />,
  FiXCircle: () => <span data-testid="icon-x" />,
}));

vi.mock('../../components/BarberPage/BarberHeader', () => ({
  default: ({ onLogout, onTabChange }) => (
    <div>
      <button type="button" onClick={onLogout}>Barber logout</button>
      <button type="button" onClick={() => onTabChange('servicos')}>Barber servicos</button>
      <button type="button" onClick={() => onTabChange('gerenciar-barbearia')}>Barber gerenciar</button>
    </div>
  ),
}));

vi.mock('../../components/BarberPage/BarberNavbar', () => ({
  default: ({ onTabChange }) => (
    <button type="button" onClick={() => onTabChange('novo-agendamento')}>Novo agendamento nav</button>
  ),
}));

vi.mock('../../components/HomePage/CustomerHeader', () => ({
  default: ({ onLogout }) => <button type="button" onClick={onLogout}>Customer logout</button>,
}));

vi.mock('../../components/HomePage/CustomerNavbar', () => ({
  default: () => <div>Customer navbar</div>,
}));

vi.mock('../../components/RescheduleModal/RescheduleModal', () => ({
  default: ({ appointment, onClose, onConfirm, isSubmitting }) => (
    <div role="dialog" aria-label="Remarcar agendamento">
      <span>{appointment.id}</span>
      <button type="button" onClick={() => onConfirm('2030-01-05T15:00:00', 'barber-2')} disabled={isSubmitting}>
        Confirmar remarcacao
      </button>
      <button type="button" onClick={onClose}>Fechar remarcacao</button>
    </div>
  ),
}));

vi.mock('../../services/authService', () => ({
  logoutUser: vi.fn(),
}));

vi.mock('../../services/userContext', () => ({
  isCustomer: vi.fn(),
  isOwnerUser: vi.fn(),
  isLoggedIn: vi.fn(),
  getBarbershopId: vi.fn(),
}));

vi.mock('../../services/offlineTransactionalService', () => ({
  isOfflineTransactionalError: vi.fn(() => false),
  getOfflineTransactionalMessage: vi.fn(() => 'Operacao offline pendente.'),
}));

vi.mock('../../services/appointmentService', () => ({
  getMyAppointments: vi.fn(),
  cancelAppointment: vi.fn(),
  concludeAppointment: vi.fn(),
  rescheduleAppointment: vi.fn(),
  getBarbershopSchedule: vi.fn(),
}));

vi.mock('../../services/barbershopService', () => ({
  createBarbershopReview: vi.fn(),
  hasReviewedBarbershop: vi.fn(),
}));

import { logoutUser } from '../../services/authService';
import {
  cancelAppointment,
  concludeAppointment,
  getBarbershopSchedule,
  getMyAppointments,
  rescheduleAppointment,
} from '../../services/appointmentService';
import {
  createBarbershopReview,
  hasReviewedBarbershop,
} from '../../services/barbershopService';
import {
  getBarbershopId,
  isCustomer,
  isLoggedIn,
  isOwnerUser,
} from '../../services/userContext';
import MeusAgendamentosPage from '../MeusAgendamentosPage';

const customerAppointments = [
  {
    id: 'appt-1',
    status: 'SCHEDULED',
    startTime: '2030-01-05T10:00:00',
    endTime: '2030-01-05T10:30:00',
    barberId: 'barber-1',
    barberName: 'Joao',
    barbershopId: 'shop-1',
    barbershopName: 'Barbearia Central',
    activityNames: ['Corte'],
  },
  {
    id: 'appt-2',
    status: 'COMPLETED',
    startTime: '2030-01-04T10:00:00',
    endTime: '2030-01-04T10:45:00',
    barberName: 'Carlos',
    barbershopId: 'shop-1',
    barbershopName: 'Barbearia Central',
    activityNames: ['Barba'],
  },
];

describe('MeusAgendamentosPage', () => {
  beforeEach(() => {
    navigate.mockReset();
    toastError.mockReset();
    toastSuccess.mockReset();
    toastWarn.mockReset();
    locationSearch = '';
    localStorage.clear();
    localStorage.setItem('userName', 'Usuario Teste');
    vi.mocked(logoutUser).mockReset();
    vi.mocked(getMyAppointments).mockReset();
    vi.mocked(cancelAppointment).mockReset();
    vi.mocked(concludeAppointment).mockReset();
    vi.mocked(rescheduleAppointment).mockReset();
    vi.mocked(getBarbershopSchedule).mockReset();
    vi.mocked(createBarbershopReview).mockReset();
    vi.mocked(hasReviewedBarbershop).mockReset();
    vi.mocked(isCustomer).mockReset();
    vi.mocked(isOwnerUser).mockReset();
    vi.mocked(isLoggedIn).mockReset();
    vi.mocked(getBarbershopId).mockReset();
    vi.mocked(isLoggedIn).mockReturnValue(true);
    vi.mocked(isCustomer).mockReturnValue(true);
    vi.mocked(isOwnerUser).mockReturnValue(false);
    vi.mocked(getBarbershopId).mockReturnValue('shop-1');
  });

  it('loads customer appointments and handles reschedule, cancel, conclude and review flows', async () => {
    vi.mocked(getMyAppointments).mockResolvedValue(customerAppointments);
    vi.mocked(hasReviewedBarbershop).mockResolvedValue(false);
    vi.mocked(cancelAppointment).mockResolvedValue({});
    vi.mocked(concludeAppointment).mockResolvedValue({});
    vi.mocked(rescheduleAppointment).mockResolvedValue({});
    vi.mocked(createBarbershopReview).mockResolvedValue({});

    render(<MeusAgendamentosPage />);

    expect(await screen.findByText(/com: joao \(barbearia central\)/i)).toBeInTheDocument();
    expect(screen.getByText('Corte')).toBeInTheDocument();
    expect(screen.getByText('Barba')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /reagendar/i }));
    expect(screen.getByRole('dialog', { name: /remarcar agendamento/i })).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /confirmar remarcacao/i }));
    await waitFor(() => expect(rescheduleAppointment).toHaveBeenCalledWith(
      'appt-1',
      '2030-01-05T15:00:00',
      'barber-2',
    ));

    fireEvent.click(screen.getByRole('button', { name: /^cancelar$/i }));
    fireEvent.click(screen.getByRole('button', { name: /confirmar cancelamento/i }));
    await waitFor(() => expect(cancelAppointment).toHaveBeenCalledWith('appt-1'));

    fireEvent.click(screen.getByRole('button', { name: /avaliar/i }));
    fireEvent.change(screen.getByLabelText(/comentario/i), {
      target: { value: 'Atendimento excelente' },
    });
    fireEvent.click(screen.getByRole('button', { name: /enviar avaliação/i }));

    await waitFor(() => expect(createBarbershopReview).toHaveBeenCalledWith('shop-1', {
      rating: 5,
      comment: 'Atendimento excelente',
    }));
    expect(toastSuccess).toHaveBeenCalledWith('Avaliacao enviada com sucesso!');
  });

  it('loads owner team agenda, refreshes schedule and navigates barber tabs', async () => {
    locationSearch = '?view=team';
    vi.mocked(isCustomer).mockReturnValue(false);
    vi.mocked(isOwnerUser).mockReturnValue(true);
    vi.mocked(getBarbershopId).mockReturnValue('shop-99');
    vi.mocked(getMyAppointments).mockResolvedValue([]);
    vi.mocked(getBarbershopSchedule).mockResolvedValue([
      {
        id: 'team-1',
        status: 'CONFIRMED',
        startTime: '2026-05-22T09:00:00',
        endTime: '2026-05-22T09:30:00',
        barberName: 'Profissional',
        customerName: 'Cliente Agenda',
        activityNames: ['Degrade'],
      },
    ]);

    render(<MeusAgendamentosPage />);

    expect(getBarbershopSchedule).toHaveBeenCalledWith('shop-99', expect.objectContaining({
      from: expect.any(String),
      to: expect.any(String),
    }));

    fireEvent.click(screen.getByRole('button', { name: /atualizar/i }));
    await waitFor(() => expect(getBarbershopSchedule.mock.calls.length).toBeGreaterThanOrEqual(2));

    fireEvent.click(screen.getByText('Barber servicos'));
    expect(navigate).toHaveBeenCalledWith('/barberHome/servicos');

    fireEvent.click(screen.getByText('Barber gerenciar'));
    expect(navigate).toHaveBeenCalledWith('/barberHome/gerenciar-barbearia');

    fireEvent.click(screen.getByText('Novo agendamento nav'));
    expect(navigate).toHaveBeenCalledWith('/barberHome/novo-agendamento');
  });

  it('redirects unauthenticated users and logs out from the customer header', async () => {
    vi.mocked(getMyAppointments).mockResolvedValue([]);
    vi.mocked(isLoggedIn).mockReturnValueOnce(false);

    render(<MeusAgendamentosPage />);

    expect(navigate).toHaveBeenCalledWith('/');
    fireEvent.click(await screen.findByText('Customer logout'));
    await waitFor(() => expect(logoutUser).toHaveBeenCalled());
    expect(navigate).toHaveBeenCalledWith('/');
  });
});
