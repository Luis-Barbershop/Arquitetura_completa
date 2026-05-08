import './App.css'
import { useEffect, useState } from 'react';
import 'react-toastify/dist/ReactToastify.css';
import AppRoutes from './AppRoutes';
import { BrowserRouter as Router, useLocation } from 'react-router-dom';
import { ToastContainer } from 'react-toastify';
import UpdateAvailableBanner from './components/UpdateAvailableBanner';
import {
  applyServiceWorkerUpdate,
  requestPwaInstall,
  subscribeToServiceWorkerUpdate,
} from './services/pwaService';
import { requestPushNotificationsPermissionAndRegister } from './services/pushNotificationService';

const LOGIN_SUCCESS_EVENT = 'cortaai:login-success'

function AppShell() {
  const location = useLocation()
  const [isUpdateAvailable, setIsUpdateAvailable] = useState(false)

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
    const handleLoginSuccess = () => {
      void requestPwaInstall()
      void requestPushNotificationsPermissionAndRegister()
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
    setIsUpdateAvailable(false)
  }

  return (
    <main className="premiumRouteShell">
      <div key={location.pathname} className="premiumRouteEnter">
        <AppRoutes/>
      </div>
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
