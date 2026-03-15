import Styles from "./CSS/SignIn_inputs.module.css"
import { useState } from "react"
import { useNavigate, useLocation } from "react-router-dom"
import { registerCustomer, registerBarber, loginWithGoogle, completeProfile, signInWithGoogle } from "../../services/authService"

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


    // --- GOOGLE LOGIN & MODAL LOGIC ---
    const [showModal, setShowModal] = useState(false);
    // Dados temporários do Google
    const [googleUser, setGoogleUser] = useState(null);

    // Função para login com Google
    const handleGoogleSignIn = async (e) => {
        e.preventDefault();
        setError(null);
        setLoading(true);
        try {
            const data = await signInWithGoogle();
            if (data.profileComplete) {
                // Login normal
                if (data.userType === 'BARBER') {
                    navigate('/barberHome');
                } else {
                    navigate('/homepage');
                }
            } else {
                // Perfil incompleto: abrir modal
                setGoogleUser(data);
                setShowModal(true);
            }
        } catch (err) {
            setError('Erro ao autenticar com Google.');
        } finally {
            setLoading(false);
        }
    };

    // Função para salvar dados do modal
    const handleSaveCompleteProfile = async (profileData) => {
        setError(null);
        setLoading(true);
        try {
            await completeProfile(googleUser.userType, {
                ...profileData,
                id: googleUser.id,
                email: googleUser.email,
            });
            setShowModal(false);
            if (googleUser.userType === 'BARBER') {
                navigate('/barberHome');
            } else {
                navigate('/homepage');
            }
        } catch (err) {
            setError('Erro ao completar perfil.');
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
                onClick={handleGoogleSignIn}
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

            {/* Modal para completar perfil (exemplo simples) */}
            {showModal && (
                <div style={{ position: 'fixed', top: 0, left: 0, width: '100vw', height: '100vh', background: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 9999 }}>
                    <div style={{ background: '#fff', padding: 32, borderRadius: 12, minWidth: 320 }}>
                        <h3>Complete seu perfil</h3>
                        <form onSubmit={e => { e.preventDefault(); handleSaveCompleteProfile({ cpf: e.target.cpf.value, phone: e.target.phone.value }); }}>
                            <label>CPF:<br /><input name="cpf" type="text" required /></label><br />
                            <label>Telefone:<br /><input name="phone" type="text" required /></label><br />
                            <button type="submit">Salvar</button>
                            <button type="button" onClick={() => setShowModal(false)} style={{ marginLeft: 8 }}>Cancelar</button>
                        </form>
                    </div>
                </div>

            )}

// Funções devem estar dentro do componente, antes do return:
// Função para login Google (permite clique sem inputs)
// (mover para cima do return, dentro do componente)

            <p className={Styles.login_link}>Já possui conta? Entrar</p>
        </div>
    );
}

export default SignIn_inputs;
