import React, { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { FiCalendar, FiChevronLeft, FiChevronRight, FiRefreshCw, FiScissors, FiCheckCircle, FiXCircle, FiClock } from 'react-icons/fi';
import { toast } from 'react-toastify';
import { getBarbershopSchedule } from '../services/appointmentService';
import { isOwnerUser, isLoggedIn, getBarbershopId } from '../services/userContext';
import { logoutUser } from '../services/authService';
import BarberHeader from '../components/BarberPage/BarberHeader';
import BarberNavbar from '../components/BarberPage/BarberNavbar';
import Styles from './CSS/MeusAgendamentos.module.css';

const today = () => new Date().toLocaleDateString('en-CA');

const AgendaBarbeariaPage = () => {
    const navigate = useNavigate();
    const isOwner = isOwnerUser();
    const barbershopId = getBarbershopId();
    const userName = localStorage.getItem('userName') || 'Profissional';

    const [selectedDate, setSelectedDate] = useState(today());
    const [appointments, setAppointments] = useState([]);
    const [loading, setLoading] = useState(false);
    const [activeFilter, setActiveFilter] = useState('ALL');
    const [currentPage, setCurrentPage] = useState(1);

    useEffect(() => {
        if (!isLoggedIn() || !isOwner) {
            navigate('/');
        }
    }, [navigate, isOwner]);

    const carregarAgenda = useCallback(async () => {
        if (!barbershopId || !selectedDate) return;
        setLoading(true);
        try {
            const data = await getBarbershopSchedule(barbershopId, selectedDate);
            setAppointments(data);
            setCurrentPage(1);
        } catch {
            toast.error('Não foi possível carregar a agenda da barbearia.');
        } finally {
            setLoading(false);
        }
    }, [barbershopId, selectedDate]);

    useEffect(() => {
        carregarAgenda();
    }, [carregarAgenda]);

    const shiftDate = (days) => {
        const d = new Date(selectedDate + 'T00:00:00');
        d.setDate(d.getDate() + days);
        setSelectedDate(d.toLocaleDateString('en-CA'));
    };

    const translateStatus = (status) => {
        const map = {
            SCHEDULED: 'Agendado',
            WALK_IN: 'Encaixe',
            CANCELLED: 'Cancelado',
            COMPLETED: 'Concluído',
        };
        return map[status] || status;
    };

    const getStatusClass = (status) => {
        if (status === 'SCHEDULED') return Styles.statusScheduled;
        if (status === 'CANCELLED') return Styles.statusCancelled;
        if (status === 'COMPLETED') return Styles.statusCompleted;
        return '';
    };

    const formatHora = (isoString) => {
        const d = new Date(isoString);
        return d.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
    };

    const filterItems = [
        { key: 'ALL',       label: 'Todos' },
        { key: 'SCHEDULED', label: 'Agendados' },
        { key: 'WALK_IN',   label: 'Encaixe' },
        { key: 'COMPLETED', label: 'Concluídos' },
        { key: 'CANCELLED', label: 'Cancelados' },
    ];

    const filtered = appointments
        .filter((a) => activeFilter === 'ALL' || a.status === activeFilter)
        .sort((a, b) => new Date(a.startTime) - new Date(b.startTime));

    const itemsPerPage = 10;
    const totalPages = Math.max(1, Math.ceil(filtered.length / itemsPerPage));
    const currentPageSafe = Math.min(currentPage, totalPages);
    const paginated = filtered.slice(
        (currentPageSafe - 1) * itemsPerPage,
        currentPageSafe * itemsPerPage
    );

    const handleBarberTabChange = (tab) => {
        if (tab === 'agenda-equipe' || tab === 'agenda-barbearia') return;
        if (tab === 'home') { navigate('/barberHome'); return; }
        if (tab === 'agenda') { navigate('/meus-agendamentos'); return; }
        if (tab === 'servicos') { navigate('/barberHome/servicos'); return; }
        if (tab === 'estoque') { navigate('/barberHome/estoque'); return; }
        if (tab === 'perfil') { navigate('/barberHome/perfil'); return; }
        if (tab === 'time') { navigate('/barberHome/time'); return; }
        if (tab === 'dashboards') { navigate('/barberHome/dashboard'); return; }
        if (tab === 'novo-agendamento') { navigate('/barberHome/novo-agendamento'); return; }
        navigate('/barberHome', { state: { activeTab: tab } });
    };

    const handleBarberLogout = () => {
        logoutUser();
        navigate('/');
    };

    return (
        <div className={Styles.container}>
            <div className={Styles.content}>
                <BarberHeader
                    barber={{ name: userName }}
                    onLogout={handleBarberLogout}
                    activeTab="agenda-equipe"
                    onTabChange={handleBarberTabChange}
                    isOwner={isOwner}
                    barbershopId={barbershopId}
                />

                <section className={Styles.heroBlock}>
                    <p className={Styles.kicker}>PAINEL DO OWNER</p>
                    <h1 className={Styles.title}>Agenda da Equipe</h1>
                    <p className={Styles.subtitle}>Visualize todos os atendimentos da sua equipe no dia selecionado. O encaixe continua sendo individual de cada barbeiro.</p>
                </section>

                {/* Navegação por data */}
                <div className={Styles.filtersRow} style={{ alignItems: 'center', gap: '0.5rem' }}>
                    <button className={Styles.filterButton} onClick={() => shiftDate(-1)} aria-label="Dia anterior">
                        <FiChevronLeft />
                    </button>
                    <input
                        type="date"
                        value={selectedDate}
                        onChange={(e) => setSelectedDate(e.target.value)}
                        className={Styles.filterButton}
                        style={{ cursor: 'pointer' }}
                    />
                    <button className={Styles.filterButton} onClick={() => shiftDate(1)} aria-label="Próximo dia">
                        <FiChevronRight />
                    </button>
                    <button className={Styles.filterButton} onClick={carregarAgenda} aria-label="Atualizar">
                        <FiRefreshCw />
                    </button>
                    <button className={Styles.filterButton} onClick={() => setSelectedDate(today())}>
                        Hoje
                    </button>
                </div>

                {/* Filtros de status */}
                <div className={Styles.filtersRow}>
                    {filterItems.map((f) => (
                        <button
                            key={f.key}
                            className={activeFilter === f.key ? Styles.filterButtonActive : Styles.filterButton}
                            onClick={() => { setActiveFilter(f.key); setCurrentPage(1); }}
                        >
                            {f.label}
                        </button>
                    ))}
                </div>

                {loading ? (
                    <div className={Styles.loadingState}>Carregando agenda...</div>
                ) : filtered.length === 0 ? (
                    <div className={Styles.empty}>
                        <h3>Nenhum atendimento encontrado para este dia.</h3>
                        <p>Tente outro dia ou verifique os filtros.</p>
                    </div>
                ) : (
                    <div>
                        <div className={Styles.list}>
                            {paginated.map((app) => (
                                <div key={app.id} className={Styles.card}>
                                    <div className={Styles.info}>
                                        <span className={Styles.datePill}>
                                            <FiClock />
                                            {formatHora(app.startTime)} — {formatHora(app.endTime)}
                                        </span>
                                        <span className={Styles.mainInfo}>
                                            Barbeiro: <strong>{app.barberName}</strong>
                                        </span>
                                        <span className={Styles.mainInfo}>
                                            Cliente: {app.customerName}
                                        </span>
                                        <span className={Styles.details}>
                                            {app.activityNames ? app.activityNames.join(', ') : 'Serviço'}
                                        </span>
                                        <span className={`${Styles.statusChip} ${getStatusClass(app.status)}`}>
                                            {app.status === 'SCHEDULED' && <FiCalendar />}
                                            {app.status === 'COMPLETED' && <FiCheckCircle />}
                                            {app.status === 'CANCELLED' && <FiXCircle />}
                                            {app.status === 'WALK_IN' && <FiScissors />}
                                            {translateStatus(app.status)}
                                        </span>
                                    </div>
                                </div>
                            ))}
                        </div>

                        {totalPages > 1 && (
                            <div className={Styles.paginationRow}>
                                <button
                                    className={Styles.paginationButton}
                                    onClick={() => setCurrentPage((p) => Math.max(p - 1, 1))}
                                    disabled={currentPageSafe === 1}
                                >
                                    Anterior
                                </button>
                                <div className={Styles.paginationNumbers}>
                                    {Array.from({ length: totalPages }, (_, i) => i + 1).map((p) => (
                                        <button
                                            key={p}
                                            className={p === currentPageSafe ? Styles.pageNumberActive : Styles.pageNumber}
                                            onClick={() => setCurrentPage(p)}
                                        >
                                            {p}
                                        </button>
                                    ))}
                                </div>
                                <button
                                    className={Styles.paginationButton}
                                    onClick={() => setCurrentPage((p) => Math.min(p + 1, totalPages))}
                                    disabled={currentPageSafe === totalPages}
                                >
                                    Próxima
                                </button>
                            </div>
                        )}
                    </div>
                )}
            </div>

            <BarberNavbar
                activeTab="agenda-equipe"
                onTabChange={handleBarberTabChange}
                isOwner={isOwner}
                barbershopId={barbershopId}
            />
        </div>
    );
};

export default AgendaBarbeariaPage;
