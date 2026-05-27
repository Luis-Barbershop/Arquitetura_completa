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

vi.mock('../../services/barbershopService', () => ({
  createBarbershop: vi.fn(),
}));

vi.mock('../../services/authService', () => ({
  refreshSession: vi.fn(),
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

import { refreshSession } from '../../services/authService';
import { createBarbershop } from '../../services/barbershopService';
import CreateBarbershopPage from '../CreateBarbershopPage';

describe('CreateBarbershopPage', () => {
  beforeEach(() => {
    navigate.mockReset();
    toastError.mockReset();
    toastSuccess.mockReset();
    toastWarn.mockReset();
    vi.mocked(createBarbershop).mockReset();
    vi.mocked(refreshSession).mockReset();
    global.fetch = vi.fn();
    vi.stubGlobal('URL', {
      createObjectURL: vi.fn(() => 'blob:preview'),
      revokeObjectURL: vi.fn(),
    });
  });

  it('consults CEP, builds the full address and creates the barbershop', async () => {
    vi.mocked(global.fetch).mockResolvedValueOnce({
      json: async () => ({
        logradouro: 'Rua das Navalhas',
        bairro: 'Centro',
        localidade: 'Sao Paulo',
        uf: 'SP',
      }),
    });
    vi.mocked(createBarbershop).mockResolvedValueOnce({});
    vi.mocked(refreshSession).mockResolvedValueOnce({});

    render(<CreateBarbershopPage />);

    fireEvent.change(screen.getByPlaceholderText(/barbearia central/i), {
      target: { value: 'Barbearia Nova' },
    });
    fireEvent.change(screen.getByPlaceholderText('00.000.000/0001-00'), {
      target: { value: '12345678000190' },
    });

    const cepInput = screen.getByPlaceholderText('00000-000');
    fireEvent.change(cepInput, { target: { value: '01001000' } });
    fireEvent.blur(cepInput);

    expect(await screen.findByDisplayValue('Rua das Navalhas')).toBeInTheDocument();
    expect(screen.getByDisplayValue('Centro')).toBeInTheDocument();
    expect(screen.getByDisplayValue('Sao Paulo')).toBeInTheDocument();
    expect(screen.getByDisplayValue('SP')).toBeInTheDocument();

    fireEvent.change(screen.getByPlaceholderText(/ex: 123/i), { target: { value: '55' } });
    fireEvent.change(screen.getByPlaceholderText(/sala, loja/i), { target: { value: 'Sala 2' } });
    fireEvent.click(screen.getByRole('button', { name: /confirmar criação/i }));

    await waitFor(() => expect(createBarbershop).toHaveBeenCalledWith({
      name: 'Barbearia Nova',
      cnpj: '12.345.678/0001-90',
      address: 'Rua das Navalhas, 55, Sala 2, Centro, Sao Paulo - SP, CEP: 01001-000',
    }, null));
    expect(refreshSession).toHaveBeenCalled();
    expect(toastSuccess).toHaveBeenCalledWith('Barbearia criada com sucesso!');
    expect(navigate).toHaveBeenCalledWith('/barberHome', {
      replace: true,
      state: { activeTab: 'home' },
    });
  });

  it('validates address data before submit and supports logo crop', async () => {
    const { container } = render(<CreateBarbershopPage />);

    fireEvent.submit(container.querySelector('form'));
    expect(toastWarn).toHaveBeenCalledWith('Consulte um CEP válido antes de continuar.');

    const fileInput = container.querySelector('input[type="file"]');
    fireEvent.change(fileInput, {
      target: { files: [new File(['logo'], 'logo.png', { type: 'image/png' })] },
    });

    expect(screen.getByRole('dialog', { name: /ajustar logo da barbearia/i })).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /confirmar crop/i }));

    await waitFor(() => expect(screen.getByAltText(/preview da logo/i)).toBeInTheDocument());
    expect(URL.createObjectURL).toHaveBeenCalledTimes(2);

    fireEvent.click(screen.getByRole('button', { name: /cancelar/i }));
    expect(navigate).toHaveBeenCalledWith('/barberHome');
  });
});
