import Styles from "./CSS/SignIn_inputs.module.css"
import { useState } from "react"
import { useNavigate, useLocation } from "react-router-dom"
import { registerCustomer, registerBarber } from "../../services/authService"

function SignIn_inputs() {

    const [step, setStep] = useState(1);
    const [registered, setRegistered] = useState(false); // mostra tela de "verifique seu e-mail"

    const [name, setName] = useState("");
    const [email, setEmail] = useState("");
    const [cpf, setCpf] = useState("");

    const [tell, setTell] = useState("");
    const [password, setPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");

    const [workStart, setWorkStart] = useState("09:00");
    const [workEnd, setWorkEnd] = useState("18:00");

    const [error, setError] = useState(null);

    const navigate = useNavigate();
    const location = useLocation();
    const userType = location.state?.role || "customer";


    const handleNextStep = (e) => {
        e.preventDefault();

        if (!name || !email || !cpf) {
            setError("Preencha todos os campos.");
            return;
        }

        setError(null);
        setStep(2);
    };


    const handleRegister = async (e) => {
        e.preventDefault();
        setError(null);

        if (password !== confirmPassword) {
            setError("As senhas não coincidem.");
            return;
        }

        try {
            if (userType === "customer") {
                await registerCustomer({
                    name, email, documentCPF: cpf, tell, password
                });
            } else {
                await registerBarber({
                    name, email, documentCPF: cpf, tell, password,
                    workStartTime: workStart,
                    workEndTime: workEnd,
                });
            }

            // Cadastro feito — Firebase enviou e-mail de verificação automaticamente
            // Usuário precisa verificar antes de fazer login
            setRegistered(true);

        } catch (err) {
            console.error(err);
            const msg = err.response?.data?.message || "Erro ao cadastrar. Verifique os dados.";
            setError(msg);
        }
    };

    // ── Tela pós-cadastro: avisa para verificar o e-mail ─────────────────────
    if (registered) {
        return (
            <div className={Styles.SignIn_inputs_container}>
                <div style={{ textAlign: 'center', padding: '2rem' }}>
                    <h3>✅ Cadastro realizado!</h3>
                    <p style={{ marginTop: '1rem' }}>
                        Um e-mail de verificação foi enviado para <strong>{email}</strong>.
                    </p>
                    <p style={{ marginTop: '0.5rem', color: '#888' }}>
                        Verifique sua caixa de entrada (e o spam) e clique no link antes de fazer login.
                    </p>
                    <button
                        style={{ marginTop: '1.5rem' }}
                        className={Styles.SignIn_button}
                        onClick={() => navigate("/login")}
                    >
                        Ir para o Login
                    </button>
                </div>
            </div>
        );
    }


    return (
        <div className={Styles.SignIn_inputs_container}>
            <div className={Styles.stepHeader}>
                <span className={step === 1 ? Styles.activeStep : Styles.step}>1. Dados pessoais</span>
                <span className={step === 2 ? Styles.activeStep : Styles.step}>2. Conta e acesso</span>
            </div>

            <h3 className={Styles.formType}>Cadastro de {userType === "barber" ? "Barbeiro" : "Cliente"}</h3>

            <form onSubmit={step === 1 ? handleNextStep : handleRegister}>
                {step === 1 && (
                    <>
                        <label className={Styles.label_name}>
                            <p>Nome completo</p>
                            <input 
                                className={Styles.formInput}
                                type="text"
                                value={name}
                                onChange={e => setName(e.target.value)}
                                placeholder="Digite seu nome completo"
                                required
                            />
                        </label>

                        <label className={Styles.label_email}>
                            <p>E-mail</p>
                            <input 
                                className={Styles.formInput}
                                type="email"
                                value={email}
                                onChange={e => setEmail(e.target.value)}
                                placeholder="seuemail@exemplo.com"
                                required
                            />
                        </label>

                        <label className={Styles.label_name}>
                            <p>CPF</p>
                            <input
                                className={Styles.formInput}
                                type="text"
                                value={cpf}
                                onChange={e => setCpf(e.target.value)}
                                placeholder="Somente números"
                                required
                            />
                        </label>

                        {error && <p className={Styles.error_message}>{error}</p>}

                        <button type="submit" className={Styles.SignIn_button}>
                            Continuar
                        </button>
                    </>
                )}

                {step === 2 && (
                    <>
                        <label className={Styles.label_name}>
                            <p>Telefone</p>
                            <input 
                                className={Styles.formInput}
                                type="text"
                                value={tell}
                                onChange={e => setTell(e.target.value)}
                                placeholder="11999999999"
                                required
                            />
                        </label>

                        {userType === "barber" && (
                            <>
                                <label className={Styles.label_name}>
                                    <p>Inicio de expediente</p>
                                    <input 
                                        className={Styles.formInput}
                                        type="time"
                                        value={workStart}
                                        onChange={e => setWorkStart(e.target.value)}
                                        required
                                    />
                                </label>

                                <label className={Styles.label_name}>
                                    <p>Fim de expediente</p>
                                    <input 
                                        className={Styles.formInput}
                                        type="time"
                                        value={workEnd}
                                        onChange={e => setWorkEnd(e.target.value)}
                                        required
                                    />
                                </label>
                            </>
                        )}

                        <label className={Styles.label_name}>
                            <p>Senha</p>
                            <input 
                                className={Styles.formInput}
                                type="password"
                                value={password}
                                onChange={e => setPassword(e.target.value)}
                                required
                            />
                        </label>

                        <label className={Styles.label_name}>
                            <p>Confirmar senha</p>
                            <input 
                                className={Styles.formInput}
                                type="password"
                                value={confirmPassword}
                                onChange={e => setConfirmPassword(e.target.value)}
                                required
                            />
                        </label>

                        {error && <p className={Styles.error_message}>{error}</p>}

                        <div className={Styles.actionsRow}>
                            <button type="button" className={Styles.secondaryButton} onClick={() => setStep(1)}>
                                Voltar
                            </button>
                            <button type="submit" className={Styles.SignIn_button}>
                                Cadastrar
                            </button>
                        </div>
                    </>
                )}
            </form>

        </div>
    );
}

export default SignIn_inputs;
