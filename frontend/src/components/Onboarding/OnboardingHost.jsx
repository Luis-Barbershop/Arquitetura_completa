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
  'customer-home': 'Home do cliente',
  'customer-barbershop-detail': 'Detalhe da barbearia',
  'customer-booking': 'Agendamento',
  'customer-appointments': 'Meus agendamentos',
  'customer-profile': 'Perfil do cliente',
  'barber-home': 'Home do profissional',
  'barber-services': 'Habilidades',
  'owner-services': 'Serviços da barbearia',
  'barber-unavailability': 'Indisponibilidade',
  'barber-profile': 'Perfil do barbeiro',
  'barber-manual-booking': 'Novo encaixe',
  'barber-appointments': 'Minha agenda',
  'owner-stock': 'Estoque',
  'owner-team': 'Meu time',
  'owner-dashboard': 'Dashboard',
  'owner-manage-shop': 'Gerenciar barbearia',
  'owner-team-agenda': 'Agenda da equipe',
  'barber-create-shop': 'Criar barbearia',
};

function OnboardingHost() {
  const location = useLocation();
  const [isOpen, setIsOpen] = useState(false);
  const [hydrationReady, setHydrationReady] = useState(false);

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

    const hydrate = async () => {
      if (!userScope) {
        if (isMounted) setHydrationReady(false);
        return;
      }

      await hydrateOnboardingFromRemote({ userScope });
      if (isMounted) setHydrationReady(true);
    };

    setHydrationReady(false);
    hydrate();

    return () => {
      isMounted = false;
    };
  }, [userScope]);

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

  const close = () => setIsOpen(false);

  const complete = () => {
    if (userScope && roleVariant && pageKey) {
      markPageOnboardingCompleted({
        userScope,
        roleVariant,
        pageKey,
      });
      void syncOnboardingToRemote({ userScope });
    }
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
