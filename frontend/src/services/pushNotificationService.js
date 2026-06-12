import { getMessaging, getToken, isSupported, deleteToken } from 'firebase/messaging'
import api from './api'
import firebaseApp from './firebase'

const ENABLE_PUSH = import.meta.env.VITE_ENABLE_PUSH === 'true'
const VAPID_KEY = import.meta.env.VITE_FIREBASE_VAPID_KEY || ''
const PUSH_TOKEN_STORAGE_KEY = 'pushToken'

export const PUSH_REGISTRATION_FAILURE = {
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
}

export const getPushRegistrationReadiness = async () => {
  if (!ENABLE_PUSH) return { ready: false, reason: PUSH_REGISTRATION_FAILURE.DISABLED }
  if (!localStorage.getItem('token')) return { ready: false, reason: PUSH_REGISTRATION_FAILURE.UNAUTHENTICATED }
  if (!('Notification' in window)) return { ready: false, reason: PUSH_REGISTRATION_FAILURE.NOTIFICATION_UNSUPPORTED }
  if (!('serviceWorker' in navigator)) return { ready: false, reason: PUSH_REGISTRATION_FAILURE.SERVICE_WORKER_UNSUPPORTED }
  if (!VAPID_KEY) return { ready: false, reason: PUSH_REGISTRATION_FAILURE.MISSING_VAPID_KEY }

  const firebaseSupported = await isSupported()
  if (!firebaseSupported) return { ready: false, reason: PUSH_REGISTRATION_FAILURE.FIREBASE_UNSUPPORTED }

  return { ready: true, reason: null }
}

export const isPushRegisteredLocally = () =>
  'Notification' in window
  && Notification.permission === 'granted'
  && !!localStorage.getItem(PUSH_TOKEN_STORAGE_KEY)

export const canPromptForPushNotifications = async () => {
  const readiness = await getPushRegistrationReadiness()
  if (!readiness.ready) {
    return false
  }

  return Notification.permission === 'default'
}

export const registerPushNotifications = async ({ requestPermission = false } = {}) => {
  const readiness = await getPushRegistrationReadiness()
  if (!readiness.ready) {
    return { ok: false, reason: readiness.reason }
  }

  if (Notification.permission === 'default') {
    if (!requestPermission) {
      return { ok: false, reason: PUSH_REGISTRATION_FAILURE.PERMISSION_NOT_REQUESTED }
    }

    const permission = await Notification.requestPermission()
    if (permission !== 'granted') {
      return { ok: false, reason: PUSH_REGISTRATION_FAILURE.PERMISSION_DENIED }
    }
  }

  if (Notification.permission !== 'granted') {
    return { ok: false, reason: PUSH_REGISTRATION_FAILURE.PERMISSION_DENIED }
  }

  try {
    const serviceWorkerRegistration = await navigator.serviceWorker.ready
    const messaging = getMessaging(firebaseApp)
    const deviceToken = await getToken(messaging, {
      vapidKey: VAPID_KEY,
      serviceWorkerRegistration,
    })

    if (!deviceToken) {
      return { ok: false, reason: PUSH_REGISTRATION_FAILURE.NO_DEVICE_TOKEN }
    }

    const previousToken = localStorage.getItem(PUSH_TOKEN_STORAGE_KEY)
    if (previousToken === deviceToken) {
      return { ok: true, token: deviceToken }
    }

    await api.post('/notifications/device-tokens', {
      token: deviceToken,
      platform: 'WEB',
    })

    localStorage.setItem(PUSH_TOKEN_STORAGE_KEY, deviceToken)
    return { ok: true, token: deviceToken }
  } catch (error) {
    return {
      ok: false,
      reason: PUSH_REGISTRATION_FAILURE.REGISTRATION_FAILED,
      message: error?.message,
    }
  }
}

export const registerPushNotificationsIfPossible = async (options) => {
  const result = await registerPushNotifications(options)
  return result.ok
}

export const requestPushNotificationsPermissionAndRegister = () =>
  registerPushNotifications({ requestPermission: true })

export const unregisterPushNotificationsIfPossible = async () => {
  const token = localStorage.getItem(PUSH_TOKEN_STORAGE_KEY)

  if (token) {
    try {
      await api.delete('/notifications/device-tokens', {
        params: { token },
      })
    } catch {
      // best-effort
    } finally {
      localStorage.removeItem(PUSH_TOKEN_STORAGE_KEY)
    }
  }

  // Revoga a subscrição push no Firebase para garantir estado limpo na próxima ativação.
  // Sem isso, getToken() tenta reaproveitar o token revogado e falha silenciosamente.
  try {
    if (await isSupported()) {
      const messaging = getMessaging(firebaseApp)
      await deleteToken(messaging)
    }
  } catch {
    // best-effort — pode não ter subscrição ativa para revogar
  }
}
