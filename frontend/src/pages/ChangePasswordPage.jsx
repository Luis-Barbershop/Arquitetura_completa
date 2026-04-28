import { useEffect, useMemo, useRef, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import {
    confirmPasswordReset,
    onAuthStateChanged,
    signInWithEmailAndPassword,
    verifyPasswordResetCode,
} from "firebase/auth";
import { auth } from "../services/firebase";
import { changePassword } from "../services/authService";
import Styles from "./CSS/LoginPage.module.css";
import FPStyles from "./CSS/ForgotPasswordPage.module.css";
import CPStyles from "./CSS/ChangePasswordPage.module.css";

// ─── Avalia força da senha ────────────────────────────────────────────────────
function evaluatePasswordStrength(pwd) {
    if (!pwd) return { score: 0, label: '', color: '' };
    let score = 0;
    if (pwd.length >= 8) score++;
    if (/[A-Z]/.test(pwd)) score++;
    if (/\d/.test(pwd)) score++;
    if (/[^A-Za-z0-9]/.test(pwd)) score++;
    const map = [
        { label: 'Muito fraca', color: '#e74c3c' },
        { label: 'Fraca',       color: '#e67e22' },
        { label: 'Média',       color: '#f1c40f' },
        { label: 'Forte',       color: '#2ecc71' },
        { label: 'Muito forte', color: '#27ae60' },
    ];
    return { score, ...map[score] };
}

function ChangePasswordPage() {
    const [newPassword, setNewPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");
    const [loading, setLoading] = useState(false);
    const [success, setSuccess] = useState(false);
    const [error, setError] = useState(null);
    // null = resolvendo, true = pode trocar, false = login social
    const [canChangePassword, setCanChangePassword] = useState(null);
    const [resetEmail, setResetEmail] = useState("");
    const [resetCodeValid, setResetCodeValid] = useState(false);
    // Bloqueia o redirect enquanto o Firebase SDK não resolveu o estado inicial
    const [initializing, setInitializing] = useState(true);
    const navigate = useNavigate();
    const location = useLocation();
    // Ref para evitar redirect do onAuthStateChanged enquanto mudança está em andamento
    const changingPassword = useRef(false);

    const idToken = localStorage.getItem("token");
    const searchParams = new URLSearchParams(location.search);
    const mode = searchParams.get("mode");
    const oobCode = searchParams.get("oobCode");
    const isPasswordResetFlow = mode === "resetPassword" && Boolean(oobCode);

    // Fluxo autenticado usa authProvider vindo do backend. O Firebase SDK pode
    // não estar logado no browser quando o login por e-mail veio via backend.
    useEffect(() => {
        if (isPasswordResetFlow) {
            verifyPasswordResetCode(auth, oobCode)
                .then((email) => {
                    setResetEmail(email);
                    setResetCodeValid(true);
                    setCanChangePassword(true);
                    setInitializing(false);
                })
                .catch(() => {
                    setResetCodeValid(false);
                    setError("Link de redefinicao invalido ou expirado. Solicite um novo link.");
                    setInitializing(false);
                });
            return undefined;
        }

        const storedProvider = (localStorage.getItem("authProvider") || "").toUpperCase();
        if (storedProvider) {
            setCanChangePassword(storedProvider === "EMAIL");
            setInitializing(false);
            return undefined;
        }

        if (idToken) {
            setCanChangePassword(true);
            setInitializing(false);
            return undefined;
        }

        const unsubscribe = onAuthStateChanged(auth, (firebaseUser) => {
            setInitializing(false);
            if (!firebaseUser) {
                if (changingPassword.current) return;
                navigate('/');
                return;
            }
            const providers = firebaseUser.providerData.map((p) => p.providerId);
            const hasPasswordProvider = providers.includes('password');
            setCanChangePassword(hasPasswordProvider);
        });
        return () => unsubscribe();
    }, [idToken, isPasswordResetFlow, navigate, oobCode]);

    const passwordStrength = useMemo(() => evaluatePasswordStrength(newPassword), [newPassword]);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError(null);

        if (!canChangePassword) {
            setError("Contas com login social não podem alterar senha por esta rota. Use o provedor de login original.");
            return;
        }

        if (passwordStrength.score < 4) {
            setError("A senha não é forte o suficiente. Use no mínimo 8 caracteres, 1 maiúscula, 1 número e 1 caractere especial.");
            return;
        }
        if (newPassword !== confirmPassword) {
            setError("As senhas nao coincidem.");
            return;
        }
        if (isPasswordResetFlow && !resetCodeValid) {
            setError("Link de redefinicao invalido ou expirado. Solicite um novo link.");
            return;
        }

        if (!isPasswordResetFlow && !idToken) {
            setError("Sessao expirada. Faca login novamente.");
            navigate("/");
            return;
        }

        setLoading(true);
        try {
            changingPassword.current = true;
            if (isPasswordResetFlow) {
                await confirmPasswordReset(auth, oobCode, newPassword);
                setSuccess(true);
                return;
            }

            const { data } = await changePassword(idToken, newPassword);

            // Ressincroniza a sessão do Firebase SDK com a nova senha para evitar logout automático
            const email = localStorage.getItem('userEmail');
            if (email) {
                try {
                    const cred = await signInWithEmailAndPassword(auth, email, newPassword);
                    const freshToken = await cred.user.getIdToken();
                    localStorage.setItem('token', freshToken);
                } catch {
                    // Fallback: usa o idToken retornado pelo backend
                    if (data?.idToken) localStorage.setItem('token', data.idToken);
                }
            } else if (data?.idToken) {
                localStorage.setItem('token', data.idToken);
            }

            setSuccess(true);
        } catch (err) {
            changingPassword.current = false;
            const msg = err.response?.data?.message || "Nao foi possivel alterar a senha. Tente novamente.";
            setError(msg);
        } finally {
            setLoading(false);
        }
    };

    if (initializing) return null;

    return (
        <div className={Styles.loginStage}>
            <div className={Styles.loginShell}>
                <aside className={Styles.brandPanel}>
                    <div className={Styles.brandBadge}>
                        <img src="/Icons/scissors_icon.png" alt="Icone CortaAI" />
                        <span>CortaAI</span>
                    </div>

                    <p className={Styles.kicker}>SEGURANCA DA CONTA</p>
                    <h1 className={Styles.title_login}>Mantenha sua conta sempre protegida.</h1>
                    <p className={Styles.subtitle}>Crie uma nova senha forte para continuar acessando seus agendamentos com seguranca.</p>

                    <div className={Styles.tagRow}>
                        <span className={Styles.softTag}>Mínimo 8 caracteres</span>
                        <span className={Styles.softTag}>Sessões anteriores encerradas</span>
                    </div>

                    <ul className={Styles.featuresList}>
                        <li>Use letras, números e símbolos</li>
                        <li>Evite senhas já utilizadas</li>
                        <li>Você permanecerá conectado</li>
                    </ul>
                </aside>

                <section className={Styles.formPanel}>
                    {success ? (
                        <div className={FPStyles.successBox}>
                            <span className={FPStyles.successIcon}>✓</span>
                            <h2>Senha alterada!</h2>
                            <p>
                                {isPasswordResetFlow
                                    ? "Sua senha foi atualizada com sucesso. Faca login com a nova senha."
                                    : "Sua senha foi atualizada com sucesso. Você continua conectado."}
                            </p>
                            {isPasswordResetFlow && (
                                <Link to="/login" className={FPStyles.backBtn}>Ir para o login</Link>
                            )}
                        </div>
                    ) : (
                        <>
                            <h2>{isPasswordResetFlow ? "Redefinir senha" : "Alterar senha"}</h2>
                            <p>
                                {isPasswordResetFlow
                                    ? `Escolha uma nova senha para ${resetEmail || "sua conta"}.`
                                    : "Escolha uma nova senha para sua conta."}
                            </p>

                            {/* Resolvendo: aguardando Firebase confirmar o provedor */}
                            {canChangePassword === null && !(isPasswordResetFlow && error) && (
                                <div className={FPStyles.successBox}>
                                    <h2>{isPasswordResetFlow ? "Validando link..." : "Validando login..."}</h2>
                                    <p>{isPasswordResetFlow ? "Conferindo o codigo de redefinicao." : "Identificando seu provedor de autenticação."}</p>
                                </div>
                            )}

                            {isPasswordResetFlow && error && canChangePassword !== true && (
                                <div className={FPStyles.successBox}>
                                    <h2>Link indisponivel</h2>
                                    <p className={FPStyles.errorMsg}>{error}</p>
                                    <Link to="/forgot-password" className={FPStyles.backBtn}>Solicitar novo link</Link>
                                </div>
                            )}

                            {/* Login social: bloqueia completamente o formulário */}
                            {canChangePassword === false && !isPasswordResetFlow && (
                                <div className={FPStyles.successBox}>
                                    <h2>🔒 Conta vinculada ao Google</h2>
                                    <p>
                                        Você acessa o CortaAi pelo Google, portanto não existe uma senha
                                        específica do CortaAi para alterar. Para gerenciar sua senha,
                                        acesse as configurações de segurança da sua conta Google.
                                    </p>
                                    <a
                                        href="https://myaccount.google.com/security"
                                        target="_blank"
                                        rel="noopener noreferrer"
                                        className={FPStyles.backBtn}
                                    >
                                        Abrir Segurança da Conta Google
                                    </a>
                                </div>
                            )}

                            {/* Usuário email/senha: exibe o formulário */}
                            {canChangePassword === true && (
                            <form onSubmit={handleSubmit} className={FPStyles.form}>
                                <label className={FPStyles.fieldLabel}>
                                    <p className={FPStyles.labelText}>Nova senha</p>
                                    <div className={CPStyles.inputWrapper}>
                                        <input
                                            type="password"
                                            placeholder="Mín. 8 caracteres, 1 maiúscula, 1 número, 1 especial"
                                            value={newPassword}
                                            onChange={(e) => setNewPassword(e.target.value)}
                                            required
                                            minLength={8}
                                            className={FPStyles.input}
                                        />
                                    </div>
                                    {/* Medidor de força da senha */}
                                    {newPassword.length > 0 && (
                                        <div style={{ marginTop: 8 }}>
                                            <div style={{ display: 'flex', gap: 4 }}>
                                                {[1, 2, 3, 4].map(i => (
                                                    <div key={i} style={{
                                                        flex: 1, height: 4, borderRadius: 2,
                                                        background: i <= passwordStrength.score
                                                            ? passwordStrength.color
                                                            : 'rgba(255,255,255,0.15)',
                                                        transition: 'background 0.3s',
                                                    }} />
                                                ))}
                                            </div>
                                            <p style={{ fontSize: 11, color: passwordStrength.color, marginTop: 4 }}>
                                                {passwordStrength.label} — Mín. 8 caracteres, 1 maiúscula, 1 número, 1 especial
                                            </p>
                                        </div>
                                    )}
                                </label>

                                <label className={FPStyles.fieldLabel}>
                                    <p className={FPStyles.labelText}>Confirmar nova senha</p>
                                    <input
                                        type="password"
                                        placeholder="Repita a nova senha"
                                        value={confirmPassword}
                                        onChange={(e) => setConfirmPassword(e.target.value)}
                                        required
                                        className={`${FPStyles.input} ${confirmPassword && confirmPassword !== newPassword ? CPStyles.inputError : ""}`}
                                    />
                                    {confirmPassword && confirmPassword !== newPassword && (
                                        <span className={CPStyles.fieldHint}>As senhas nao coincidem</span>
                                    )}
                                </label>

                                {error && <p className={FPStyles.errorMsg}>{error}</p>}

                                <button
                                    type="submit"
                                    disabled={loading}
                                    className={FPStyles.submitBtn}
                                >
                                    {loading ? "Alterando..." : "Alterar senha"}
                                </button>
                            </form>
                            )}

                            <div className={Styles.footerActions}>
                                <Link className={Styles.homeLink} to={isPasswordResetFlow ? "/login" : "/homepage"}>Cancelar</Link>
                            </div>
                        </>
                    )}
                </section>
            </div>
        </div>
    );
}

export default ChangePasswordPage;
