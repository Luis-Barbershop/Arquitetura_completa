import './App.css'
import { useEffect, useState } from 'react';
import 'react-toastify/dist/ReactToastify.css';
import AppRoutes from './AppRoutes';
import { BrowserRouter as Router } from 'react-router-dom';
import { ToastContainer } from 'react-toastify';
import UpdateAvailableBanner from './components/UpdateAvailableBanner';
import { applyServiceWorkerUpdate, subscribeToServiceWorkerUpdate } from './services/pwaService';
import { PWA_METRICS, trackPwaMetric } from './services/pwaTelemetryService';

function App() {
  const [isUpdateAvailable, setIsUpdateAvailable] = useState(false)

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

  const handleUpdateNow = () => {
    applyServiceWorkerUpdate()
  }

  const handleDismissUpdate = () => {
    trackPwaMetric(PWA_METRICS.SW_UPDATE_DISMISSED)
    setIsUpdateAvailable(false)
  }

  return (
    <Router>
      <AppRoutes/>
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
