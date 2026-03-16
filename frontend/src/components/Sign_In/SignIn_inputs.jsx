import React, { useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { signInWithGoogle, completeProfileApi, register } from '../../services/authService';
import styles from './CSS/SignIn_inputs.module.css';

function SignIn_inputs() {
    const navigate = useNavigate();
    const location = useLocation();
    const isBarber = location.pathname.includes('/barber'); 

    const [name, setName] = useState('');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [cpf, setCpf] = useState('');
    const [phone, setPhone] = useState('');

    const [showModal, setShowModal] = useState(false);
    const [tempAuthData, setTempAuthData] = useState(null);
    const [extraData, setExtraData] = useState({ cpf: '', phone: '' });

    const handleSubmitManual = async (e) => {
        e.preventDefault();
        if (password !== confirmPassword) {
            alert('As senhas não coincidem!');
            return;
        }
        try {
            const userData = { name, documentCPF: cpf, tell: phone };
            const userType = isBarber ? 'BARBER' : 'CUSTOMER';
            const data = await register(email, password, userData, userType);
            finalizeLogin(data);
        } catch (error) {
            console.error(error);
            alert('Erro ao criar conta.');
        }
    };

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

    const handleSaveProfile = async () => {
        try {
            const payload = {
                uid: tempAuthData.user.firebaseUid,
                documentCPF: extraData.cpf,
                tell: extraData.phone
            };
            await completeProfileApi(tempAuthData.userType, payload);
            const finalData = { ...tempAuthData, profileComplete: true };
            finalData.user.documentCPF = extraData.cpf;
            finalData.user.tell = extraData.phone;
            finalizeLogin(finalData);
        } catch (error) {
            console.error(error);
            alert("Erro ao salvar dados.");
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
            <h2>Criar Conta {isBarber ? 'Barbeiro' : 'Cliente'}</h2>
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
            <button onClick={handleGoogleSignIn} className={styles.googleBtn} type="button">Entrar com Google</button>
            {showModal && (
                <div className={styles.modalOverlay}>
                    <div className={styles.modal}>
                        <h3>Complete seus dados</h3>
                        <p>A sua conta Google não partilha o CPF e o Telefone. Precisamos deles para finalizar!</p>
                        <input placeholder="CPF" onChange={(e) => setExtraData({...extraData, cpf: e.target.value})} />
                        <input placeholder="Telefone" onChange={(e) => setExtraData({...extraData, phone: e.target.value})} />
                        <button onClick={handleSaveProfile} className={styles.saveBtn}>Finalizar Cadastro</button>
                    </div>
                </div>
            )}
        </div>
    );
}
export default SignIn_inputs;