import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { changePassword, logoutUser } from "../services/authService";
import Styles from "./CSS/LoginPage.module.css";
import FPStyles from "./CSS/ForgotPasswordPage.module.css";
import CPStyles from "./CSS/ChangePasswordPage.module.css";

function ChangePasswordPage() {
    const [newPassword, setNewPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");
    const [loading, setLoading] = useState(false);
    const [success, setSuccess] = useState(false);
    const [error, setError] = useState(null);
    const navigate = useNavigate();

    const idToken = localStorage.getItem("token");

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError(null);

        if (newPassword.length < 6) {
            setError("A senha deve ter pelo menos 6 caracteres.");
            return;
        }
        if (newPassword !== confirmPassword) {
            setError("As senhas nao coincidem.");
            return;
        }
        if (!idToken) {
            setError("Sessao expirada. Faca login novamente.");
            navigate("/login");
            return;
        }

        setLoading(true);
        try {
            await changePassword(idToken, newPassword);
            setSuccess(true);
            // Firebase invalida o token após alterar — deslogar após 3 segundos
            setTimeout(() => {
                logoutUser();
                navigate("/login");
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
                        <span className={Styles.softTag}>Minimo 6 caracteres</span>
                        <span className={Styles.softTag}>Sessoes anteriores encerradas</span>
                    </div>

                    <ul className={Styles.featuresList}>
                        <li>Use letras, numeros e simbolos</li>
                        <li>Evite senhas ja utilizadas</li>
                        <li>Voce sera redirecionado ao login</li>
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

                            <form onSubmit={handleSubmit} className={FPStyles.form}>
                                <label className={FPStyles.fieldLabel}>
                                    <p className={FPStyles.labelText}>Nova senha</p>
                                    <div className={CPStyles.inputWrapper}>
                                        <input
                                            type="password"
                                            placeholder="Minimo 6 caracteres"
                                            value={newPassword}
                                            onChange={(e) => setNewPassword(e.target.value)}
                                            required
                                            minLength={6}
                                            className={FPStyles.input}
                                        />
                                    </div>
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
