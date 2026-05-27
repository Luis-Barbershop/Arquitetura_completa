import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const navigate = vi.fn();

vi.mock('react-router-dom', () => ({
  useNavigate: () => navigate,
}));

vi.mock('../../services/barbershopService', () => ({
  getAllBarbershops: vi.fn(),
  getBarbershops: vi.fn(),
  getShopServices: vi.fn(),
}));

import {
  getAllBarbershops,
  getBarbershops,
  getShopServices,
} from '../../services/barbershopService';
import Barbershops from './Barbershops/Barbershops';
import ContainerBarbericons from './Barbershops/Container_Barbericons';
import FavoriteBarbershops from './Favorite_barbershops/Favorite_barbershops';
import SearchBar from './SearchBar';

describe('HomePage components', () => {
  beforeEach(() => {
    navigate.mockReset();
    vi.mocked(getAllBarbershops).mockReset();
    vi.mocked(getBarbershops).mockReset();
    vi.mocked(getShopServices).mockReset();
  });

  it('updates search term through SearchBar', () => {
    const onSearchChange = vi.fn();

    render(<SearchBar searchTerm="" onSearchChange={onSearchChange} />);

    fireEvent.change(screen.getByPlaceholderText(/busque por nome/i), {
      target: { value: 'Prime' },
    });

    expect(onSearchChange).toHaveBeenCalledWith('Prime');
  });

  it('loads barbershops, filters by search and toggles favorite', async () => {
    const onToggleFavorite = vi.fn();
    vi.mocked(getBarbershops).mockResolvedValueOnce([
      { id: 's1', name: 'Barbearia Prime', address: 'Rua A', averageRating: 4.5, reviewsCount: 8, distanceKm: 1.2 },
      { id: 's2', name: 'Outro Corte', address: 'Rua B' },
    ]);
    vi.mocked(getShopServices)
      .mockResolvedValueOnce([{ id: 'svc1' }])
      .mockResolvedValueOnce([]);

    render(
      <Barbershops
        searchTerm="prime"
        favoriteIds={['s1']}
        onToggleFavorite={onToggleFavorite}
        userLocation={{ lat: -23, lng: -46 }}
      />,
    );

    expect(await screen.findByText('Barbearia Prime')).toBeInTheDocument();
    expect(screen.queryByText('Outro Corte')).not.toBeInTheDocument();
    expect(screen.getByText(/4.5 estrelas/i)).toBeInTheDocument();
    expect(screen.getByText(/1 servicos disponiveis/i)).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /remover dos favoritos/i }));
    expect(onToggleFavorite).toHaveBeenCalledWith('s1', true);
  });

  it('shows empty state when no barbershop matches', async () => {
    vi.mocked(getBarbershops).mockResolvedValueOnce([{ id: 's1', name: 'Central', address: 'Rua A' }]);
    vi.mocked(getShopServices).mockResolvedValueOnce([]);

    render(<Barbershops searchTerm="inexistente" />);

    expect(await screen.findByText(/nenhuma barbearia encontrada/i)).toBeInTheDocument();
  });

  it('loads favorite barbershops and navigates to detail', async () => {
    vi.mocked(getAllBarbershops).mockResolvedValueOnce([
      { id: 's1', name: 'Favorita', logoUrl: 'logo.png' },
      { id: 's2', name: 'Outra' },
    ]);

    render(<FavoriteBarbershops favoriteIds={['s1']} />);

    fireEvent.click(await screen.findByRole('button', { name: /favorita/i }));
    expect(navigate).toHaveBeenCalledWith('/barbearia/s1');
  });

  it('handles favorite loading error', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => {});
    vi.mocked(getAllBarbershops).mockRejectedValueOnce(new Error('failed'));

    render(<FavoriteBarbershops favoriteIds={['s1']} />);

    expect(await screen.findByText(/nao foi possivel carregar/i)).toBeInTheDocument();
  });

  it('navigates from barbershop card and handles image fallback', () => {
    const onToggleFavorite = vi.fn();
    render(
      <ContainerBarbericons
        id="s1"
        name="Card Teste"
        address="Rua A"
        image="broken.png"
        isFavorite={false}
        onToggleFavorite={onToggleFavorite}
        rating={null}
        reviewsCount={0}
      />,
    );

    fireEvent.error(screen.getByAltText(/logo da card teste/i));
    expect(screen.getByAltText(/logo da card teste/i)).toHaveAttribute('src', './barbershop.png');

    fireEvent.click(screen.getByRole('button', { name: /adicionar aos favoritos/i }));
    expect(onToggleFavorite).toHaveBeenCalledWith('s1', false);

    fireEvent.click(screen.getByText('Card Teste'));
    expect(navigate).toHaveBeenCalledWith('/barbearia/s1');
  });
});
