import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import api from '../services/api';
import { logoutUser } from '../services/authService';
import { isCustomer } from '../services/userContext';
import BarberHeader from '../components/BarberPage/BarberHeader';
import BarberNavbar from '../components/BarberPage/BarberNavbar';
import styles from './CSS/BarberHomePage.module.css';

/**
 * Página de Perfil do Barbeiro — exibe e permite editar dados pessoais.
 */
function BarberProfilePage() {
    const navigate = useNavigate();
    const [barber, setBarber] = useState(null);
    const [loading, setLoading] = useState(true);
    const [actAsBarber, setActAsBarber] = useState(true);
    const [savingActAsBarber, setSavingActAsBarber] = useState(false);

    useEffect(() => {
        // Guard: cliente não pode acessar páginas de barbeiro
        if (isCustomer()) {
            navigate('/homepage', { replace: true });
            return;
        }
        const token = localStorage.getItem('token');
        if (!token) {
            navigate('/', { replace: true });
            return;
        }
        api.get('/auth/me')
            .then(res => {
                setBarber(res.data);
                setActAsBarber(res.data?.actAsBarber ?? true);
                setLoading(false);
            })
            .catch(() => { setLoading(false); navigate('/'); });
    }, [navigate]);

    const handleLogout = async () => {
        await logoutUser();
        navigate('/');
    };

    const handleActAsBarberToggle = async (newValue) => {
        setSavingActAsBarber(true);
        try {
            await api.put(`/barbers/${barber.id}`, { actAsBarber: newValue });
            setActAsBarber(newValue);
            toast.success(
                newValue
                    ? 'Você voltou a aparecer como barbeiro nos agendamentos.'
                    : 'Você não aparecerá como barbeiro para novos agendamentos.'
            );
        } catch {
            toast.error('Erro ao salvar configuração. Tente novamente.');
        } finally {
            setSavingActAsBarber(false);
        }
    };

    const hasLinkedBarbershop = !!barber?.barbershopId;

    const handleTabChange = (tab) => {
        if (tab === 'home') navigate('/barberHome');
        else if (tab === 'agenda') navigate('/meus-agendamentos');
        else if (tab === 'servicos') navigate('/barberHome/servicos');
        else if (tab === 'estoque') navigate('/barberHome/estoque');
        else if (tab === 'time') navigate('/barberHome/time');
        else if (tab === 'dashboards') navigate('/barberHome/dashboard');
    };

    if (loading) return <div className={styles.loadingContainer}>Carregando perfil...</div>;

    return (
        <div className={`${styles.pageContainer} ${hasLinkedBarbershop ? styles.withNavbar : styles.withoutNavbar}`}>
            <div className={styles.contentWrapper}>
                <BarberHeader
                    barber={barber}
                    onLogout={handleLogout}
                    activeTab="perfil"
                    isOwner={barber?.isOwner === true}
                    barbershopId={barber?.barbershopId}
                    onTabChange={handleTabChange}
                />
                <section className={styles.heroSection}>
                    <p className={styles.heroKicker}>MEU PERFIL</p>
                    <h1>Suas informações</h1>
                </section>
                <section className={styles.dashboardSection}>
                    {barber && (
                        <div style={{
                            background: 'rgba(255,255,255,0.04)',
                            border: '1px solid #2f2f2f',
                            borderRadius: 16,
                            padding: 24,
                            display: 'flex',
                            flexDirection: 'column',
                            gap: 14,
                            maxWidth: 600,
                        }}>
                            {barber.imageUrl && (
                                <img
                                    src={barber.imageUrl}
                                    alt="Foto de perfil"
                                    style={{ width: 80, height: 80, borderRadius: '50%', objectFit: 'cover' }}
                                />
                            )}
                            <div><strong>Nome:</strong> {barber.name}</div>
                            <div><strong>E-mail:</strong> {barber.email}</div>
                            <div><strong>Telefone:</strong> {barber.tell || '—'}</div>
                            <div><strong>CPF:</strong> {barber.documentCPF || '—'}</div>
                            <div>
                                <strong>Barbearia:</strong>{' '}
                                {barber.barbershopId ? `Vinculado (ID: ${barber.barbershopId})` : 'Sem barbearia'}
                            </div>
                            <div>
                                <strong>Função:</strong>{' '}
                                {barber.isOwner ? 'Dono do estabelecimento' : 'Colaborador'}
                            </div>
                            {barber.isOwner && (
                                <div style={{
                                    marginTop: 8,
                                    padding: '14px 16px',
                                    background: 'rgba(193,144,6,0.08)',
                                    borderRadius: 10,
                                    border: '1px solid #4a3a17'
                                }}>
                                    <p style={{ color: '#d4af37', fontWeight: 700, marginBottom: 10, fontSize: 14 }}>
                                        ⚙️ Configurações da Barbearia
                                    </p>
                                    <label style={{
                                        display: 'flex', alignItems: 'center', gap: 10,
                                        cursor: savingActAsBarber ? 'not-allowed' : 'pointer',
                                        opacity: savingActAsBarber ? 0.6 : 1
                                    }}>
                                        <input
                                            type="checkbox"
                                            checked={actAsBarber}
                                            disabled={savingActAsBarber}
                                            onChange={e => handleActAsBarberToggle(e.target.checked)}
                                            style={{ width: 18, height: 18, accentColor: '#d4af37', cursor: 'inherit' }}
                                        />
                                        <span style={{ fontSize: 13, lineHeight: 1.4 }}>
                                            Atuar como barbeiro — aparecer na lista de profissionais disponíveis para agendamento
                                        </span>
                                    </label>
                                </div>
                            )}
                            <p style={{ fontSize: 12, color: 'rgba(255,255,255,0.4)', marginTop: 4 }}>
                                Para atualizar sua foto de perfil, use o botão de upload disponível nas configurações.
                            </p>
                        </div>
                    )}
                </section>
            </div>
            {hasLinkedBarbershop && (
                <BarberNavbar
                    activeTab="perfil"
                    onTabChange={handleTabChange}
                    isOwner={barber?.isOwner === true}
                    barbershopId={barber?.barbershopId}
                />
            )}
        </div>
    );
}

export default BarberProfilePage;
