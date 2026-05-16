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
import { FixedExpensesPiePanel, FixedExpensesTable } from '../components/Dashboard/panels/FixedExpensesPanel';
import { ExportPDFModal } from '../components/Dashboard/ExportPDFModal';
import {
    getFinancialOverview,
    getMyShopBarberPerformance,
    getStockHealthAlert,
    getAgendaThermometer,
    getBarberSkillMatrix,
    getCustomerAcquisition,
    getCustomerRetention,
} from '../services/analyticsService';
import { getMyFixedExpenses, createFixedExpense, deleteFixedExpense } from '../services/barbershopService';
import styles from './CSS/BarberHomePage.module.css';

function BarberDashboardPage() {
    const navigate = useNavigate();
    const [barber, setBarber] = useState(null);
    const [loading, setLoading] = useState(true);
    const [showExportModal, setShowExportModal] = useState(false);

    const [barberPerf, setBarberPerf] = useState([]);
    const [stockHealth, setStockHealth] = useState([]);
    const [agendaThermo, setAgendaThermo] = useState([]);
    const [skillMatrix, setSkillMatrix] = useState([]);
    const [customerAcq, setCustomerAcq] = useState([]);
    const [customerRet, setCustomerRet] = useState([]);
    const [fixedExpenses, setFixedExpenses] = useState([]);
    const [financialOverview, setFinancialOverview] = useState(null);

    // ── Gastos fixos modal ──────────────────────────────────────────────────
    const [showExpenseModal, setShowExpenseModal] = useState(false);
    const [expenseForm, setExpenseForm] = useState({
        category: 'OUTROS',
        customName: '',
        amount: '',
        month: new Date().getMonth() + 1,
        year: new Date().getFullYear(),
        recurringMonthly: true,
    });
    const [savingExpense, setSavingExpense] = useState(false);
    const [expenseMonth, setExpenseMonth] = useState(new Date().getMonth() + 1);
    const [expenseYear, setExpenseYear] = useState(new Date().getFullYear());

    const EXPENSE_CATEGORIES = [
        { value: 'AGUA',          label: 'Água' },
        { value: 'LUZ',           label: 'Luz' },
        { value: 'ALUGUEL',       label: 'Aluguel' },
        { value: 'INTERNET',      label: 'Internet' },
        { value: 'ENERGIA',       label: 'Energia' },
        { value: 'FUNCIONARIOS',  label: 'Funcionários' },
        { value: 'MATERIAL',      label: 'Material' },
        { value: 'SISTEMA',       label: 'Sistema' },
        { value: 'CONTABILIDADE', label: 'Contabilidade' },
        { value: 'MARKETING',     label: 'Marketing' },
        { value: 'MANUTENCAO',    label: 'Manutenção' },
        { value: 'OUTROS',        label: 'Outros' },
    ];

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
        const now = new Date();
        const monthStartDate = new Date(expenseYear, expenseMonth - 1, 1);
        const monthEndDate = new Date(expenseYear, expenseMonth, 0);
        const selectedCurrentMonth = expenseYear === now.getFullYear() && expenseMonth === now.getMonth() + 1;
        const periodEndDate = selectedCurrentMonth ? now : monthEndDate;
        const period = {
            from: monthStartDate.toLocaleDateString('en-CA'),
            to: periodEndDate.toLocaleDateString('en-CA'),
        };
        const [overview, perf, stock, thermo, skill, acq, ret, expenses] = await Promise.allSettled([
            getFinancialOverview(barbershopId, period),
            getMyShopBarberPerformance(barbershopId, period),
            getStockHealthAlert(barbershopId),
            getAgendaThermometer(barbershopId),
            getBarberSkillMatrix(barbershopId),
            getCustomerAcquisition(),
            getCustomerRetention(),
            getMyFixedExpenses(expenseMonth, expenseYear),
        ]);
        if (overview.status === 'fulfilled') setFinancialOverview(overview.value);
        if (perf.status === 'fulfilled') setBarberPerf(perf.value);
        if (stock.status === 'fulfilled') setStockHealth(stock.value);
        if (thermo.status === 'fulfilled') setAgendaThermo(thermo.value);
        if (skill.status === 'fulfilled') setSkillMatrix(skill.value);
        if (acq.status === 'fulfilled') setCustomerAcq(acq.value);
        if (ret.status === 'fulfilled') setCustomerRet(ret.value);
        if (expenses.status === 'fulfilled') setFixedExpenses(expenses.value);
    }, [barbershopId, expenseMonth, expenseYear]);

    useEffect(() => { fetchAll(); }, [fetchAll]);

    const handleLogout = async () => { await logoutUser(); navigate('/'); };
    const handleTabChange = (tab) => navigateToBarberTab(tab, navigate, { isOwner: true, currentPath: '/barberHome/dashboard' });

    const handleSaveExpense = async (e) => {
        e.preventDefault();
        if (!expenseForm.amount || Number(expenseForm.amount) <= 0) return;
        setSavingExpense(true);
        try {
            await createFixedExpense({
                category: expenseForm.category,
                customName: expenseForm.customName || null,
                amount: parseFloat(expenseForm.amount),
                month: expenseForm.month,
                year: expenseForm.year,
                recurringMonthly: expenseForm.recurringMonthly,
            });
            setExpenseForm(prev => ({ ...prev, customName: '', amount: '' }));
            setExpenseMonth(expenseForm.month);
            setExpenseYear(expenseForm.year);
            const updated = await getMyFixedExpenses(expenseForm.month, expenseForm.year);
            setFixedExpenses(updated);
            setShowExpenseModal(false);
        } catch {
            // toast já aparece se necessário
        } finally {
            setSavingExpense(false);
        }
    };

    const handleDeleteExpense = async (id) => {
        await deleteFixedExpense(id);
        const updated = await getMyFixedExpenses(expenseMonth, expenseYear);
        setFixedExpenses(updated);
    };

    const handleExpenseFilterChange = async (month, year) => {
        setExpenseMonth(month);
        setExpenseYear(year);
        const updated = await getMyFixedExpenses(month, year);
        setFixedExpenses(updated);
    };

    if (loading) return <div className={styles.loadingContainer}>Carregando...</div>;

    const asCurrency = (value) => `R$ ${Number(value || 0).toFixed(2).replace('.', ',')}`;
    const fixedExpensesTotal = fixedExpenses.reduce((sum, expense) => sum + Number(expense?.amount || 0), 0);
    const grossRevenue = Number(financialOverview?.totalServiceRevenue || 0);
    const productExpenses = Number(financialOverview?.productExpenses || 0);
    const estimatedResult = grossRevenue - productExpenses - fixedExpensesTotal;

    return (
        <div className={`${styles.pageContainer} ${styles.withNavbar}`}>
            <div className={styles.contentWrapper}>
                <BarberHeader barber={barber} onLogout={handleLogout} activeTab="dashboards" onTabChange={handleTabChange} isOwner={true} barbershopId={barbershopId} />

                <section className={styles.heroSection}>
                    <p className={styles.heroKicker}>DASHBOARD & RELATÓRIOS</p>
                    <h1>Análise da sua barbearia</h1>
                    <div style={{ display: 'flex', gap: '0.75rem', marginTop: '0.75rem', flexWrap: 'wrap' }}>
                        <button
                            onClick={() => setShowExportModal(true)}
                            className={styles.dashboardCtaButton}
                            style={{ fontSize: '0.9rem', padding: '0.55rem 1.2rem' }}
                            type="button"
                        >
                            📄 Exportar PDF
                        </button>
                        <button
                            onClick={fetchAll}
                            className={styles.dashboardCtaButton}
                            style={{ fontSize: '0.9rem', padding: '0.55rem 1.2rem', background: '#333', color: '#fff' }}
                            type="button"
                        >
                            � Atualizar dados
                        </button>
                    </div>
                </section>

                <section className={styles.dashboardStatsGrid}>
                    <div className={styles.dashboardStatCard}>
                        <p className={styles.dashboardStatValue}>{asCurrency(grossRevenue)}</p>
                        <p className={styles.dashboardStatLabel}>Faturamento do mês</p>
                    </div>
                    <div className={styles.dashboardStatCard}>
                        <p className={styles.dashboardStatValue}>{asCurrency(productExpenses)}</p>
                        <p className={styles.dashboardStatLabel}>Gastos com produtos</p>
                    </div>
                    <div className={styles.dashboardStatCard}>
                        <p className={styles.dashboardStatValue}>{asCurrency(fixedExpensesTotal)}</p>
                        <p className={styles.dashboardStatLabel}>Gastos fixos</p>
                    </div>
                    <div className={styles.dashboardStatCard}>
                        <p className={styles.dashboardStatValue}>{asCurrency(estimatedResult)}</p>
                        <p className={styles.dashboardStatLabel}>Resultado estimado</p>
                    </div>
                </section>

                <section className={`${styles.dashboardSection} ${styles.animateItem} ${styles.delay2}`}>

                    <DashReportPanel
                        title="Performance dos Barbeiros"
                        dashContent={<BarberPerformancePanel data={barberPerf} />}
                        reportContent={<BarberPerformanceTable data={barberPerf} />}
                    />

                    <DashReportPanel
                        title="Saúde do Estoque"
                        dashContent={<StockHealthPanel data={stockHealth} />}
                        reportContent={<StockHealthTable data={stockHealth} />}
                    />

                    <DashReportPanel
                        title="Termômetro de Agenda"
                        dashContent={<AgendaThermometerPanel data={agendaThermo} />}
                        reportContent={<AgendaThermometerTable data={agendaThermo} />}
                    />

                    <DashReportPanel
                        title="Matriz de Habilidades"
                        dashContent={<BarberSkillMatrixPanel data={skillMatrix} />}
                        reportContent={<BarberSkillMatrixTable data={skillMatrix} />}
                    />

                    <DashReportPanel
                        title="Aquisição de Clientes"
                        dashContent={<CustomerAcquisitionPanel data={customerAcq} />}
                        reportContent={<CustomerAcquisitionTable data={customerAcq} />}
                    />

                    <DashReportPanel
                        title="Retenção de Clientes"
                        dashContent={<CustomerRetentionPanel data={customerRet} />}
                        reportContent={<CustomerRetentionTable data={customerRet} />}
                    />

                    {/* ── Gastos Fixos ── */}
                    <DashReportPanel
                        title="Gastos Fixos do Mês"
                        onRefresh={() => handleExpenseFilterChange(expenseMonth, expenseYear)}
                        dashContent={
                            <div>
                                <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center', marginBottom: '0.75rem', flexWrap: 'wrap' }}>
                                    <select
                                        value={expenseMonth}
                                        onChange={e => handleExpenseFilterChange(Number(e.target.value), expenseYear)}
                                        style={{ background: '#1a1a1a', color: '#fff', border: '1px solid #333', borderRadius: 6, padding: '0.3rem 0.5rem', fontSize: '0.85rem' }}
                                    >
                                        {[1,2,3,4,5,6,7,8,9,10,11,12].map(m => (
                                            <option key={m} value={m}>{m.toString().padStart(2,'0')}</option>
                                        ))}
                                    </select>
                                    <input
                                        type="number"
                                        value={expenseYear}
                                        onChange={e => handleExpenseFilterChange(expenseMonth, Number(e.target.value))}
                                        min={2020} max={2099}
                                        style={{ background: '#1a1a1a', color: '#fff', border: '1px solid #333', borderRadius: 6, padding: '0.3rem 0.5rem', fontSize: '0.85rem', width: '80px' }}
                                    />
                                    <button
                                        type="button"
                                        onClick={() => setShowExpenseModal(true)}
                                        style={{ background: '#d4af37', color: '#000', border: 'none', borderRadius: 6, padding: '0.3rem 0.85rem', fontWeight: 700, fontSize: '0.85rem', cursor: 'pointer' }}
                                    >
                                        + Adicionar Gasto
                                    </button>
                                </div>
                                <FixedExpensesPiePanel data={fixedExpenses} />
                            </div>
                        }
                        reportContent={
                            <FixedExpensesTable data={fixedExpenses} onDelete={handleDeleteExpense} />
                        }
                    />

                    <div className={styles.dashboardCtaSection}>
                        <button onClick={() => navigate('/barberHome/novo-agendamento')} className={styles.dashboardCtaButton}>
                            ✂️ Novo Encaixe
                        </button>
                        <p className={styles.dashboardCtaHint}>Registre um atendimento presencial sem app.</p>
                    </div>
                </section>

                {showExpenseModal && (
                    <div className={styles.modalBackdrop} onClick={() => setShowExpenseModal(false)}>
                        <div className={styles.modalCard} onClick={e => e.stopPropagation()}>
                            <p className={styles.modalKicker}>GASTOS FIXOS</p>
                            <h3 className={styles.modalTitle}>Adicionar Gasto</h3>
                            <form onSubmit={handleSaveExpense} className={styles.shopEditForm}>
                                <div className={styles.expenseModeGroup} role="group" aria-label="Tipo de gasto">
                                    <label className={`${styles.expenseModeOption} ${expenseForm.recurringMonthly ? styles.expenseModeOptionActive : ''}`}>
                                        <input
                                            type="radio"
                                            name="expenseMode"
                                            checked={expenseForm.recurringMonthly}
                                            onChange={() => setExpenseForm(p => ({ ...p, recurringMonthly: true }))}
                                        />
                                        <span>Fixo mensal</span>
                                        <small>Repete automaticamente nos próximos meses.</small>
                                    </label>
                                    <label className={`${styles.expenseModeOption} ${!expenseForm.recurringMonthly ? styles.expenseModeOptionActive : ''}`}>
                                        <input
                                            type="radio"
                                            name="expenseMode"
                                            checked={!expenseForm.recurringMonthly}
                                            onChange={() => setExpenseForm(p => ({ ...p, recurringMonthly: false }))}
                                        />
                                        <span>Somente este mês</span>
                                        <small>Lançamento pontual para o mês escolhido.</small>
                                    </label>
                                </div>
                                <label className={styles.shopField}>
                                    <span>Categoria</span>
                                    <select
                                        value={expenseForm.category}
                                        onChange={e => setExpenseForm(p => ({ ...p, category: e.target.value }))}
                                    >
                                        {EXPENSE_CATEGORIES.map(c => (
                                            <option key={c.value} value={c.value}>{c.label}</option>
                                        ))}
                                    </select>
                                </label>
                                <label className={styles.shopField}>
                                    <span>Nome personalizado (opcional)</span>
                                    <input
                                        value={expenseForm.customName}
                                        onChange={e => setExpenseForm(p => ({ ...p, customName: e.target.value }))}
                                        maxLength={80}
                                        placeholder="Ex: Aluguel sala 2"
                                    />
                                </label>
                                <label className={styles.shopField}>
                                    <span>Valor (R$)</span>
                                    <input
                                        type="number"
                                        min="0.01"
                                        step="0.01"
                                        value={expenseForm.amount}
                                        onChange={e => setExpenseForm(p => ({ ...p, amount: e.target.value }))}
                                        required
                                    />
                                </label>
                                <div style={{ display: 'flex', gap: '0.75rem' }}>
                                    <label className={styles.shopField} style={{ flex: 1 }}>
                                        <span>{expenseForm.recurringMonthly ? 'A partir do mês' : 'Mês'}</span>
                                        <select
                                            value={expenseForm.month}
                                            onChange={e => setExpenseForm(p => ({ ...p, month: Number(e.target.value) }))}
                                        >
                                            {[1,2,3,4,5,6,7,8,9,10,11,12].map(m => (
                                                <option key={m} value={m}>{m.toString().padStart(2,'0')}</option>
                                            ))}
                                        </select>
                                    </label>
                                    <label className={styles.shopField} style={{ flex: 1 }}>
                                        <span>{expenseForm.recurringMonthly ? 'A partir do ano' : 'Ano'}</span>
                                        <select
                                            value={expenseForm.year}
                                            onChange={e => setExpenseForm(p => ({ ...p, year: Number(e.target.value) }))}
                                        >
                                            {[2024, 2025, 2026, 2027].map(y => (
                                                <option key={y} value={y}>{y}</option>
                                            ))}
                                        </select>
                                    </label>
                                </div>
                                <div className={styles.modalActions}>
                                    <button
                                        type="button"
                                        className={styles.modalSecondaryButton}
                                        onClick={() => setShowExpenseModal(false)}
                                    >
                                        Cancelar
                                    </button>
                                    <button
                                        type="submit"
                                        disabled={savingExpense}
                                        className={styles.dashboardCtaButton}
                                    >
                                        {savingExpense ? 'Salvando...' : 'Salvar'}
                                    </button>
                                </div>
                            </form>
                        </div>
                    </div>
                )}

                {showExportModal && (
                    <ExportPDFModal
                        barbershopName={barber?.barbershopName ?? 'Barbearia'}
                        analyticsData={{ barberPerf, stockHealth, agendaThermo, skillMatrix, customerAcq, customerRet }}
                        onClose={() => setShowExportModal(false)}
                    />
                )}
            </div>
            <BarberNavbar activeTab="dashboards" onTabChange={handleTabChange} isOwner={true} barbershopId={barbershopId} />
        </div>
    );
}

export default BarberDashboardPage;
