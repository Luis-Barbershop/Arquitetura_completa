import Styles from "./CSS/SignIn_inputs.module.css"
import { useState, useMemo, useEffect } from "react"
import { useNavigate, useLocation } from "react-router-dom"
import {
    registerCustomer,
    registerBarber,
    loginWithGoogle,
    completeProfileCustomer,
    completeProfileBarber,
    translateFirebaseError
} from "../../services/authService"

// ─── Avalia força da senha ────────────────────────────────────────────────────
function evaluatePasswordStrength(pwd) {
    if (!pwd) return { score: 0, label: '', color: '' };
    let score = 0;
    if (pwd.length >= 8) score++;
    if (/[A-Z]/.test(pwd)) score++;
    if (/\d/.test(pwd)) score++;
    if (/[!@#$%^&*()_+\-=[\]{};':"\\|,.<>/?]/.test(pwd)) score++;
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

    // step 1 = "Conta e acesso" (e-mail/senha ou botão Google)
    // step 2 = "Dados pessoais" (nome, tel, CPF, nascimento)
    const [step, setStep] = useState(location.state?.step === 2 ? 2 : 1);

    // Dados do Google — guardados em state para sobreviver a re-navegações
    // dentro da mesma rota (/signin → /signin) sem re-montar o componente.
    const [googleData, setGoogleData] = useState(location.state?.googleData || null);
    const isGoogleFlow = !!googleData;

    // ── Step 1: dados de acesso ───────────────────────────────────────────────
    const [email, setEmail] = useState(location.state?.prefillEmail || "");
    const [password, setPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");

    // ── Step 2: dados pessoais ────────────────────────────────────────────────
    const [name, setName]           = useState(location.state?.prefillName || "");
    const [tell, setTell]           = useState("");
    const [cpf, setCpf]             = useState("");
    const [birthDate, setBirthDate] = useState("");

    // Quando o navigate() é chamado dentro da própria rota /signin (ex: USER_NOT_FOUND
    // ou PROFILE_INCOMPLETE do Google), o componente NÃO re-monta — só o location muda.
    // Este effect sincroniza step, nome, e-mail e googleData com o novo location.state.
    useEffect(() => {
        if (location.state?.step === 2) setStep(2);
        if (location.state?.prefillName)  setName(location.state.prefillName);
        if (location.state?.prefillEmail) setEmail(location.state.prefillEmail);
        if (location.state?.googleData)   setGoogleData(location.state.googleData);
    }, [location.state]);

    const [error, setError] = useState(null);
    const [loadingGoogle, setLoadingGoogle] = useState(false);

    const passwordStrength = useMemo(() => evaluatePasswordStrength(password), [password]);

    // Retorna a rota certa após login Google bem-sucedido
    function getRedirectPath() {
        const role = localStorage.getItem('userRole') || 'ROLE_CUSTOMER';
        if (role === 'ROLE_OWNER' || role === 'ROLE_BARBER') return '/barberHome';
        return '/homepage';
    }

    // ── Google ────────────────────────────────────────────────────────────────
    const handleGoogleSignIn = async () => {
        setError(null);
        setLoadingGoogle(true);
        sessionStorage.setItem('user_intent', userType);
        try {
            await loginWithGoogle();
            navigate(getRedirectPath());
        } catch (err) {
            setLoadingGoogle(false);
            if (err.code === 'PROFILE_INCOMPLETE') {
                // Usuário já existe no Firebase mas sem perfil no CortaAI.
                // Salva o googleData no state e vai direto para o step 2.
                // NÃO navega — mantém na página para não perder o state.
                // O idToken já está em localStorage.token (salvo pelo loginWithGoogle).
                const gData = err.googleData || {
                    idToken:     localStorage.getItem('token'),
                    uid:         localStorage.getItem('userId'),
                    email:       err.profileData?.email || localStorage.getItem('userEmail'),
                    displayName: err.profileData?.name  || '',
                };
                if (gData.email) setEmail(gData.email);
                if (gData.displayName) setName(gData.displayName);
                setGoogleData(gData);
                setStep(2);
                return;
            }
            if (err.code === 'ROLE_CONFLICT') {
                sessionStorage.removeItem('user_intent');
                setError(err.serverMessage || 'Você possui uma conta em outro portal. Acesse o portal correto.');
                return;
            }
            if (err.code === 'USER_NOT_FOUND') {
                // Usuário Google não cadastrado — pula direto para step 2 com dados pré-preenchidos.
                // Usa navigate para passar googleData; o useEffect acima sincroniza o state.
                navigate('/signin', {
                    state: {
                        mode: 'register',
                        step: 2,
                        role: userType,
                        prefillEmail: err.googleData?.email || '',
                        prefillName:  err.googleData?.displayName || '',
                        googleData:   err.googleData,
                    }
                });
                return;
            }
            const msg = translateFirebaseError(
                err.response?.data?.message || err.message || '',
                'Falha ao entrar com o Google. Tente novamente.'
            );
            setError(msg);
        }
    };

    // ── Step 1: valida e-mail/senha e avança para step 2 ─────────────────────
    const handleNextStep = (e) => {
        e.preventDefault();
        if (!email) { setError("Informe o e-mail."); return; }
        if (password !== confirmPassword) { setError("As senhas não coincidem."); return; }
        if (passwordStrength.score < 4) {
            setError("Senha fraca. Use ao menos 8 caracteres, 1 maiúscula, 1 número e 1 caractere especial.");
            return;
        }
        setError(null);
        setStep(2);
    };

    // ── Step 2: cadastro final ────────────────────────────────────────────────
    const handleRegister = async (e) => {
        e.preventDefault();
        setError(null);

        if (!name || !cpf || !tell || !birthDate) {
            setError("Preencha todos os campos obrigatórios.");
            return;
        }

        // ── Fluxo Google: usuário já existe no Firebase, só completa o perfil ──
        if (isGoogleFlow && googleData?.idToken) {
            localStorage.setItem('token', googleData.idToken);
            localStorage.setItem('userId', googleData.uid || '');
            localStorage.setItem('userEmail', googleData.email || '');
            try {
                let profileData;
                if (userType === "barber") {
                    profileData = await completeProfileBarber({
                        tell, documentCPF: cpf, name, birthDate,
                    });
                } else {
                    profileData = await completeProfileCustomer({
                        tell, documentCPF: cpf, name, birthDate,
                    });
                }
                const role = profileData?.role || (userType === "barber" ? "ROLE_BARBER" : "ROLE_CUSTOMER");
                localStorage.setItem('userRole', role);
                localStorage.setItem('isOwner', String(profileData?.isOwner || false));
                sessionStorage.removeItem('user_intent');

                // Se o e-mail ainda não foi verificado (conta email/senha com perfil incompleto),
                // redireciona para aguardar confirmação em vez de liberar acesso direto.
                if (profileData?.emailVerified === false) {
                    navigate('/verify-email', {
                        state: { mode: 'waiting', email: googleData.email || '', role: userType }
                    });
                    return;
                }

                navigate(getRedirectPath());
            } catch (err) {
                console.error(err);
                setError(err.response?.data?.message || "Erro ao completar perfil. Verifique os dados.");
                localStorage.removeItem('token');
                localStorage.removeItem('userId');
                localStorage.removeItem('userEmail');
            }
            return;
        }

        // ── Fluxo e-mail/senha ────────────────────────────────────────────────
        try {
            if (userType === "customer") {
                await registerCustomer({
                    name, email, documentCPF: cpf, tell, password, birthDate,
                });
            } else {
                await registerBarber({
                    name, email, documentCPF: cpf, tell, password, birthDate,
                });
            }
            // Redireciona para tela de "aguardando verificação de e-mail"
            navigate('/verify-email', {
                state: { mode: 'waiting', email, password, role: userType }
            });
        } catch (err) {
            console.error(err);
            const rawMsg = err?.response?.data?.message || err?.message || '';
            const registerMsg = translateFirebaseError(rawMsg, 'Erro ao cadastrar. Verifique os dados.');
            setError(registerMsg);
        }
    };

    return (
        <div className={Styles.SignIn_inputs_container}>
            {/* No fluxo Google não há step 1 de e-mail/senha, então omitimos o indicador */}
            {!isGoogleFlow && (
                <div className={Styles.stepHeader}>
                    <span className={step === 1 ? Styles.activeStep : Styles.step}>1. Conta e acesso</span>
                    <span className={step === 2 ? Styles.activeStep : Styles.step}>2. Dados pessoais</span>
                </div>
            )}
            {isGoogleFlow && (
                <div className={Styles.stepHeader}>
                    <span className={Styles.activeStep}>Dados pessoais — conta Google</span>
                </div>
            )}

            <h3 className={Styles.formType}>Cadastro de {userType === "barber" ? "Barbeiro" : "Cliente"}</h3>

            <form onSubmit={step === 1 ? handleNextStep : handleRegister}>

                {/* ── STEP 1: Conta e acesso ───────────────────────────────── */}
                {step === 1 && (
                    <>
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
                            <p>Senha</p>
                            <input
                                className={Styles.formInput}
                                type="password"
                                value={password}
                                onChange={e => setPassword(e.target.value)}
                                placeholder="Mínimo 8 caracteres"
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
                                placeholder="Repita a senha"
                                required
                            />
                            {confirmPassword && password !== confirmPassword && (
                                <p className={Styles.error_message} style={{ marginTop: 4 }}>
                                    As senhas não coincidem.
                                </p>
                            )}
                        </label>

                        {error && <p className={Styles.error_message}>{error}</p>}

                        <button type="submit" className={Styles.SignIn_button}>
                            Continuar
                        </button>

                        <div className={Styles.divider}>
                            <span className={Styles.dividerLine} />
                            <span className={Styles.dividerText}>ou</span>
                            <span className={Styles.dividerLine} />
                        </div>

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
                                onError={e => { e.target.style.display = 'none'; }}
                            />
                            {loadingGoogle ? "Conectando..." : "Cadastrar com o Google"}
                        </button>
                    </>
                )}

                {/* ── STEP 2: Dados pessoais ───────────────────────────────── */}
                {step === 2 && (
                    <>
                        <label className={Styles.label_name}>
                            <p>Nome completo</p>
                            <input
                                className={Styles.formInput}
                                type="text"
                                value={name}
                                onChange={e => setName(e.target.value)}
                                placeholder="Seu nome completo"
                                required
                            />
                        </label>

                        <label className={Styles.label_name}>
                            <p>Telefone</p>
                            <input
                                className={Styles.formInput}
                                type="text"
                                value={tell}
                                onChange={e => setTell(e.target.value.replace(/\D/g, '').slice(0, 11))}
                                placeholder="11999999999"
                                maxLength={11}
                                inputMode="numeric"
                                required
                            />
                        </label>

                        <label className={Styles.label_name}>
                            <p>CPF</p>
                            <input
                                className={Styles.formInput}
                                type="text"
                                value={cpf}
                                onChange={e => setCpf(e.target.value.replace(/\D/g, '').slice(0, 11))}
                                placeholder="Somente números (11 dígitos)"
                                maxLength={11}
                                inputMode="numeric"
                                required
                            />
                        </label>

                        <label className={Styles.label_name}>
                            <p>Data de nascimento</p>
                            <input
                                className={Styles.formInput}
                                type="date"
                                value={birthDate}
                                onChange={e => setBirthDate(e.target.value)}
                                max={new Date().toISOString().split('T')[0]}
                                required
                            />
                        </label>

                        {error && <p className={Styles.error_message}>{error}</p>}

                        <div className={Styles.actionsRow}>
                            {/* Fluxo Google não tem senha no step 1 — não faz sentido voltar */}
                            {!isGoogleFlow && (
                                <button
                                    type="button"
                                    className={Styles.secondaryButton}
                                    onClick={() => setStep(1)}
                                >
                                    Voltar
                                </button>
                            )}
                            <button type="submit" className={Styles.SignIn_button}>
                                {isGoogleFlow ? "Finalizar cadastro" : "Cadastrar"}
                            </button>
                        </div>
                    </>
                )}
            </form>

        </div>
    );
}

export default SignIn_inputs;
