import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { login, signInWithGoogle, completeProfileApi } from '../../services/authService';
import styles from './CSS/Login_inputs.module.css';

const Login_Inputs = () => {
    const navigate = useNavigate();
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState(null);
    const [loading, setLoading] = useState(false);
    const [showModal, setShowModal] = useState(false);
    const [tempAuthData, setTempAuthData] = useState(null);
    const [extraData, setExtraData] = useState({ cpf: '', phone: '' });

    // LOGIN COM EMAIL/SENHA
    const handleEmailLogin = async (e) => {
        e.preventDefault();
        setError(null);
        setLoading(true);
        try {
            const data = await login(email, password);
            finalizeLogin(data);
        } catch (err) {
            console.error(err);
            setError('Email ou senha inválidos.');
        } finally {
            setLoading(false);
        }
    };

    // LOGIN COM GOOGLE
    const handleGoogleSignIn = async () => {
        try {
            const data = await signInWithGoogle();
            
            if (!data.profileComplete) {
                // Se o Java disse que falta CPF/Telefone, abre o modal
                setTempAuthData(data);
                setShowModal(true);
            } else {
                // Se está completo, salva e entra
                finalizeLogin(data);
            }
        } catch (error) {
            console.error(error);
            alert("Erro ao entrar com Google.");
        }
    };

    // SALVAR DADOS DO MODAL
    const handleSaveProfile = async () => {
        try {
            const payload = {
                uid: tempAuthData.user.firebaseUid,
                documentCPF: extraData.cpf,
                tell: extraData.phone
            };
            
            await completeProfileApi(tempAuthData.userType, payload);
            
            const finalData = { ...tempAuthData, profileComplete: true };
            finalizeLogin(finalData);
        } catch (error) {
            alert("Erro ao salvar dados complementares.");
        }
    };

    const finalizeLogin = (data) => {
        localStorage.setItem('token', data.token);
        localStorage.setItem('user', JSON.stringify(data.user));
        if (data.userType === 'BARBER') {
            navigate('/barberHome');
        } else {
            navigate('/homepage');
        }
    };

    return (
        <div className={styles.container}>
            {/* Formulário de login com email/senha */}
            <form onSubmit={handleEmailLogin} className={styles.form}>
                <input 
                    type="email" 
                    placeholder="Email" 
                    value={email} 
                    onChange={(e) => setEmail(e.target.value)} 
                    required 
                />
                <input 
                    type="password" 
                    placeholder="Senha" 
                    value={password} 
                    onChange={(e) => setPassword(e.target.value)} 
                    required 
                />
                {error && <p className={styles.error}>{error}</p>}
                <button type="submit" disabled={loading} className={styles.submitBtn}>
                    {loading ? 'Entrando...' : 'Entrar'}
                </button>
            </form>

            <div className={styles.divider}>ou</div>

            <button onClick={handleGoogleSignIn} className={styles.googleBtn} type="button" disabled={loading}>
                Entrar com Google
            </button>

            {/* MODAL DE CONCLUSÃO DE CADASTRO */}
            {showModal && (
                <div className={styles.modalOverlay}>
                    <div className={styles.modal}>
                        <h3>Complete seus dados</h3>
                        <input 
                            placeholder="CPF" 
                            onChange={(e) => setExtraData({...extraData, cpf: e.target.value})}
                        />
                        <input 
                            placeholder="Telefone" 
                            onChange={(e) => setExtraData({...extraData, phone: e.target.value})}
                        />
                        <button onClick={handleSaveProfile}>Finalizar Cadastro</button>
                    </div>
                </div>
            )}
        </div>
    );
};

export default Login_Inputs;