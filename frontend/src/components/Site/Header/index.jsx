import Styles from './Header.module.css'
import logo from '../../../../public/CortaAiLogo.png'
import { useNavigate } from 'react-router-dom';

function HeaderSite() {
 const navigate = useNavigate();
  const handleNavigation = (action) => {
    navigate('/identificacao', { state: { mode: action } });
  };

  return (
    <div className={Styles.header}>
        <div className={Styles.logo}>
            <img src={logo} alt="Corta Ai" />
        </div>

        <div className={Styles.nav}>
            
                <ul>
                    <li>INICIO</li>
                    <li>SOBRE NÓS</li>
                    <li>NOSSOS SERVIÇOS</li>
                    <li>COMO FUNCIONA</li>
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