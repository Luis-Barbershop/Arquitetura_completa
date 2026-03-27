import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { login, signInWithGoogle, completeProfileApi } from '../../services/authService';
import Styles from './CSS/Login_inputs.module.css';

const Login_Inputs = ({ role }) => {
    const navigate = useNavigate();
    const isBarber = role === 'barber';
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState(null);
    const [loading, setLoading] = useState(false);
    const [showModal, setShowModal] = useState(false);
    const [tempAuthData, setTempAuthData] = useState(null);
    const [extraData, setExtraData] = useState({ cpf: '', phone: '', workStart: '09:00', workEnd: '18:00' });

    const handleEmailLogin = async (e) => {
        e.preventDefault();
        setError(null);
        setLoading(true);
        try {
            const data = await login(email, password);
            if (!data.profileComplete) {
                setTempAuthData(data);
                setShowModal(true);
            } else {
                finalizeLogin(data);
            }
        } catch (err) {
            console.error(err);
            if (err.code === 'auth/email-not-verified') {
                setError('Seu e-mail ainda nao foi verificado. Verifique sua caixa de entrada.');
            } else {
                setError('Email ou senha invalidos.');
            }
        } finally {
            setLoading(false);
        }
    };

    const handleGoogleSignIn = async () => {
        setLoading(true);
        try {
            const type = isBarber ? 'BARBER' : 'CUSTOMER';
            const data = await signInWithGoogle(type);
            if (!data.profileComplete) {
                setTempAuthData(data);
                setShowModal(true);
            } else {
                finalizeLogin(data);
            }
        } catch (err) {
            console.error(err);
            alert('Erro ao entrar com Google.');
        } finally {
            setLoading(false);
        }
    };

    const handleSaveProfile = async () => {
        setLoading(true);
        try {
            const isBarberProfile = tempAuthData.userType === 'BARBER';
            const payload = {
                documentCPF: extraData.cpf,
                tell: extraData.phone,
            };
            // Se for barbeiro, inclui campos obrigatórios de horário de trabalho
            if (isBarberProfile) {
                payload.workStartTime = extraData.workStart;
                payload.workEndTime = extraData.workEnd;
                payload.isOwner = false;
            }
            // Envia dados para criar/completar o registro no banco
            const result = await completeProfileApi(
                tempAuthData.userType, 
                payload,
                { name: tempAuthData.user?.name }
            );
            // O backend agora retorna o AuthResponseDTO completo após salvar
            const finalData = {
                user: {
                    id: result.id,
                    name: result.name,
                    email: result.email,
                    phone: result.phone,
                    photoUrl: result.photoUrl,
                    firebaseUid: tempAuthData.user?.firebaseUid,
                },
                userType: result.userType,
                profileComplete: result.profileComplete,
                role: result.role,
                authProvider: result.authProvider,
            };
            finalizeLogin(finalData);
        } catch (err) {
            console.error(err);
            alert('Erro ao salvar dados complementares.');
        } finally {
            setLoading(false);
        }
    };

    const finalizeLogin = (data) => {
        localStorage.setItem('user', JSON.stringify(data.user));
        localStorage.setItem('userType', data.userType);
        if (data?.user?.id) {
            localStorage.setItem('userId', String(data.user.id));
        }
        if (data.userType === 'BARBER') {
            navigate('/barberHome');
        } else {
            navigate('/homepage');
        }
    };

    return (
        <div className={Styles.Login_Inputs_container}>
            <form onSubmit={handleEmailLogin}>
                <label>
                    <p className={Styles.label_input}>E-mail:</p>
                    <input id={Styles.email_input} type="email" placeholder="seuemail@exemplo.com" value={email} onChange={(e) => setEmail(e.target.value)} required />
                </label>
                <label>
                    <p className={Styles.label_input}>Senha:</p>
                    <input id={Styles.password_input} type="password" placeholder="********" value={password} onChange={(e) => setPassword(e.target.value)} required />
                    <p className={Styles.forgot_password_text}>Esqueceu a senha?</p>
                </label>
                {error && <p style={{ color: '#ff4444', fontSize: '14px', marginTop: '8px' }}>{error}</p>}
                <button className={Styles.Login_button} type="submit" disabled={loading}>{loading ? 'Entrando...' : 'Entrar'}</button>
            </form>

            <div style={{ display: 'flex', alignItems: 'center', margin: '20px 0', width: '60%' }}>
                <div style={{ flex: 1, height: '1px', backgroundColor: '#555' }}></div>
                <span style={{ padding: '0 12px', color: '#999', fontSize: '13px' }}>ou entre com</span>
                <div style={{ flex: 1, height: '1px', backgroundColor: '#555' }}></div>
            </div>

            <button onClick={handleGoogleSignIn} disabled={loading} type="button" style={{ width: '60%', padding: '12px 20px', borderRadius: '8px', border: '1px solid #555', backgroundColor: '#fff', color: '#333', fontSize: '15px', fontWeight: '600', cursor: loading ? 'not-allowed' : 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '10px', opacity: loading ? 0.6 : 1, transition: 'all 0.2s', marginTop: '5px' }}>
                <svg width="20" height="20" viewBox="0 0 48 48"><path fill="#EA4335" d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z"/><path fill="#4285F4" d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z"/><path fill="#FBBC05" d="M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z"/><path fill="#34A853" d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z"/></svg>
                {loading ? 'Aguarde...' : 'Entrar com Google'}
            </button>

            {showModal && (
                <div style={{ position: 'fixed', top: 0, left: 0, width: '100vw', height: '100vh', background: 'rgba(0,0,0,0.6)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 9999 }}>
                    <div style={{ background: '#2a2a2a', padding: '32px', borderRadius: '12px', minWidth: '320px', maxWidth: '400px', color: '#fff', display: 'flex', flexDirection: 'column', gap: '12px' }}>
                        <h3 style={{ margin: 0, color: '#c19006' }}>Complete seu perfil</h3>
                        <p style={{ fontSize: '14px', color: '#ccc', margin: 0 }}>
                            Precisamos de alguns dados para finalizar o cadastro.
                        </p>
                        <input placeholder="CPF (somente numeros)" value={extraData.cpf} onChange={(e) => setExtraData({ ...extraData, cpf: e.target.value })} style={{ padding: '12px', borderRadius: '8px', border: 'none', backgroundColor: '#4F4F4F', color: '#fff', fontSize: '15px' }} />
                        <input placeholder="Telefone (11999999999)" value={extraData.phone} onChange={(e) => setExtraData({ ...extraData, phone: e.target.value })} style={{ padding: '12px', borderRadius: '8px', border: 'none', backgroundColor: '#4F4F4F', color: '#fff', fontSize: '15px' }} />
                        {tempAuthData?.userType === 'BARBER' && (
                            <>
                                <label style={{ fontSize: '14px', color: '#ccc', margin: 0 }}>Início de Expediente:</label>
                                <input type="time" value={extraData.workStart} onChange={(e) => setExtraData({ ...extraData, workStart: e.target.value })} style={{ padding: '12px', borderRadius: '8px', border: 'none', backgroundColor: '#4F4F4F', color: '#fff', fontSize: '15px' }} />
                                <label style={{ fontSize: '14px', color: '#ccc', margin: 0 }}>Fim de Expediente:</label>
                                <input type="time" value={extraData.workEnd} onChange={(e) => setExtraData({ ...extraData, workEnd: e.target.value })} style={{ padding: '12px', borderRadius: '8px', border: 'none', backgroundColor: '#4F4F4F', color: '#fff', fontSize: '15px' }} />
                            </>
                        )}
                        <div style={{ display: 'flex', gap: '10px', marginTop: '8px' }}>
                            <button onClick={handleSaveProfile} disabled={loading} style={{ flex: 1, padding: '12px', borderRadius: '8px', border: 'none', backgroundColor: '#c19006', color: '#333', fontWeight: 'bold', fontSize: '15px', cursor: 'pointer' }}>{loading ? 'Salvando...' : 'Finalizar Cadastro'}</button>
                            <button onClick={() => setShowModal(false)} type="button" style={{ padding: '12px 20px', borderRadius: '8px', border: '1px solid #555', backgroundColor: 'transparent', color: '#ccc', cursor: 'pointer', fontSize: '14px' }}>Cancelar</button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default Login_Inputs;
