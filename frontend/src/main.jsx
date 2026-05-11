import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import './styles/tokens.css'
import './styles/grid.css'
import 'leaflet/dist/leaflet.css'
import App from './App.jsx'
import { registerServiceWorkerIfEnabled } from './services/pwaService'
import { registerPushNotificationsIfPossible } from './services/pushNotificationService'

registerServiceWorkerIfEnabled()
void registerPushNotificationsIfPossible()

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
