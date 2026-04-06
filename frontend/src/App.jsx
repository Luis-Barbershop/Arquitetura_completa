import './App.css'
import 'react-toastify/dist/ReactToastify.css';
import AppRoutes from './AppRoutes';
import { BrowserRouter as Router } from 'react-router-dom';
import { ToastContainer } from 'react-toastify';

function App() {
  return (
    <Router>
      <AppRoutes/>
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
