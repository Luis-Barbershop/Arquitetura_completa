import Styles from "./MenuNavbar.module.css"
import { useNavigate } from 'react-router-dom';

function MenuNavBar() {

  const navigate = useNavigate();

  return (
    <div className={Styles.MenuNavBar_container}>
        <div>
            <h4>Inicio</h4>
        </div>
         <div onClick={() => navigate('/meus-agendamentos')}>
            <h4>Meus Atendimentos</h4>
        </div>
    </div>
  )
}

export default MenuNavBar