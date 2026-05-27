import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';

import StockMovementModal from './StockMovementModal';

describe('StockMovementModal', () => {
  const product = { id: 'p1', name: 'Pomada' };

  it('does not render without a product', () => {
    const { container } = render(<StockMovementModal product={null} />);
    expect(container).toBeEmptyDOMElement();
  });

  it('submits a consumption movement with normalized payload', async () => {
    const onConfirm = vi.fn().mockResolvedValue();

    render(<StockMovementModal product={product} onClose={vi.fn()} onConfirm={onConfirm} />);

    fireEvent.change(screen.getByLabelText(/quantidade/i), { target: { value: '3' } });
    fireEvent.change(screen.getByLabelText(/observacao/i), { target: { value: '  usado no atendimento  ' } });
    fireEvent.click(screen.getByRole('button', { name: /registrar/i }));

    await waitFor(() => expect(onConfirm).toHaveBeenCalledWith({
      productId: 'p1',
      type: 'OUT_CONSUMPTION',
      quantity: 3,
      unitSalePrice: null,
      notes: 'usado no atendimento',
    }));
  });

  it('requires sale price for sale movements', async () => {
    const onConfirm = vi.fn().mockResolvedValue();

    render(<StockMovementModal product={product} onClose={vi.fn()} onConfirm={onConfirm} />);

    fireEvent.click(screen.getByRole('button', { name: /venda/i }));
    fireEvent.click(screen.getByRole('button', { name: /registrar/i }));
    expect(onConfirm).not.toHaveBeenCalled();

    fireEvent.change(screen.getByLabelText(/preco unitario/i), { target: { value: '25.5' } });
    fireEvent.click(screen.getByRole('button', { name: /registrar/i }));

    await waitFor(() => expect(onConfirm).toHaveBeenCalledWith(expect.objectContaining({
      type: 'OUT_SALE',
      unitSalePrice: 25.5,
    })));
  });
});
