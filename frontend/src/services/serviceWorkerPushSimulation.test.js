import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { cwd } from 'node:process'
import { describe, expect, it, vi } from 'vitest'

const loadServiceWorker = (hostname = 'localhost') => {
  const listeners = {}
  const showNotification = vi.fn().mockResolvedValue()
  const selfMock = {
    location: {
      hostname,
      origin: `http://${hostname}`,
    },
    registration: {
      showNotification,
    },
    clients: {
      matchAll: vi.fn(),
      openWindow: vi.fn(),
    },
    addEventListener: vi.fn((type, listener) => {
      listeners[type] = listener
    }),
    skipWaiting: vi.fn(),
  }
  const source = readFileSync(resolve(cwd(), 'public/sw.js'), 'utf8')

  Function('self', 'caches', 'Response', 'URL', source)(
    selfMock,
    { open: vi.fn(), keys: vi.fn() },
    Response,
    URL,
  )

  return { listeners, showNotification }
}

describe('service worker push simulation', () => {
  it('shows a browser notification from a local simulation message', async () => {
    const { listeners, showNotification } = loadServiceWorker('localhost')
    let notificationPromise

    listeners.message({
      data: {
        type: 'SIMULATE_PUSH_NOTIFICATION',
        payload: {
          notification: {
            title: 'Teste CortaAi',
            body: 'Notificacao simulada',
          },
          data: {
            deepLink: '/notificacoes',
          },
        },
      },
      waitUntil: vi.fn((promise) => {
        notificationPromise = promise
      }),
    })

    await notificationPromise

    expect(showNotification).toHaveBeenCalledWith('Teste CortaAi', {
      body: 'Notificacao simulada',
      icon: '/pwa/icon-192.svg',
      badge: '/pwa/icon-192.svg',
      data: {
        deepLink: '/notificacoes',
      },
    })
  })

  it('ignores simulation messages outside local development hosts', () => {
    const { listeners, showNotification } = loadServiceWorker('web.cortaai.shop')

    listeners.message({
      data: {
        type: 'SIMULATE_PUSH_NOTIFICATION',
        payload: {
          title: 'Teste CortaAi',
        },
      },
      waitUntil: vi.fn(),
    })

    expect(showNotification).not.toHaveBeenCalled()
  })
})
