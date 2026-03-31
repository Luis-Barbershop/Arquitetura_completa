import { Link, useLocation } from "react-router-dom"
import SignIn_inputs from "../components/Sign_In/SignIn_inputs"
import Styles from "./CSS/SignInPage.module.css"

function SignInPage() {
    const location = useLocation();
    const role = location.state?.role || "customer";
    const roleLabel = role === "barber" ? "Barbeiro" : "Cliente";

    return (
        <div className={Styles.registerStage}>
            <div className={Styles.registerShell}>
                <aside className={Styles.brandPanel}>
                    <div className={Styles.brandBadge}>
                        <img src="/Icons/scissors_icon.png" alt="Icone CortaAI" />
                        <span>CortaAI</span>
                    </div>

                    <p className={Styles.kicker}>NOVO CADASTRO</p>
                    <h1 className={Styles.title}>Crie sua conta e comece a agendar em minutos.</h1>
                    <p className={Styles.subtitle}>Configure seu perfil, organize seus atendimentos e entre no ecossistema CortaAI.</p>

                    <div className={Styles.tagRow}>
                        <span className={Styles.roleTag}>Perfil: <strong>{roleLabel}</strong></span>
                        <span className={Styles.softTag}>Fluxo rapido</span>
                    </div>

                    <ul className={Styles.featuresList}>
                        <li>Cadastro em duas etapas objetivas</li>
                        <li>Dados protegidos com autenticacao segura</li>
                        <li>Painel preparado para cliente e barbeiro</li>
                    </ul>

                    <div className={Styles.moodCard}>
                        <p className={Styles.moodTitle}>Experiencia atualizada</p>
                        <p className={Styles.moodText}>Visual limpo, fluxo rapido e foco total em agendar sem atrito.</p>
                    </div>
                </aside>

                <section className={Styles.formPanel}>
                    <h2>Criar conta</h2>
                    <p>Preencha os dados abaixo para finalizar seu cadastro.</p>

                    <div className={Styles.formCard}>
                        <SignIn_inputs />
                    </div>

                    <div className={Styles.footerActions}>
                        <h3>Ja possui conta? <Link className={Styles.mainLink} to="/login" state={{ role }}>Entrar</Link></h3>
                        <div className={Styles.secondaryLinks}>
                            <Link className={Styles.secondaryLink} to="/identificacao" state={{ mode: "register", role }}>Trocar perfil</Link>
                            <Link className={Styles.secondaryLink} to="/">Voltar para a pagina inicial</Link>
                        </div>
                    </div>
                </section>
            </div>
        </div>
    )
}

export default SignInPage