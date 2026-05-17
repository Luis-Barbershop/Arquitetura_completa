import { useCallback, useEffect, useState } from 'react'
import {
  canPromptForPushNotifications,
  requestPushNotificationsPermissionAndRegister,
  unregisterPushNotificationsIfPossible,
} from '../../services/pushNotificationService'
import styles from './PushNotificationToggle.module.css'

const ENABLE_PUSH = import.meta.env.VITE_ENABLE_PUSH === 'true'

const resolveState = async () => {
  if (!ENABLE_PUSH) return 'unavailable'
  if (!('Notification' in window)) return 'unavailable'
  if (Notification.permission === 'granted') return 'granted'
  if (Notification.permission === 'denied') return 'denied'
  const canPrompt = await canPromptForPushNotifications()
  return canPrompt ? 'default' : 'unavailable'
}

export default function PushNotificationToggle() {
  const [state, setState] = useState('loading')
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    resolveState().then(setState)
  }, [])

  const handleEnable = useCallback(async () => {
    setBusy(true)
    const ok = await requestPushNotificationsPermissionAndRegister()
    setState(ok ? 'granted' : Notification.permission === 'denied' ? 'denied' : 'default')
    setBusy(false)
  }, [])

  const handleDisable = useCallback(async () => {
    setBusy(true)
    await unregisterPushNotificationsIfPossible()
    setState('default')
    setBusy(false)
  }, [])

  if (state === 'loading' || state === 'unavailable') return null

  return (
    <section className={styles.section}>
      <div className={styles.info}>
        <span className={styles.icon}>🔔</span>
        <div>
          <p className={styles.title}>Notificações push</p>
          <p className={styles.description}>
            {state === 'granted' && 'Ativadas — você recebe alertas mesmo com o app fechado.'}
            {state === 'denied' && 'Bloqueadas pelo navegador. Reative nas configurações do seu browser.'}
            {state === 'default' && 'Receba alertas de agendamentos e pagamentos mesmo com o app fechado.'}
          </p>
        </div>
      </div>

      {state === 'granted' && (
        <button
          type="button"
          className={styles.disableButton}
          onClick={handleDisable}
          disabled={busy}
        >
          {busy ? 'Aguarde...' : 'Desativar'}
        </button>
      )}

      {state === 'default' && (
        <button
          type="button"
          className={styles.enableButton}
          onClick={handleEnable}
          disabled={busy}
        >
          {busy ? 'Aguarde...' : 'Ativar'}
        </button>
      )}

      {state === 'denied' && (
        <span className={styles.deniedBadge}>Bloqueado</span>
      )}
    </section>
  )
}
