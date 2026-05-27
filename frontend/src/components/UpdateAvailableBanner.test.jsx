import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';

import UpdateAvailableBanner from './UpdateAvailableBanner';

describe('UpdateAvailableBanner', () => {
  it('shows update copy and dispatches actions', () => {
    const onDismiss = vi.fn();
    const onUpdateNow = vi.fn();

    render(<UpdateAvailableBanner onDismiss={onDismiss} onUpdateNow={onUpdateNow} />);

    expect(screen.getByRole('status')).toHaveTextContent('Nova versão disponível');
    fireEvent.click(screen.getByRole('button', { name: /depois/i }));
    fireEvent.click(screen.getByRole('button', { name: /atualizar agora/i }));

    expect(onDismiss).toHaveBeenCalledTimes(1);
    expect(onUpdateNow).toHaveBeenCalledTimes(1);
  });
});
