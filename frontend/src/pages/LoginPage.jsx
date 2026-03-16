import Login_Inputs from "../components/Login/Login_Inputs"
import Styles from "./CSS/LoginPage.module.css"
import { useNavigate, useLocation } from "react-router-dom";

function LoginPage() {
    const navigate = useNavigate();
    const location = useLocation();
    const role = location.state?.role || 'customer';

    return (
        <div className={Styles.LoginPage_container}>
            <div className={Styles.content_container}>
                <div className={Styles.logo_container}>
                     <img src="./Icons/cortaAi.jpg" alt="Logo APP" />
                </div>
                <h1 className={Styles.title_login}>Acesse sua Conta</h1>
            </div>

            <Login_Inputs role={role} />

            <div>
                <h3>Não tem uma conta? <span className={Styles.Link} style={{ cursor: 'pointer' }} onClick={() => navigate('/identificacao', { state: { mode: 'register' } })}>Crie uma Agora</span></h3>
            </div>
        </div>
    )
}

export default LoginPage