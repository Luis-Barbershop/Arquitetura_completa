import { renderHook, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const navigate = vi.fn();

vi.mock('react-router-dom', () => ({
  useNavigate: () => navigate,
}));

import { useAuthGuard } from './useAuthGuard';

describe('useAuthGuard', () => {
  beforeEach(() => {
    navigate.mockReset();
    localStorage.clear();
  });

  it('redirects unauthenticated users', async () => {
    const { result } = renderHook(() => useAuthGuard({ redirectIfUnauth: '/login' }));

    await waitFor(() => expect(navigate).toHaveBeenCalledWith('/login', { replace: true }));
    expect(result.current.isAuthorized).toBe(false);
  });

  it('authorizes an allowed barber owner', async () => {
    localStorage.setItem('token', 'token');
    localStorage.setItem('userRole', 'ROLE_BARBER');
    localStorage.setItem('isOwner', 'true');

    const { result } = renderHook(() => useAuthGuard({ requireOwner: true }));

    await waitFor(() => expect(result.current.isAuthorized).toBe(true));
    expect(navigate).not.toHaveBeenCalled();
  });

  it('redirects customers denied from barber routes', async () => {
    localStorage.setItem('token', 'token');
    localStorage.setItem('userRole', 'ROLE_CUSTOMER');

    renderHook(() => useAuthGuard({ allowCustomer: false, redirectIfCustomerDenied: '/homepage' }));

    await waitFor(() => expect(navigate).toHaveBeenCalledWith('/homepage', { replace: true }));
  });
});
