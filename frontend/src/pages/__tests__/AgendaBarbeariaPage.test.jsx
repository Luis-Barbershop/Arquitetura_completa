import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const navigate = vi.fn();
const toastError = vi.fn();

vi.mock('react-router-dom', () => ({
  useNavigate: () => navigate,
}));

vi.mock('react-toastify', () => ({
  toast: {
    error: (...args) => toastError(...args),
  },
}));

vi.mock('react-icons/fi', () => ({
  FiCalendar: () => <span data-testid="icon-calendar" />,
  FiChevronLeft: () => <span data-testid="icon-left" />,
  FiChevronRight: () => <span data-testid="icon-right" />,
  FiRefreshCw: () => <span data-testid="icon-refresh" />,
  FiScissors: () => <span data-testid="icon-scissors" />,
  FiCheckCircle: () => <span data-testid="icon-check" />,
  FiXCircle: () => <span data-testid="icon-x" />,
  FiClock: () => <span data-testid="icon-clock" />,
}));

vi.mock('../../services/appointmentService', () => ({
  getBarbershopSchedule: vi.fn(),
}));

vi.mock('../../services/userContext', () => ({
  isOwnerUser: vi.fn(),
  isLoggedIn: vi.fn(),
  getBarbershopId: vi.fn(),
}));

vi.mock('../../services/authService', () => ({
  logoutUser: vi.fn(),
}));

vi.mock('../../components/BarberPage/BarberHeader', () => ({
  default: ({ onLogout, onTabChange }) => (
    <div>
      <button type="button" onClick={onLogout}>Header logout</button>
      <button type="button" onClick={() => onTabChange('servicos')}>Header servicos</button>
      <button type="button" onClick={() => onTabChange('agenda-equipe')}>Header agenda atual</button>
    </div>
  ),
}));

vi.mock('../../components/BarberPage/BarberNavbar', () => ({
  default: ({ onTabChange }) => (
    <button type="button" onClick={() => onTabChange('novo-agendamento')}>Navbar novo agendamento</button>
  ),
}));

import { logoutUser } from '../../services/authService';
import { getBarbershopSchedule } from '../../services/appointmentService';
import { getBarbershopId, isLoggedIn, isOwnerUser } from '../../services/userContext';
import AgendaBarbeariaPage from '../AgendaBarbeariaPage';

const manyAppointments = Array.from({ length: 11 }, (_, index) => ({
  id: `appt-${index}`,
  startTime: `2026-05-22T${String(8 + (index % 10)).padStart(2, '0')}:00:00`,
  endTime: `2026-05-22T${String(8 + (index % 10)).padStart(2, '0')}:30:00`,
  barberName: index === 0 ? 'Ana Barbeira' : `Barbeiro ${index}`,
  customerName: index === 0 ? 'Cliente Especial' : `Cliente ${index}`,
  activityNames: index === 0 ? ['Corte', 'Barba'] : ['Corte'],
  status: index === 1 ? 'COMPLETED' : index === 2 ? 'CANCELLED' : index === 3 ? 'WALK_IN' : 'SCHEDULED',
}));

describe('AgendaBarbeariaPage', () => {
  beforeEach(() => {
    navigate.mockReset();
    toastError.mockReset();
    localStorage.clear();
    localStorage.setItem('userName', 'Dono Agenda');
    vi.mocked(getBarbershopSchedule).mockReset();
    vi.mocked(isOwnerUser).mockReset();
    vi.mocked(isLoggedIn).mockReset();
    vi.mocked(getBarbershopId).mockReset();
    vi.mocked(logoutUser).mockReset();
    vi.mocked(isOwnerUser).mockReturnValue(true);
    vi.mocked(isLoggedIn).mockReturnValue(true);
    vi.mocked(getBarbershopId).mockReturnValue('shop-1');
  });

  it('redirects owner to team view, loads agenda, filters status and paginates', async () => {
    vi.mocked(getBarbershopSchedule).mockResolvedValue(manyAppointments);

    render(<AgendaBarbeariaPage />);

    await waitFor(() => expect(navigate).toHaveBeenCalledWith('/meus-agendamentos?view=team', { replace: true }));
    expect(await screen.findByText('Ana Barbeira')).toBeInTheDocument();
    expect(screen.getByText(/cliente especial/i)).toBeInTheDocument();
    expect(screen.getByText('Corte, Barba')).toBeInTheDocument();
    expect(getBarbershopSchedule).toHaveBeenCalledWith('shop-1', expect.any(String));

    fireEvent.click(screen.getByRole('button', { name: /concluídos/i }));
    expect(screen.getByText('Barbeiro 1')).toBeInTheDocument();
    expect(screen.queryByText('Ana Barbeira')).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /todos/i }));
    fireEvent.click(screen.getByRole('button', { name: /^2$/i }));
    expect(screen.getByText('Barbeiro 10')).toBeInTheDocument();
    fireEvent.click(screen.getByText('Anterior'));
    expect(screen.getByText('Barbeiro 9')).toBeInTheDocument();
  });

  it('changes date, refreshes, navigates tabs and logs out', async () => {
    vi.mocked(getBarbershopSchedule).mockResolvedValue([]);

    const { container } = render(<AgendaBarbeariaPage />);

    expect(await screen.findByText(/nenhum atendimento encontrado/i)).toBeInTheDocument();
    fireEvent.change(container.querySelector('input[type="date"]'), {
      target: { value: '2026-05-24' },
    });
    await waitFor(() => expect(getBarbershopSchedule).toHaveBeenCalledWith('shop-1', '2026-05-24'));

    fireEvent.click(screen.getByRole('button', { name: /atualizar/i }));
    await waitFor(() => expect(getBarbershopSchedule.mock.calls.length).toBeGreaterThanOrEqual(3));

    fireEvent.click(screen.getByText('Header servicos'));
    expect(navigate).toHaveBeenCalledWith('/barberHome/servicos');
    fireEvent.click(screen.getByText('Navbar novo agendamento'));
    expect(navigate).toHaveBeenCalledWith('/barberHome/novo-agendamento');

    fireEvent.click(screen.getByText('Header logout'));
    expect(logoutUser).toHaveBeenCalled();
    expect(navigate).toHaveBeenCalledWith('/');
  });

  it('redirects unauthenticated or non-owner users and shows load errors', async () => {
    vi.mocked(isOwnerUser).mockReturnValueOnce(false);
    vi.mocked(getBarbershopSchedule).mockRejectedValueOnce(new Error('fail'));

    render(<AgendaBarbeariaPage />);

    expect(navigate).toHaveBeenCalledWith('/');
    await waitFor(() => expect(toastError).toHaveBeenCalledWith('Não foi possível carregar a agenda da barbearia.'));
  });
});
