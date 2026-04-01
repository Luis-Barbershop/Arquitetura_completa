import Styles from "./CSS/Login_inputs.module.css"
import { useState } from "react"
import { useNavigate } from "react-router-dom"
import { loginUser } from "../../services/authService"

function Login_Inputs() {

    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState(null);
    const navigate = useNavigate();

   const handleLogin = async (e) => {
        e.preventDefault();
        setError(null);

        try {
            await loginUser(email, password);
            // Redireciona para homepage — o app detecta o perfil do usuário pelo token
            navigate("/homepage");

        } catch (err) {
            console.error(err);
            const msg = err.response?.data?.message || "Falha no login. Verifique seus dados.";
            setError(msg);
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