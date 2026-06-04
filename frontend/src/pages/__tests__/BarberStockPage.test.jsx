import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

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

vi.mock('../../services/api', () => ({
  default: {
    delete: vi.fn(),
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
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
      <button onClick={() => onTabChange('gerenciar-barbearia')}>Go gerenciar</button>
    </div>
  ),
}));

vi.mock('../../components/BarberPage/BarberNavbar', () => ({
  default: ({ onTabChange }) => <button onClick={() => onTabChange('home')}>Go home</button>,
}));

vi.mock('../../components/StockMovementModal/StockMovementModal', () => ({
  default: ({ product, onClose, onConfirm }) => (
    <div>
      <span>Movement for {product.name}</span>
      <button onClick={() => onConfirm({ productId: product.id, type: 'IN', quantity: 2 })}>Confirm movement</button>
      <button onClick={onClose}>Close movement</button>
    </div>
  ),
}));

import api from '../../services/api';
import { logoutUser } from '../../services/authService';
import { isCustomer, isOwnerUser } from '../../services/userContext';
import BarberStockPage from '../BarberStockPage';

describe('BarberStockPage', () => {
  beforeEach(() => {
    navigate.mockReset();
    toastError.mockReset();
    toastInfo.mockReset();
    toastSuccess.mockReset();
    localStorage.clear();
    vi.mocked(api.delete).mockReset();
    vi.mocked(api.get).mockReset();
    vi.mocked(api.post).mockReset();
    vi.mocked(api.put).mockReset();
    vi.mocked(logoutUser).mockReset();
    vi.mocked(isCustomer).mockReset();
    vi.mocked(isOwnerUser).mockReset();
    vi.mocked(isCustomer).mockReturnValue(false);
    vi.mocked(isOwnerUser).mockReturnValue(true);
    localStorage.setItem('token', 'token');
    window.confirm = vi.fn(() => true);
  });

  const mockStockLoad = () => {
    vi.mocked(api.get).mockImplementation((url) => {
      const responses = {
        '/auth/me': { id: 'barber-1', name: 'Owner', barbershopId: 'shop-1' },
        '/products': [
          {
            id: 'p1',
            name: 'Pomada',
            categoryId: 'c1',
            categoryName: 'Finalizacao',
            stockQuantity: 2,
            minStockQuantity: 3,
            price: 25,
          },
        ],
        '/products/categories': [{ id: 'c1', name: 'Finalizacao' }],
      };
      return Promise.resolve({ data: responses[url] });
    });
  };

  it('loads stock, creates product/category, updates category, moves and deletes product', async () => {
    mockStockLoad();
    vi.mocked(api.post).mockImplementation((url) => {
      if (url === '/products') {
        return Promise.resolve({
          data: {
            id: 'p2',
            name: 'Shampoo',
            categoryId: 'c1',
            categoryName: 'Finalizacao',
            stockQuantity: 5,
            minStockQuantity: 2,
            price: 30,
          },
        });
      }
      if (url === '/products/categories') {
        return Promise.resolve({ data: { id: 'c2', name: 'Barba' } });
      }
      return Promise.resolve({});
    });
    vi.mocked(api.put).mockResolvedValue({ data: { id: 'c1', name: 'Finalizacao premium' } });
    vi.mocked(api.delete).mockResolvedValue({});

    render(<BarberStockPage />);

    expect(await screen.findByText('Pomada')).toBeInTheDocument();
    expect(screen.getByText('2 un.')).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText(/nome do produto/i), { target: { value: 'Shampoo' } });
    fireEvent.change(screen.getByLabelText(/categoria/i), { target: { value: 'c1' } });
    fireEvent.change(screen.getByLabelText(/valor unitario/i), { target: { value: '30' } });
    fireEvent.change(screen.getByLabelText(/quantidade inicial/i), { target: { value: '5' } });
    fireEvent.change(screen.getByLabelText(/quantidade minima/i), { target: { value: '2' } });
    fireEvent.click(screen.getByRole('button', { name: /adicionar produto/i }));

    await waitFor(() => expect(api.post).toHaveBeenCalledWith('/products', {
      barbershopId: 'shop-1',
      name: 'Shampoo',
      categoryId: 'c1',
      stockQuantity: 5,
      minStockQuantity: 2,
      price: 30,
    }));

    fireEvent.click(screen.getByRole('button', { name: /categorias/i }));
    fireEvent.change(screen.getByPlaceholderText(/ex: finalizacao/i), { target: { value: 'Barba' } });
    fireEvent.click(screen.getByRole('button', { name: /criar categoria/i }));

    await waitFor(() => expect(api.post).toHaveBeenCalledWith('/products/categories', { name: 'Barba' }, {
      params: { barbershopId: 'shop-1' },
    }));

    fireEvent.click(screen.getAllByRole('button', { name: /editar categoria/i })[1]);
    fireEvent.change(screen.getByDisplayValue('Finalizacao'), { target: { value: 'Finalizacao premium' } });
    fireEvent.click(screen.getByRole('button', { name: /^salvar$/i }));
    await waitFor(() => expect(api.put).toHaveBeenCalledWith('/products/categories/c1', { name: 'Finalizacao premium' }, {
      params: { barbershopId: 'shop-1' },
    }));

    fireEvent.click(screen.getByRole('button', { name: /produtos/i }));
    fireEvent.click(screen.getAllByRole('button', { name: /movimentar estoque/i })[0]);
    expect(screen.getByText(/movement for/i)).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /confirm movement/i }));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith('/products/stock-movements', {
      productId: 'p2',
      type: 'IN',
      quantity: 2,
    }));

    fireEvent.click(screen.getAllByRole('button', { name: /excluir item/i })[0]);
    await waitFor(() => expect(api.delete).toHaveBeenCalledWith('/products/p1'));

    fireEvent.click(screen.getByText('Go gerenciar'));
    expect(navigate).toHaveBeenCalledWith('/barberHome/gerenciar-barbearia');
  });

  it('redirects customer or non-owner users', () => {
    vi.mocked(isCustomer).mockReturnValueOnce(true);

    render(<BarberStockPage />);

    expect(navigate).toHaveBeenCalledWith('/homepage', { replace: true });
  });
});
