import styles from './PushNotificationPrompt.module.css'

function PushNotificationPrompt({ isRegistering = false, onEnable, onDismiss }) {
  return (
    <aside className={styles.overlay} role="dialog" aria-modal="true" aria-labelledby="push-prompt-title">
      <div className={styles.modal}>
        <h2 id="push-prompt-title" className={styles.title}>
          Ativar notificações
        </h2>
        <p className={styles.message}>
          Receba avisos sobre agendamentos, pagamentos e convites mesmo quando o CortaAi estiver em segundo plano.
        </p>

        <div className={styles.actions}>
          <button type="button" className={styles.secondaryButton} onClick={onDismiss} disabled={isRegistering}>
            Agora não
          </button>
          <button type="button" className={styles.primaryButton} onClick={onEnable} disabled={isRegistering}>
            {isRegistering ? 'Ativando...' : 'Ativar'}
          </button>
        </div>
      </div>
    </aside>
  )
}

export default PushNotificationPrompt
