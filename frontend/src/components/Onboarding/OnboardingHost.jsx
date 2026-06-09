import { useEffect, useMemo, useState } from 'react';
import { useLocation } from 'react-router-dom';
import OnboardingTour from './OnboardingTour';
import { getOnboardingSteps } from '../../services/onboardingCatalog';
import {
  ONBOARDING_REPLAY_EVENT,
  getCurrentRoleVariant,
  getCurrentUserScope,
  hydrateOnboardingFromRemote,
  isPageOnboardingCompleted,
  markPageOnboardingCompleted,
  resolvePageKeyFromLocation,
  syncOnboardingToRemote,
} from '../../services/onboardingService';

const PAGE_TITLES = {
  'customer-home': 'Sua casa',
  'customer-barbershop-detail': 'Vitrine da loja',
  'customer-booking': 'Marcação',
  'customer-appointments': 'Sua agenda',
  'customer-profile': 'Seu perfil',
  'barber-home': 'Painel rápido',
  'barber-home-unlinked': 'Começo do pro',
  'barber-services': 'Seus serviços',
  'owner-services': 'Catálogo da loja',
  'barber-unavailability': 'Bloqueios',
  'barber-profile': 'Perfil pro',
  'barber-manual-booking': 'Novo encaixe',
  'barber-appointments': 'Agenda do dia',
  'owner-stock': 'Estoque',
  'owner-team': 'Time da loja',
  'owner-dashboard': 'Painel de números',
  'owner-manage-shop': 'Dados da loja',
  'owner-team-agenda': 'Agenda da equipe',
  'barber-create-shop': 'Abrir sua loja',
};

function OnboardingHost() {
  const location = useLocation();
  const [isOpen, setIsOpen] = useState(false);
  const [hydrationReady, setHydrationReady] = useState(false);
  const [authVersion, setAuthVersion] = useState(0);

  const userScope = getCurrentUserScope();
  const roleVariant = getCurrentRoleVariant();

  const pageKey = useMemo(
    () => resolvePageKeyFromLocation(location, roleVariant),
    [location, roleVariant]
  );

  const steps = useMemo(() => {
    if (!pageKey) return [];
    return getOnboardingSteps(pageKey);
  }, [pageKey]);

  useEffect(() => {
    let isMounted = true;

    const handleAuthContextChange = () => {
      setAuthVersion((previousVersion) => previousVersion + 1);
      setIsOpen(false);
    };

    window.addEventListener('cortaai:login-success', handleAuthContextChange);
    window.addEventListener('cortaai:logout', handleAuthContextChange);

    const hydrate = async () => {
      if (!userScope) {
        if (isMounted) setHydrationReady(false);
        return;
      }

      const hydrated = await hydrateOnboardingFromRemote({ userScope, force: authVersion > 0 });
      if (isMounted) setHydrationReady(hydrated);
    };

    setHydrationReady(false);
    hydrate();

    return () => {
      isMounted = false;
      window.removeEventListener('cortaai:login-success', handleAuthContextChange);
      window.removeEventListener('cortaai:logout', handleAuthContextChange);
    };
  }, [authVersion, userScope]);

  useEffect(() => {
    if (!hydrationReady || !userScope || !roleVariant || !pageKey || steps.length === 0) {
      setIsOpen(false);
      return;
    }

    const completed = isPageOnboardingCompleted({
      userScope,
      roleVariant,
      pageKey,
    });

    setIsOpen(!completed);
  }, [hydrationReady, pageKey, roleVariant, steps.length, userScope]);

  useEffect(() => {
    const handleReplay = (event) => {
      if (!pageKey || steps.length === 0) return;

      const requestedPageKey = event?.detail?.pageKey;
      if (requestedPageKey && requestedPageKey !== pageKey) return;

      setIsOpen(true);
    };

    window.addEventListener(ONBOARDING_REPLAY_EVENT, handleReplay);
    return () => window.removeEventListener(ONBOARDING_REPLAY_EVENT, handleReplay);
  }, [pageKey, steps.length]);

  const persistCompletion = () => {
    if (userScope && roleVariant && pageKey) {
      markPageOnboardingCompleted({
        userScope,
        roleVariant,
        pageKey,
      });
      void syncOnboardingToRemote({ userScope });
    }
  };

  const close = () => {
    persistCompletion();
    setIsOpen(false);
  };

  const complete = () => {
    persistCompletion();
    setIsOpen(false);
  };

  if (!pageKey || steps.length === 0) return null;

  return (
    <OnboardingTour
      open={isOpen}
      pageTitle={PAGE_TITLES[pageKey] || 'Onboarding'}
      steps={steps}
      onSkip={close}
      onClose={close}
      onComplete={complete}
    />
  );
}

export default OnboardingHost;
