import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';
import { logoutUser } from '../services/authService';
import { isCustomer, isOwnerUser } from '../services/userContext';
import BarberHeader from '../components/BarberPage/BarberHeader';
import BarberNavbar from '../components/BarberPage/BarberNavbar';
import styles from './CSS/BarberHomePage.module.css';

/**
 * Página de Dashboard / Relatórios do Barbeiro.
 * Exibe resumo de agendamentos, receita e métricas básicas.
 */
function BarberDashboardPage() {
    const navigate = useNavigate();
    const [barber, setBarber] = useState(null);
    const [loading, setLoading] = useState(true);
    const [stats, setStats] = useState(null);

    useEffect(() => {
        // Guard: cliente não pode acessar painel de barbeiro
        if (isCustomer()) {
            navigate('/homepage', { replace: true });
            return;
        }
        // Guard: dashboard é exclusivo de owner
        if (!isOwnerUser()) {
            navigate('/barberHome', { replace: true });
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
                setLoading(false);
            })
            .catch(() => { setLoading(false); navigate('/'); });
    }, [navigate]);

    const handleLogout = async () => {
        await logoutUser();
        navigate('/');
    };

    const handleTabChange = (tab) => {
        if (tab === 'home') navigate('/barberHome');
        else if (tab === 'agenda') navigate('/meus-agendamentos');
        else if (tab === 'servicos') navigate('/barberHome/servicos');
        else if (tab === 'estoque') navigate('/barberHome/estoque');
        else if (tab === 'perfil') navigate('/barberHome/perfil');
        else if (tab === 'time') navigate('/barberHome/time');
        else if (tab === 'agenda-equipe') navigate('/barberHome/agenda-equipe');
        else if (tab === 'novo-agendamento') navigate('/barberHome/novo-agendamento');
    };

    const cardStyle = {
        background: 'rgba(255,255,255,0.05)', borderRadius: 12, padding: 20,
        textAlign: 'center', flex: '1 1 140px'
    };

    if (loading) return <div className={styles.loadingContainer}>Carregando...</div>;

    return (
        <div className={`${styles.pageContainer} ${styles.withNavbar}`}>
            <div className={styles.contentWrapper}>
            <BarberHeader barber={barber} onLogout={handleLogout} activeTab="dashboards" onTabChange={handleTabChange} isOwner={true} barbershopId={barber?.barbershopId} />

            <section className={styles.heroSection}>
                <p className={styles.heroKicker}>DASHBOARD</p>
                <h1>Resumo da sua atividade</h1>
            </section>

            <section className={styles.dashboardSection}>
                <div style={{ display: 'flex', gap: 16, flexWrap: 'wrap', marginBottom: 32 }}>
                    <div style={cardStyle}>
                        <p style={{ fontSize: 28, fontWeight: 700, margin: '0 0 6px' }}>—</p>
                        <p style={{ fontSize: 13, color: 'rgba(255,255,255,0.5)', margin: 0 }}>Agendamentos hoje</p>
                    </div>
                    <div style={cardStyle}>
                        <p style={{ fontSize: 28, fontWeight: 700, margin: '0 0 6px' }}>—</p>
                        <p style={{ fontSize: 13, color: 'rgba(255,255,255,0.5)', margin: 0 }}>Agendamentos este mês</p>
                    </div>
                    <div style={cardStyle}>
                        <p style={{ fontSize: 28, fontWeight: 700, margin: '0 0 6px' }}>—</p>
                        <p style={{ fontSize: 13, color: 'rgba(255,255,255,0.5)', margin: 0 }}>Receita este mês</p>
                    </div>
                    <div style={cardStyle}>
                        <p style={{ fontSize: 28, fontWeight: 700, margin: '0 0 6px' }}>—</p>
                        <p style={{ fontSize: 13, color: 'rgba(255,255,255,0.5)', margin: 0 }}>Avaliação média</p>
                    </div>
                </div>

                <div style={{
                    background: 'rgba(255,255,255,0.04)', border: '1px dashed rgba(255,255,255,0.15)',
                    borderRadius: 12, padding: 32, textAlign: 'center'
                }}>
                    <p style={{ fontSize: 14, color: 'rgba(255,255,255,0.4)', margin: 0 }}>
                        📊 Relatórios detalhados em breve.<br />
                        Acesse a aba <strong>Minha Agenda</strong> para ver seus agendamentos completos.
                    </p>
                </div>

                {/* Ação rápida: encaixe */}
                <div style={{ marginTop: 28, textAlign: 'center' }}>
                    <button
                        onClick={() => navigate('/barberHome/novo-agendamento')}
                        style={{
                            background: '#d4af37', color: '#1a1a1a', fontWeight: 700,
                            fontSize: 15, border: 'none', borderRadius: 10,
                            padding: '13px 32px', cursor: 'pointer',
                        }}
                    >
                        ✂️ Novo Encaixe
                    </button>
                    <p style={{ marginTop: 8, fontSize: 12, color: 'rgba(255,255,255,0.35)' }}>
                        Registre um atendimento presencial sem app.
                    </p>
                </div>
            </section>
            </div>
            <BarberNavbar activeTab="dashboards" onTabChange={handleTabChange} isOwner={true} barbershopId={barber?.barbershopId} />
        </div>
    );
}

export default BarberDashboardPage;
