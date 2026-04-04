import { useState } from "react";
import { Link } from "react-router-dom";
import { forgotPassword } from "../services/authService";
import Styles from "./CSS/LoginPage.module.css";
import FPStyles from "./CSS/ForgotPasswordPage.module.css";

function ForgotPasswordPage() {
    const [email, setEmail] = useState("");
    const [loading, setLoading] = useState(false);
    const [success, setSuccess] = useState(false);
    const [error, setError] = useState(null);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError(null);
        setLoading(true);

        try {
            await forgotPassword(email);
            setSuccess(true);
        } catch (err) {
            const msg = err.response?.data?.message || "Não foi possível enviar o e-mail. Tente novamente.";
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

                    <p className={Styles.kicker}>RECUPERAR ACESSO</p>
                    <h1 className={Styles.title_login}>Redefinir sua senha e voltar a agendar.</h1>
                    <p className={Styles.subtitle}>Informe seu e-mail e enviaremos um link para voce criar uma nova senha com seguranca.</p>

                    <div className={Styles.tagRow}>
                        <span className={Styles.softTag}>Link seguro via Firebase</span>
                        <span className={Styles.softTag}>Expira em 24h</span>
                    </div>

                    <ul className={Styles.featuresList}>
                        <li>Verifique sua caixa de entrada e spam</li>
                        <li>O link e valido por 24 horas</li>
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
