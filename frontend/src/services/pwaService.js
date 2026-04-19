import { PWA_METRICS, trackPwaMetric } from './pwaTelemetryService'

const SW_PATH = '/sw.js'

let waitingServiceWorker = null
let hasControllerChanged = false
const updateListeners = new Set()

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

export const registerServiceWorkerIfEnabled = () => {
  if (import.meta.env.VITE_ENABLE_PWA !== 'true') {
    return
  }

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
