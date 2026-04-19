const SW_PATH = '/sw.js'

export const registerServiceWorkerIfEnabled = () => {
  if (import.meta.env.VITE_ENABLE_PWA !== 'true') {
    return
  }

  if (!('serviceWorker' in navigator)) {
    return
  }

  window.addEventListener('load', () => {
    navigator.serviceWorker.register(SW_PATH)
  })
}
