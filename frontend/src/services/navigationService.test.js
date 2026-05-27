import { describe, expect, it, vi } from 'vitest';

import { getAvailableBarberTabs, navigateToBarberTab, resolveBarberRoute } from './navigationService';

describe('navigationService', () => {
  it('resolves barber routes with owner restrictions', () => {
    expect(resolveBarberRoute('home')).toBe('/barberHome');
    expect(resolveBarberRoute('estoque', { isOwner: false })).toBe('/barberHome');
    expect(resolveBarberRoute('estoque', { isOwner: true })).toBe('/barberHome/estoque');
    expect(resolveBarberRoute('home', { currentPath: '/barberHome' })).toBeNull();
    expect(resolveBarberRoute('tab-inexistente')).toBeNull();
    expect(resolveBarberRoute(null)).toBeNull();
  });

  it('returns the available tabs according to ownership', () => {
    expect(getAvailableBarberTabs({ isOwner: false })).toEqual([
      'home',
      'agenda',
      'servicos',
      'indisponibilidade',
      'perfil',
      'novo-agendamento',
    ]);

    expect(getAvailableBarberTabs({ isOwner: true })).toContain('dashboards');
    expect(getAvailableBarberTabs({ isOwner: true })).toContain('estoque');
  });

  it('navigates only when the route exists', () => {
    const navigate = vi.fn();

    navigateToBarberTab('agenda', navigate);
    navigateToBarberTab('estoque', navigate, { isOwner: false });
    navigateToBarberTab('tab-inexistente', navigate);

    expect(navigate).toHaveBeenCalledTimes(2);
    expect(navigate).toHaveBeenNthCalledWith(1, '/meus-agendamentos');
    expect(navigate).toHaveBeenNthCalledWith(2, '/barberHome');
  });
});