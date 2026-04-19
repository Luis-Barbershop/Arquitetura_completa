const PWA_METRIC_EVENT_NAME = 'cortaai:pwa-metric'

const DEV_MODE = import.meta.env.DEV

export const PWA_METRICS = {
  SW_REGISTER_SUCCESS: 'sw_register_success',
  SW_REGISTER_FAILURE: 'sw_register_failure',
  SW_UPDATE_AVAILABLE: 'sw_update_available',
  SW_UPDATE_APPLY_REQUESTED: 'sw_update_apply_requested',
  SW_UPDATE_DISMISSED: 'sw_update_dismissed',
  SW_CONTROLLER_CHANGED: 'sw_controller_changed',
}

const emitBrowserEvent = (payload) => {
  if (typeof window === 'undefined') {
    return
  }

  window.dispatchEvent(new CustomEvent(PWA_METRIC_EVENT_NAME, { detail: payload }))
}

const emitDataLayerEvent = (payload) => {
  if (typeof window === 'undefined') {
    return
  }

  const dataLayer = window.dataLayer
  if (Array.isArray(dataLayer)) {
    dataLayer.push({
      event: 'pwa_metric',
      ...payload,
    })
  }
}

export const trackPwaMetric = (metric, metadata = {}) => {
  const payload = {
    metric,
    metadata,
    timestamp: new Date().toISOString(),
  }

  emitBrowserEvent(payload)
  emitDataLayerEvent(payload)

  if (DEV_MODE) {
    console.info('[PWA_METRIC]', payload)
  }
}
