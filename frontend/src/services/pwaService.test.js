import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('./pwaTelemetryService', () => ({
  PWA_METRICS: {
    PWA_INSTALL_PROMPT_ACCEPTED: 'accepted',
    PWA_INSTALL_PROMPT_AVAILABLE: 'available',
    PWA_INSTALL_PROMPT_DISMISSED: 'dismissed',
    PWA_INSTALL_PROMPT_NOT_AVAILABLE: 'not_available',
    PWA_INSTALL_PROMPT_REQUESTED: 'requested',
    PWA_INSTALL_INSTALLED: 'installed',
    SW_UPDATE_APPLY_REQUESTED: 'apply',
  },
  trackPwaMetric: vi.fn(),
}));

const importPwaService = async (enabled = 'true') => {
  vi.resetModules();
  vi.stubEnv('VITE_ENABLE_PWA', enabled);
  return import('./pwaService');
};

describe('pwaService', () => {
  beforeEach(() => {
    vi.stubGlobal('navigator', {});
  });

  afterEach(() => {
    vi.unstubAllEnvs();
    vi.unstubAllGlobals();
  });

  it('does nothing when PWA is disabled', async () => {
    const addEventListener = vi.spyOn(window, 'addEventListener');
    const { registerServiceWorkerIfEnabled } = await importPwaService('false');

    registerServiceWorkerIfEnabled();

    expect(addEventListener).not.toHaveBeenCalledWith('beforeinstallprompt', expect.any(Function));
  });

  it('tracks install prompt availability and accepted install requests', async () => {
    const {
      isInstallPromptAvailable,
      registerServiceWorkerIfEnabled,
      requestPwaInstall,
      subscribeToInstallPrompt,
    } = await importPwaService('true');
    const listener = vi.fn();
    const prompt = vi.fn().mockResolvedValue();
    const event = Object.assign(new Event('beforeinstallprompt'), {
      prompt,
      userChoice: Promise.resolve({ outcome: 'accepted' }),
    });

    subscribeToInstallPrompt(listener);
    registerServiceWorkerIfEnabled();
    window.dispatchEvent(event);

    expect(listener).toHaveBeenLastCalledWith(true);
    expect(isInstallPromptAvailable()).toBe(true);
    await expect(requestPwaInstall()).resolves.toBe(true);
    expect(prompt).toHaveBeenCalledTimes(1);
    expect(listener).toHaveBeenLastCalledWith(false);
  });

  it('posts a simulated push notification to the active service worker in test mode', async () => {
    const postMessage = vi.fn();
    vi.stubGlobal('navigator', {
      serviceWorker: {
        ready: Promise.resolve({
          active: { postMessage },
        }),
      },
    });
    const { simulatePushNotificationForTesting } = await importPwaService('true');

    await expect(simulatePushNotificationForTesting({
      title: 'Teste CortaAi',
      body: 'Notificação simulada',
      data: { deepLink: '/notificacoes' },
    })).resolves.toBe(true);

    expect(postMessage).toHaveBeenCalledWith({
      type: 'SIMULATE_PUSH_NOTIFICATION',
      payload: {
        title: 'Teste CortaAi',
        body: 'Notificação simulada',
        data: { deepLink: '/notificacoes' },
      },
    });
  });

  it('does not simulate push when there is no active service worker', async () => {
    vi.stubGlobal('navigator', {
      serviceWorker: {
        ready: Promise.resolve({}),
      },
    });
    const { simulatePushNotificationForTesting } = await importPwaService('true');

    await expect(simulatePushNotificationForTesting({ title: 'Teste' })).resolves.toBe(false);
  });
});
