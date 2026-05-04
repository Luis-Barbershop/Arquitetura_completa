import { getMessaging, getToken, isSupported } from 'firebase/messaging'
import api from './api'
import firebaseApp from './firebase'

const ENABLE_PUSH = import.meta.env.VITE_ENABLE_PUSH === 'true'
const VAPID_KEY = import.meta.env.VITE_FIREBASE_VAPID_KEY || ''
const PUSH_TOKEN_STORAGE_KEY = 'pushToken'

const canAttemptPushRegistration = async () => {
  if (!ENABLE_PUSH) return false
  if (!localStorage.getItem('token')) return false
  if (!('Notification' in window)) return false
  if (!('serviceWorker' in navigator)) return false
  if (!VAPID_KEY) return false
  return isSupported()
}

export const canPromptForPushNotifications = async () => {
  if (!(await canAttemptPushRegistration())) {
    return false
  }

  return Notification.permission === 'default'
}

export const registerPushNotificationsIfPossible = async ({ requestPermission = false } = {}) => {
  if (!(await canAttemptPushRegistration())) {
    return false
  }

  if (Notification.permission === 'default') {
    if (!requestPermission) {
      return false
    }

    const permission = await Notification.requestPermission()
    if (permission !== 'granted') {
      return false
    }
  }

  if (Notification.permission !== 'granted') {
    return false
  }

  try {
    const serviceWorkerRegistration = await navigator.serviceWorker.ready
    const messaging = getMessaging(firebaseApp)
    const deviceToken = await getToken(messaging, {
      vapidKey: VAPID_KEY,
      serviceWorkerRegistration,
    })

    if (!deviceToken) {
      return false
    }

    const previousToken = localStorage.getItem(PUSH_TOKEN_STORAGE_KEY)
    if (previousToken === deviceToken) {
      return true
    }

    await api.post('/notifications/device-tokens', {
      token: deviceToken,
      platform: 'WEB',
    })

    localStorage.setItem(PUSH_TOKEN_STORAGE_KEY, deviceToken)
    return true
  } catch {
    return false
  }
}

export const requestPushNotificationsPermissionAndRegister = () =>
  registerPushNotificationsIfPossible({ requestPermission: true })

export const unregisterPushNotificationsIfPossible = async () => {
  const token = localStorage.getItem(PUSH_TOKEN_STORAGE_KEY)
  if (!token) {
    return
  }

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
