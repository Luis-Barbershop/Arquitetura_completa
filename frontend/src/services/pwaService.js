import { PWA_METRICS, trackPwaMetric } from './pwaTelemetryService'

const SW_PATH = '/sw.js'

let waitingServiceWorker = null
let hasControllerChanged = false
const updateListeners = new Set()
let deferredInstallPromptEvent = null
const installPromptListeners = new Set()

const notifyInstallPromptAvailability = (isAvailable) => {
  installPromptListeners.forEach((listener) => {
    listener(isAvailable)
  })
}

const notifyUpdateAvailable = () => {
  updateListeners.forEach((listener) => {
    listener(true)
  })
}

const setWaitingServiceWorker = (serviceWorker) => {
  waitingServiceWorker = serviceWorker
  trackPwaMetric(PWA_METRICS.SW_UPDATE_AVAILABLE)
  notifyUpdateAvailable()
}

const watchServiceWorkerInstallation = (registration) => {
  registration.addEventListener('updatefound', () => {
    const newWorker = registration.installing
    if (!newWorker) {
      return
    }

    newWorker.addEventListener('statechange', () => {
      if (newWorker.state === 'installed' && navigator.serviceWorker.controller) {
        setWaitingServiceWorker(newWorker)
      }
    })
  })
}

const setupControllerChangeReload = () => {
  navigator.serviceWorker.addEventListener('controllerchange', () => {
    if (hasControllerChanged) {
      return
    }

    hasControllerChanged = true
    trackPwaMetric(PWA_METRICS.SW_CONTROLLER_CHANGED)
    window.location.reload()
  })
}

const setupInstallPromptHandling = () => {
  window.addEventListener('beforeinstallprompt', (event) => {
    event.preventDefault()
    deferredInstallPromptEvent = event
    trackPwaMetric(PWA_METRICS.PWA_INSTALL_PROMPT_AVAILABLE)
    notifyInstallPromptAvailability(true)
  })

  window.addEventListener('appinstalled', () => {
    deferredInstallPromptEvent = null
    trackPwaMetric(PWA_METRICS.PWA_INSTALL_INSTALLED)
    notifyInstallPromptAvailability(false)
  })
}

export const registerServiceWorkerIfEnabled = () => {
  if (import.meta.env.VITE_ENABLE_PWA !== 'true') {
    return
  }

  setupInstallPromptHandling()

  if (!('serviceWorker' in navigator)) {
    return
  }

  window.addEventListener('load', () => {
    navigator.serviceWorker
      .register(SW_PATH)
      .then((registration) => {
        trackPwaMetric(PWA_METRICS.SW_REGISTER_SUCCESS)

        if (registration.waiting) {
          setWaitingServiceWorker(registration.waiting)
        }

        watchServiceWorkerInstallation(registration)
        setupControllerChangeReload()
      })
      .catch((error) => {
        trackPwaMetric(PWA_METRICS.SW_REGISTER_FAILURE, {
          message: error?.message,
        })
        waitingServiceWorker = null
      })
  })
}

export const subscribeToServiceWorkerUpdate = (listener) => {
  updateListeners.add(listener)

  if (waitingServiceWorker) {
    listener(true)
  }

  return () => {
    updateListeners.delete(listener)
  }
}

export const applyServiceWorkerUpdate = () => {
  trackPwaMetric(PWA_METRICS.SW_UPDATE_APPLY_REQUESTED)

  if (!waitingServiceWorker) {
    window.location.reload()
    return
  }

  waitingServiceWorker.postMessage({ type: 'SKIP_WAITING' })
}

export const subscribeToInstallPrompt = (listener) => {
  installPromptListeners.add(listener)

  listener(Boolean(deferredInstallPromptEvent))

  return () => {
    installPromptListeners.delete(listener)
  }
}

export const requestPwaInstall = async () => {
  if (!deferredInstallPromptEvent) {
    trackPwaMetric(PWA_METRICS.PWA_INSTALL_PROMPT_NOT_AVAILABLE)
    return false
  }

  trackPwaMetric(PWA_METRICS.PWA_INSTALL_PROMPT_REQUESTED)
  await deferredInstallPromptEvent.prompt()
  const choice = await deferredInstallPromptEvent.userChoice

  const accepted = choice?.outcome === 'accepted'

  trackPwaMetric(
    accepted
      ? PWA_METRICS.PWA_INSTALL_PROMPT_ACCEPTED
      : PWA_METRICS.PWA_INSTALL_PROMPT_DISMISSED,
  )

  deferredInstallPromptEvent = null
  notifyInstallPromptAvailability(false)

  return accepted
}
