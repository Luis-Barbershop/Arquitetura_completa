import styles from './InstallAppPopup.module.css'

function InstallAppPopup({ isNativeInstallPromptAvailable, onInstall, onDismiss }) {
  const message = isNativeInstallPromptAvailable
    ? 'Tenha acesso mais rápido, melhor experiência e recursos offline instalando o app agora.'
    : 'Você pode instalar o CortaAi para acessar mais rápido e usar recursos offline. No navegador, abra o menu e escolha “Instalar app” ou “Adicionar à tela inicial”.'

  return (
    <aside className={styles.overlay} role="dialog" aria-modal="true" aria-labelledby="install-app-title">
      <div className={styles.modal}>
        <h2 id="install-app-title" className={styles.title}>
          Instale o CortaAi no seu dispositivo
        </h2>
        <p className={styles.message}>
          {message}
        </p>

        <div className={styles.actions}>
          {isNativeInstallPromptAvailable ? (
            <>
              <button type="button" className={styles.secondaryButton} onClick={onDismiss}>
                Agora não
              </button>
              <button type="button" className={styles.primaryButton} onClick={onInstall}>
                Instalar app
              </button>
            </>
          ) : (
            <button type="button" className={styles.primaryButton} onClick={onDismiss}>
              Entendi
            </button>
          )}
        </div>
      </div>
    </aside>
  )
}

export default InstallAppPopup
