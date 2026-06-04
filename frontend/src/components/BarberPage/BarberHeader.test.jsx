import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('react-router-dom', () => ({
  useNavigate: () => vi.fn(),
}));

vi.mock('../../services/api', () => ({
  default: {
    get: vi.fn().mockResolvedValue({ data: { linked: false, hasPublicKey: false } }),
  },
}));

vi.mock('../NotificationBell/NotificationBell', () => ({
  default: () => <div data-testid="notification-bell" />,
}));

import BarberHeader from './BarberHeader';

describe('BarberHeader', () => {
  beforeEach(() => {
    localStorage.clear();
    localStorage.setItem('isOwner', 'true');
    localStorage.setItem('userRole', 'ROLE_BARBER');
    localStorage.setItem('barbershopId', 'shop-1');
    localStorage.setItem('authProvider', 'EMAIL');
  });

  it('permite clicar em Gestao e navegar para Gerenciar Barbearia', async () => {
    const onTabChange = vi.fn();

    render(
      <BarberHeader
        barber={{ id: 'owner-1', name: 'Owner Teste', barbershopName: 'Barber Prime' }}
        activeTab="home"
        onTabChange={onTabChange}
      />
    );

    fireEvent.click(screen.getByRole('button', { name: /gestão/i }));
    fireEvent.click(await screen.findByRole('button', { name: /gerenciar barbearia/i }));

    await waitFor(() => {
      expect(onTabChange).toHaveBeenCalledWith('gerenciar-barbearia');
    });
  });
});
