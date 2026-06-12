import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const pushApi = {
  canPromptForPushNotifications: vi.fn(),
  isPushRegisteredLocally: vi.fn(),
  requestPushNotificationsPermissionAndRegister: vi.fn(),
  unregisterPushNotificationsIfPossible: vi.fn(),
}

vi.mock('../../services/pushNotificationService', () => ({
  canPromptForPushNotifications: (...args) => pushApi.canPromptForPushNotifications(...args),
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
    pushApi.canPromptForPushNotifications.mockResolvedValue(true)
    pushApi.isPushRegisteredLocally.mockReturnValue(false)
    pushApi.requestPushNotificationsPermissionAndRegister.mockResolvedValue(true)
    pushApi.unregisterPushNotificationsIfPossible.mockResolvedValue()
  })

  it('mostra desativar quando push já está ativo', async () => {
    setNotificationPermission('granted')
    pushApi.isPushRegisteredLocally.mockReturnValue(true)

    await renderToggle()

    expect(await screen.findByRole('button', { name: 'Desativar' })).toBeInTheDocument()
  })

  it('desativa e mantém estado de permissão concedida com opção de reativar', async () => {
    setNotificationPermission('granted')
    pushApi.isPushRegisteredLocally.mockReturnValue(true)

    await renderToggle()

    fireEvent.click(await screen.findByRole('button', { name: 'Desativar' }))

    await waitFor(() => {
      expect(pushApi.unregisterPushNotificationsIfPossible).toHaveBeenCalledTimes(1)
    })

    expect(await screen.findByRole('button', { name: 'Reativar' })).toBeInTheDocument()
    expect(screen.getByText(/Permissão do navegador concedida, mas alertas desativados no CortaAi/i)).toBeInTheDocument()
  })

  it('permite ativar quando permissão ainda está default', async () => {
    setNotificationPermission('default')
    pushApi.canPromptForPushNotifications.mockResolvedValue(true)

    await renderToggle()

    fireEvent.click(await screen.findByRole('button', { name: 'Ativar' }))

    await waitFor(() => {
      expect(pushApi.requestPushNotificationsPermissionAndRegister).toHaveBeenCalledTimes(1)
    })
  })
})
