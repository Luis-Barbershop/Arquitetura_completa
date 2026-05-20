import Login_Inputs from "../components/Login/Login_Inputs"
import Styles from "./CSS/LoginPage.module.css"
import { Link, useLocation } from "react-router-dom";

function LoginPage() {
    const location = useLocation();
    const role = location.state?.role || "customer";
    const roleLabel = role === "barber" ? "Barbeiro" : "Cliente";

    return (
        <div className={Styles.loginStage}>
            <div className={Styles.loginShell}>
                <aside className={Styles.brandPanel}>
                    <div className={Styles.brandBadge}>
                        <img src="/Icons/scissors_icon.png" alt="Icone CortaAI" />
                        <span>CortaAI</span>
                    </div>

                    <p className={Styles.kicker}>BEM-VINDO DE VOLTA</p>
                    <h1 className={Styles.title_login}>Seu próximo corte começa aqui.</h1>
                    <p className={Styles.subtitle}>Entre para acompanhar horários, favoritos e serviços em um painel mais rápido.</p>

                    <div className={Styles.tagRow}>
                        <span className={Styles.roleTag}>Perfil: <strong>{roleLabel}</strong></span>
                        <span className={Styles.softTag}>Acesso seguro</span>
                    </div>

                    <ul className={Styles.featuresList}>
                        <li>Agenda organizada em poucos toques</li>
                        <li>Histórico de atendimentos em um só lugar</li>
                        <li>Fluxo simples para cliente e barbeiro</li>
                    </ul>
                </aside>

                <section className={Styles.formPanel}>
                    <h2>Entrar na conta</h2>
                    <p>Preencha seus dados para continuar.</p>

                    <Login_Inputs />

                    <div className={Styles.footerActions}>
                        <Link className={Styles.homeLink} to="/">Voltar para a pagina inicial</Link>
                    </div>
                </section>
            </div>
        </div>
    )
}

export default LoginPage