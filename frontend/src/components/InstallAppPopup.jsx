import styles from './InstallAppPopup.module.css'

function InstallAppPopup({ onInstall, onDismiss }) {
  return (
    <aside className={styles.overlay} role="dialog" aria-modal="true" aria-labelledby="install-app-title">
      <div className={styles.modal}>
        <h2 id="install-app-title" className={styles.title}>
          Instale o CortaAi no seu dispositivo
        </h2>
        <p className={styles.message}>
          Tenha acesso mais rápido, melhor experiência e recursos offline instalando o app agora.
        </p>

        <div className={styles.actions}>
          <button type="button" className={styles.secondaryButton} onClick={onDismiss}>
            Agora não
          </button>
          <button type="button" className={styles.primaryButton} onClick={onInstall}>
            Instalar app
          </button>
        </div>
      </div>
    </aside>
  )
}

export default InstallAppPopup
