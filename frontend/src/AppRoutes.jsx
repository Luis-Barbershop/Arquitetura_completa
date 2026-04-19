import { Routes, Route } from 'react-router-dom'
import RedirectionPage from "./pages/RedirectionPage";
import StartPage from "./pages/StartPage"
import LoginPage from "./pages/LoginPage";
import SignInPage from "./pages/SignInPage";
import HomePage from "./pages/HomePage";
import BarberHomePage from './pages/BarberHomePage';
import BarberServicesPage from './pages/BarberServicesPage';
import BarberStockPage from './pages/BarberStockPage';
import AgendamentoPage from './pages/AgendamentoPage';
import CreateBarbershopPage from './pages/CreateBarbershopPage';
import MeusAgendamentosPage from './pages/MeusAgendamentosPage';
import ForgotPasswordPage from './pages/ForgotPasswordPage';
import ChangePasswordPage from './pages/ChangePasswordPage';
import VerifyEmailPage from './pages/VerifyEmailPage';
import BarberProfilePage from './pages/BarberProfilePage';
import BarberTeamPage from './pages/BarberTeamPage';
import BarberDashboardPage from './pages/BarberDashboardPage';
import BarberManualBookingPage from './pages/BarberManualBookingPage';
import AgendaBarbeariaPage from './pages/AgendaBarbeariaPage';
import Site from './pages/Site';


function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<Site/>} />
      <Route path="/identificacao" element={<RedirectionPage/>} />
      <Route path="/login" element= {<LoginPage/>}/>
      <Route path="/SignIn" element={<SignInPage/>}/>
      <Route path="/signin" element={<SignInPage/>}/>
      <Route path="/homepage" element={<HomePage/>}/>
      <Route path='/agendamentoPage/:barbershopId' element={<AgendamentoPage/>}/>
      <Route path='/barberHome' element={<BarberHomePage/>}/>
      <Route path='/barberHome/servicos' element={<BarberServicesPage/>}/>
      <Route path='/barberHome/estoque' element={<BarberStockPage/>}/>
      <Route path='/create-barbershop' element={<CreateBarbershopPage/>}/>
      <Route path="/meus-agendamentos" element={<MeusAgendamentosPage />} />
      <Route path="/forgot-password" element={<ForgotPasswordPage />} />
      <Route path="/change-password" element={<ChangePasswordPage />} />
      <Route path="/verify-email" element={<VerifyEmailPage />} />
      <Route path='/barberHome/perfil' element={<BarberProfilePage />} />
      <Route path='/barberHome/time' element={<BarberTeamPage />} />
      <Route path='/barberHome/dashboard' element={<BarberDashboardPage />} />
      <Route path='/barberHome/novo-agendamento' element={<BarberManualBookingPage />} />
      <Route path='/barberHome/agenda-barbearia' element={<AgendaBarbeariaPage />} />
      <Route path='/barberHome/agenda-equipe' element={<AgendaBarbeariaPage />} />
    </Routes>
  )
}

export default AppRoutes