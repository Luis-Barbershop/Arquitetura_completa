import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';
import { logoutUser } from '../services/authService';
import { useAuthGuard } from '../hooks/useAuthGuard';
import { navigateToBarberTab } from '../services/navigationService';
import BarberHeader from '../components/BarberPage/BarberHeader';
import BarberNavbar from '../components/BarberPage/BarberNavbar';
import DashReportPanel from '../components/Dashboard/DashReportPanel';
import { BarberPerformancePanel, BarberPerformanceTable } from '../components/Dashboard/panels/BarberPerformancePanel';
import { StockHealthPanel, StockHealthTable } from '../components/Dashboard/panels/StockHealthPanel';
import { AgendaThermometerPanel, AgendaThermometerTable } from '../components/Dashboard/panels/AgendaThermometerPanel';
import { BarberSkillMatrixPanel, BarberSkillMatrixTable } from '../components/Dashboard/panels/BarberSkillMatrixPanel';
import { CustomerAcquisitionPanel, CustomerAcquisitionTable } from '../components/Dashboard/panels/CustomerAcquisitionPanel';
import { CustomerRetentionPanel, CustomerRetentionTable } from '../components/Dashboard/panels/CustomerRetentionPanel';
import {
    getBarberPerformance,
    getStockHealthAlert,
    getAgendaThermometer,
    getBarberSkillMatrix,
    getCustomerAcquisition,
    getCustomerRetention,
} from '../services/analyticsService';
import styles from './CSS/BarberHomePage.module.css';

function BarberDashboardPage() {
    const navigate = useNavigate();
    const [barber, setBarber] = useState(null);
    const [loading, setLoading] = useState(true);

    const [barberPerf, setBarberPerf] = useState([]);
    const [stockHealth, setStockHealth] = useState([]);
    const [agendaThermo, setAgendaThermo] = useState([]);
    const [skillMatrix, setSkillMatrix] = useState([]);
    const [customerAcq, setCustomerAcq] = useState([]);
    const [customerRet, setCustomerRet] = useState([]);

    const { isAuthorized } = useAuthGuard({
        allowCustomer: false,
        allowBarber: true,
        requireOwner: true,
        redirectIfOwnerDenied: '/barberHome',
    });

    useEffect(() => {
        if (!isAuthorized) return;
        api.get('/auth/me')
            .then(res => { setBarber(res.data); setLoading(false); })
            .catch(() => { setLoading(false); navigate('/'); });
    }, [isAuthorized, navigate]);

    const barbershopId = barber?.barbershopId;

    const fetchAll = useCallback(async () => {
        if (!barbershopId) return;
        const [perf, stock, thermo, skill, acq, ret] = await Promise.allSettled([
            getBarberPerformance(barbershopId),
            getStockHealthAlert(barbershopId),
            getAgendaThermometer(barbershopId),
            getBarberSkillMatrix(barbershopId),
            getCustomerAcquisition(),
            getCustomerRetention(),
        ]);
        if (perf.status === 'fulfilled') setBarberPerf(perf.value);
        if (stock.status === 'fulfilled') setStockHealth(stock.value);
        if (thermo.status === 'fulfilled') setAgendaThermo(thermo.value);
        if (skill.status === 'fulfilled') setSkillMatrix(skill.value);
        if (acq.status === 'fulfilled') setCustomerAcq(acq.value);
        if (ret.status === 'fulfilled') setCustomerRet(ret.value);
    }, [barbershopId]);

    useEffect(() => { fetchAll(); }, [fetchAll]);

    const handleLogout = async () => { await logoutUser(); navigate('/'); };
    const handleTabChange = (tab) => navigateToBarberTab(tab, navigate, { isOwner: true, currentPath: '/barberHome/dashboard' });

    if (loading) return <div className={styles.loadingContainer}>Carregando...</div>;

    return (
        <div className={`${styles.pageContainer} ${styles.withNavbar}`}>
            <div className={styles.contentWrapper}>
                <BarberHeader barber={barber} onLogout={handleLogout} activeTab="dashboards" onTabChange={handleTabChange} isOwner={true} barbershopId={barbershopId} />

                <section className={styles.heroSection}>
                    <p className={styles.heroKicker}>DASHBOARD & RELATÓRIOS</p>
                    <h1>Análise da sua barbearia</h1>
                </section>

                <section className={`${styles.dashboardSection} ${styles.animateItem} ${styles.delay2}`}>

                    <DashReportPanel
                        title="Performance dos Barbeiros"
                        onRefresh={fetchAll}
                        dashContent={<BarberPerformancePanel data={barberPerf} />}
                        reportContent={<BarberPerformanceTable data={barberPerf} />}
                    />

                    <DashReportPanel
                        title="Saúde do Estoque"
                        onRefresh={fetchAll}
                        dashContent={<StockHealthPanel data={stockHealth} />}
                        reportContent={<StockHealthTable data={stockHealth} />}
                    />

                    <DashReportPanel
                        title="Termômetro de Agenda"
                        onRefresh={fetchAll}
                        dashContent={<AgendaThermometerPanel data={agendaThermo} />}
                        reportContent={<AgendaThermometerTable data={agendaThermo} />}
                    />

                    <DashReportPanel
                        title="Matriz de Habilidades"
                        onRefresh={fetchAll}
                        dashContent={<BarberSkillMatrixPanel data={skillMatrix} />}
                        reportContent={<BarberSkillMatrixTable data={skillMatrix} />}
                    />

                    <DashReportPanel
                        title="Aquisição de Clientes"
                        onRefresh={fetchAll}
                        dashContent={<CustomerAcquisitionPanel data={customerAcq} />}
                        reportContent={<CustomerAcquisitionTable data={customerAcq} />}
                    />

                    <DashReportPanel
                        title="Retenção de Clientes"
                        onRefresh={fetchAll}
                        dashContent={<CustomerRetentionPanel data={customerRet} />}
                        reportContent={<CustomerRetentionTable data={customerRet} />}
                    />

                    <div className={styles.dashboardCtaSection}>
                        <button onClick={() => navigate('/barberHome/novo-agendamento')} className={styles.dashboardCtaButton}>
                            ✂️ Novo Encaixe
                        </button>
                        <p className={styles.dashboardCtaHint}>Registre um atendimento presencial sem app.</p>
                    </div>
                </section>
            </div>
            <BarberNavbar activeTab="dashboards" onTabChange={handleTabChange} isOwner={true} barbershopId={barbershopId} />
        </div>
    );
}

export default BarberDashboardPage;
