import Styles from './Header.module.css'
import { useNavigate } from 'react-router-dom';

function HeaderSite() {
  const navigate = useNavigate();

  const handleNavigation = (action) => {
    navigate('/identificacao', { state: { mode: action } });
  };

  const scrollTo = (id) => {
    const el = document.getElementById(id);
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  };

  return (
    <div className={Styles.header}>
        <div className={Styles.logo}>
            <img src="/CortaAiLogo.png" alt="Corta Ai" />
        </div>

        <div className={Styles.nav}>
            <ul>
                <li onClick={() => scrollTo('inicio')}>INICIO</li>
                <li onClick={() => scrollTo('sobre')}>SOBRE NÓS</li>
                  <li onClick={() => scrollTo('como-funciona')}>COMO FUNCIONA</li>
                <li onClick={() => scrollTo('servicos')}>NOSSOS SERVIÇOS</li>
              
            </ul>
        </div>

        <div className={Styles.buttons}>
            <button className={Styles.registerButton} onClick={() => handleNavigation('register')}>
                Cadastro
            </button>
            <button className={Styles.loginButton} onClick={() => handleNavigation('login')}>
                Login
            </button>
        </div>
    </div>
  )
}

export default HeaderSite