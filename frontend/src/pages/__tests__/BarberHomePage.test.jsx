import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const navigate = vi.fn();
let mockedLocation = { search: '', state: null };
const toastError = vi.fn();
const toastInfo = vi.fn();
const toastSuccess = vi.fn();

vi.mock('react-router-dom', () => ({
  useNavigate: () => navigate,
  useLocation: () => mockedLocation,
}));

vi.mock('react-toastify', () => ({
  toast: {
    error: (...args) => toastError(...args),
    info: (...args) => toastInfo(...args),
    success: (...args) => toastSuccess(...args),
  },
}));

vi.mock('../../hooks/useAuthGuard', () => ({
  useAuthGuard: vi.fn(),
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
  isOwnerUser: vi.fn(),
}));

vi.mock('../../services/navigationService', () => ({
  navigateToBarberTab: vi.fn(),
}));

vi.mock('../../components/BarberPage/BarberHeader', () => ({
  default: ({ activeTab, onLogout, onTabChange }) => (
    <div>
      <span>Header active {activeTab}</span>
      <button type="button" onClick={onLogout}>Header logout</button>
      <button type="button" onClick={() => onTabChange('estoque')}>Header estoque</button>
    </div>
  ),
}));

vi.mock('../../components/BarberPage/BarberNavbar', () => ({
  default: ({ onLogout, onTabChange }) => (
    <div>
      <button type="button" onClick={onLogout}>Navbar logout</button>
      <button type="button" onClick={() => onTabChange('servicos')}>Navbar servicos</button>
    </div>
  ),
}));

vi.mock('../../components/BarberPage/NoBarbershopPanel', () => ({
  default: ({ onCreateShop, onGoToProfile }) => (
    <div>
      <span>Sem barbearia vinculada</span>
      <button type="button" onClick={onCreateShop}>Criar barbearia</button>
      <button type="button" onClick={onGoToProfile}>Ir ao perfil</button>
    </div>
  ),
}));

vi.mock('../../components/BarberPage/Invoicing', () => ({
  default: () => <div>Invoicing widget</div>,
}));

vi.mock('../../components/BarberPage/Buttonsbarber', () => ({
  default: ({ onReportsClick, onMyBookingsClick }) => (
    <div>
      <button type="button" onClick={onReportsClick}>Relatorios</button>
      <button type="button" onClick={onMyBookingsClick}>Minha agenda</button>
    </div>
  ),
}));

vi.mock('../../components/BarberPage/ActionsBarber', () => ({
  default: ({ onNavigateToStock, showInsights }) => (
    <div>
      <span>Insights {String(showInsights)}</span>
      <button type="button" onClick={onNavigateToStock}>Ir estoque</button>
    </div>
  ),
}));

vi.mock('../../components/BarberPage/NextScheduling', () => ({
  default: ({ onViewAll }) => <button type="button" onClick={onViewAll}>Ver todos agenda</button>,
}));

vi.mock('../../components/BarberPage/ServicesHomeBarber', () => ({
  default: ({ onNavigateToServices }) => <button type="button" onClick={onNavigateToServices}>Ver servicos</button>,
}));

import api from '../../services/api';
import { logoutUser } from '../../services/authService';
import { useAuthGuard } from '../../hooks/useAuthGuard';
import { navigateToBarberTab } from '../../services/navigationService';
import { isOwnerUser } from '../../services/userContext';
import BarberHomePage from '../BarberHomePage';

describe('BarberHomePage', () => {
  beforeEach(() => {
    navigate.mockReset();
    toastError.mockReset();
    toastInfo.mockReset();
    toastSuccess.mockReset();
    mockedLocation = { search: '', state: null };
    localStorage.clear();
    vi.mocked(api.get).mockReset();
    vi.mocked(logoutUser).mockReset();
    vi.mocked(useAuthGuard).mockReset();
    vi.mocked(navigateToBarberTab).mockReset();
    vi.mocked(isOwnerUser).mockReset();
    vi.mocked(useAuthGuard).mockReturnValue({ isAuthorized: true });
    vi.mocked(isOwnerUser).mockReturnValue(false);
  });

  it('loads a linked owner home and handles shortcuts, tab navigation and logout modal', async () => {
    vi.mocked(api.get).mockResolvedValueOnce({
      data: {
        id: 'barber-1',
        name: 'Dono Principal',
        isOwner: true,
        barbershopId: 'shop-1',
        imageUrl: 'foto.png',
      },
    });

    render(<BarberHomePage />);

    expect(await screen.findByText(/olá, dono/i)).toBeInTheDocument();
    expect(screen.getByText('Invoicing widget')).toBeInTheDocument();
    expect(localStorage.getItem('isOwner')).toBe('true');
    expect(localStorage.getItem('barbershopId')).toBe('shop-1');
    expect(localStorage.getItem('userProfileImage')).toBe('foto.png');

    fireEvent.click(screen.getByText('Relatorios'));
    expect(navigate).toHaveBeenCalledWith('/barberHome/dashboard');
    fireEvent.click(screen.getByText('Minha agenda'));
    expect(navigate).toHaveBeenCalledWith('/meus-agendamentos');
    fireEvent.click(screen.getByText('Ir estoque'));
    expect(navigate).toHaveBeenCalledWith('/barberHome/estoque');
    fireEvent.click(screen.getByText('Ver todos agenda'));
    expect(navigate).toHaveBeenCalledWith('/meus-agendamentos');
    fireEvent.click(screen.getByText('Ver servicos'));
    expect(navigate).toHaveBeenCalledWith('/barberHome/servicos');

    fireEvent.click(screen.getByText('Header estoque'));
    expect(navigateToBarberTab).toHaveBeenCalledWith('estoque', navigate, {
      isOwner: true,
      currentPath: '/barberHome',
    });

    fireEvent.click(screen.getByText('Header logout'));
    expect(screen.getByText(/deseja sair da sua conta/i)).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /permanecer/i }));
    expect(screen.queryByText(/deseja sair da sua conta/i)).not.toBeInTheDocument();

    fireEvent.click(screen.getByText('Navbar logout'));
    fireEvent.click(screen.getByRole('button', { name: /sair da conta/i }));
    expect(logoutUser).toHaveBeenCalled();
    expect(navigate).toHaveBeenCalledWith('/');
  });

  it('shows no-shop panel and navigates to create shop/profile', async () => {
    vi.mocked(api.get).mockResolvedValueOnce({
      data: { id: 'barber-2', name: 'Sem Loja', isOwner: false, barbershopId: null },
    });

    render(<BarberHomePage />);

    expect(await screen.findByText('Sem barbearia vinculada')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /criar barbearia/i }));
    expect(navigate).toHaveBeenCalledWith('/create-barbershop');
    fireEvent.click(screen.getByRole('button', { name: /ir ao perfil/i }));
    expect(navigate).toHaveBeenCalledWith('/barberHome/perfil');
  });

  it('shows Mercado Pago feedback and redirects on auth failure', async () => {
    mockedLocation = { search: '?mpLinked=false&mpReason=oauth_disabled_in_test', state: { activeTab: 'servicos' } };
    vi.mocked(api.get).mockRejectedValueOnce({ response: { status: 403 } });

    render(<BarberHomePage />);

    expect(toastInfo).toHaveBeenCalledWith('Vinculação simulada (ambiente de teste). Em produção o OAuth real será usado.');
    await waitFor(() => expect(navigate).toHaveBeenCalledWith('/'));
  });

  it('keeps loading shell while authorization is pending and handles mp success', () => {
    mockedLocation = { search: '?mpLinked=true', state: null };
    vi.mocked(useAuthGuard).mockReturnValueOnce({ isAuthorized: false });

    render(<BarberHomePage />);

    expect(screen.getByText('Carregando...')).toBeInTheDocument();
    expect(toastSuccess).toHaveBeenCalledWith('Conta Mercado Pago vinculada com sucesso!');
    expect(api.get).not.toHaveBeenCalled();
  });
});
