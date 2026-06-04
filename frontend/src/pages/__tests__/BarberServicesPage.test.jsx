import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const navigate = vi.fn();

vi.mock('react-router-dom', () => ({
  useNavigate: () => navigate,
}));

vi.mock('../../services/api', () => ({
  default: {
    get: vi.fn(),
  },
}));

vi.mock('../../services/authService', () => ({
  logoutUser: vi.fn(),
}));

vi.mock('../../services/userContext', () => ({
  getBarbershopId: vi.fn(() => 'shop-1'),
  isCustomer: vi.fn(),
  isOwnerUser: vi.fn(),
}));

vi.mock('../../services/barbershopService', () => ({
  createService: vi.fn(),
  deleteService: vi.fn(),
  getMyServices: vi.fn(),
}));

vi.mock('../../components/BarberPage/BarberHeader', () => ({
  default: ({ onLogout, onTabChange }) => (
    <div>
      <button onClick={onLogout}>Header logout</button>
      <button onClick={() => onTabChange('estoque')}>Go estoque</button>
      <button onClick={() => onTabChange('gerenciar-barbearia')}>Go gerenciar</button>
    </div>
  ),
}));

vi.mock('../../components/BarberPage/BarberNavbar', () => ({
  default: ({ onTabChange }) => <button onClick={() => onTabChange('home')}>Go home</button>,
}));

vi.mock('../../components/BarberPage/ManageMySkills', () => ({
  default: ({ shopId, refreshKey }) => <div>Manage skills {shopId} {refreshKey}</div>,
}));

import api from '../../services/api';
import { logoutUser } from '../../services/authService';
import { createService, deleteService, getMyServices } from '../../services/barbershopService';
import { isCustomer, isOwnerUser } from '../../services/userContext';
import BarberServicesPage from '../BarberServicesPage';

describe('BarberServicesPage', () => {
  beforeEach(() => {
    navigate.mockReset();
    localStorage.clear();
    vi.mocked(api.get).mockReset();
    vi.mocked(logoutUser).mockReset();
    vi.mocked(isCustomer).mockReset();
    vi.mocked(isOwnerUser).mockReset();
    vi.mocked(createService).mockReset();
    vi.mocked(deleteService).mockReset();
    vi.mocked(getMyServices).mockReset();
    vi.mocked(isCustomer).mockReturnValue(false);
    vi.mocked(isOwnerUser).mockReturnValue(true);
    localStorage.setItem('token', 'token');
    localStorage.setItem('isOwner', 'true');
    vi.mocked(api.get).mockResolvedValue({
      data: { id: 'barber-1', name: 'Barbeiro', barbershopId: 'shop-1', isOwner: true },
    });
  });

  it('loads owner services, creates and deletes a service', async () => {
    vi.mocked(getMyServices)
      .mockResolvedValueOnce([{ id: 'svc-1', activityName: 'Corte', price: 50, durationMinutes: 30 }])
      .mockResolvedValueOnce([
        { id: 'svc-1', activityName: 'Corte', price: 50, durationMinutes: 30 },
        { id: 'svc-2', activityName: 'Barba', price: 35, durationMinutes: 20 },
      ])
      .mockResolvedValueOnce([]);
    vi.mocked(createService).mockResolvedValueOnce({ id: 'svc-2' });
    vi.mocked(deleteService).mockResolvedValueOnce();

    render(<BarberServicesPage />);

    expect(await screen.findByText('Corte')).toBeInTheDocument();
    expect(screen.getAllByText('R$ 50,00')).toHaveLength(2);
    expect(screen.getByText(/Manage skills shop-1/)).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText(/nome do serviço/i), { target: { value: 'Barba' } });
    fireEvent.change(screen.getByLabelText(/preço/i), { target: { value: '35' } });
    fireEvent.change(screen.getByLabelText(/duração/i), { target: { value: '20' } });
    fireEvent.click(screen.getByRole('button', { name: /adicionar serviço/i }));

    await waitFor(() => expect(createService).toHaveBeenCalledWith({
      activityName: 'Barba',
      price: 35,
      durationMinutes: 20,
    }));
    expect(await screen.findByText(/servico adicionado com sucesso/i)).toBeInTheDocument();

    fireEvent.click(screen.getAllByRole('button', { name: /excluir/i })[0]);
    expect(screen.getByText(/deseja realmente excluir/i)).toBeInTheDocument();
    fireEvent.click(screen.getAllByRole('button', { name: /^excluir$/i }).at(-1));

    await waitFor(() => expect(deleteService).toHaveBeenCalledWith('svc-1'));
  });

  it('validates invalid service inputs and redirects denied users', async () => {
    vi.mocked(getMyServices).mockResolvedValueOnce([]);

    render(<BarberServicesPage />);

    await screen.findByText(/novo serviço/i);
    fireEvent.change(screen.getByLabelText(/nome do serviço/i), { target: { value: 'Teste' } });
    fireEvent.change(screen.getByLabelText(/preço/i), { target: { value: '0' } });
    fireEvent.change(screen.getByLabelText(/duração/i), { target: { value: '30' } });
    fireEvent.click(screen.getByRole('button', { name: /adicionar serviço/i }));

    expect(screen.getByText(/preco valido maior que zero/i)).toBeInTheDocument();

    fireEvent.click(screen.getByText('Go estoque'));
    expect(navigate).toHaveBeenCalledWith('/barberHome/estoque');

    fireEvent.click(screen.getByText('Go gerenciar'));
    expect(navigate).toHaveBeenCalledWith('/barberHome/gerenciar-barbearia');
  });

  it('redirects customer users before loading page', () => {
    vi.mocked(isCustomer).mockReturnValueOnce(true);

    render(<BarberServicesPage />);

    expect(navigate).toHaveBeenCalledWith('/homepage', { replace: true });
  });
});
