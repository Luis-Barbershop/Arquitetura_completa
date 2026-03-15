import Styles from "./CSS/Login_inputs.module.css"
import { useState } from "react"
import { useNavigate, useLocation } from "react-router-dom"
import { loginUser, loginWithGoogle } from "../../services/authService"

function Login_Inputs() {

    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState(null);
    const [loading, setLoading] = useState(false);
    const location = useLocation();
    const navigate = useNavigate();

    const userType = location.state?.role || "customer";

    // Redireciona conforme os dados retornados do backend
    const handleSuccess = (data) => {
        if (!data.profileComplete) {
            navigate("/SignIn", { state: { role: userType, completeProfile: true } });
            return;
        }
        if (data.role === 'ROLE_CUSTOMER') {
            navigate("/homepage");
        } else {
            navigate("/barberHome");
        }
    };

    // Login com e-mail e senha
    const handleLogin = async (e) => {
        e.preventDefault();
        setError(null);
        setLoading(true);

        try {
            const data = await loginUser(email, password, userType);
            handleSuccess(data);
        } catch (error) {
            console.error("Erro no login:", error);
            const msg = error.code === 'auth/invalid-credential'
                ? "E-mail ou senha incorretos."
                : error.code === 'auth/user-not-found'
                ? "Usuário não encontrado."
                : "Falha no login. Verifique seus dados.";
            setError(msg);
        } finally {
            setLoading(false);
        }
    };

    // Login com Google
    const handleGoogleLogin = async () => {
        setError(null);
        setLoading(true);

        try {
            const data = await loginWithGoogle(userType);
            handleSuccess(data);
        } catch (error) {
            console.error("Erro no login Google:", error);
            if (error.code !== 'auth/popup-closed-by-user') {
                setError("Falha ao entrar com Google. Tente novamente.");
            }
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className={Styles.Login_Inputs_container}>
            <form action="submit">
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

                {error && <p style={{color: '#ff4444', fontSize: '14px', marginTop: '8px'}}>{error}</p>}

                <button type="submit" onClick={handleLogin} className={Styles.Login_button} disabled={loading}>
                    {loading ? "Entrando..." : "Entrar com E-mail"}
                </button>
            </form>

            {/* Divisor */}
            <div style={{
                display: 'flex', alignItems: 'center', margin: '20px 0', width: '100%'
            }}>
                <div style={{ flex: 1, height: '1px', backgroundColor: '#555' }}></div>
                <span style={{ padding: '0 12px', color: '#999', fontSize: '13px' }}>ou</span>
                <div style={{ flex: 1, height: '1px', backgroundColor: '#555' }}></div>
            </div>

            {/* Botão Google */}
            <button
                onClick={handleGoogleLogin}
                disabled={loading}
                style={{
                    width: '100%',
                    padding: '12px 20px',
                    borderRadius: '8px',
                    border: '1px solid #555',
                    backgroundColor: '#fff',
                    color: '#333',
                    fontSize: '15px',
                    fontWeight: '600',
                    cursor: loading ? 'not-allowed' : 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    gap: '10px',
                    opacity: loading ? 0.6 : 1,
                    transition: 'all 0.2s',
                }}
            >
                <svg width="20" height="20" viewBox="0 0 48 48">
                    <path fill="#EA4335" d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z"/>
                    <path fill="#4285F4" d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z"/>
                    <path fill="#FBBC05" d="M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z"/>
                    <path fill="#34A853" d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z"/>
                </svg>
                {loading ? "Entrando..." : "Entrar com Google"}
            </button>
        </div>
    )
}

export default Login_Inputs