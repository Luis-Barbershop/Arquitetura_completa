import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { forgotPassword, resendForgotPassword } from "../services/authService";
import Styles from "./CSS/LoginPage.module.css";
import FPStyles from "./CSS/ForgotPasswordPage.module.css";

function ForgotPasswordPage() {
    const [email, setEmail] = useState("");
    const [loading, setLoading] = useState(false);
    const [resending, setResending] = useState(false);
    const [success, setSuccess] = useState(false);
    const [cooldownSeconds, setCooldownSeconds] = useState(0);
    const [error, setError] = useState(null);

    useEffect(() => {
        if (cooldownSeconds <= 0) return;
        const timer = setInterval(() => {
            setCooldownSeconds((prev) => (prev <= 1 ? 0 : prev - 1));
        }, 1000);

        return () => clearInterval(timer);
    }, [cooldownSeconds]);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError(null);
        setLoading(true);

        try {
            await forgotPassword(email);
            setSuccess(true);
            setCooldownSeconds(120);
        } catch (err) {
            const msg = err.response?.data?.message || "Não foi possível enviar o e-mail. Tente novamente.";
            setError(msg);
        } finally {
            setLoading(false);
        }
    };

    const handleResend = async () => {
        if (!email || cooldownSeconds > 0) return;

        setError(null);
        setResending(true);

        try {
            await resendForgotPassword(email);
            setCooldownSeconds(120);
        } catch (err) {
            const msg = err.response?.data?.message || "Não foi possível reenviar o link agora.";
            setError(msg);
        } finally {
            setResending(false);
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

                    <p className={Styles.kicker}>RECUPERAR ACESSO</p>
                    <h1 className={Styles.title_login}>Redefinir sua senha e voltar a agendar.</h1>
                    <p className={Styles.subtitle}>Informe seu e-mail e enviaremos um link para você criar uma nova senha com segurança.</p>

                    <div className={Styles.tagRow}>
                        <span className={Styles.softTag}>Link seguro via Firebase</span>
                        <span className={Styles.softTag}>Validade limitada por segurança</span>
                    </div>

                    <ul className={Styles.featuresList}>
                        <li>Verifique sua caixa de entrada e spam</li>
                        <li>Se expirar, solicite o reenvio do link</li>
                        <li>Apos redefinir, faca login normalmente</li>
                    </ul>
                </aside>

                <section className={Styles.formPanel}>
                    {success ? (
                        <div className={FPStyles.successBox}>
                            <span className={FPStyles.successIcon}>✓</span>
                            <h2>E-mail enviado!</h2>
                            <p>Verifique sua caixa de entrada em <strong>{email}</strong> e siga as instrucoes para redefinir sua senha.</p>
                            <p className={FPStyles.spamHint}>Nao recebeu? Verifique a pasta de spam ou lixo eletronico.</p>
                            <button
                                type="button"
                                onClick={handleResend}
                                disabled={resending || cooldownSeconds > 0}
                                className={FPStyles.submitBtn}
                                style={{ marginTop: 12 }}
                            >
                                {resending
                                    ? "Reenviando..."
                                    : cooldownSeconds > 0
                                        ? `Reenviar em ${cooldownSeconds}s`
                                        : "Reenviar link"}
                            </button>
                            <Link to="/login" className={FPStyles.backBtn}>Voltar para o login</Link>
                        </div>
                    ) : (
                        <>
                            <h2>Esqueci minha senha</h2>
                            <p>Informe o e-mail da sua conta para receber o link de recuperacao.</p>

                            <form onSubmit={handleSubmit} className={FPStyles.form}>
                                <label className={FPStyles.fieldLabel}>
                                    <p className={FPStyles.labelText}>E-mail</p>
                                    <input
                                        type="email"
                                        placeholder="seu@email.com"
                                        value={email}
                                        onChange={(e) => setEmail(e.target.value)}
                                        required
                                        className={FPStyles.input}
                                    />
                                </label>

                                {error && <p className={FPStyles.errorMsg}>{error}</p>}

                                <button
                                    type="submit"
                                    disabled={loading}
                                    className={FPStyles.submitBtn}
                                >
                                    {loading ? "Enviando..." : "Enviar link de recuperacao"}
                                </button>
                            </form>

                            <div className={Styles.footerActions}>
                                <Link className={Styles.homeLink} to="/login">Voltar para o login</Link>
                            </div>
                        </>
                    )}
                </section>
            </div>
        </div>
    );
}

export default ForgotPasswordPage;
