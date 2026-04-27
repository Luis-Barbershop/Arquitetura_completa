import './App.css'
import { useEffect, useState } from 'react';
import 'react-toastify/dist/ReactToastify.css';
import AppRoutes from './AppRoutes';
import { BrowserRouter as Router, useLocation } from 'react-router-dom';
import { ToastContainer } from 'react-toastify';
import UpdateAvailableBanner from './components/UpdateAvailableBanner';
import InstallAppPopup from './components/InstallAppPopup';
import {
  applyServiceWorkerUpdate,
  isInstallPromptAvailable,
  requestPwaInstall,
  subscribeToInstallPrompt,
  subscribeToServiceWorkerUpdate,
} from './services/pwaService';
import { PWA_METRICS, trackPwaMetric } from './services/pwaTelemetryService';

const INSTALL_AFTER_LOGIN_FLAG = 'pwa_install_after_login'
const LOGIN_SUCCESS_EVENT = 'cortaai:login-success'

const shouldShowInstallPromptNow = () =>
  sessionStorage.getItem(INSTALL_AFTER_LOGIN_FLAG) === 'true' && isInstallPromptAvailable()

function AppShell() {
  const location = useLocation()
  const [isUpdateAvailable, setIsUpdateAvailable] = useState(false)
  const [isInstallPromptVisible, setIsInstallPromptVisible] = useState(false)

  useEffect(() => {
    const mediaQuery = window.matchMedia('(display-mode: standalone)')

    const applyDisplayMode = () => {
      const standalone = mediaQuery.matches || window.navigator.standalone === true
      document.body.setAttribute('data-display-mode', standalone ? 'standalone' : 'browser')
    }

    applyDisplayMode()
    mediaQuery.addEventListener('change', applyDisplayMode)

    return () => {
      mediaQuery.removeEventListener('change', applyDisplayMode)
    }
  }, [])

  useEffect(() => {
    const unsubscribe = subscribeToServiceWorkerUpdate((hasUpdate) => {
      if (hasUpdate) {
        setIsUpdateAvailable(true)
      }
    })

    return () => {
      unsubscribe()
    }
  }, [])

  useEffect(() => {
    const unsubscribe = subscribeToInstallPrompt((isAvailable) => {
      const shouldShow = isAvailable && sessionStorage.getItem(INSTALL_AFTER_LOGIN_FLAG) === 'true'
      setIsInstallPromptVisible(shouldShow)
    })

    return () => {
      unsubscribe()
    }
  }, [])

  useEffect(() => {
    const handleLoginSuccess = () => {
      if (shouldShowInstallPromptNow()) {
        setIsInstallPromptVisible(true)
      }
    }

    window.addEventListener(LOGIN_SUCCESS_EVENT, handleLoginSuccess)

    return () => {
      window.removeEventListener(LOGIN_SUCCESS_EVENT, handleLoginSuccess)
    }
  }, [])

  const handleUpdateNow = () => {
    applyServiceWorkerUpdate()
  }

  const handleDismissUpdate = () => {
    trackPwaMetric(PWA_METRICS.SW_UPDATE_DISMISSED)
    setIsUpdateAvailable(false)
  }

  const handleInstallApp = async () => {
    const installed = await requestPwaInstall()
    sessionStorage.removeItem(INSTALL_AFTER_LOGIN_FLAG)

    if (!installed) {
      setIsInstallPromptVisible(false)
    }
  }

  const handleDismissInstall = () => {
    trackPwaMetric(PWA_METRICS.PWA_INSTALL_PROMPT_DISMISSED)
    sessionStorage.removeItem(INSTALL_AFTER_LOGIN_FLAG)
    setIsInstallPromptVisible(false)
  }

  return (
    <main className="premiumRouteShell">
      <div key={location.pathname} className="premiumRouteEnter">
        <AppRoutes/>
      </div>
      {isInstallPromptVisible && (
        <InstallAppPopup
          onInstall={handleInstallApp}
          onDismiss={handleDismissInstall}
        />
      )}
      {isUpdateAvailable && (
        <UpdateAvailableBanner
          onUpdateNow={handleUpdateNow}
          onDismiss={handleDismissUpdate}
        />
      )}
      <ToastContainer
        position="top-right"
        autoClose={4000}
        hideProgressBar={false}
        newestOnTop
        closeOnClick
        pauseOnHover
        theme="dark"
      />
    </main>
  )
}

function App() {
  return (
    <Router>
      <AppShell />
    </Router>
  )
}

export default App
