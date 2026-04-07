import Styles from "./CSS/SignIn_inputs.module.css"
import { useState, useMemo } from "react"
import { useNavigate, useLocation } from "react-router-dom"
import { registerCustomer, registerBarber, loginWithGoogle, translateFirebaseError } from "../../services/authService"

// ─── Avalia força da senha ────────────────────────────────────────────────────
function evaluatePasswordStrength(pwd) {
    if (!pwd) return { score: 0, label: '', color: '' };
    let score = 0;
    if (pwd.length >= 8) score++;
    if (/[A-Z]/.test(pwd)) score++;
    if (/\d/.test(pwd)) score++;
    if (/[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>/?]/.test(pwd)) score++;
    const map = [
        { label: 'Muito fraca', color: '#e74c3c' },
        { label: 'Fraca',       color: '#e67e22' },
        { label: 'Média',       color: '#f1c40f' },
        { label: 'Forte',       color: '#2ecc71' },
        { label: 'Muito forte', color: '#27ae60' },
    ];
    return { score, ...map[score] };
}

function SignIn_inputs() {

    const navigate = useNavigate();
    const location = useLocation();
    const userType = location.state?.role || "customer";

    const [step, setStep] = useState(1);
    const [registered, setRegistered] = useState(false);

    const [name, setName] = useState("");
    // Pré-preenche e-mail se vier do redirecionamento do login
    const [email, setEmail] = useState(location.state?.prefillEmail || "");
    const [cpf, setCpf] = useState("");

    const [tell, setTell] = useState("");
    const [password, setPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");

    const [workStart, setWorkStart] = useState("09:00");
    const [workEnd, setWorkEnd] = useState("18:00");

    const [error, setError] = useState(null);
    const [loadingGoogle, setLoadingGoogle] = useState(false);

    const passwordStrength = useMemo(() => evaluatePasswordStrength(password), [password]);

    // Retorna a rota certa após login Google bem-sucedido
    function getRedirectPath() {
        const role = localStorage.getItem('userRole') || 'ROLE_CUSTOMER';
        if (role === 'ROLE_OWNER' || role === 'ROLE_BARBER') return '/barberHome';
        return '/homepage';
    }

    const handleGoogleSignIn = async () => {
        setError(null);
        setLoadingGoogle(true);
        // Persiste intenção para cross-validation e redirect correto
        sessionStorage.setItem('user_intent', userType);
        try {
            await loginWithGoogle();
            navigate(getRedirectPath());
        } catch (err) {
            setLoadingGoogle(false);
            if (err.code === 'ROLE_CONFLICT') {
                sessionStorage.removeItem('user_intent');
                setError(err.serverMessage || 'Você possui uma conta em outro portal. Acesse o portal correto.');
                return;
            }
            if (err.code === 'USER_NOT_FOUND') {
                // Usuário Google não cadastrado — pré-preenche e-mail e nome do Google
                setEmail(err.googleData?.email || email);
                if (err.googleData?.displayName) {
                    setName(err.googleData.displayName);
                }
                setError('Conta Google não encontrada. Complete seu cadastro abaixo.');
                return;
            }
            const msg = translateFirebaseError(
                err.response?.data?.message || err.message || '',
                'Falha ao entrar com o Google. Tente novamente.'
            );
            setError(msg);
        }
    };

    const handleNextStep = (e) => {
        e.preventDefault();

        if (!name || !email || !cpf) {
            setError("Preencha todos os campos.");
            return;
        }

        setError(null);
        setStep(2);
    };


    const handleRegister = async (e) => {
        e.preventDefault();
        setError(null);

        if (password !== confirmPassword) {
            setError("As senhas não coincidem.");
            return;
        }

        if (passwordStrength.score < 4) {
            setError("Senha fraca. Use pelo menos 8 caracteres, 1 maiúscula, 1 número e 1 caractere especial.");
            return;
        }
        try {
            if (userType === "customer") {
                await registerCustomer({
                    name, email, documentCPF: cpf, tell, password
                });
            } else {
                await registerBarber({
                    name, email, documentCPF: cpf, tell, password,
                    workStartTime: workStart,
                    workEndTime: workEnd,
                });
            }

            // Cadastro feito — Firebase enviou e-mail de verificação automaticamente
            // Usuário precisa verificar antes de fazer login
            setRegistered(true);

        } catch (err) {
            console.error(err);
            const msg = err.response?.data?.message || "Erro ao cadastrar. Verifique os dados.";
            setError(msg);
        }
    };

    // ── Tela pós-cadastro: avisa para verificar o e-mail ─────────────────────
    if (registered) {
        return (
            <div className={Styles.SignIn_inputs_container}>
                <div className={Styles.registeredState}>
                    <h3 className={Styles.registeredTitle}>Cadastro realizado!</h3>
                    <p className={Styles.registeredText}>
                        Um e-mail de verificacao foi enviado para <strong>{email}</strong>.
                    </p>
                    <p className={Styles.registeredHint}>
                        Verifique sua caixa de entrada (e o spam) e clique no link antes de fazer login.
                    </p>
                    <button
                        className={Styles.SignIn_button}
                        onClick={() => navigate("/login")}
                    >
                        Ir para o Login
                    </button>
                </div>
            </div>
        );
    }


    return (
        <div className={Styles.SignIn_inputs_container}>
            <div className={Styles.stepHeader}>
                <span className={step === 1 ? Styles.activeStep : Styles.step}>1. Dados pessoais</span>
                <span className={step === 2 ? Styles.activeStep : Styles.step}>2. Conta e acesso</span>
            </div>

            <h3 className={Styles.formType}>Cadastro de {userType === "barber" ? "Barbeiro" : "Cliente"}</h3>

            {step === 1 && (
                <>
                    <button
                        type="button"
                        className={Styles.googleButton}
                        onClick={handleGoogleSignIn}
                        disabled={loadingGoogle}
                    >
                        <img
                            src="/Icons/google_icon.svg"
                            alt="Google"
                            className={Styles.googleIcon}
                            onError={(e) => { e.target.style.display = 'none'; }}
                        />
                        {loadingGoogle ? "Conectando..." : "Cadastrar com o Google"}
                    </button>

                    <div className={Styles.divider}>
                        <span className={Styles.dividerLine} />
                        <span className={Styles.dividerText}>ou</span>
                        <span className={Styles.dividerLine} />
                    </div>
                </>
            )}

            <form onSubmit={step === 1 ? handleNextStep : handleRegister}>
                {step === 1 && (
                    <>
                        <label className={Styles.label_name}>
                            <p>Nome completo</p>
                            <input 
                                className={Styles.formInput}
                                type="text"
                                value={name}
                                onChange={e => setName(e.target.value)}
                                placeholder="Digite seu nome completo"
                                required
                            />
                        </label>

                        <label className={Styles.label_email}>
                            <p>E-mail</p>
                            <input 
                                className={Styles.formInput}
                                type="email"
                                value={email}
                                onChange={e => setEmail(e.target.value)}
                                placeholder="seuemail@exemplo.com"
                                required
                            />
                        </label>

                        <label className={Styles.label_name}>
                            <p>CPF</p>
                            <input
                                className={Styles.formInput}
                                type="text"
                                value={cpf}
                                onChange={e => setCpf(e.target.value)}
                                placeholder="Somente números"
                                required
                            />
                        </label>

                        {error && <p className={Styles.error_message}>{error}</p>}

                        <button type="submit" className={Styles.SignIn_button}>
                            Continuar
                        </button>
                    </>
                )}

                {step === 2 && (
                    <>
                        <label className={Styles.label_name}>
                            <p>Telefone</p>
                            <input 
                                className={Styles.formInput}
                                type="text"
                                value={tell}
                                onChange={e => setTell(e.target.value)}
                                placeholder="11999999999"
                                required
                            />
                        </label>

                        {userType === "barber" && (
                            <>
                                <label className={Styles.label_name}>
                                    <p>Inicio de expediente</p>
                                    <input 
                                        className={Styles.formInput}
                                        type="time"
                                        value={workStart}
                                        onChange={e => setWorkStart(e.target.value)}
                                        required
                                    />
                                </label>

                                <label className={Styles.label_name}>
                                    <p>Fim de expediente</p>
                                    <input 
                                        className={Styles.formInput}
                                        type="time"
                                        value={workEnd}
                                        onChange={e => setWorkEnd(e.target.value)}
                                        required
                                    />
                                </label>
                            </>
                        )}

                        <label className={Styles.label_name}>
                            <p>Senha</p>
                            <input 
                                className={Styles.formInput}
                                type="password"
                                value={password}
                                onChange={e => setPassword(e.target.value)}
                                required
                            />
                            {password && (
                                <div style={{ marginTop: 6 }}>
                                    <div style={{ display: 'flex', gap: 4 }}>
                                        {[1,2,3,4].map(i => (
                                            <div key={i} style={{
                                                flex: 1, height: 4, borderRadius: 2,
                                                background: i <= passwordStrength.score ? passwordStrength.color : 'rgba(255,255,255,0.15)',
                                                transition: 'background 0.3s'
                                            }} />
                                        ))}
                                    </div>
                                    <p style={{ fontSize: 11, color: passwordStrength.color, marginTop: 4 }}>
                                        {passwordStrength.label} — Min. 8 caracteres, 1 maiúscula, 1 número, 1 especial
                                    </p>
                                </div>
                            )}
                        </label>

                        <label className={Styles.label_name}>
                            <p>Confirmar senha</p>
                            <input 
                                className={Styles.formInput}
                                type="password"
                                value={confirmPassword}
                                onChange={e => setConfirmPassword(e.target.value)}
                                required
                            />
                        </label>

                        {error && <p className={Styles.error_message}>{error}</p>}

                        <div className={Styles.actionsRow}>
                            <button type="button" className={Styles.secondaryButton} onClick={() => setStep(1)}>
                                Voltar
                            </button>
                            <button type="submit" className={Styles.SignIn_button}>
                                Cadastrar
                            </button>
                        </div>
                    </>
                )}
            </form>

        </div>
    );
}

export default SignIn_inputs;
