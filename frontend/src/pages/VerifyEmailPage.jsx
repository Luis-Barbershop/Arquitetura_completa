import { useEffect, useState } from "react";
import { useSearchParams, Link, useNavigate, useLocation } from "react-router-dom";
import { applyActionCode } from "firebase/auth";
import { auth } from "../services/firebase";
import { resendVerificationEmail } from "../services/authService";
import Styles from "./CSS/VerifyEmailPage.module.css";

/**
 * Página de verificação de e-mail do CortaAI — dois modos:
 *
 * MODO "waiting" (state.mode === 'waiting'):
 *   Exibido após cadastro com e-mail/senha. Mostra "verifique sua caixa".
 *   Permite reenviar o link ou alterar o e-mail.
 *
 * MODO "verifyEmail" (Firebase redirect com ?mode=verifyEmail&oobCode=...):
 *   Aplica o applyActionCode do Firebase SDK.
 */
function VerifyEmailPage() {
    const [searchParams] = useSearchParams();
    const location = useLocation();
    const navigate = useNavigate();

    // ── Detecta o modo ───────────────────────────────────────────────────────
    const locationMode = location.state?.mode;
    const queryMode    = searchParams.get("mode");
    const isWaiting    = locationMode === 'waiting';
    const isVerify     = queryMode === 'verifyEmail';

    // ─────────────────────────────────────────────────────────────────────────
    // MODO: WAITING — aguardando o usuário clicar no link enviado por e-mail
    // ─────────────────────────────────────────────────────────────────────────
    const [waitEmail, setWaitEmail]           = useState(location.state?.email || "");
    const [waitPassword, setWaitPassword]     = useState(location.state?.password || "");
    const [editingEmail, setEditingEmail]     = useState(false);
    const [newEmail, setNewEmail]             = useState(location.state?.email || "");
    const [resendStatus, setResendStatus]     = useState(null); // null | 'sending' | 'sent' | 'error' | 'needPassword'
    const [resendMsg, setResendMsg]           = useState("");
    const [passwordInput, setPasswordInput]   = useState("");

    const handleResend = async (overridePassword) => {
        const pwd = overridePassword || waitPassword;
        if (!pwd) {
            // Pedir senha ao usuário
            setResendStatus('needPassword');
            return;
        }
        setResendStatus('sending');
        setResendMsg('');
        try {
            await resendVerificationEmail(newEmail, pwd);
            setWaitEmail(newEmail);
            setWaitPassword(pwd);
            setEditingEmail(false);
            setPasswordInput('');
            setResendStatus('sent');
            setResendMsg(`Link reenviado para ${newEmail}. Verifique sua caixa de entrada.`);
        } catch (err) {
            setResendStatus('error');
            setResendMsg(err.response?.data?.message || err.message || 'Não foi possível reenviar. Tente novamente.');
        }
    };

    // ─────────────────────────────────────────────────────────────────────────
    // MODO: VERIFY EMAIL — Firebase redirect com oobCode
    // ─────────────────────────────────────────────────────────────────────────
    const [status, setStatus]     = useState("loading"); // "loading" | "success" | "error"
    const [errorMsg, setErrorMsg] = useState("");

    useEffect(() => {
        if (!isVerify) {
            // Nem modo waiting nem oobCode → acesso direto inválido
            if (!isWaiting) {
                setStatus("error");
                setErrorMsg("Link inválido ou expirado. Solicite um novo e-mail de verificação.");
            }
            return;
        }

        const oobCode = searchParams.get("oobCode");
        if (!oobCode) {
            setStatus("error");
            setErrorMsg("Link inválido ou expirado. Solicite um novo e-mail de verificação.");
            return;
        }

        applyActionCode(auth, oobCode)
            .then(() => setStatus("success"))
            .catch(() => {
                setStatus("error");
                setErrorMsg("Parece que este e-mail já foi verificado! :(");
            });
    }, [searchParams, isVerify, isWaiting]);

    // ─────────────────────────────────────────────────────────────────────────
    // RENDER: modo "waiting"
    // ─────────────────────────────────────────────────────────────────────────
    if (isWaiting) {
        return (
            <div className={Styles.page}>
                <div className={Styles.brandBadge}>
                    <Link to="/" className={Styles.brandLink}>
                        <img src="/Icons/scissors_icon.png" alt="CortaAI" />
                        <span>CortaAI</span>
                    </Link>
                </div>

                <div className={Styles.card}>
                    {/* Ícone de envelope */}
                    <div className={Styles.envelopeIcon}>✉️</div>

                    <h2 className={Styles.waitingTitle}>Verifique seu e-mail</h2>

                    <p className={Styles.waitingText}>
                        Enviamos um link de confirmação para{' '}
                        <strong className={Styles.emailHighlight}>{waitEmail}</strong>.
                        <br />
                        Clique no link para ativar sua conta.
                    </p>

                    <p className={Styles.waitingHint}>
                        Não encontrou? Verifique a pasta de spam ou clique em "Reenviar".
                    </p>

                    {/* Área de reenvio / alterar e-mail */}
                    {!editingEmail ? (
                        <div className={Styles.resendRow}>
                            <button
                                className={Styles.ctaButtonGhost}
                                onClick={() => handleResend()}
                                disabled={resendStatus === 'sending'}
                            >
                                {resendStatus === 'sending' ? 'Reenviando...' : 'Reenviar link'}
                            </button>
                            <button
                                className={Styles.linkButton}
                                onClick={() => { setEditingEmail(true); setResendStatus(null); setResendMsg(''); }}
                            >
                                Alterar e-mail
                            </button>
                        </div>
                    ) : (
                        <div className={Styles.editEmailArea}>
                            <input
                                type="email"
                                className={Styles.emailInput}
                                value={newEmail}
                                onChange={e => setNewEmail(e.target.value)}
                                placeholder="Novo e-mail"
                            />
                            <div className={Styles.editEmailActions}>
                                <button
                                    className={Styles.ctaButton}
                                    onClick={() => handleResend()}
                                    disabled={resendStatus === 'sending' || !newEmail}
                                >
                                    {resendStatus === 'sending' ? 'Enviando...' : 'Enviar para este e-mail'}
                                </button>
                                <button
                                    className={Styles.linkButton}
                                    onClick={() => { setEditingEmail(false); setNewEmail(waitEmail); setResendStatus(null); }}
                                >
                                    Cancelar
                                </button>
                            </div>
                        </div>
                    )}

                    {/* Campo de senha quando não foi passada no state (vindo do login) */}
                    {resendStatus === 'needPassword' && (
                        <div className={Styles.editEmailArea}>
                            <p className={Styles.waitingHint}>
                                Para reenviar, confirme sua senha:
                            </p>
                            <input
                                type="password"
                                className={Styles.emailInput}
                                value={passwordInput}
                                onChange={e => setPasswordInput(e.target.value)}
                                placeholder="Sua senha"
                            />
                            <div className={Styles.editEmailActions}>
                                <button
                                    className={Styles.ctaButton}
                                    onClick={() => handleResend(passwordInput)}
                                    disabled={!passwordInput}
                                >
                                    Reenviar
                                </button>
                                <button
                                    className={Styles.linkButton}
                                    onClick={() => { setResendStatus(null); setPasswordInput(''); }}
                                >
                                    Cancelar
                                </button>
                            </div>
                        </div>
                    )}

                    {/* Feedback de reenvio */}
                    {resendStatus === 'sent' && (
                        <p className={Styles.feedbackSuccess}>{resendMsg}</p>
                    )}
                    {resendStatus === 'error' && (
                        <p className={Styles.feedbackError}>{resendMsg}</p>
                    )}

                    <button
                        className={Styles.ctaButtonSecondary}
                        onClick={() => navigate('/login', { state: { role: location.state?.role } })}
                    >
                        Já verifiquei — Fazer login
                    </button>
                </div>
            </div>
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RENDER: modo "verifyEmail" (Firebase oobCode) ou fallback de erro
    // ─────────────────────────────────────────────────────────────────────────
    return (
        <div className={Styles.page}>
            <div className={Styles.brandBadge}>
                <Link to="/" className={Styles.brandLink}>
                    <img src="/Icons/scissors_icon.png" alt="CortaAI" />
                    <span>CortaAI</span>
                </Link>
            </div>

            <div className={`${Styles.card} ${status === "success" ? Styles.successCard : status === "error" ? Styles.errorCard : ""}`}>
                {status === "loading" && (
                    <>
                        <div className={Styles.spinner} />
                        <p className={Styles.loadingText}>Verificando seu e-mail...</p>
                    </>
                )}

                {status === "success" && (
                    <>
                        <div className={Styles.checkmarkWrapper}>
                            <svg className={Styles.checkmark} viewBox="0 0 52 52">
                                <circle className={Styles.checkmarkCircle} cx="26" cy="26" r="25" fill="none" />
                                <path className={Styles.checkmarkCheck} fill="none" d="M14.1 27.2l7.1 7.2 16.7-16.8" />
                            </svg>
                        </div>
                        <h2 className={Styles.successTitle}>E-mail verificado!</h2>
                        <p className={Styles.successSubtitle}>
                            Sua conta está ativa. Agora você pode aproveitar todos os recursos do CortaAI.
                        </p>
                        <button
                            className={Styles.ctaButton}
                            onClick={() => navigate("/identificacao", { state: { mode: "login" } })}
                        >
                            Fazer login agora
                        </button>
                    </>
                )}

                {status === "error" && (
                    <>
                        <div className={Styles.errorIcon}>:(</div>
                        <h2 className={Styles.errorTitle}>Ops!</h2>
                        <p className={Styles.errorText}>{errorMsg}</p>
                        <button
                            className={Styles.ctaButtonGhost}
                            onClick={() => navigate("/identificacao", { state: { mode: "login" } })}
                        >
                            Voltar para o login
                        </button>
                    </>
                )}
            </div>
        </div>
    );
}

export default VerifyEmailPage;
