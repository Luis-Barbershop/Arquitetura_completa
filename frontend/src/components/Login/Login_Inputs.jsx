import Styles from "./CSS/Login_inputs.module.css"
import { useState } from "react"
import { useNavigate, Link, useLocation } from "react-router-dom"
import { loginUser, loginWithGoogle, checkEmailExists, translateFirebaseError } from "../../services/authService"

const INSTALL_AFTER_LOGIN_FLAG = 'pwa_install_after_login'
const LOGIN_SUCCESS_EVENT = 'cortaai:login-success'

const triggerPostLoginInstallPrompt = () => {
    sessionStorage.setItem(INSTALL_AFTER_LOGIN_FLAG, 'true')
    window.dispatchEvent(new CustomEvent(LOGIN_SUCCESS_EVENT))
}

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

    // Centraliza detalhes do erro de auth para facilitar debug do /auth/verify.
    const getAuthErrorDetails = (err) => ({
        code: err?.code || err?.response?.data?.code || "UNKNOWN_AUTH_ERROR",
        status: err?.response?.status || null,
        message: err?.serverMessage || err?.response?.data?.message || err?.message || "Erro desconhecido no login.",
        profileComplete: err?.profileData?.profileComplete,
        roleReturned: err?.profileData?.role,
    });

   const handleLogin = async (e) => {
        e.preventDefault();
        setError(null);

        // Persiste a intenção do usuário antes de tentar login (para cross-validation e redirect)
        sessionStorage.setItem('user_intent', role);

        try {
            await loginUser(email, password);
            triggerPostLoginInstallPrompt();
            navigate(getRedirectPath());

        } catch (err) {
            getAuthErrorDetails(err);

            // ── Perfil incompleto — redireciona para finalizar cadastro ──────────
            if (err.code === 'PROFILE_INCOMPLETE') {
                const profileData = err.profileData || {};
                const gData = {
                    idToken:     localStorage.getItem('token'),
                    uid:         localStorage.getItem('userId'),
                    email:       profileData.email || localStorage.getItem('userEmail') || email,
                    displayName: profileData.name  || '',
                };
                navigate('/signin', {
                    state: {
                        mode: 'complete-profile',
                        step: 2,
                        role: profileData.role?.includes('BARBER') || profileData.role?.includes('OWNER')
                            ? 'barber'
                            : role,
                        prefillEmail: gData.email,
                        prefillName:  gData.displayName,
                        googleData:   gData,
                    }
                });
                return;
            }

            // ── Conflito de perfil (barbeiro no portal cliente ou vice-versa) ─────
            if (err.code === 'ROLE_CONFLICT') {
                sessionStorage.removeItem('user_intent');
                setError(err.serverMessage || 'Você possui uma conta em outro portal. Verifique o link de acesso correto.');
                return;
            }

            // ── E-mail não verificado → tela de espera ───────────────────────────
            if (err.code === 'VERIFICATION_REQUIRED' || err.verificationRequired || err.response?.data?.verificationRequired) {
                navigate('/verify-email', {
                    // Passa a senha para que o reenvio não precise pedí-la de novo
                    state: { mode: 'waiting', email: err.verificationEmail || email, password, role }
                });
                return;
            }

            // ── Redirecionamento inteligente ──────────────────────────────────────
            const rawMsg = err.response?.data?.message || "";
            const isCredentialError = rawMsg.includes("INVALID_PASSWORD")
                || rawMsg.includes("EMAIL_NOT_FOUND")
                || rawMsg.includes("INVALID_LOGIN_CREDENTIALS")
                || err.response?.status === 401;

            if (isCredentialError) {
                try {
                    const { exists } = await checkEmailExists(email);
                    if (!exists) {
                        navigate("/signin", {
                            state: { mode: "register", role, prefillEmail: email }
                        });
                        return;
                    }
                } catch { /* se o check falhar, exibe erro normal */ }
            }

            const friendlyMsg = translateFirebaseError(rawMsg, rawMsg || "Falha no login. Verifique seus dados.");
            setError(friendlyMsg);
        }
    };

    const handleGoogleLogin = async () => {
        setError(null);
        setLoadingGoogle(true);

        // Persiste a intenção antes do popup Google (o popup pode mudar o contexto)
        sessionStorage.setItem('user_intent', role);

        try {
            await loginWithGoogle();
            triggerPostLoginInstallPrompt();
            navigate(getRedirectPath());
        } catch (err) {
            setLoadingGoogle(false);

            // ── Perfil incompleto — redireciona para finalizar cadastro (step 2) ─
            if (err.code === 'PROFILE_INCOMPLETE') {
                const profileData = err.profileData || {};
                const gData = {
                    idToken:     localStorage.getItem('token'),
                    uid:         localStorage.getItem('userId'),
                    email:       profileData.email || localStorage.getItem('userEmail') || '',
                    displayName: profileData.name  || '',
                };
                navigate('/signin', {
                    state: {
                        mode: 'complete-profile',
                        step: 2,
                        role: profileData.role?.includes('BARBER') || profileData.role?.includes('OWNER')
                            ? 'barber'
                            : role,
                        prefillEmail: gData.email,
                        prefillName:  gData.displayName,
                        googleData:   gData,
                    }
                });
                return;
            }

            // ── Conflito de perfil ────────────────────────────────────────────────
            if (err.code === 'ROLE_CONFLICT') {
                sessionStorage.removeItem('user_intent');
                setError(err.serverMessage || 'Você possui uma conta em outro portal. Verifique o link de acesso correto.');
                return;
            }

            if (err.code === "USER_NOT_FOUND") {
                // Usuário autenticado no Google mas não cadastrado no CortaAI
                // Pula direto para a etapa 2 do cadastro (telefone + senha), sem mostrar etapa 1
                const intent = sessionStorage.getItem('user_intent') || role;
                sessionStorage.removeItem('user_intent');
                navigate("/signin", {
                    state: {
                        mode: "register",
                        step: 2,                               // ← salta etapa 1
                        role: intent,
                        prefillEmail: err.googleData?.email || "",
                        prefillName: err.googleData?.displayName || "",
                        googleData: err.googleData,
                    }
                });
                return;
            }
            const msg = translateFirebaseError(
                err.response?.data?.message || err.message || '',
                err.response?.data?.message || err.message || "Falha no login com Google."
            );
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

            <p style={{ textAlign: 'center', marginTop: 16, fontSize: 14, color: 'rgba(255,255,255,0.6)' }}>
                Não tem uma conta?{' '}
                <button
                    type="button"
                    onClick={() => navigate('/signin', { state: { mode: 'register', role, prefillEmail: email } })}
                    style={{ background: 'none', border: 'none', color: '#e8a045', cursor: 'pointer', fontWeight: 600, fontSize: 14, padding: 0 }}
                >
                    Crie agora
                </button>
            </p>

            {/* Alternância de perfil: permite trocar entre portal Barbeiro e portal Cliente */}
            <p style={{ textAlign: 'center', marginTop: 8, fontSize: 13, color: 'rgba(255,255,255,0.45)' }}>
                <button
                    type="button"
                    onClick={() => navigate('/login', {
                        state: { role: role === 'barber' ? 'customer' : 'barber' }
                    })}
                    style={{ background: 'none', border: 'none', color: '#a0c4ff', cursor: 'pointer', fontWeight: 600, fontSize: 13, padding: 0 }}
                >
                    {role === 'barber' ? 'Entrar como Cliente' : 'Entrar como Barbeiro'}
                </button>
            </p>
        </div>
    )
}

export default Login_Inputs