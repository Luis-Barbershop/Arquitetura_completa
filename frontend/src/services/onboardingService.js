import { isBarber, isCustomer, isLoggedIn, isOwnerUser } from './userContext';
import api from './api';

const STORAGE_KEY = 'cortaai:onboarding-progress';
const ONBOARDING_VERSION = 1;
const REMOTE_PROGRESS_ENDPOINT = '/auth/me/onboarding-progress';
const ROLE_COMPLETION_PAGE_KEY = '__role-onboarding-completed__';
export const ONBOARDING_REPLAY_EVENT = 'cortaai:onboarding-replay';

const hydratedUsers = new Set();

const createEmptyState = () => ({
  version: ONBOARDING_VERSION,
  users: {},
});

const normalizeState = (raw) => {
  if (!raw || typeof raw !== 'object') {
    return createEmptyState();
  }

  if (raw.version !== ONBOARDING_VERSION || typeof raw.users !== 'object' || raw.users === null) {
    return createEmptyState();
  }

  return raw;
};

const readState = () => {
  try {
    const parsed = JSON.parse(localStorage.getItem(STORAGE_KEY) || 'null');
    return normalizeState(parsed);
  } catch {
    return createEmptyState();
  }
};

const writeState = (state) => {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
};

const ensureUserRoleState = (state, userScope, roleVariant) => {
  if (!state.users[userScope]) {
    state.users[userScope] = {};
  }

  if (!state.users[userScope][roleVariant]) {
    state.users[userScope][roleVariant] = { completedPages: {} };
  }

  if (!state.users[userScope][roleVariant].completedPages) {
    state.users[userScope][roleVariant].completedPages = {};
  }
};

const getCompletedPages = (state, userScope, roleVariant) => (
  state.users?.[userScope]?.[roleVariant]?.completedPages || {}
);

export const isRoleOnboardingCompleted = ({ userScope, roleVariant }) => {
  if (!userScope || !roleVariant) return false;

  const state = readState();
  const completedPages = getCompletedPages(state, userScope, roleVariant);
  return Object.keys(completedPages).length > 0;
};

const getLocalSnapshotForUser = (userScope) => {
  const state = readState();
  return {
    version: ONBOARDING_VERSION,
    progressByRole: state.users?.[userScope] || {},
  };
};

const mergeRemoteSnapshotForUser = (userScope, remoteSnapshot) => {
  if (!userScope || !remoteSnapshot || typeof remoteSnapshot !== 'object') return;

  const state = readState();
  if (!state.users[userScope]) {
    state.users[userScope] = {};
  }

  const progressByRole = remoteSnapshot.progressByRole;
  if (!progressByRole || typeof progressByRole !== 'object') {
    writeState(state);
    return;
  }

  Object.entries(progressByRole).forEach(([roleVariant, rolePayload]) => {
    if (!rolePayload || typeof rolePayload !== 'object') return;

    ensureUserRoleState(state, userScope, roleVariant);
    const completedPages = rolePayload.completedPages;
    if (!completedPages || typeof completedPages !== 'object') return;

    Object.entries(completedPages).forEach(([pageKey, value]) => {
      if (!pageKey) return;
      state.users[userScope][roleVariant].completedPages[pageKey] =
        value && typeof value === 'object'
          ? value
          : { completedAt: new Date().toISOString() };
    });
  });

  writeState(state);
};

export const getCurrentUserScope = () => {
  const internalUserId = localStorage.getItem('internalUserId');
  if (internalUserId) return `internal:${internalUserId}`;

  const userId = localStorage.getItem('userId');
  if (userId) return `firebase:${userId}`;

  const email = localStorage.getItem('userEmail');
  if (email) return `email:${email.toLowerCase()}`;

  return null;
};

export const getCurrentRoleVariant = () => {
  if (!isLoggedIn()) return null;
  if (isCustomer()) return 'customer';
  if (isOwnerUser()) return 'owner';
  if (isBarber()) return 'barber';
  return null;
};

const hasLinkedBarbershop = () => {
  const barbershopId = localStorage.getItem('barbershopId');
  return Boolean(barbershopId && String(barbershopId).trim() !== '');
};

export const resolvePageKeyFromLocation = ({ pathname, search }, roleVariant) => {
  if (!pathname || !roleVariant) return null;

  if (pathname === '/homepage') return 'customer-home';
  if (pathname.startsWith('/barbearia/')) return 'customer-barbershop-detail';
  if (pathname.startsWith('/agendamentoPage/')) return 'customer-booking';
  if (pathname === '/homepage/perfil') return 'customer-profile';

  if (pathname === '/meus-agendamentos') {
    const params = new URLSearchParams(search || '');
    const teamView = params.get('view') === 'team';

    if (roleVariant === 'customer') return 'customer-appointments';
    if (roleVariant === 'owner' && teamView) return 'owner-team-agenda';
    return 'barber-appointments';
  }

  if (pathname === '/barberHome') return hasLinkedBarbershop() ? 'barber-home' : 'barber-home-unlinked';
  if (pathname === '/barberHome/servicos') return roleVariant === 'owner' ? 'owner-services' : 'barber-services';
  if (pathname === '/barber/indisponibilidade') return 'barber-unavailability';
  if (pathname === '/barberHome/perfil') return 'barber-profile';
  if (pathname === '/barberHome/novo-agendamento') return 'barber-manual-booking';
  if (pathname === '/create-barbershop') return 'barber-create-shop';

  if (pathname === '/barberHome/estoque') return 'owner-stock';
  if (pathname === '/barberHome/time') return 'owner-team';
  if (pathname === '/barberHome/dashboard') return 'owner-dashboard';
  if (pathname === '/barberHome/gerenciar-barbearia') return 'owner-manage-shop';
  if (pathname === '/barberHome/agenda-barbearia' || pathname === '/barberHome/agenda-equipe') return 'owner-team-agenda';

  return null;
};

export const isPageOnboardingCompleted = ({ userScope, roleVariant, pageKey }) => {
  if (!userScope || !roleVariant || !pageKey) return false;

  const state = readState();
  return Boolean(
    isRoleOnboardingCompleted({ userScope, roleVariant })
      || getCompletedPages(state, userScope, roleVariant)[pageKey]
  );
};

export const markPageOnboardingCompleted = ({ userScope, roleVariant, pageKey }) => {
  if (!userScope || !roleVariant || !pageKey) return;

  const state = readState();
  ensureUserRoleState(state, userScope, roleVariant);

  state.users[userScope][roleVariant].completedPages[pageKey] = {
    completedAt: new Date().toISOString(),
  };

  writeState(state);
};

export const markRoleOnboardingCompleted = ({ userScope, roleVariant, pageKey }) => {
  if (!userScope || !roleVariant) return;

  const state = readState();
  ensureUserRoleState(state, userScope, roleVariant);

  const completedAt = new Date().toISOString();
  state.users[userScope][roleVariant].completedPages[ROLE_COMPLETION_PAGE_KEY] = {
    completedAt,
  };

  if (pageKey) {
    state.users[userScope][roleVariant].completedPages[pageKey] = {
      completedAt,
    };
  }

  writeState(state);
};

export const resetPageOnboarding = ({ userScope, roleVariant, pageKey }) => {
  if (!userScope || !roleVariant || !pageKey) return;

  const state = readState();
  const completedPages = state.users?.[userScope]?.[roleVariant]?.completedPages;

  if (!completedPages || !completedPages[pageKey]) return;

  delete completedPages[pageKey];
  writeState(state);
};

export const hydrateOnboardingFromRemote = async ({ userScope, force = false }) => {
  if (!userScope || !isLoggedIn()) return false;
  if (hydratedUsers.has(userScope) && !force) return true;

  try {
    const response = await api.get(REMOTE_PROGRESS_ENDPOINT);
    mergeRemoteSnapshotForUser(userScope, response?.data);
    hydratedUsers.add(userScope);
    return true;
  } catch {
    return false;
  }
};

export const syncOnboardingToRemote = async ({ userScope }) => {
  if (!userScope || !isLoggedIn()) return false;

  try {
    await api.put(REMOTE_PROGRESS_ENDPOINT, getLocalSnapshotForUser(userScope));
    return true;
  } catch {
    return false;
  }
};

export const requestOnboardingReplay = (pageKey = null) => {
  window.dispatchEvent(
    new CustomEvent(ONBOARDING_REPLAY_EVENT, {
      detail: { pageKey },
    })
  );
};
