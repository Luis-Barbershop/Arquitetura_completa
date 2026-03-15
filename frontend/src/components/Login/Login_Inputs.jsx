import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { signInWithGoogle, completeProfileApi } from '../../services/authService';
import styles from './CSS/Login_inputs.module.css';

const Login_Inputs = () => {
    const navigate = useNavigate();
    const [showModal, setShowModal] = useState(false);
    const [tempAuthData, setTempAuthData] = useState(null);
    const [extraData, setExtraData] = useState({ cpf: '', phone: '' });

    // LOGIN / CADASTRO COM GOOGLE
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
        navigate(data.userType === 'BARBER' ? '/barber-home' : '/home');
    };

    return (
        <div className={styles.container}>
            {/* Aqui ficaria o formulário manual de cadastro se você ainda usar */}
            <button onClick={handleGoogleSignIn} className={styles.googleBtn}>
                Cadastrar com Google
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