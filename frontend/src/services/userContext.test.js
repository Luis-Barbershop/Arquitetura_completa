import { describe, expect, it, beforeEach } from 'vitest';

import {
  getBarberAccess,
  getBarbershopId,
  getHomeRouteByRole,
  getUserRole,
  isBarber,
  isCustomer,
  isLoggedIn,
  isOwnerUser,
} from './userContext';

describe('userContext', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('reads the role and login state from localStorage', () => {
    expect(getUserRole()).toBeNull();
    expect(isLoggedIn()).toBe(false);

    localStorage.setItem('userRole', 'ROLE_CUSTOMER');
    localStorage.setItem('token', 'token');

    expect(getUserRole()).toBe('ROLE_CUSTOMER');
    expect(isCustomer()).toBe(true);
    expect(isBarber()).toBe(false);
    expect(isLoggedIn()).toBe(true);
    expect(getHomeRouteByRole()).toBe('/homepage');
  });

  it('identifies barber and owner permissions correctly', () => {
    localStorage.setItem('userRole', 'ROLE_BARBER');
    localStorage.setItem('isOwner', 'true');
    localStorage.setItem('barbershopId', '42');

    expect(isBarber()).toBe(true);
    expect(isOwnerUser()).toBe(true);
    expect(getBarbershopId()).toBe('42');
    expect(getHomeRouteByRole()).toBe('/barberHome');
    expect(getBarberAccess()).toEqual({
      canManageTeam: true,
      canManageStock: true,
      canViewDashboard: true,
      canManageShopServices: true,
    });
  });

  it('keeps owner access disabled for a non-owner barber', () => {
    localStorage.setItem('userRole', 'ROLE_BARBER');
    localStorage.setItem('isOwner', 'false');

    expect(isOwnerUser()).toBe(false);
    expect(getBarberAccess()).toEqual({
      canManageTeam: false,
      canManageStock: false,
      canViewDashboard: false,
      canManageShopServices: false,
    });
  });
});