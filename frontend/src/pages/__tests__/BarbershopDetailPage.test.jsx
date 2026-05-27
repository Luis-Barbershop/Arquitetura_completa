import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const navigate = vi.fn();
const toastError = vi.fn();
let params = { barbershopId: 's1' };

vi.mock('react-router-dom', () => ({
  useNavigate: () => navigate,
  useParams: () => params,
}));

vi.mock('react-toastify', () => ({
  toast: {
    error: (...args) => toastError(...args),
  },
}));

vi.mock('../../components/HomePage/CustomerHeader', () => ({
  default: ({ onLogout }) => <button onClick={onLogout}>Header logout</button>,
}));

vi.mock('../../components/HomePage/CustomerNavbar', () => ({
  default: ({ onLogout }) => <button onClick={onLogout}>Navbar logout</button>,
}));

vi.mock('../../components/BarbershopMap/BarbershopMap', () => ({
  default: ({ latitude, longitude, barbershopName }) => (
    <div>Map {barbershopName} {latitude},{longitude}</div>
  ),
}));

vi.mock('../../services/authService', () => ({
  logoutUser: vi.fn(),
}));

vi.mock('../../services/barbershopService', () => ({
  geocodeAddress: vi.fn(),
  getBarbershopById: vi.fn(),
  getShopBarbers: vi.fn(),
  getShopServices: vi.fn(),
  updateMyBarbershop: vi.fn(),
}));

import { logoutUser } from '../../services/authService';
import {
  geocodeAddress,
  getBarbershopById,
  getShopBarbers,
  getShopServices,
  updateMyBarbershop,
} from '../../services/barbershopService';
import BarbershopDetailPage from '../BarbershopDetailPage';

describe('BarbershopDetailPage', () => {
  beforeEach(() => {
    navigate.mockReset();
    toastError.mockReset();
    params = { barbershopId: 's1' };
    localStorage.clear();
    vi.mocked(logoutUser).mockReset();
    vi.mocked(geocodeAddress).mockReset();
    vi.mocked(getBarbershopById).mockReset();
    vi.mocked(getShopBarbers).mockReset();
    vi.mocked(getShopServices).mockReset();
    vi.mocked(updateMyBarbershop).mockReset();
  });

  it('loads shop details, geocodes owner shop and navigates to scheduling', async () => {
    localStorage.setItem('userRole', 'ROLE_BARBER');
    localStorage.setItem('isOwner', 'true');
    localStorage.setItem('barbershopId', 's1');
    vi.mocked(getBarbershopById).mockResolvedValueOnce({
      id: 's1',
      name: 'Barbearia Prime',
      address: 'Rua A',
      description: 'Descricao da barbearia',
      averageRating: 4.5,
      reviewsCount: 2,
    });
    vi.mocked(getShopServices).mockResolvedValueOnce([
      { id: 'svc1', activityName: 'Corte', durationMinutes: 30, price: 50 },
    ]);
    vi.mocked(getShopBarbers).mockResolvedValueOnce([{ id: 'b1', name: 'Joao Silva' }]);
    vi.mocked(geocodeAddress).mockResolvedValueOnce({ lat: -23.5, lng: -46.6 });
    vi.mocked(updateMyBarbershop).mockResolvedValueOnce({});

    render(<BarbershopDetailPage />);

    expect(screen.getByText(/carregando/i)).toBeInTheDocument();
    expect(await screen.findByRole('heading', { name: 'Barbearia Prime' })).toBeInTheDocument();
    expect(screen.getByText('Corte')).toBeInTheDocument();
    expect(screen.getByText(/R\$.*50,00/)).toBeInTheDocument();
    expect(screen.getByText(/map barbearia prime/i)).toBeInTheDocument();
    expect(updateMyBarbershop).toHaveBeenCalledWith({ latitude: -23.5, longitude: -46.6 });

    fireEvent.click(screen.getByRole('button', { name: /agendar agora/i }));
    expect(navigate).toHaveBeenCalledWith('/agendamentoPage/s1');
  });

  it('shows empty state when shop is not found', async () => {
    vi.mocked(getBarbershopById).mockResolvedValueOnce(null);
    vi.mocked(getShopServices).mockResolvedValueOnce([]);
    vi.mocked(getShopBarbers).mockResolvedValueOnce([]);

    render(<BarbershopDetailPage />);

    expect(await screen.findByText(/barbearia não encontrada/i)).toBeInTheDocument();
  });

  it('shows toast when loading fails and logs out', async () => {
    vi.mocked(getBarbershopById).mockRejectedValueOnce(new Error('failed'));
    vi.mocked(getShopServices).mockResolvedValueOnce([]);
    vi.mocked(getShopBarbers).mockResolvedValueOnce([]);

    render(<BarbershopDetailPage />);

    await waitFor(() => expect(toastError).toHaveBeenCalledWith('Erro ao carregar informações da barbearia.'));
    fireEvent.click(screen.getByRole('button', { name: /header logout/i }));
    expect(logoutUser).toHaveBeenCalled();
    expect(navigate).toHaveBeenCalledWith('/');
  });
});
