import Styles from "./CSS/RedirectionPage.module.css"
import { useNavigate, useLocation } from "react-router-dom"

function RedirectionPage() {

  const navigate = useNavigate();
  const location = useLocation();
  const mode = location.state?.mode || 'login';

  const handleProfileSelection = (profile) => {
    // profile será 'customer' ou 'barber'

    const targetPath = mode === 'login' ? '/login' : '/SignIn';

    navigate(targetPath, { state: { role: profile } });
  };

  const handleNavigationHome = () => {
    navigate('/');
  };

  return (
    <div className={Styles.RedirectionPage_Container}>

      <div className={Styles.title_redirection}>
        <img src="./Icons/scissors_icon.png" alt="Icone de Tesoura" />
        <h1>CortaAI</h1>
        <h2>{mode === 'login' ? 'Como você quer Entrar?' : 'Como você quer se Cadastrar?'}</h2>
      </div>


      <div className={Styles.redirection_buttons_container}>
        <button className={Styles.redirection_buttons} onClick={() => handleProfileSelection('customer')}>
          <img src="./Icons/user_icon.png" alt="Icone de Usuário" />
          <p className={Styles.title_button}>{mode === 'login' ? 'Entrar como Cliente' : 'Cadastrar como Cliente'}</p>
          <p className={Styles.text_button}>Quero agendar um serviço</p>
        </button>

        <button className={Styles.redirection_buttons} onClick={() => handleProfileSelection('barber')}>
          <img src="./Icons/barber_icon.png" alt="Icone de Barbearia" />
          <p className={Styles.title_button}>{mode === 'login' ? 'Entrar como Barbeiro' : 'Cadastrar como Barbeiro'}</p>
          <p className={Styles.text_button}>Quero gerenciar minha Agenda</p>
        </button>

        <button className={Styles.redirection_button_exit} onClick={handleNavigationHome}>
          Sair
        </button>
      </div>
    </div>
  )
}

export default RedirectionPage