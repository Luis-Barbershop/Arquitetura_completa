const BARBER_TAB_ROUTES = {
  home: '/barberHome',
  agenda: '/meus-agendamentos',
  'agenda-equipe': '/meus-agendamentos?view=team',
  servicos: '/barberHome/servicos',
  indisponibilidade: '/barber/indisponibilidade',
  estoque: '/barberHome/estoque',
  perfil: '/barberHome/perfil',
  time: '/barberHome/time',
  dashboards: '/barberHome/dashboard',
  'novo-agendamento': '/barberHome/novo-agendamento',
};

const OWNER_ONLY_TABS = new Set(['estoque', 'time', 'dashboards', 'agenda-equipe']);

export const resolveBarberRoute = (tab, options = {}) => {
  const { isOwner = false, currentPath = null } = options;

  if (!tab || typeof tab !== 'string') {
    return null;
  }

  if (OWNER_ONLY_TABS.has(tab) && !isOwner) {
    return '/barberHome';
  }

  const route = BARBER_TAB_ROUTES[tab] || null;

  if (!route) {
    return null;
  }

  if (currentPath && currentPath === route) {
    return null;
  }

  return route;
};

export const navigateToBarberTab = (tab, navigate, options = {}) => {
  if (typeof navigate !== 'function') {
    return;
  }

  const route = resolveBarberRoute(tab, options);
  if (route) {
    navigate(route);
  }
};

export const getAvailableBarberTabs = ({ isOwner = false } = {}) => {
  const tabs = Object.keys(BARBER_TAB_ROUTES);
  if (isOwner) {
    return tabs;
  }
  return tabs.filter((tab) => !OWNER_ONLY_TABS.has(tab));
};
