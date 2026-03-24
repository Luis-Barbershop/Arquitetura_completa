import Styles from "./CSS/Login_inputs.module.css"
import { useState } from "react"
import { useNavigate, useLocation } from "react-router-dom"
import { loginUser } from "../../services/authService"

function Login_Inputs() {

    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState(null);
    const location = useLocation();
    const navigate = useNavigate();

    const userType = location.state?.role || "customer";

   const handleLogin = async (e) => {
        e.preventDefault();
        setError(null);

        try {
            // Usa o userType que veio automático da RedirectionPage
            const data = await loginUser(email, password, userType);

            if (data.role === 'ROLE_CUSTOMER') {
                navigate("/homepage"); 
            } else {
                navigate("/barberHome");
            }

        } catch (error) {
            setError("Falha no login. Verifique seus dados.");
        }
    };

    return (
        <div className={Styles.Login_Inputs_container}>
            <form onSubmit={handleLogin}>
                <label className={Styles.label_email}>
                    <p className={Styles.label_input}>Email</p>
                    <input type="email" name="email_area" id={Styles.email_input}
                        placeholder="Digite seu Email" value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        required />
                </label>

                <label className={Styles.label_password}>
                    <p className={Styles.label_input}>Senha</p>
                    <input type="password" name="password_area" id={Styles.password_input}
                        placeholder="Digite a sua Senha" value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        required />
                    <p className={Styles.forgot_password_text}>Esqueceu a Senha?</p>
                </label>

                {error && <p className={Styles.errorMessage}>{error}</p>}


                <button type="submit" className={Styles.Login_button}>Entrar</button>

            </form>
        </div>
    )
}

export default Login_Inputs