import styles from './UpdateAvailableBanner.module.css'

function UpdateAvailableBanner({ onUpdateNow, onDismiss }) {
  return (
    <aside className={styles.container} role="status" aria-live="polite">
      <div className={styles.content}>
        <strong className={styles.title}>Nova versão disponível</strong>
        <p className={styles.message}>Atualize para aplicar correções e melhorias.</p>
      </div>

      <div className={styles.actions}>
        <button type="button" className={styles.secondaryButton} onClick={onDismiss}>
          Depois
        </button>
        <button type="button" className={styles.primaryButton} onClick={onUpdateNow}>
          Atualizar agora
        </button>
      </div>
    </aside>
  )
}

export default UpdateAvailableBanner
