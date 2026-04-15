import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { changePassword, logoutUser } from "../services/authService";
import api from "../services/api";
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
    const [authProvider, setAuthProvider] = useState((localStorage.getItem('authProvider') || '').toUpperCase());
    const [resolvingProvider, setResolvingProvider] = useState(authProvider.length === 0);
    const navigate = useNavigate();

    const idToken = localStorage.getItem("token");
    const canChangePassword = authProvider === 'EMAIL';

    useEffect(() => {
        if (!idToken || authProvider) {
            setResolvingProvider(false);
            return;
        }

        let isMounted = true;

        const loadProvider = async () => {
            try {
                const response = await api.get('/auth/me');
                const provider = (response?.data?.authProvider || '').toUpperCase();
                if (!isMounted) return;
                setAuthProvider(provider);
                if (provider) {
                    localStorage.setItem('authProvider', provider);
                }
            } catch {
                if (!isMounted) return;
                setAuthProvider('');
            } finally {
                if (isMounted) {
                    setResolvingProvider(false);
                }
            }
        };

        loadProvider();

        return () => {
            isMounted = false;
        };
    }, [idToken, authProvider]);

    const passwordStrength = useMemo(() => evaluatePasswordStrength(newPassword), [newPassword]);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError(null);

        if (resolvingProvider) {
            setError("Estamos validando seu método de login. Tente novamente em alguns instantes.");
            return;
        }

        if (!canChangePassword) {
            setError("Contas Google/Redes sociais não podem alterar senha por aqui. Use o provedor de login original.");
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
        if (!idToken) {
            setError("Sessao expirada. Faca login novamente.");
            navigate("/");
            return;
        }

        setLoading(true);
        try {
            await changePassword(idToken, newPassword);
            setSuccess(true);
            // Firebase invalida o token após alterar — deslogar após 3 segundos
            setTimeout(() => {
                logoutUser();
                navigate("/");
            }, 3000);
        } catch (err) {
            const msg = err.response?.data?.message || "Nao foi possivel alterar a senha. Tente novamente.";
            setError(msg);
        } finally {
            setLoading(false);
        }
    };

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
                        <li>Você será redirecionado ao login</li>
                    </ul>
                </aside>

                <section className={Styles.formPanel}>
                    {success ? (
                        <div className={FPStyles.successBox}>
                            <span className={FPStyles.successIcon}>✓</span>
                            <h2>Senha alterada!</h2>
                            <p>Sua senha foi atualizada com sucesso. Redirecionando para o login em instantes...</p>
                        </div>
                    ) : (
                        <>
                            <h2>Alterar senha</h2>
                            <p>Escolha uma nova senha para sua conta.</p>

                            {resolvingProvider && (
                                <div className={FPStyles.successBox}>
                                    <h2>Validando login</h2>
                                    <p>Estamos identificando seu provedor de autenticação para liberar as opções corretas de segurança.</p>
                                </div>
                            )}

                            {!resolvingProvider && !canChangePassword && (
                                <div className={FPStyles.successBox}>
                                    <h2>Opção indisponível</h2>
                                    <p>Esta conta usa login social. A troca de senha deve ser feita no provedor original.</p>
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

                            {canChangePassword && !resolvingProvider && (
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
                                    disabled={loading || !canChangePassword || resolvingProvider}
                                    className={FPStyles.submitBtn}
                                >
                                    {loading ? "Alterando..." : "Alterar senha"}
                                </button>
                            </form>
                            )}

                            <div className={Styles.footerActions}>
                                <Link className={Styles.homeLink} to="/homepage">Cancelar</Link>
                            </div>
                        </>
                    )}
                </section>
            </div>
        </div>
    );
}

export default ChangePasswordPage;
