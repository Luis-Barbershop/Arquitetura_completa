import Styles from "./CSS/SignIn_inputs.module.css"
import { useState } from "react"
import { useNavigate, useLocation } from "react-router-dom"
import { registerCustomer, registerBarber, loginWithGoogle } from "../../services/authService"

function SignIn_inputs() {

    // Estados do formulário
    const [step, setStep] = useState(1); // <<< controla o progresso

    const [name, setName] = useState("");
    const [email, setEmail] = useState("");
    const [cpf, setCpf] = useState("");

    const [tell, setTell] = useState("");
    const [password, setPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");

    const [workStart, setWorkStart] = useState("09:00");
    const [workEnd, setWorkEnd] = useState("18:00");

    const [error, setError] = useState(null);
    const [loading, setLoading] = useState(false);

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

        setLoading(true);

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

            alert("Cadastro realizado com sucesso!");
            // Após registro Firebase, o usuário já está logado
            if (userType === "customer") {
                navigate("/homepage");
            } else {
                navigate("/barberHome");
            }

        } catch (err) {
            console.error(err);
            const msg = err.code === 'auth/email-already-in-use'
                ? "Este e-mail já está cadastrado."
                : err.code === 'auth/weak-password'
                ? "A senha deve ter pelo menos 6 caracteres."
                : "Erro ao cadastrar. Verifique os dados.";
            setError(msg);
        } finally {
            setLoading(false);
        }
    };


    return (
        <div className={Styles.SignIn_inputs_container}>

            <h3 className={Styles.title_register}>
                Cadastro de {userType === "barber" ? "Barbeiro" : "Cliente"}
            </h3>

            <form>
                {step === 1 && (
                    <>
                        <label className={Styles.label_name}>
                            <p>Nome Completo:</p>
                            <input 
                                id={Styles.name_input}
                                type="text"
                                value={name}
                                onChange={e => setName(e.target.value)}
                                placeholder="Digite seu nome completo"
                                required
                            />
                        </label>

                        <label className={Styles.label_email}>
                            <p>E-mail:</p>
                            <input 
                                id={Styles.email_input}
                                type="email"
                                value={email}
                                onChange={e => setEmail(e.target.value)}
                                placeholder="seuemail@exemplo.com"
                                required
                            />
                        </label>

                        <label className={Styles.label_name}>
                            <p>CPF:</p>
                            <input
                                 id={Styles.email_input}
                                type="text"
                                value={cpf}
                                onChange={e => setCpf(e.target.value)}
                                placeholder="Somente números"
                                required
                            />
                        </label>

                        {error && <p className={Styles.error_message}>{error}</p>}

                        <button className={Styles.SignIn_button} onClick={handleNextStep}>
                            Próximo
                        </button>
                    </>
                )}

                {step === 2 && (
                    <>
                        <label className={Styles.label_name}>
                            <p>Telefone:</p>
                            <input 
                             id={Styles.name_input}
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
                                    <p>Início de Expediente:</p>
                                    <input 
                                     id={Styles.name_input}
                                        type="time"
                                        value={workStart}
                                        onChange={e => setWorkStart(e.target.value)}
                                        required
                                    />
                                </label>

                                <label className={Styles.label_name}>
                                    <p>Fim de Expediente:</p>
                                    <input 
                                     id={Styles.name_input}
                                        type="time"
                                        value={workEnd}
                                        onChange={e => setWorkEnd(e.target.value)}
                                        required
                                    />
                                </label>
                            </>
                        )}

                        <label className={Styles.label_name}>
                            <p>Senha:</p>
                            <input 
                             id={Styles.name_input}
                                type="password"
                                value={password}
                                onChange={e => setPassword(e.target.value)}
                                required
                            />
                        </label>

                        <label className={Styles.label_name}>
                            <p>Confirmar senha:</p>
                            <input 
                             id={Styles.name_input}
                                type="password"
                                value={confirmPassword}
                                onChange={e => setConfirmPassword(e.target.value)}
                                required
                            />
                        </label>

                        {error && <p className={Styles.error_message}>{error}</p>}

                        <button className={Styles.SignIn_button} onClick={handleRegister} disabled={loading}>
                            {loading ? "Cadastrando..." : "Cadastrar"}
                        </button>
                    </>
                )}
            </form>

            {/* Divisor */}
            <div style={{
                display: 'flex', alignItems: 'center', margin: '20px 0', width: '100%'
            }}>
                <div style={{ flex: 1, height: '1px', backgroundColor: '#555' }}></div>
                <span style={{ padding: '0 12px', color: '#999', fontSize: '13px' }}>ou cadastre-se com</span>
                <div style={{ flex: 1, height: '1px', backgroundColor: '#555' }}></div>
            </div>

            {/* Botão Google */}
            <button
                onClick={async () => {
                    setError(null);
                    setLoading(true);
                    try {
                        const data = await loginWithGoogle(userType);
                        // Se perfil incompleto e veio do Google, precisa completar
                        if (!data.profileComplete) {
                            alert("Complete seus dados para continuar.");
                            // Mantém na tela de registro para preencher CPF/telefone
                            return;
                        }
                        alert("Cadastro via Google realizado!");
                        navigate(userType === "customer" ? "/homepage" : "/barberHome");
                    } catch (err) {
                        console.error(err);
                        if (err.code !== 'auth/popup-closed-by-user') {
                            setError("Falha ao cadastrar com Google.");
                        }
                    } finally {
                        setLoading(false);
                    }
                }}
                disabled={loading}
                style={{
                    width: '100%',
                    padding: '12px 20px',
                    borderRadius: '8px',
                    border: '1px solid #555',
                    backgroundColor: '#fff',
                    color: '#333',
                    fontSize: '15px',
                    fontWeight: '600',
                    cursor: loading ? 'not-allowed' : 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    gap: '10px',
                    opacity: loading ? 0.6 : 1,
                    transition: 'all 0.2s',
                }}
            >
                <svg width="20" height="20" viewBox="0 0 48 48">
                    <path fill="#EA4335" d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z"/>
                    <path fill="#4285F4" d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z"/>
                    <path fill="#FBBC05" d="M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z"/>
                    <path fill="#34A853" d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z"/>
                </svg>
                {loading ? "Aguarde..." : "Cadastrar com Google"}
            </button>

            <p className={Styles.login_link}>Já possui conta? Entrar</p>
        </div>
    );
}

export default SignIn_inputs;
