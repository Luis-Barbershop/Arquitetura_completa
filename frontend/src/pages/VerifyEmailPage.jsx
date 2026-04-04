import { useEffect, useState } from "react";
import { useSearchParams, Link, useNavigate } from "react-router-dom";
import { applyActionCode } from "firebase/auth";
import { auth } from "../services/firebase";
import Styles from "./CSS/VerifyEmailPage.module.css";

/**
 * Página customizada de verificação de e-mail do Firebase.
 *
 * O Firebase redireciona para: /verify-email?mode=verifyEmail&oobCode=XXXXX&apiKey=...
 * Esta página captura o oobCode e aplica o applyActionCode do Firebase SDK.
 *
 * Regras de UI/UX:
 * - Sucesso: card muda para verde com checkmark animado
 * - Erro "já verificado" ou código expirado: mensagem exata especificada
 */
function VerifyEmailPage() {
    const [searchParams] = useSearchParams();
    const [status, setStatus] = useState("loading"); // "loading" | "success" | "error"
    const [errorMsg, setErrorMsg] = useState("");
    const navigate = useNavigate();

    useEffect(() => {
        const oobCode = searchParams.get("oobCode");
        const mode = searchParams.get("mode");

        if (!oobCode || mode !== "verifyEmail") {
            setStatus("error");
            setErrorMsg("Link inválido ou expirado. Solicite um novo e-mail de verificação.");
            return;
        }

        applyActionCode(auth, oobCode)
            .then(() => {
                setStatus("success");
            })
            .catch((err) => {
                setStatus("error");
                const code = err.code || "";
                if (
                    code === "auth/invalid-action-code" ||
                    code === "auth/expired-action-code" ||
                    code === "auth/email-already-verified"
                ) {
                    setErrorMsg("Parece que este e-mail já foi verificado! :(");
                } else {
                    setErrorMsg("Parece que este e-mail já foi verificado! :(");
                }
            });
    }, [searchParams]);

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
