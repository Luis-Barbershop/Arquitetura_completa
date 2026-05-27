import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

const navigate = vi.fn();
const toastError = vi.fn();
const toastInfo = vi.fn();
const toastSuccess = vi.fn();

vi.mock('react-router-dom', () => ({
  useNavigate: () => navigate,
}));

vi.mock('react-toastify', () => ({
  toast: {
    error: (...args) => toastError(...args),
    info: (...args) => toastInfo(...args),
    success: (...args) => toastSuccess(...args),
  },
}));

vi.mock('../../components/HomePage/CustomerHeader', () => ({
  default: ({ onLogout }) => <button onClick={onLogout}>Header logout</button>,
}));

vi.mock('../../components/HomePage/CustomerNavbar', () => ({
  default: ({ onLogout }) => <button onClick={onLogout}>Navbar logout</button>,
}));

vi.mock('../../components/HomePage/Barbershops/Barbershops', () => ({
  default: ({ searchTerm, favoriteIds, onToggleFavorite, userLocation }) => (
    <div>
      <span>Barbershops mocked {searchTerm}</span>
      <span>Favorites {favoriteIds.join(',')}</span>
      <span>Location {userLocation ? `${userLocation.lat},${userLocation.lng}` : 'none'}</span>
      <button onClick={() => onToggleFavorite('s2', false)}>Add favorite</button>
      <button onClick={() => onToggleFavorite('s1', true)}>Remove favorite</button>
    </div>
  ),
}));

vi.mock('../../components/HomePage/Favorite_barbershops/Favorite_barbershops', () => ({
  default: ({ favoriteIds }) => <div>Favorite mocked {favoriteIds.join(',')}</div>,
}));

vi.mock('../../services/authService', () => ({
  logoutUser: vi.fn(),
}));

vi.mock('../../services/userContext', () => ({
  isBarber: vi.fn(),
}));

vi.mock('../../services/userProfileService', () => ({
  getMyProfile: vi.fn(),
}));

vi.mock('../../services/barbershopService', () => ({
  addFavoriteBarbershop: vi.fn(),
  getMyFavoriteBarbershopsIds: vi.fn(),
  removeFavoriteBarbershop: vi.fn(),
}));

import { logoutUser } from '../../services/authService';
import {
  addFavoriteBarbershop,
  getMyFavoriteBarbershopsIds,
  removeFavoriteBarbershop,
} from '../../services/barbershopService';
import { isBarber } from '../../services/userContext';
import { getMyProfile } from '../../services/userProfileService';
import HomePage from '../HomePage';

describe('HomePage', () => {
  beforeEach(() => {
    navigate.mockReset();
    toastError.mockReset();
    toastInfo.mockReset();
    toastSuccess.mockReset();
    sessionStorage.clear();
    localStorage.clear();
    vi.mocked(logoutUser).mockReset();
    vi.mocked(isBarber).mockReset();
    vi.mocked(getMyProfile).mockReset();
    vi.mocked(addFavoriteBarbershop).mockReset();
    vi.mocked(getMyFavoriteBarbershopsIds).mockReset();
    vi.mocked(removeFavoriteBarbershop).mockReset();
    vi.mocked(isBarber).mockReturnValue(false);
    vi.mocked(getMyProfile).mockResolvedValue({ name: 'Cliente Teste', imageUrl: 'foto.png' });
    vi.mocked(getMyFavoriteBarbershopsIds).mockResolvedValue(['s1']);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('loads customer profile, favorites and handles search/favorite/logout', async () => {
    vi.mocked(addFavoriteBarbershop).mockResolvedValue();
    vi.mocked(removeFavoriteBarbershop).mockResolvedValue();

    render(<HomePage />);

    await waitFor(() => expect(getMyProfile).toHaveBeenCalled());
    await screen.findByText(/favorite mocked s1/i);

    fireEvent.change(screen.getByPlaceholderText(/busque por nome/i), {
      target: { value: 'Prime' },
    });
    expect(screen.getByText(/barbershops mocked prime/i)).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /add favorite/i }));
    await waitFor(() => expect(addFavoriteBarbershop).toHaveBeenCalledWith('s2'));

    fireEvent.click(screen.getByRole('button', { name: /remove favorite/i }));
    await waitFor(() => expect(removeFavoriteBarbershop).toHaveBeenCalledWith('s1'));

    fireEvent.click(screen.getByRole('button', { name: /header logout/i }));
    expect(logoutUser).toHaveBeenCalled();
    expect(navigate).toHaveBeenCalledWith('/');
    expect(localStorage.getItem('userName')).toBe('Cliente Teste');
  });

  it('redirects barber users to barber home', () => {
    vi.mocked(isBarber).mockReturnValue(true);

    render(<HomePage />);

    expect(navigate).toHaveBeenCalledWith('/barberHome', { replace: true });
    expect(getMyProfile).not.toHaveBeenCalled();
  });

  it('requests geolocation and stores user location', async () => {
    vi.stubGlobal('navigator', {
      geolocation: {
        getCurrentPosition: (success) => success({ coords: { latitude: -23.5, longitude: -46.6 } }),
      },
    });

    render(<HomePage />);

    fireEvent.click(screen.getByRole('button', { name: /usar minha localização/i }));

    expect(toastSuccess).toHaveBeenCalledWith('Localização obtida! Exibindo barbearias próximas.');
    expect(sessionStorage.getItem('userLocation')).toBe(JSON.stringify({ lat: -23.5, lng: -46.6 }));
  });

  it('shows toast when favorite update fails or geolocation is unavailable', async () => {
    vi.mocked(addFavoriteBarbershop).mockRejectedValueOnce(new Error('failed'));
    vi.stubGlobal('navigator', {
      geolocation: {
        getCurrentPosition: (_success, error) => error(),
      },
    });

    render(<HomePage />);

    fireEvent.click(screen.getByRole('button', { name: /add favorite/i }));
    await waitFor(() => expect(toastError).toHaveBeenCalledWith('Não foi possível atualizar suas favoritas agora.'));

    fireEvent.click(screen.getByRole('button', { name: /usar minha localização/i }));
    expect(toastInfo).toHaveBeenCalledWith('Localização não disponível. Exibindo todas as barbearias.');
  });
});
