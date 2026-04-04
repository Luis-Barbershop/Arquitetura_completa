import Styles from "./CSS/Login_inputs.module.css"
import { useState } from "react"
import { useNavigate, Link, useLocation } from "react-router-dom"
import { loginUser, loginWithGoogle, checkEmailExists } from "../../services/authService"

// Retorna a rota de destino com base no role salvo no localStorage
function getRedirectPath() {
    const role = localStorage.getItem('userRole') || 'ROLE_CUSTOMER';
    if (role === 'ROLE_OWNER' || role === 'ROLE_BARBER') return '/barberHome';
    return '/homepage';
}

function Login_Inputs() {

    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState(null);
    const [loadingGoogle, setLoadingGoogle] = useState(false);
    const navigate = useNavigate();
    const location = useLocation();
    const role = location.state?.role || "customer";

   const handleLogin = async (e) => {
        e.preventDefault();
        setError(null);

        try {
            await loginUser(email, password);
            navigate(getRedirectPath());

        } catch (err) {
            console.error(err);

            // ── Redirecionamento inteligente ──────────────────────────────────────
            // Se o erro indica credenciais inválidas, verificamos se o e-mail
            // sequer existe no banco antes de mostrar o erro ao usuário.
            const rawMsg = err.response?.data?.message || "";
            const isCredentialError = rawMsg.includes("INVALID_PASSWORD")
                || rawMsg.includes("EMAIL_NOT_FOUND")
                || rawMsg.includes("INVALID_LOGIN_CREDENTIALS")
                || err.response?.status === 401;

            if (isCredentialError) {
                try {
                    const { exists } = await checkEmailExists(email);
                    if (!exists) {
                        // E-mail não existe → redireciona para cadastro pré-preenchido
                        navigate("/signin", {
                            state: { mode: "register", role, prefillEmail: email }
                        });
                        return;
                    }
                } catch (_) {
                    // Se o check falhar, exibe erro normal
                }
            }

            const msg = rawMsg || "Falha no login. Verifique seus dados.";
            setError(msg);
        }
    };

    const handleGoogleLogin = async () => {
        setError(null);
        setLoadingGoogle(true);
        try {
            await loginWithGoogle();
            navigate(getRedirectPath());
        } catch (err) {
            setLoadingGoogle(false);
            if (err.code === "USER_NOT_FOUND") {
                // Usuário autenticado no Google mas não cadastrado no CortaAI
                navigate("/signin", {
                    state: {
                        mode: "register",
                        role,
                        prefillEmail: err.googleData?.email || "",
                        googleData: err.googleData,
                    }
                });
                return;
            }
            const msg = err.response?.data?.message || err.message || "Falha no login com Google.";
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
                    <Link to="/forgot-password" className={Styles.forgot_password_text}>Esqueceu a Senha?</Link>
                </label>

                {error && <p className={Styles.errorMessage}>{error}</p>}

                <button type="submit" className={Styles.Login_button}>Entrar</button>

            </form>

            <div className={Styles.divider}>
                <span className={Styles.dividerLine} />
                <span className={Styles.dividerText}>ou</span>
                <span className={Styles.dividerLine} />
            </div>

            <button
                type="button"
                className={Styles.googleButton}
                onClick={handleGoogleLogin}
                disabled={loadingGoogle}
            >
                <img
                    src="/Icons/google_icon.svg"
                    alt="Google"
                    className={Styles.googleIcon}
                    onError={(e) => { e.target.style.display = 'none'; }}
                />
                {loadingGoogle ? "Conectando..." : "Entrar com o Google"}
            </button>
        </div>
    )
}

export default Login_Inputs