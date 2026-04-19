import './App.css'
import { useEffect, useState } from 'react';
import 'react-toastify/dist/ReactToastify.css';
import AppRoutes from './AppRoutes';
import { BrowserRouter as Router } from 'react-router-dom';
import { ToastContainer } from 'react-toastify';
import UpdateAvailableBanner from './components/UpdateAvailableBanner';
import InstallAppPopup from './components/InstallAppPopup';
import {
  applyServiceWorkerUpdate,
  requestPwaInstall,
  subscribeToInstallPrompt,
  subscribeToServiceWorkerUpdate,
} from './services/pwaService';
import { PWA_METRICS, trackPwaMetric } from './services/pwaTelemetryService';

function App() {
  const [isUpdateAvailable, setIsUpdateAvailable] = useState(false)
  const [isInstallPromptVisible, setIsInstallPromptVisible] = useState(false)

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
      setIsInstallPromptVisible(isAvailable)
    })

    return () => {
      unsubscribe()
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

    if (!installed) {
      setIsInstallPromptVisible(false)
    }
  }

  const handleDismissInstall = () => {
    trackPwaMetric(PWA_METRICS.PWA_INSTALL_PROMPT_DISMISSED)
    setIsInstallPromptVisible(false)
  }

  return (
    <Router>
      <AppRoutes/>
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
    </Router>
  )
}

export default App
