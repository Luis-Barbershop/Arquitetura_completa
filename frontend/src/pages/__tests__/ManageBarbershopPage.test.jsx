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

vi.mock('../../services/barbershopService', () => ({
  getBarbershopById: vi.fn(),
  updateMyBarbershop: vi.fn(),
  uploadMyBarbershopLogo: vi.fn(),
  uploadMyBarbershopBanner: vi.fn(),
  geocodeAddress: vi.fn(),
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
  default: ({ onLogout, onTabChange }) => (
    <div>
      <button type="button" onClick={onLogout}>Navbar logout</button>
      <button type="button" onClick={() => onTabChange('home')}>Navbar home</button>
    </div>
  ),
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
  geocodeAddress,
  getBarbershopById,
  updateMyBarbershop,
  uploadMyBarbershopBanner,
  uploadMyBarbershopLogo,
} from '../../services/barbershopService';
import { navigateToBarberTab } from '../../services/navigationService';
import { isOwnerUser } from '../../services/userContext';
import ManageBarbershopPage from '../ManageBarbershopPage';

describe('ManageBarbershopPage', () => {
  beforeEach(() => {
    navigate.mockReset();
    toastError.mockReset();
    toastSuccess.mockReset();
    localStorage.clear();
    localStorage.setItem('token', 'token');
    vi.mocked(api.get).mockReset();
    vi.mocked(logoutUser).mockReset();
    vi.mocked(isOwnerUser).mockReset();
    vi.mocked(navigateToBarberTab).mockReset();
    vi.mocked(getBarbershopById).mockReset();
    vi.mocked(updateMyBarbershop).mockReset();
    vi.mocked(uploadMyBarbershopLogo).mockReset();
    vi.mocked(uploadMyBarbershopBanner).mockReset();
    vi.mocked(geocodeAddress).mockReset();
    vi.mocked(isOwnerUser).mockReturnValue(false);
    vi.mocked(api.get).mockResolvedValue({
      data: { id: 'owner-1', name: 'Dono', isOwner: true, barbershopId: 'shop-1' },
    });
    vi.mocked(getBarbershopById).mockResolvedValue({
      name: 'Barbearia Antiga',
      address: 'Rua Velha, 10',
      description: 'Descricao antiga',
      phone: '(11) 90000-0000',
      latitude: null,
      longitude: null,
      logoUrl: '',
      bannerUrl: '',
    });
    vi.stubGlobal('URL', {
      createObjectURL: vi.fn(() => 'blob:image'),
      revokeObjectURL: vi.fn(),
    });
  });

  it('loads shop data, geocodes the address and saves public information', async () => {
    vi.mocked(geocodeAddress).mockResolvedValueOnce({ lat: -23.5, lng: -46.6 });
    vi.mocked(updateMyBarbershop).mockResolvedValueOnce({});

    render(<ManageBarbershopPage />);

    expect(await screen.findByDisplayValue('Barbearia Antiga')).toBeInTheDocument();

    fireEvent.change(screen.getByDisplayValue('Barbearia Antiga'), {
      target: { name: 'name', value: ' Barbearia Atualizada ' },
    });
    fireEvent.change(screen.getByDisplayValue('Rua Velha, 10'), {
      target: { name: 'address', value: ' Avenida Nova, 123 ' },
    });
    fireEvent.change(screen.getByDisplayValue('Descricao antiga'), {
      target: { name: 'description', value: ' Ambiente renovado ' },
    });
    fireEvent.change(screen.getByDisplayValue('(11) 90000-0000'), {
      target: { name: 'phone', value: ' (11) 98888-7777 ' },
    });
    fireEvent.click(screen.getByRole('button', { name: /salvar dados da barbearia/i }));

    await waitFor(() => expect(geocodeAddress).toHaveBeenCalledWith('Avenida Nova, 123'));
    expect(updateMyBarbershop).toHaveBeenCalledWith({
      name: 'Barbearia Atualizada',
      address: 'Avenida Nova, 123',
      description: 'Ambiente renovado',
      phone: '(11) 98888-7777',
      latitude: -23.5,
      longitude: -46.6,
    });
    expect(localStorage.getItem('barbershopName')).toBe('Barbearia Atualizada');
    expect(toastSuccess).toHaveBeenCalledWith('Dados da barbearia atualizados!');
  });

  it('uploads logo and banner through the crop modal', async () => {
    vi.mocked(uploadMyBarbershopLogo).mockResolvedValueOnce({ logoUrl: 'logo-nova.png' });
    vi.mocked(uploadMyBarbershopBanner).mockResolvedValueOnce({ bannerUrl: 'banner-novo.png' });

    const { container } = render(<ManageBarbershopPage />);

    await screen.findByText(/sem miniatura/i);
    const [bannerInput, logoInput] = container.querySelectorAll('input[type="file"]');

    fireEvent.change(logoInput, {
      target: { files: [new File(['logo'], 'logo.png', { type: 'image/png' })] },
    });
    fireEvent.click(screen.getByRole('button', { name: /confirmar crop/i }));

    await waitFor(() => expect(uploadMyBarbershopLogo).toHaveBeenCalled());
    expect(await screen.findByAltText(/miniatura/i)).toHaveAttribute('src', 'logo-nova.png');

    fireEvent.change(bannerInput, {
      target: { files: [new File(['banner'], 'banner.png', { type: 'image/png' })] },
    });
    fireEvent.click(screen.getByRole('button', { name: /confirmar crop/i }));

    await waitFor(() => expect(uploadMyBarbershopBanner).toHaveBeenCalled());
    expect(await screen.findByAltText(/banner da barbearia/i)).toHaveAttribute('src', 'banner-novo.png');
  });

  it('captures current location, redirects non owners and delegates navigation/logout', async () => {
    const getCurrentPosition = vi.fn((success) => success({ coords: { latitude: -20.1, longitude: -40.2 } }));
    vi.stubGlobal('navigator', {
      ...navigator,
      geolocation: { getCurrentPosition },
    });

    render(<ManageBarbershopPage />);

    await screen.findByDisplayValue('Barbearia Antiga');
    fireEvent.click(screen.getByRole('button', { name: /usar minha localização atual/i }));

    expect(getCurrentPosition).toHaveBeenCalled();
    expect(screen.getByText('-20.10000, -40.20000')).toBeInTheDocument();
    expect(toastSuccess).toHaveBeenCalledWith('Localização capturada! Salve para confirmar.');

    fireEvent.click(screen.getByText('Header estoque'));
    expect(navigateToBarberTab).toHaveBeenCalledWith('estoque', navigate, {
      isOwner: true,
      currentPath: '/barberHome/gerenciar-barbearia',
    });

    fireEvent.click(screen.getByText('Navbar logout'));
    await waitFor(() => expect(logoutUser).toHaveBeenCalled());
    expect(navigate).toHaveBeenCalledWith('/');
  });

  it('redirects users without owner permission', async () => {
    vi.mocked(api.get).mockResolvedValueOnce({
      data: { id: 'barber-1', name: 'Barbeiro', isOwner: false, barbershopId: 'shop-1' },
    });

    render(<ManageBarbershopPage />);

    await waitFor(() => expect(navigate).toHaveBeenCalledWith('/barberHome', { replace: true }));
    expect(getBarbershopById).not.toHaveBeenCalled();
  });
});
