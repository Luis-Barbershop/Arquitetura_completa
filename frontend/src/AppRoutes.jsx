import { Routes, Route } from 'react-router-dom'
import RedirectionPage from "./pages/RedirectionPage";
import StartPage from "./pages/StartPage"
import LoginPage from "./pages/LoginPage";
import SignInPage from "./pages/SignInPage";
import HomePage from "./pages/HomePage";
import Agendamento from "./pages/Agendamento";
import BarberHomePage from './pages/BarberHomePage';
import AgendamentoPage from './pages/AgendamentoPage';
import CreateBarbershopPage from './pages/CreateBarbershopPage';
import MeusAgendamentosPage from './pages/MeusAgendamentosPage';
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
      <Route path='/create-barbershop' element={<CreateBarbershopPage/>}/>
      <Route path="/meus-agendamentos" element={<MeusAgendamentosPage />} />
    </Routes>
  )
}

export default AppRoutes