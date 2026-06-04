import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter } from 'react-router-dom';

const onboardingApi = {
  getCurrentRoleVariant: vi.fn(),
  getCurrentUserScope: vi.fn(),
  hydrateOnboardingFromRemote: vi.fn(),
  isPageOnboardingCompleted: vi.fn(),
  markPageOnboardingCompleted: vi.fn(),
  resolvePageKeyFromLocation: vi.fn(),
  syncOnboardingToRemote: vi.fn(),
};

vi.mock('../../services/onboardingCatalog', () => ({
  getOnboardingSteps: vi.fn(() => [
    { title: 'Passo 1', description: 'Descricao 1' },
  ]),
}));

vi.mock('../../services/onboardingService', () => ({
  ONBOARDING_REPLAY_EVENT: 'cortaai:onboarding-replay',
  getCurrentRoleVariant: (...args) => onboardingApi.getCurrentRoleVariant(...args),
  getCurrentUserScope: (...args) => onboardingApi.getCurrentUserScope(...args),
  hydrateOnboardingFromRemote: (...args) => onboardingApi.hydrateOnboardingFromRemote(...args),
  isPageOnboardingCompleted: (...args) => onboardingApi.isPageOnboardingCompleted(...args),
  markPageOnboardingCompleted: (...args) => onboardingApi.markPageOnboardingCompleted(...args),
  resolvePageKeyFromLocation: (...args) => onboardingApi.resolvePageKeyFromLocation(...args),
  syncOnboardingToRemote: (...args) => onboardingApi.syncOnboardingToRemote(...args),
}));

vi.mock('./OnboardingTour', () => ({
  default: ({ open, onSkip, onClose, onComplete }) => {
    if (!open) return null;
    return (
      <div>
        <button type="button" onClick={onSkip}>Pular onboarding</button>
        <button type="button" onClick={onClose}>Fechar onboarding</button>
        <button type="button" onClick={onComplete}>Concluir onboarding</button>
      </div>
    );
  },
}));

import OnboardingHost from './OnboardingHost';

describe('OnboardingHost', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    onboardingApi.getCurrentUserScope.mockReturnValue('internal:owner-1');
    onboardingApi.getCurrentRoleVariant.mockReturnValue('owner');
    onboardingApi.resolvePageKeyFromLocation.mockReturnValue('owner-manage-shop');
    onboardingApi.hydrateOnboardingFromRemote.mockResolvedValue(true);
    onboardingApi.isPageOnboardingCompleted.mockReturnValue(false);
    onboardingApi.syncOnboardingToRemote.mockResolvedValue(true);
  });

  it('marca onboarding como concluido ao clicar em pular', async () => {
    render(
      <MemoryRouter initialEntries={['/barberHome/gerenciar-barbearia']}>
        <OnboardingHost />
      </MemoryRouter>
    );

    await screen.findByRole('button', { name: 'Pular onboarding' });
    fireEvent.click(screen.getByRole('button', { name: 'Pular onboarding' }));

    await waitFor(() => {
      expect(onboardingApi.markPageOnboardingCompleted).toHaveBeenCalledWith({
        userScope: 'internal:owner-1',
        roleVariant: 'owner',
        pageKey: 'owner-manage-shop',
      });
    });

    expect(onboardingApi.syncOnboardingToRemote).toHaveBeenCalledWith({ userScope: 'internal:owner-1' });
  });

  it('marca onboarding como concluido ao clicar em fechar', async () => {
    render(
      <MemoryRouter initialEntries={['/barberHome/gerenciar-barbearia']}>
        <OnboardingHost />
      </MemoryRouter>
    );

    await screen.findByRole('button', { name: 'Fechar onboarding' });
    fireEvent.click(screen.getByRole('button', { name: 'Fechar onboarding' }));

    await waitFor(() => {
      expect(onboardingApi.markPageOnboardingCompleted).toHaveBeenCalledWith({
        userScope: 'internal:owner-1',
        roleVariant: 'owner',
        pageKey: 'owner-manage-shop',
      });
    });
  });
});
