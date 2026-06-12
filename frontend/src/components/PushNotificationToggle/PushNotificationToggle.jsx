import { useCallback, useEffect, useState } from 'react'
import { toast } from 'react-toastify'
import {
  getPushRegistrationReadiness,
  isPushRegisteredLocally,
  PUSH_REGISTRATION_FAILURE,
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
    return isPushRegisteredLocally() ? 'granted' : 'granted-unregistered'
  }

  const readiness = await getPushRegistrationReadiness()
  return readiness.ready ? 'default' : 'unavailable'
}

const activationFailureMessage = (result) => {
  if (Notification.permission === 'denied' || result?.reason === PUSH_REGISTRATION_FAILURE.PERMISSION_DENIED) {
    return 'Notificações bloqueadas no navegador. Reative nas configurações do browser.'
  }

  if (result?.reason === PUSH_REGISTRATION_FAILURE.MISSING_VAPID_KEY) {
    return 'Chave VAPID de push não configurada para este ambiente.'
  }

  if (
    result?.reason === PUSH_REGISTRATION_FAILURE.SERVICE_WORKER_UNSUPPORTED
    || result?.reason === PUSH_REGISTRATION_FAILURE.FIREBASE_UNSUPPORTED
    || result?.reason === PUSH_REGISTRATION_FAILURE.NOTIFICATION_UNSUPPORTED
  ) {
    return 'Este navegador não suporta notificações push do CortaAi.'
  }

  if (result?.reason === PUSH_REGISTRATION_FAILURE.UNAUTHENTICATED) {
    return 'Faça login novamente para ativar notificações push.'
  }

  return 'Não foi possível ativar agora. Tente novamente em alguns instantes.'
}

export default function PushNotificationToggle() {
  const [state, setState] = useState('loading')
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    resolveState().then(setState)
  }, [])

  const handleEnable = useCallback(async () => {
    setBusy(true)
    try {
      const result = await requestPushNotificationsPermissionAndRegister()
      const nextState = await resolveState()
      setState(nextState)

      if (result.ok && nextState === 'granted') {
        toast.success('Notificações push ativadas no CortaAi.')
      } else {
        toast.warn(activationFailureMessage(result))
      }
    } finally {
      setBusy(false)
    }
  }, [])

  const handleDisable = useCallback(async () => {
    setBusy(true)
    try {
      await unregisterPushNotificationsIfPossible()
      const nextState = await resolveState()
      setState(nextState === 'granted' ? 'granted-unregistered' : nextState)
      toast.info('Notificações desativadas no CortaAi para este navegador.')
    } finally {
      setBusy(false)
    }
  }, [])

  if (state === 'loading' || state === 'unavailable') return null

  return (
    <section className={styles.section}>
      <div className={styles.info}>
        <span className={styles.icon}>🔔</span>
        <div>
          <p className={styles.title}>Notificações push</p>
          <p className={styles.description}>
            {state === 'granted' && 'Ativadas neste navegador para alertas de agendamentos e pagamentos.'}
            {state === 'granted-unregistered' && 'Desativadas no CortaAi para este navegador. A permissão do browser continua concedida.'}
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
          {busy ? 'Aguarde...' : 'Ativar'}
        </button>
      )}

      {state === 'denied' && (
        <span className={styles.deniedBadge}>Bloqueado</span>
      )}
    </section>
  )
}
