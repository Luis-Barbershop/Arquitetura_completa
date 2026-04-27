const SW_VERSION = 'cortaai-sw-v2'
const APP_SHELL_CACHE = `${SW_VERSION}-app-shell`

const APP_SHELL_ASSETS = [
  '/',
  '/index.html',
  '/manifest.webmanifest',
  '/pwa/icon-192.svg',
  '/pwa/icon-512.svg',
]

const TRANSACIONAL_ENDPOINTS = [
  '/api/appointments',
  '/api/payments',
  '/api/auth',
]

const API_PREFIX = '/api/'

const isHttpRequest = (request) => request.url.startsWith('http')

const isNavigationRequest = (request) => request.mode === 'navigate'

const isAppShellAssetRequest = (requestUrl) => {
  const url = new URL(requestUrl)
  return APP_SHELL_ASSETS.includes(url.pathname)
}

const isStaticAssetRequest = (requestUrl) => {
  const url = new URL(requestUrl)
  return /\.(?:js|css|png|jpg|jpeg|svg|webp|ico|woff2?)$/i.test(url.pathname)
}

const isApiRequest = (requestUrl) => {
  const url = new URL(requestUrl)
  return url.pathname.startsWith(API_PREFIX)
}

const isTransacionalApiRequest = (requestUrl) => {
  const url = new URL(requestUrl)
  return TRANSACIONAL_ENDPOINTS.some((endpoint) => url.pathname.startsWith(endpoint))
}

const networkFirstForTransacionalRequest = async (request) => {
  try {
    return await fetch(request)
  } catch {
    return new Response(
      JSON.stringify({
        error: 'OFFLINE_TRANSACIONAL_UNAVAILABLE',
        message: 'Operacao transacional indisponivel offline. Tente novamente quando voltar a conexao.',
      }),
      {
        status: 503,
        headers: { 'Content-Type': 'application/json' },
      },
    )
  }
}

const networkFirstForApiRequest = async (request) => {
  try {
    return await fetch(request)
  } catch {
    return new Response(
      JSON.stringify({
        error: 'OFFLINE_API_UNAVAILABLE',
        message: 'Operacao indisponivel offline. Tente novamente quando voltar a conexao.',
      }),
      {
        status: 503,
        headers: { 'Content-Type': 'application/json' },
      },
    )
  }
}

const networkFirstForNavigationRequest = async (request) => {
  const cache = await caches.open(APP_SHELL_CACHE)

  try {
    const response = await fetch(request)
    if (response && response.ok) {
      cache.put(request, response.clone())
    }
    return response
  } catch {
    const cached = await cache.match(request)
    if (cached) {
      return cached
    }

    const indexFallback = await cache.match('/index.html')
    if (indexFallback) {
      return indexFallback
    }

    return new Response('Offline', { status: 503 })
  }
}

const cacheFirstForAppShellRequest = async (request) => {
  const cache = await caches.open(APP_SHELL_CACHE)
  const cached = await cache.match(request)
  if (cached) {
    return cached
  }

  const response = await fetch(request)
  if (response && response.ok) {
    cache.put(request, response.clone())
  }
  return response
}

self.addEventListener('fetch', (event) => {
  const { request } = event

  if (!isHttpRequest(request)) {
    return
  }

  if (isTransacionalApiRequest(request.url)) {
    event.respondWith(networkFirstForTransacionalRequest(request))
    return
  }

  if (isApiRequest(request.url)) {
    event.respondWith(networkFirstForApiRequest(request))
    return
  }

  if (isNavigationRequest(request)) {
    event.respondWith(networkFirstForNavigationRequest(request))
    return
  }

  if (isAppShellAssetRequest(request.url) || isStaticAssetRequest(request.url)) {
    event.respondWith(cacheFirstForAppShellRequest(request))
  }
})

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches
      .open(APP_SHELL_CACHE)
      .then((cache) => cache.addAll(APP_SHELL_ASSETS))
      .then(() => self.skipWaiting()),
  )
})

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((keys) =>
      Promise.all(
        keys
          .filter((key) => key !== APP_SHELL_CACHE)
          .map((key) => caches.delete(key)),
      ),
    ).then(() => self.clients.claim()),
  )
})

self.addEventListener('message', (event) => {
  if (event.data?.type === 'SKIP_WAITING') {
    self.skipWaiting()
    return
  }

  if (event.data === 'GET_SW_VERSION') {
    event.source?.postMessage({ type: 'SW_VERSION', version: SW_VERSION })
  }
})
