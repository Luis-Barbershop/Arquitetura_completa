import { Link, useSearchParams } from "react-router-dom";
import ChangePasswordPage from "./ChangePasswordPage";
import VerifyEmailPage from "./VerifyEmailPage";
import Styles from "./CSS/VerifyEmailPage.module.css";

function AccountActionPage() {
    const [searchParams] = useSearchParams();
    const mode = searchParams.get("mode");

    if (mode === "resetPassword") {
        return <ChangePasswordPage />;
    }

    if (mode === "verifyEmail" || mode === "verifyAndChangeEmail") {
        return <VerifyEmailPage />;
    }

    return (
        <div className={Styles.page}>
            <div className={Styles.brandBadge}>
                <Link to="/" className={Styles.brandLink}>
                    <img src="/Icons/scissors_icon.png" alt="CortaAI" />
                    <span>CortaAI</span>
                </Link>
            </div>

            <div className={`${Styles.card} ${Styles.errorCard}`}>
                <h2 className={Styles.errorTitle}>Link indisponivel</h2>
                <p className={Styles.errorText}>
                    Este link de gerenciamento de conta e invalido ou expirou.
                </p>
                <Link to="/login" className={Styles.ctaButton}>
                    Voltar para o login
                </Link>
            </div>
        </div>
    );
}

export default AccountActionPage;
