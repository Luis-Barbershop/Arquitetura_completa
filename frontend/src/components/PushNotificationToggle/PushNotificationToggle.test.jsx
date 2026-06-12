import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const pushApi = {
  getPushRegistrationReadiness: vi.fn(),
  isPushRegisteredLocally: vi.fn(),
  requestPushNotificationsPermissionAndRegister: vi.fn(),
  unregisterPushNotificationsIfPossible: vi.fn(),
}

vi.mock('react-toastify', () => ({
  toast: {
    success: vi.fn(),
    warn: vi.fn(),
    info: vi.fn(),
  },
}))

vi.mock('../../services/pushNotificationService', () => ({
  PUSH_REGISTRATION_FAILURE: {
    DISABLED: 'disabled',
    UNAUTHENTICATED: 'unauthenticated',
    NOTIFICATION_UNSUPPORTED: 'notification-unsupported',
    SERVICE_WORKER_UNSUPPORTED: 'service-worker-unsupported',
    MISSING_VAPID_KEY: 'missing-vapid-key',
    FIREBASE_UNSUPPORTED: 'firebase-unsupported',
    PERMISSION_NOT_REQUESTED: 'permission-not-requested',
    PERMISSION_DENIED: 'permission-denied',
    NO_DEVICE_TOKEN: 'no-device-token',
    REGISTRATION_FAILED: 'registration-failed',
  },
  getPushRegistrationReadiness: (...args) => pushApi.getPushRegistrationReadiness(...args),
  isPushRegisteredLocally: (...args) => pushApi.isPushRegisteredLocally(...args),
  requestPushNotificationsPermissionAndRegister: (...args) => pushApi.requestPushNotificationsPermissionAndRegister(...args),
  unregisterPushNotificationsIfPossible: (...args) => pushApi.unregisterPushNotificationsIfPossible(...args),
}))

const renderToggle = async () => {
  const module = await import('./PushNotificationToggle')
  const PushNotificationToggle = module.default
  return render(<PushNotificationToggle />)
}

const setNotificationPermission = (permission) => {
  Object.defineProperty(window, 'Notification', {
    configurable: true,
    value: {
      permission,
    },
  })
}

describe('PushNotificationToggle', () => {
  beforeEach(() => {
    vi.resetModules()
    vi.stubEnv('VITE_ENABLE_PUSH', 'true')
    vi.clearAllMocks()
    setNotificationPermission('default')
    pushApi.getPushRegistrationReadiness.mockResolvedValue({ ready: true, reason: null })
    pushApi.isPushRegisteredLocally.mockReturnValue(false)
    pushApi.requestPushNotificationsPermissionAndRegister.mockResolvedValue({ ok: true, token: 'token-123' })
    pushApi.unregisterPushNotificationsIfPossible.mockResolvedValue()
  })

  it('mostra desativar quando push já está ativo', async () => {
    setNotificationPermission('granted')
    pushApi.isPushRegisteredLocally.mockReturnValue(true)

    await renderToggle()

    expect(await screen.findByRole('button', { name: 'Desativar' })).toBeInTheDocument()
  })

  it('desativa e mantém estado de permissão concedida com opção de ativar novamente', async () => {
    setNotificationPermission('granted')
    pushApi.isPushRegisteredLocally
      .mockReturnValueOnce(true)
      .mockReturnValue(false)

    await renderToggle()

    fireEvent.click(await screen.findByRole('button', { name: 'Desativar' }))

    await waitFor(() => {
      expect(pushApi.unregisterPushNotificationsIfPossible).toHaveBeenCalledTimes(1)
    })

    expect(await screen.findByRole('button', { name: 'Ativar' })).toBeInTheDocument()
    expect(screen.getByText(/Desativadas no CortaAi para este navegador/i)).toBeInTheDocument()
  })

  it('permite ativar quando permissão ainda está default', async () => {
    setNotificationPermission('default')

    await renderToggle()

    fireEvent.click(await screen.findByRole('button', { name: 'Ativar' }))

    await waitFor(() => {
      expect(pushApi.requestPushNotificationsPermissionAndRegister).toHaveBeenCalledTimes(1)
    })
  })

  it('mostra ativar quando o navegador permitiu mas não há token local no CortaAi', async () => {
    setNotificationPermission('granted')
    pushApi.isPushRegisteredLocally.mockReturnValue(false)

    await renderToggle()

    expect(await screen.findByRole('button', { name: 'Ativar' })).toBeInTheDocument()
    expect(screen.getByText(/Desativadas no CortaAi para este navegador/i)).toBeInTheDocument()
  })

  it('mostra estado bloqueado quando o navegador negou notificações', async () => {
    setNotificationPermission('denied')

    await renderToggle()

    expect(await screen.findByText('Bloqueado')).toBeInTheDocument()
    expect(screen.getByText(/Bloqueadas pelo navegador/i)).toBeInTheDocument()
  })

  it('mantém ativar e informa falha quando o registro não conclui', async () => {
    const { toast } = await import('react-toastify')
    pushApi.requestPushNotificationsPermissionAndRegister.mockResolvedValue({
      ok: false,
      reason: 'registration-failed',
    })

    await renderToggle()

    fireEvent.click(await screen.findByRole('button', { name: 'Ativar' }))

    await waitFor(() => {
      expect(toast.warn).toHaveBeenCalledWith('Não foi possível ativar agora. Tente novamente em alguns instantes.')
    })
    expect(await screen.findByRole('button', { name: 'Ativar' })).toBeInTheDocument()
  })
})
