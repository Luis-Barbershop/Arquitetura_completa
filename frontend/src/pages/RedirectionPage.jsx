import Styles from "./CSS/RedirectionPage.module.css"
import { useNavigate, useLocation } from "react-router-dom"

function RedirectionPage() {

  const navigate = useNavigate();
  const location = useLocation();
  const mode = location.state?.mode || 'login';
  const actionLabel = mode === 'login' ? 'entrar' : 'criar sua conta';
  const actionTitle = mode === 'login' ? 'Bem-vindo de volta.' : 'Vamos afiar a lâmina.';
  const customerLabel = mode === 'login' ? 'Entrar como Cliente' : 'Cadastrar como Cliente';
  const barberLabel = mode === 'login' ? 'Entrar como Barbeiro' : 'Cadastrar como Barbeiro';

  const handleProfileSelection = (profile) => {
    // profile será 'customer' ou 'barber'

    const targetPath = mode === 'login' ? '/login' : '/signin';

    navigate(targetPath, { state: { role: profile } });
  };

  const handleNavigationHome = () => {
    navigate('/');
  };

  return (
    <div className={Styles.RedirectionPage_Container}>
      <div className={Styles.redirection_card}>
        <div className={Styles.title_redirection}>
          <div className={Styles.brandBadge}>
            <img src="/Icons/scissors_icon.png" alt="Ícone de tesoura" />
            <span>CortaAI</span>
          </div>

          <p className={Styles.kicker}>ESCOLHA DE PERFIL</p>
          <h1>{actionTitle}</h1>
          <h2>Como você quer {actionLabel}?</h2>
        </div>

        <div className={Styles.redirection_buttons_container}>
          <button className={Styles.redirection_buttons} onClick={() => handleProfileSelection('customer')}>
            <img src="/Icons/user_icon.png" alt="Ícone de usuário" />
            <div>
              <p className={Styles.title_button}>{customerLabel}</p>
              <p className={Styles.text_button}>Quero agendar um serviço com rapidez.</p>
            </div>
          </button>

          <button className={Styles.redirection_buttons} onClick={() => handleProfileSelection('barber')}>
            <img src="/Icons/barber_icon.png" alt="Ícone de barbearia" />
            <div>
              <p className={Styles.title_button}>{barberLabel}</p>
              <p className={Styles.text_button}>Quero organizar agenda e atendimentos.</p>
            </div>
          </button>
        </div>

        <button className={Styles.redirection_button_exit} onClick={handleNavigationHome}>
          Voltar para a página inicial
        </button>
      </div>
    </div>
  )
}

export default RedirectionPage