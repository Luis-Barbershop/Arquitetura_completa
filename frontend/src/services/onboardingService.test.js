import { beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('./api', () => ({
  default: {
    get: vi.fn(),
    put: vi.fn(),
  },
}));

import api from './api';
import {
  getCurrentRoleVariant,
  getCurrentUserScope,
  hydrateOnboardingFromRemote,
  isPageOnboardingCompleted,
  markPageOnboardingCompleted,
  resolvePageKeyFromLocation,
  syncOnboardingToRemote,
} from './onboardingService';

const clearAuthStorage = () => {
  localStorage.clear();
};

describe('onboardingService', () => {
  beforeEach(() => {
    clearAuthStorage();
    vi.clearAllMocks();
  });

  it('resolve a chave da pagina conforme rota, papel e query', () => {
    expect(resolvePageKeyFromLocation({ pathname: '/homepage', search: '' }, 'customer')).toBe('customer-home');
    expect(resolvePageKeyFromLocation({ pathname: '/meus-agendamentos', search: '?view=team' }, 'owner')).toBe('owner-team-agenda');
    expect(resolvePageKeyFromLocation({ pathname: '/meus-agendamentos', search: '?view=team' }, 'barber')).toBe('barber-appointments');
    expect(resolvePageKeyFromLocation({ pathname: '/barberHome/estoque', search: '' }, 'owner')).toBe('owner-stock');
    expect(resolvePageKeyFromLocation({ pathname: '/rota-inexistente', search: '' }, 'customer')).toBeNull();
  });

  it('determina userScope e roleVariant a partir do localStorage', () => {
    localStorage.setItem('token', 'token-valido');
    localStorage.setItem('internalUserId', '123');
    localStorage.setItem('userRole', 'ROLE_CUSTOMER');

    expect(getCurrentUserScope()).toBe('internal:123');
    expect(getCurrentRoleVariant()).toBe('customer');

    localStorage.setItem('userRole', 'ROLE_BARBER');
    localStorage.setItem('isOwner', 'true');
    expect(getCurrentRoleVariant()).toBe('owner');
  });

  it('marca e consulta conclusão por usuário, papel e página', () => {
    const payload = {
      userScope: 'internal:999',
      roleVariant: 'customer',
      pageKey: 'customer-home',
    };

    expect(isPageOnboardingCompleted(payload)).toBe(false);
    markPageOnboardingCompleted(payload);
    expect(isPageOnboardingCompleted(payload)).toBe(true);

    expect(
      isPageOnboardingCompleted({
        userScope: 'internal:999',
        roleVariant: 'barber',
        pageKey: 'customer-home',
      })
    ).toBe(false);
  });

  it('hidrata progresso remoto e reflete no estado local', async () => {
    localStorage.setItem('token', 'token-valido');
    const userScope = 'internal:777';

    api.get.mockResolvedValueOnce({
      data: {
        version: 1,
        progressByRole: {
          customer: {
            completedPages: {
              'customer-home': {
                completedAt: '2026-05-29T10:00:00.000Z',
              },
            },
          },
        },
      },
    });

    const hydrated = await hydrateOnboardingFromRemote({ userScope, force: true });
    expect(hydrated).toBe(true);
    expect(api.get).toHaveBeenCalledTimes(1);

    expect(
      isPageOnboardingCompleted({
        userScope,
        roleVariant: 'customer',
        pageKey: 'customer-home',
      })
    ).toBe(true);
  });

  it('sincroniza snapshot local para endpoint remoto', async () => {
    localStorage.setItem('token', 'token-valido');
    const userScope = 'internal:555';

    markPageOnboardingCompleted({
      userScope,
      roleVariant: 'owner',
      pageKey: 'owner-dashboard',
    });

    api.put.mockResolvedValueOnce({ status: 200 });

    const synced = await syncOnboardingToRemote({ userScope });
    expect(synced).toBe(true);
    expect(api.put).toHaveBeenCalledTimes(1);

    const [, payload] = api.put.mock.calls[0];
    expect(payload).toMatchObject({
      version: 1,
      progressByRole: {
        owner: {
          completedPages: {
            'owner-dashboard': expect.any(Object),
          },
        },
      },
    });
  });
});
