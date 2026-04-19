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

    if (loading) return <div className={styles.loadingContainer}>Carregando...</div>;

    return (
        <div className={`${styles.pageContainer} ${styles.withNavbar}`}>
            <div className={styles.contentWrapper}>
            <BarberHeader barber={barber} onLogout={handleLogout} activeTab="dashboards" onTabChange={handleTabChange} isOwner={true} barbershopId={barber?.barbershopId} />

            <section className={styles.heroSection}>
                <p className={styles.heroKicker}>DASHBOARD</p>
                <h1>Resumo da sua atividade</h1>
            </section>

            <section className={`${styles.dashboardSection} ${styles.animateItem} ${styles.delay2}`}>
                <div className={styles.dashboardStatsGrid}>
                    <article className={styles.dashboardStatCard}>
                        <p className={styles.dashboardStatValue}>—</p>
                        <p className={styles.dashboardStatLabel}>Agendamentos hoje</p>
                    </article>
                    <article className={styles.dashboardStatCard}>
                        <p className={styles.dashboardStatValue}>—</p>
                        <p className={styles.dashboardStatLabel}>Agendamentos este mês</p>
                    </article>
                    <article className={styles.dashboardStatCard}>
                        <p className={styles.dashboardStatValue}>—</p>
                        <p className={styles.dashboardStatLabel}>Receita este mês</p>
                    </article>
                    <article className={styles.dashboardStatCard}>
                        <p className={styles.dashboardStatValue}>—</p>
                        <p className={styles.dashboardStatLabel}>Avaliação média</p>
                    </article>
                </div>

                <div className={styles.dashboardPlaceholderCard}>
                    <p className={styles.dashboardPlaceholderText}>
                        📊 Relatórios detalhados em breve.<br />
                        Acesse a aba <strong>Minha Agenda</strong> para ver seus agendamentos completos.
                    </p>
                </div>

                {/* Ação rápida: encaixe */}
                <div className={styles.dashboardCtaSection}>
                    <button
                        onClick={() => navigate('/barberHome/novo-agendamento')}
                        className={styles.dashboardCtaButton}
                    >
                        ✂️ Novo Encaixe
                    </button>
                    <p className={styles.dashboardCtaHint}>
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
