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

vi.mock('../../services/api', () => ({
  default: {
    delete: vi.fn(),
    get: vi.fn(),
    post: vi.fn(),
  },
}));

vi.mock('../../services/authService', () => ({
  logoutUser: vi.fn(),
}));

vi.mock('../../services/userContext', () => ({
  isCustomer: vi.fn(),
  isOwnerUser: vi.fn(),
}));

vi.mock('../../components/BarberPage/BarberHeader', () => ({
  default: ({ onLogout, onTabChange }) => (
    <div>
      <button onClick={onLogout}>Header logout</button>
      <button onClick={() => onTabChange('servicos')}>Go servicos</button>
    </div>
  ),
}));

vi.mock('../../components/BarberPage/BarberNavbar', () => ({
  default: ({ onTabChange }) => <button onClick={() => onTabChange('home')}>Go home</button>,
}));

import api from '../../services/api';
import { logoutUser } from '../../services/authService';
import { isCustomer, isOwnerUser } from '../../services/userContext';
import BarberTeamPage from '../BarberTeamPage';

describe('BarberTeamPage', () => {
  beforeEach(() => {
    navigate.mockReset();
    toastError.mockReset();
    toastSuccess.mockReset();
    localStorage.clear();
    vi.mocked(api.delete).mockReset();
    vi.mocked(api.get).mockReset();
    vi.mocked(api.post).mockReset();
    vi.mocked(logoutUser).mockReset();
    vi.mocked(isCustomer).mockReset();
    vi.mocked(isOwnerUser).mockReset();
    vi.mocked(isCustomer).mockReturnValue(false);
    vi.mocked(isOwnerUser).mockReturnValue(true);
    localStorage.setItem('token', 'token');
  });

  const mockTeamLoad = () => {
    vi.mocked(api.get).mockImplementation((url) => {
      const responses = {
        '/auth/me': { id: 'owner-1', name: 'Owner', barbershopId: 'shop-1' },
        '/barbershops/my-shop/team': [
          {
            barberId: 'owner-1',
            name: 'Owner',
            email: 'owner@teste.com',
            isOwner: true,
            commissions: [],
          },
          {
            barberId: 'barber-2',
            name: 'Colaborador',
            email: 'colab@teste.com',
            isOwner: false,
            commissions: [{ id: 'rule-1', activityName: 'Corte', percentage: 40 }],
          },
        ],
        '/barbershops/shop-1/activities': [{ id: 'act-1', activityName: 'Corte' }],
        '/barbershops/my-shop/team/barber-2/conflicts': [
          { id: 'app-1', startTime: '2026-05-22T10:00:00', customerName: 'Cliente', status: 'SCHEDULED' },
        ],
      };
      return Promise.resolve({ data: responses[url] ?? [] });
    });
  };

  it('loads team, saves/removes commissions, sends invite and removes member', async () => {
    mockTeamLoad();
    vi.mocked(api.post).mockResolvedValue({});
    vi.mocked(api.delete).mockResolvedValue({});

    render(<BarberTeamPage />);

    await waitFor(() => expect(screen.getAllByText('Colaborador').length).toBeGreaterThan(0));
    fireEvent.click(screen.getAllByRole('button', { name: /comissoes/i })[1]);
    expect(screen.getByText(/regras de colaborador/i)).toBeInTheDocument();
    expect(screen.getByText('40.00%')).toBeInTheDocument();

    fireEvent.change(screen.getByDisplayValue('Servico'), { target: { value: 'act-1' } });
    fireEvent.change(screen.getByPlaceholderText('%'), { target: { value: '45' } });
    fireEvent.click(screen.getByRole('button', { name: /^salvar$/i }));

    await waitFor(() => expect(api.post).toHaveBeenCalledWith('/barbershops/my-shop/team/barber-2/commissions', {
      activityId: 'act-1',
      percentage: 45,
    }));

    fireEvent.click(screen.getAllByRole('button', { name: /^remover$/i })[1]);
    await waitFor(() => expect(api.delete).toHaveBeenCalledWith('/barbershops/my-shop/team/barber-2/commissions/rule-1'));

    fireEvent.click(screen.getByRole('button', { name: /\+ convidar/i }));
    fireEvent.change(screen.getByPlaceholderText('000.000.000-00'), { target: { value: '12345678901' } });
    fireEvent.click(screen.getByRole('button', { name: /enviar convite/i }));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith('/barbershops/my-shop/invite-barber', { cpf: '12345678901' }));

    fireEvent.click(screen.getAllByRole('button', { name: /^remover$/i })[0]);
    expect(await screen.findByText(/1 agendamento/i)).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /confirmar remoção/i }));
    await waitFor(() => expect(api.delete).toHaveBeenCalledWith('/barbershops/my-shop/team/barber-2', {
      data: { action: 'CANCEL', redistributeToId: null },
    }));
  });

  it('validates invite CPF and redirects denied users', async () => {
    mockTeamLoad();

    render(<BarberTeamPage />);

    fireEvent.click(await screen.findByRole('button', { name: /\+ convidar/i }));
    fireEvent.change(screen.getByPlaceholderText('000.000.000-00'), { target: { value: '123' } });
    fireEvent.click(screen.getByRole('button', { name: /enviar convite/i }));

    expect(screen.getByText(/cpf valido/i)).toBeInTheDocument();
  });

  it('redirects non owner users and logs out', () => {
    vi.mocked(isOwnerUser).mockReturnValueOnce(false);

    render(<BarberTeamPage />);

    expect(navigate).toHaveBeenCalledWith('/barberHome', { replace: true });
  });
});
