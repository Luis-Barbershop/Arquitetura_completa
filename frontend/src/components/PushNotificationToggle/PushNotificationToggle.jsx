import { useCallback, useEffect, useState } from 'react'
import { toast } from 'react-toastify'
import {
  canPromptForPushNotifications,
  isPushRegisteredLocally,
  requestPushNotificationsPermissionAndRegister,
  unregisterPushNotificationsIfPossible,
} from '../../services/pushNotificationService'
import styles from './PushNotificationToggle.module.css'

const ENABLE_PUSH = import.meta.env.VITE_ENABLE_PUSH === 'true'

const resolveState = async () => {
  if (!ENABLE_PUSH) return 'unavailable'
  if (!('Notification' in window)) return 'unavailable'
  if (Notification.permission === 'denied') return 'denied'
  if (Notification.permission === 'granted') {
    // Permissão concedida no navegador, mas pode estar desativado no CortaAi.
    return isPushRegisteredLocally() ? 'granted' : 'granted-unregistered'
  }

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
    const enabled = await requestPushNotificationsPermissionAndRegister()
    const nextState = await resolveState()
    setState(nextState)

    if (enabled && nextState === 'granted') {
      toast.success('Notificações push ativadas no CortaAi.')
    } else if (Notification.permission === 'denied') {
      toast.warn('Notificações bloqueadas no navegador. Reative nas configurações do browser.')
    } else {
      toast.info('Não foi possível ativar agora. Tente novamente em alguns instantes.')
    }

    setBusy(false)
  }, [])

  const handleDisable = useCallback(async () => {
    setBusy(true)
    await unregisterPushNotificationsIfPossible()
    setState(Notification.permission === 'granted' ? 'granted-unregistered' : 'default')
    toast.info('Notificações desativadas no CortaAi para este navegador.')
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
            {state === 'granted-unregistered' && 'Permissão do navegador concedida, mas alertas desativados no CortaAi.'}
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

      {(state === 'default' || state === 'granted-unregistered') && (
        <button
          type="button"
          className={styles.enableButton}
          onClick={handleEnable}
          disabled={busy}
        >
          {busy ? 'Aguarde...' : state === 'granted-unregistered' ? 'Reativar' : 'Ativar'}
        </button>
      )}

      {state === 'denied' && (
        <span className={styles.deniedBadge}>Bloqueado</span>
      )}
    </section>
  )
}
