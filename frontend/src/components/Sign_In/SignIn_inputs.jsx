import React, { useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { signInWithGoogle, completeProfileApi, register } from '../../services/authService';
import styles from './CSS/SignIn_inputs.module.css';

function SignIn_inputs() {
    const navigate = useNavigate();
    const location = useLocation();
    // Verifica se veio da página de barbeiro ou cliente (ajuste consoante a lógica que já tinha)
    const isBarber = location.pathname.includes('/barber'); 

    // Estados do Formulário Manual
    const [name, setName] = useState('');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [cpf, setCpf] = useState('');
    const [phone, setPhone] = useState('');

    // Estados do Modal Google
    const [showModal, setShowModal] = useState(false);
    const [tempAuthData, setTempAuthData] = useState(null);
    const [extraData, setExtraData] = useState({ cpf: '', phone: '' });

    // ===== 1. CADASTRO MANUAL (Email/Senha) =====
    const handleSubmitManual = async (e) => {
        e.preventDefault();
        
        if (password !== confirmPassword) {
            alert('As senhas não coincidem!');
            return;
        }

        try {
            const userData = {
                name: name,
                documentCPF: cpf,
                tell: phone,
                // Adicione outros campos se necessário (ex: startTime para barbeiro)
            };

            const userType = isBarber ? 'BARBER' : 'CUSTOMER';
            
            // Chama a função única 'register' do authService
            const data = await register(email, password, userData, userType);
            
            finalizeLogin(data);
        } catch (error) {
            console.error(error);
            alert('Erro ao criar conta. Verifique os dados e tente novamente.');
        }
    };

    // ===== 2. CADASTRO / LOGIN COM GOOGLE =====
    const handleGoogleSignIn = async () => {
        try {
            const data = await signInWithGoogle();
            
            if (!data.profileComplete) {
                setTempAuthData(data);
                setShowModal(true);
            } else {
                finalizeLogin(data);
            }
        } catch (error) {
            console.error(error);
            alert("Erro ao entrar com Google.");
        }
    };

    // ===== 3. SALVAR DADOS DO MODAL (Apenas para Google Incompleto) =====
    const handleSaveProfile = async () => {
        try {
            const payload = {
                uid: tempAuthData.user.firebaseUid,
                documentCPF: extraData.cpf,
                tell: extraData.phone
            };
            
            await completeProfileApi(tempAuthData.userType, payload);
            
            const finalData = { ...tempAuthData, profileComplete: true };
            // Atualiza os dados locais com o que acabou de preencher
            finalData.user.documentCPF = extraData.cpf;
            finalData.user.tell = extraData.phone;

            finalizeLogin(finalData);
        } catch (error) {
            console.error(error);
            alert("Erro ao salvar dados complementares.");
        }
    };

    const finalizeLogin = (data) => {
        localStorage.setItem('token', data.token);
        localStorage.setItem('user', JSON.stringify(data.user));
        navigate(data.userType === 'BARBER' ? '/barber-home' : '/home');
    };

    return (
        <div className={styles.container}>
            <h2>Criar Conta {isBarber ? 'Barbeiro' : 'Cliente'}</h2>
            
            {/* FORMULÁRIO MANUAL */}
            <form onSubmit={handleSubmitManual} className={styles.form}>
                <input type="text" placeholder="Nome" value={name} onChange={(e) => setName(e.target.value)} required />
                <input type="email" placeholder="Email" value={email} onChange={(e) => setEmail(e.target.value)} required />
                <input type="password" placeholder="Senha" value={password} onChange={(e) => setPassword(e.target.value)} required />
                <input type="password" placeholder="Confirmar Senha" value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} required />
                <input type="text" placeholder="CPF" value={cpf} onChange={(e) => setCpf(e.target.value)} required />
                <input type="text" placeholder="Telefone" value={phone} onChange={(e) => setPhone(e.target.value)} required />
                
                <button type="submit" className={styles.submitBtn}>Cadastrar</button>
            </form>

            <div className={styles.divider}>ou</div>

            {/* BOTÃO GOOGLE */}
            <button onClick={handleGoogleSignIn} className={styles.googleBtn} type="button">
                Entrar com Google
            </button>

            {/* MODAL DE CONCLUSÃO DE CADASTRO */}
            {showModal && (
                <div className={styles.modalOverlay}>
                    <div className={styles.modal}>
                        <h3>Complete seus dados</h3>
                        <p>A sua conta Google não partilha o CPF e o Telefone. Precisamos deles para finalizar!</p>
                        <input 
                            placeholder="CPF" 
                            onChange={(e) => setExtraData({...extraData, cpf: e.target.value})}
                        />
                        <input 
                            placeholder="Telefone" 
                            onChange={(e) => setExtraData({...extraData, phone: e.target.value})}
                        />
                        <button onClick={handleSaveProfile} className={styles.saveBtn}>Finalizar Cadastro</button>
                    </div>
                </div>
            )}
        </div>
    );
}

export default SignIn_inputs;