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

vi.mock('../../services/api', () => ({
  default: {
    get: vi.fn(),
  },
}));

vi.mock('../../services/authService', () => ({
  logoutUser: vi.fn(),
}));

vi.mock('../../services/barberBlockService', () => ({
  createBarberBlock: vi.fn(),
  deleteBarberBlock: vi.fn(),
  getBarberBlocks: vi.fn(),
}));

vi.mock('../../services/userContext', () => ({
  isCustomer: vi.fn(),
  isOwnerUser: vi.fn(() => true),
}));

vi.mock('../../services/navigationService', () => ({
  navigateToBarberTab: vi.fn((_tab, nav) => nav('/barberHome')),
}));

vi.mock('../../components/BarberPage/BarberHeader', () => ({
  default: ({ onLogout, onTabChange }) => (
    <div>
      <button onClick={onLogout}>Header logout</button>
      <button onClick={() => onTabChange('home')}>Header home</button>
    </div>
  ),
}));

vi.mock('../../components/BarberPage/BarberNavbar', () => ({
  default: ({ onTabChange }) => <button onClick={() => onTabChange('home')}>Navbar home</button>,
}));

import api from '../../services/api';
import { logoutUser } from '../../services/authService';
import { createBarberBlock, deleteBarberBlock, getBarberBlocks } from '../../services/barberBlockService';
import { isCustomer } from '../../services/userContext';
import BarberBlockPage from '../BarberBlockPage';

describe('BarberBlockPage', () => {
  beforeEach(() => {
    navigate.mockReset();
    toastError.mockReset();
    toastSuccess.mockReset();
    toastWarn.mockReset();
    localStorage.clear();
    vi.mocked(api.get).mockReset();
    vi.mocked(logoutUser).mockReset();
    vi.mocked(createBarberBlock).mockReset();
    vi.mocked(deleteBarberBlock).mockReset();
    vi.mocked(getBarberBlocks).mockReset();
    vi.mocked(isCustomer).mockReset();
    vi.mocked(isCustomer).mockReturnValue(false);
    localStorage.setItem('token', 'token');
    vi.mocked(api.get).mockResolvedValue({ data: { id: 'barber-1', name: 'Barbeiro', barbershopId: 'shop-1' } });
    vi.mocked(getBarberBlocks).mockResolvedValue([
      { id: 'block-1', startTime: '2026-05-22T12:00:00', endTime: '2026-05-22T13:00:00', reason: 'Almoco' },
    ]);
  });

  it('loads blocks, creates hour/day blocks and removes a block', async () => {
    vi.mocked(createBarberBlock).mockResolvedValue();
    vi.mocked(deleteBarberBlock).mockResolvedValue();

    render(<BarberBlockPage />);

    expect(await screen.findByText('Almoco')).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText(/motivo/i), { target: { value: 'Consulta' } });
    fireEvent.click(screen.getByRole('button', { name: /bloquear horário/i }));

    await waitFor(() => expect(createBarberBlock).toHaveBeenCalledWith(expect.objectContaining({
      barberId: 'barber-1',
      reason: 'Consulta',
    })));
    expect(toastSuccess).toHaveBeenCalledWith('Bloqueio criado.');

    fireEvent.click(screen.getByRole('button', { name: /bloquear dia/i }));
    await waitFor(() => expect(toastSuccess).toHaveBeenCalledWith('Dia bloqueado.'));

    fireEvent.click(screen.getByRole('button', { name: /remover bloqueio/i }));
    await waitFor(() => expect(deleteBarberBlock).toHaveBeenCalledWith('block-1'));
  });

  it('validates invalid hour and redirects customer users', async () => {
    render(<BarberBlockPage />);

    await screen.findByText('Almoco');
    fireEvent.change(screen.getByLabelText(/fim/i), { target: { value: '11:00' } });
    fireEvent.click(screen.getByRole('button', { name: /bloquear horário/i }));
    expect(toastWarn).toHaveBeenCalledWith('O horário final precisa ser maior que o inicial.');
  });

  it('redirects customer users', () => {
    vi.mocked(isCustomer).mockReturnValueOnce(true);

    render(<BarberBlockPage />);

    expect(navigate).toHaveBeenCalledWith('/homepage', { replace: true });
  });
});
