import '../App.css'
import { useNavigate } from 'react-router-dom';

function StartPage() {

  const navigate = useNavigate();
  const handleNavigation = (action) => {
    navigate('/identificacao', { state: { mode: action } });
  };

  return (
    <div className='workscreen'>
      <div className='start_container'>
        <div className='image_container'>
          <img src="./Icons/cortaAi.jpg" alt="Logo APP" />
        </div>



        <div className='text_container'>
          <h1 className='slogan'>Seu Estilo, Sua Hora marcada.</h1>
          <p className='first_text'>Agende seu corte de cabelo ou barba com seu barbeiro favorito de forma rápida e fácil</p>
        </div>

        <div className='button_container'>
          <button className='start_login_button' onClick={() => handleNavigation('login')}>Login</button>
          <button className='start_sign_in_button' onClick={() => handleNavigation('register')}>Cadastre-se</button>
        </div>
      </div>
    </div>
  )
}

export default StartPage
