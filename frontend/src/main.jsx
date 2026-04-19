import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import './styles/tokens.css'
import './styles/grid.css'
import App from './App.jsx'
import { registerServiceWorkerIfEnabled } from './services/pwaService'

registerServiceWorkerIfEnabled()

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
