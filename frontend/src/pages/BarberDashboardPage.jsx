import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';
import { logoutUser } from '../services/authService';
import { useAuthGuard } from '../hooks/useAuthGuard';
import { navigateToBarberTab } from '../services/navigationService';
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
    const { isAuthorized } = useAuthGuard({
        allowCustomer: false,
        allowBarber: true,
        requireOwner: true,
        redirectIfOwnerDenied: '/barberHome',
    });

    useEffect(() => {
        if (!isAuthorized) {
            return;
        }

        api.get('/auth/me')
            .then(res => {
                setBarber(res.data);
                setLoading(false);
            })
            .catch(() => { setLoading(false); navigate('/'); });
    }, [isAuthorized, navigate]);

    const handleLogout = async () => {
        await logoutUser();
        navigate('/');
    };

    const handleTabChange = (tab) => {
        navigateToBarberTab(tab, navigate, {
            isOwner: true,
            currentPath: '/barberHome/dashboard',
        });
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
