import React, { useCallback, useEffect, useMemo, useState, startTransition } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { FiBarChart2, FiCalendar, FiCheckCircle, FiChevronLeft, FiChevronRight, FiClock, FiList, FiRefreshCw, FiScissors, FiStar, FiUsers, FiXCircle } from 'react-icons/fi';
import { toast } from 'react-toastify';
import Styles from './CSS/MeusAgendamentos.module.css';
import {
    getMyAppointments,
    cancelAppointment,
    concludeAppointment,
    rescheduleAppointment,
    getBarbershopSchedule,
} from '../services/appointmentService';
import { createBarbershopReview, hasReviewedBarbershop } from '../services/barbershopService';
import BarberHeader from '../components/BarberPage/BarberHeader';
import BarberNavbar from '../components/BarberPage/BarberNavbar';
import CustomerHeader from '../components/HomePage/CustomerHeader';
import CustomerNavbar from '../components/HomePage/CustomerNavbar';
import { logoutUser } from '../services/authService';
import { isCustomer as checkIsCustomer, isOwnerUser, isLoggedIn, getBarbershopId } from '../services/userContext';
import {
    isOfflineTransactionalError,
    getOfflineTransactionalMessage,
} from '../services/offlineTransactionalService';
import RescheduleModal from '../components/RescheduleModal/RescheduleModal';

const MeusAgendamentosPage = () => {
    const navigate = useNavigate();
    const location = useLocation();
    const [appointments, setAppointments] = useState([]);
    const [loading, setLoading] = useState(true);
    const [initialized, setInitialized] = useState(false);
    const [activeFilter, setActiveFilter] = useState('ALL');
    const [agendaView, setAgendaView] = useState('mine');
    const [teamDate, setTeamDate] = useState(new Date().toLocaleDateString('en-CA'));
    const [currentPage, setCurrentPage] = useState(1);
    const [cancelingAppointmentId, setCancelingAppointmentId] = useState(null);
    const [isCancelModalOpen, setIsCancelModalOpen] = useState(false);
    const [isSubmittingCancel, setIsSubmittingCancel] = useState(false);
    const [concludingAppointmentId, setConcludingAppointmentId] = useState(null);
    const [isConcludeModalOpen, setIsConcludeModalOpen] = useState(false);
    const [isSubmittingConclude, setIsSubmittingConclude] = useState(false);
    const [reschedulingAppointmentId, setReschedulingAppointmentId] = useState(null);
    const [reschedulingAppointment, setReschedulingAppointment] = useState(null);
    const [isRescheduleModalOpen, setIsRescheduleModalOpen] = useState(false);
    const [isSubmittingReschedule, setIsSubmittingReschedule] = useState(false);
    const [reviewingAppointment, setReviewingAppointment] = useState(null);
    const [isReviewModalOpen, setIsReviewModalOpen] = useState(false);
    const [isSubmittingReview, setIsSubmittingReview] = useState(false);
    const [reviewRating, setReviewRating] = useState(5);
    const [reviewComment, setReviewComment] = useState('');
    const [offlineTransactionalNotice, setOfflineTransactionalNotice] = useState('');
    const [viewMode, setViewMode] = useState('list'); // 'list' | 'timeline'
    const [dateFilter, setDateFilter] = useState(new Date().toLocaleDateString('en-CA'));
    const [rangeMode, setRangeMode] = useState('week'); // 'week' | 'month' — compartilhado entre mine e team
    const [reviewHover, setReviewHover] = useState(0);

    // Determina o papel com base na chave correta do localStorage ('userRole')
    const isCustomer = checkIsCustomer();
    const isOwner = isOwnerUser();
    const barbershopId = getBarbershopId();
    const userName = localStorage.getItem('userName') || (isCustomer ? 'Cliente' : 'Profissional');

    // Guard: redireciona para login se não estiver logado
    useEffect(() => {
        if (!isLoggedIn()) {
            navigate('/');
        }
    }, [navigate]);

    useEffect(() => {
        if (isCustomer || !isOwner) {
            setAgendaView('mine');
            return;
        }

        const params = new URLSearchParams(location.search);
        setAgendaView(params.get('view') === 'team' ? 'team' : 'mine');
    }, [location.search, isCustomer, isOwner]);

    const carregarAgendamentos = useCallback(async () => {
        setOfflineTransactionalNotice('');
        // Mostrar spinner apenas na primeira carga (anti-flicker)
        if (!initialized) setLoading(true);
        try {
            let data = [];

            if (!isCustomer && isOwner && agendaView === 'team') {
                data = await getBarbershopSchedule(barbershopId, teamDate);
            } else {
                data = await getMyAppointments();
            }

            const sorted = data.sort((a, b) => new Date(b.startTime) - new Date(a.startTime));
            if (isCustomer) {
                const completedShopIds = [...new Set(
                    sorted
                        .filter((item) => item.status === 'COMPLETED' && item.barbershopId)
                        .map((item) => item.barbershopId)
                )];

                const reviewEntries = await Promise.all(
                    completedShopIds.map(async (shopId) => {
                        try {
                            return [shopId, await hasReviewedBarbershop(shopId)];
                        } catch (error) {
                            console.warn('Nao foi possivel consultar avaliacao da barbearia:', shopId, error);
                            return [shopId, false];
                        }
                    })
                );
                const reviewedByShop = Object.fromEntries(reviewEntries);

                startTransition(() => {
                    setAppointments(sorted.map((item) => ({
                        ...item,
                        hasReviewed: Boolean(reviewedByShop[item.barbershopId]),
                    })));
                    if (!initialized) setInitialized(true);
                });
                return;
            }

            startTransition(() => {
                setAppointments(sorted);
                if (!initialized) setInitialized(true);
            });
        } catch (error) {
            console.error("Erro ao buscar agendamentos:", error);
            if (isOfflineTransactionalError(error)) {
                setOfflineTransactionalNotice(getOfflineTransactionalMessage(error));
            } else {
                toast.error('Nao foi possivel carregar seus agendamentos.');
            }
            startTransition(() => setAppointments([]));
        } finally {
            setLoading(false);
        }
    }, [agendaView, barbershopId, isCustomer, isOwner, teamDate, initialized]);

    useEffect(() => {
        carregarAgendamentos();
    }, [carregarAgendamentos]);

    // Ao entrar na timeline, garante modo 'semana' para o filtro de data
    useEffect(() => {
        if (viewMode === 'timeline') {
            setRangeMode('week');
        }
    }, [viewMode]);

    useEffect(() => {
        setCurrentPage(1);
    }, [activeFilter]);

    const todayStr = useMemo(() => new Date().toLocaleDateString('en-CA'), []);

    const stats = useMemo(() => ({
        today: appointments.filter(a => a.startTime?.slice(0, 10) === todayStr).length,
        active: appointments.filter(a => ['SCHEDULED', 'WALK_IN', 'CONFIRMED'].includes(a.status)).length,
        completed: appointments.filter(a => a.status === 'COMPLETED').length,
        cancelled: appointments.filter(a => a.status === 'CANCELLED').length,
        walkIn: appointments.filter(a => a.status === 'WALK_IN').length,
        pending: appointments.filter(a => ['PAYMENT_PENDING', 'EXPIRED'].includes(a.status)).length,
    }), [appointments, todayStr]);

    const getDurationLabel = (app) => {
        if (!app.startTime || !app.endTime) return null;
        const mins = Math.round((new Date(app.endTime) - new Date(app.startTime)) / 60000);
        if (mins <= 0) return null;
        return mins >= 60
            ? `${Math.floor(mins / 60)}h${mins % 60 > 0 ? `${mins % 60}min` : ''}`
            : `${mins}min`;
    };

    const handleOpenCancelModal = (id) => {
        setCancelingAppointmentId(id);
        setIsCancelModalOpen(true);
    };

    const handleCloseCancelModal = () => {
        if (isSubmittingCancel) return;
        setIsCancelModalOpen(false);
        setCancelingAppointmentId(null);
    };

    const handleConfirmCancel = async () => {
        if (!cancelingAppointmentId) return;

        try {
            setIsSubmittingCancel(true);
            await cancelAppointment(cancelingAppointmentId);
            setIsCancelModalOpen(false);
            setCancelingAppointmentId(null);
            await carregarAgendamentos();
            toast.success('Agendamento cancelado com sucesso.');
        } catch (error) {
            const message = error?.response?.data?.message || 'Erro ao cancelar. Tente novamente.';
            toast.error(message);
        } finally {
            setIsSubmittingCancel(false);
        }
    };

    const handleOpenConcludeModal = (id) => {
        setConcludingAppointmentId(id);
        setIsConcludeModalOpen(true);
    };

    const handleCloseConcludeModal = () => {
        if (isSubmittingConclude) return;
        setIsConcludeModalOpen(false);
        setConcludingAppointmentId(null);
    };

    const handleConfirmConclude = async () => {
        if (!concludingAppointmentId) return;

        try {
            setIsSubmittingConclude(true);
            await concludeAppointment(concludingAppointmentId);
            setIsConcludeModalOpen(false);
            setConcludingAppointmentId(null);
            await carregarAgendamentos();
            toast.success('Agendamento concluido com sucesso.');
        } catch (error) {
            const message = error?.response?.data?.message || 'Erro ao concluir. Tente novamente.';
            toast.error(message);
        } finally {
            setIsSubmittingConclude(false);
        }
    };

    const handleOpenRescheduleModal = (appointment) => {
        setReschedulingAppointmentId(appointment.id);
        setReschedulingAppointment(appointment);
        setIsRescheduleModalOpen(true);
    };

    const handleCloseRescheduleModal = () => {
        if (isSubmittingReschedule) return;
        setIsRescheduleModalOpen(false);
        setReschedulingAppointmentId(null);
        setReschedulingAppointment(null);
    };

    const handleConfirmReschedule = async (newStartTime, newBarberId) => {
        if (!reschedulingAppointmentId) return;

        try {
            setIsSubmittingReschedule(true);
            await rescheduleAppointment(reschedulingAppointmentId, newStartTime, newBarberId);
            setIsRescheduleModalOpen(false);
            setReschedulingAppointmentId(null);
            setReschedulingAppointment(null);
            await carregarAgendamentos();

            const dataFormatada = new Date(newStartTime).toLocaleString('pt-BR', {
                weekday: 'long', day: '2-digit', month: 'long',
                hour: '2-digit', minute: '2-digit',
            });
            toast.success(`Reagendado para ${dataFormatada}. Até lá! ✂️`, { autoClose: 5000 });
        } catch (error) {
            const message = error?.response?.data?.message || 'Erro ao reagendar. Tente novamente.';
            toast.error(message);
        } finally {
            setIsSubmittingReschedule(false);
        }
    };

    const handleOpenReviewModal = (appointment) => {
        setReviewingAppointment(appointment);
        setReviewRating(5);
        setReviewComment('');
        setIsReviewModalOpen(true);
    };

    const handleCloseReviewModal = () => {
        if (isSubmittingReview) return;
        setIsReviewModalOpen(false);
        setReviewingAppointment(null);
    };

    const handleSubmitReview = async () => {
        if (!reviewingAppointment?.barbershopId) {
            toast.warn('Nao foi possivel identificar a barbearia deste atendimento.');
            return;
        }

        try {
            setIsSubmittingReview(true);
            await createBarbershopReview(reviewingAppointment.barbershopId, {
                rating: Number(reviewRating),
                comment: reviewComment.trim() || null,
            });

            setIsReviewModalOpen(false);
            setReviewingAppointment(null);
            setAppointments((current) => current.map((appointment) => (
                appointment.barbershopId === reviewingAppointment.barbershopId
                    ? { ...appointment, hasReviewed: true }
                    : appointment
            )));
            toast.success('Avaliacao enviada com sucesso!');
        } catch (error) {
            if (error?.response?.status === 409) {
                setAppointments((current) => current.map((appointment) => (
                    appointment.barbershopId === reviewingAppointment.barbershopId
                        ? { ...appointment, hasReviewed: true }
                        : appointment
                )));
                toast.warn('Voce ja avaliou esta barbearia.');
            } else {
                toast.error('Nao foi possivel enviar sua avaliacao. Tente novamente.');
            }
        } finally {
            setIsSubmittingReview(false);
        }
    };

    // Função para formatar data bonita (Ex: 28/11 às 14:00)
    const formatData = (isoString) => {
        const date = new Date(isoString);
        return date.toLocaleString('pt-BR', { 
            day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' 
        });
    };

    // Função para traduzir status
    const translateStatus = (status) => {
        const map = {
            'SCHEDULED': 'Agendado',
            'PAYMENT_PENDING': 'Pagamento pendente',
            'EXPIRED': 'Expirado',
            'CONFIRMED': 'Confirmado',
            'WALK_IN': 'Encaixe',
            'CANCELLED': 'Cancelado',
            'COMPLETED': 'Concluido',
            'NO_SHOW': 'Nao compareceu'
        };
        return map[status] || status;
    };

    const toServiceSummary = (appointment) => {
        if (Array.isArray(appointment?.activityNames) && appointment.activityNames.length > 0) {
            return appointment.activityNames.join(', ');
        }

        if (Array.isArray(appointment?.activities) && appointment.activities.length > 0) {
            const names = appointment.activities
                .map((item) => item?.activityName)
                .filter(Boolean);

            if (names.length > 0) {
                return names.join(', ');
            }
        }

        return 'Servico';
    };

    const sortedAppointments = [...appointments].sort((a, b) => {
        // Na visão de dia do barbeiro, ordenar por hora crescente
        if (!isCustomer && agendaView === 'mine' && dateFilter) {
            return new Date(a.startTime) - new Date(b.startTime);
        }

        if (activeFilter === 'ALL') {
            const aCancelled = a.status === 'CANCELLED';
            const bCancelled = b.status === 'CANCELLED';

            if (aCancelled !== bCancelled) {
                return aCancelled ? 1 : -1;
            }
        }

        return new Date(b.startTime) - new Date(a.startTime);
    });

    const filteredAppointments = sortedAppointments.filter((app) => {
        // Filtro de status — CONFIRMED aparece junto com SCHEDULED em "Agendados"
        if (activeFilter === 'SCHEDULED') {
            if (!['SCHEDULED', 'CONFIRMED'].includes(app.status)) return false;
        } else if (activeFilter !== 'ALL' && app.status !== activeFilter) {
            return false;
        }

        // Filtro de data para barbeiro na visão "mine"
        // Quando filtrando por status específico (ex: CANCELLED, COMPLETED), ignora filtro de data
        // para mostrar todo o histórico daquele status, não só o do dia atual.
        if (!isCustomer && agendaView === 'mine' && activeFilter === 'ALL') {
            const appDate = app.startTime?.slice(0, 10);
            if (!appDate) return false;
            if (rangeMode === 'day') {
                return appDate === dateFilter;
            }
            if (rangeMode === 'week') {
                const anchor = new Date(dateFilter + 'T00:00:00');
                const weekStart = new Date(anchor);
                weekStart.setDate(anchor.getDate() - anchor.getDay() + 1);
                const weekEnd = new Date(weekStart);
                weekEnd.setDate(weekStart.getDate() + 6);
                const ws = weekStart.toLocaleDateString('en-CA');
                const we = weekEnd.toLocaleDateString('en-CA');
                return appDate >= ws && appDate <= we;
            }
            if (rangeMode === 'month') {
                return appDate.slice(0, 7) === dateFilter.slice(0, 7);
            }
        }

        // Filtro de data para team view (mesma lógica usando teamDate como âncora)
        if (!isCustomer && agendaView === 'team' && activeFilter === 'ALL') {
            const appDate = app.startTime?.slice(0, 10);
            if (!appDate) return false;
            if (rangeMode === 'day') {
                return appDate === teamDate;
            }
            if (rangeMode === 'week') {
                const anchor = new Date(teamDate + 'T00:00:00');
                const weekStart = new Date(anchor);
                weekStart.setDate(anchor.getDate() - anchor.getDay() + 1);
                const weekEnd = new Date(weekStart);
                weekEnd.setDate(weekStart.getDate() + 6);
                const ws = weekStart.toLocaleDateString('en-CA');
                const we = weekEnd.toLocaleDateString('en-CA');
                return appDate >= ws && appDate <= we;
            }
            if (rangeMode === 'month') {
                return appDate.slice(0, 7) === teamDate.slice(0, 7);
            }
        }
        return true;
    });

    const itemsPerPage = 10;
    const totalPages = Math.max(1, Math.ceil(filteredAppointments.length / itemsPerPage));
    const currentPageSafe = Math.min(currentPage, totalPages);
    const paginatedAppointments = filteredAppointments.slice(
        (currentPageSafe - 1) * itemsPerPage,
        currentPageSafe * itemsPerPage
    );

    const getStatusClass = (status) => {
        if (status === 'SCHEDULED') return Styles.statusScheduled;
        if (status === 'PAYMENT_PENDING') return Styles.statusPending;
        if (status === 'EXPIRED') return Styles.statusExpired;
        if (status === 'CANCELLED') return Styles.statusCancelled;
        if (status === 'COMPLETED') return Styles.statusCompleted;
        return '';
    };

    const canInteractWithAppointment = (status) => {
        return ['SCHEDULED', 'CONFIRMED', 'WALK_IN', 'IN_PROGRESS'].includes(status);
    };

    const handleBarberTabChange = (tab) => {
        if (tab === 'agenda') {
            setAgendaView('mine');
            navigate('/meus-agendamentos');
            return;
        }

        if (tab === 'agenda-equipe') {
            if (isOwner) {
                setAgendaView('team');
                navigate('/meus-agendamentos?view=team');
            }
            return;
        }

        if (tab === 'home') {
            navigate('/barberHome');
            return;
        }

        if (tab === 'servicos') {
            navigate('/barberHome/servicos');
            return;
        }

        if (tab === 'estoque') {
            navigate('/barberHome/estoque');
            return;
        }

        if (tab === 'perfil') {
            navigate('/barberHome/perfil');
            return;
        }

        if (tab === 'time') {
            navigate('/barberHome/time');
            return;
        }

        if (tab === 'dashboards') {
            navigate('/barberHome/dashboard');
            return;
        }

        if (tab === 'novo-agendamento') {
            navigate('/barberHome/novo-agendamento');
            return;
        }

        if (tab === 'indisponibilidade') {
            navigate('/barber/indisponibilidade');
            return;
        }

        navigate('/barberHome', { state: { activeTab: tab } });
    };

    const handleBarberLogout = () => {
        logoutUser();
        navigate('/');
    };

    const handleCustomerLogout = () => {
        logoutUser();
        navigate('/');
    };

    const shiftDateFilter = (days) => {
        const d = new Date(dateFilter + 'T00:00:00');
        if (rangeMode === 'week') {
            d.setDate(d.getDate() + days * 7);
        } else if (rangeMode === 'month') {
            d.setMonth(d.getMonth() + days);
        } else {
            d.setDate(d.getDate() + days);
        }
        setDateFilter(d.toLocaleDateString('en-CA'));
    };

    const shiftTeamDate = (direction) => {
        const d = new Date(teamDate + 'T00:00:00');
        if (rangeMode === 'week') {
            d.setDate(d.getDate() + direction * 7);
        } else if (rangeMode === 'month') {
            d.setMonth(d.getMonth() + direction);
        } else {
            d.setDate(d.getDate() + direction);
        }
        setTeamDate(d.toLocaleDateString('en-CA'));
    };

    const formatDateDisplay = (dateStr) => {
        const d = new Date(dateStr + 'T00:00:00');
        if (rangeMode === 'month') {
            return d.toLocaleDateString('pt-BR', { month: 'long', year: 'numeric' });
        }
        if (rangeMode === 'week') {
            const start = new Date(d);
            start.setDate(d.getDate() - d.getDay() + 1); // segunda
            const end = new Date(start);
            end.setDate(start.getDate() + 6); // domingo
            const fmt = (x) => x.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit' });
            return `${fmt(start)} – ${fmt(end)}`;
        }
        // Sempre retornar data formatada por extenso (sem 'Hoje/Amanhã/ontem') conforme SDD
        return d.toLocaleDateString('pt-BR', { weekday: 'long', day: '2-digit', month: 'long' });
    };

    const renderTimeline = () => {
        const activeDate = agendaView === 'team' ? teamDate : dateFilter;

        // Filtra por data E por status (igual à lista)
        const dayAppointments = appointments.filter((a) => {
            if (a.startTime?.slice(0, 10) !== activeDate) return false;
            if (activeFilter === 'SCHEDULED') return ['SCHEDULED', 'CONFIRMED'].includes(a.status);
            if (activeFilter !== 'ALL') return a.status === activeFilter;
            return true;
        });

        const PX_PER_MIN = 80 / 60;
        const START_HOUR = 7;
        const END_HOUR = 21;
        const totalHeight = (END_HOUR - START_HOUR) * 80;

        const getTop = (iso) => {
            const d = new Date(iso);
            return Math.max(0, ((d.getHours() - START_HOUR) * 60 + d.getMinutes()) * PX_PER_MIN);
        };

        const getHeight = (start, end) =>
            Math.max(36, Math.round((new Date(end) - new Date(start)) / 60000) * PX_PER_MIN);

        const borderByStatus = {
            SCHEDULED: '#c19006', CONFIRMED: '#10b981', WALK_IN: '#7c3aed',
            PAYMENT_PENDING: '#f97316', COMPLETED: '#10b981', CANCELLED: '#555',
        };
        const bgByStatus = {
            SCHEDULED: 'rgba(193,144,6,0.14)', CONFIRMED: 'rgba(16,185,129,0.12)',
            WALK_IN: 'rgba(124,58,237,0.14)', PAYMENT_PENDING: 'rgba(249,115,22,0.14)',
            COMPLETED: 'rgba(16,185,129,0.09)', CANCELLED: 'rgba(80,80,80,0.1)',
        };

        const nowTop = (() => {
            if (activeDate !== todayStr) return null;
            const now = new Date();
            const t = ((now.getHours() - START_HOUR) * 60 + now.getMinutes()) * PX_PER_MIN;
            return t >= 0 && t <= totalHeight ? t : null;
        })();

        const hourLabels = Array.from({ length: END_HOUR - START_HOUR + 1 }, (_, i) => ({
            label: `${String(START_HOUR + i).padStart(2, '0')}:00`,
            top: i * 80,
        }));

        const renderAppBlock = (app) => (
            <div
                key={app.id}
                className={Styles.timelineBlock}
                style={{
                    top: getTop(app.startTime),
                    height: getHeight(app.startTime, app.endTime),
                    background: bgByStatus[app.status] || 'rgba(193,144,6,0.14)',
                    borderLeft: `3px solid ${borderByStatus[app.status] || '#c19006'}`,
                }}
            >
                <span className={Styles.tlTime}>
                    {new Date(app.startTime).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })}
                </span>
                <span className={Styles.tlName}>{app.customerName || 'Cliente'}</span>
                <span className={Styles.tlService}>{toServiceSummary(app)}</span>
            </div>
        );

        if (dayAppointments.length === 0) {
            return (
                <div className={`${Styles.empty} ca-state ca-state--empty`} style={{ marginTop: '2rem' }}>
                    <h3>Nenhum atendimento em {new Date(activeDate + 'T00:00:00').toLocaleDateString('pt-BR', { weekday: 'long', day: '2-digit', month: 'long' })}.</h3>
                    <p>Navegue para outro dia usando as setas de data acima.</p>
                </div>
            );
        }

        if (agendaView === 'mine') {
            return (
                <div className={Styles.timelineWrapper}>
                    <div className={Styles.timelineScroll}>
                        <div className={Styles.timeLabels}>
                            {hourLabels.map(h => (
                                <div key={h.label} className={Styles.timeLabel} style={{ top: h.top }}>{h.label}</div>
                            ))}
                        </div>
                        <div className={Styles.timelineGrid} style={{ height: totalHeight }}>
                            {hourLabels.map(h => (
                                <div key={h.label} className={Styles.hourLine} style={{ top: h.top }} />
                            ))}
                            {dayAppointments.map(app => renderAppBlock(app))}
                            {nowTop !== null && <div className={Styles.nowLine} style={{ top: nowTop }} />}
                        </div>
                    </div>
                </div>
            );
        }

        // Team view: coluna por barbeiro
        const barberMap = new Map();
        dayAppointments.forEach(a => {
            const key = a.barberId || a.barberName || 'unknown';
            if (!barberMap.has(key)) barberMap.set(key, { key, name: a.barberName || 'Barbeiro', appointments: [] });
            barberMap.get(key).appointments.push(a);
        });
        const barbers = [...barberMap.values()];

        return (
            <div className={Styles.timelineWrapper}>
                <div className={Styles.timelineTeamScroll}>
                    <div className={Styles.timeLabels}>
                        {hourLabels.map(h => (
                            <div key={h.label} className={Styles.timeLabel} style={{ top: h.top }}>{h.label}</div>
                        ))}
                    </div>
                    <div className={Styles.timelineTeamColumns}>
                        {barbers.map(barber => (
                            <div key={barber.key} className={Styles.timelineColumn}>
                                <div className={Styles.tlColHeader}>
                                    <span className={Styles.tlColAvatar}>
                                        {(barber.name || 'B').charAt(0).toUpperCase()}
                                    </span>
                                    {barber.name}
                                </div>
                                <div className={Styles.timelineGrid} style={{ height: totalHeight }}>
                                    {hourLabels.map(h => (
                                        <div key={h.label} className={Styles.hourLine} style={{ top: h.top }} />
                                    ))}
                                    {barber.appointments.map(app => renderAppBlock(app))}
                                    {nowTop !== null && <div className={Styles.nowLine} style={{ top: nowTop }} />}
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            </div>
        );
    };

    return (
        <div className={Styles.container}>
            <div className={Styles.content}>

                {!isCustomer && (
                    <BarberHeader
                        barber={{ name: userName }}
                        onLogout={handleBarberLogout}
                        activeTab="agenda"
                        onTabChange={handleBarberTabChange}
                        isOwner={isOwner}
                        barbershopId={barbershopId}
                    />
                )}

                {isCustomer && (
                    <CustomerHeader activeTab="agendamentos" onLogout={handleCustomerLogout} />
                )}
                {isCustomer && (
                    <CustomerNavbar activeTab="agendamentos" onLogout={handleCustomerLogout} />
                )}

                <section className={Styles.heroBlock}>
                    <p className={Styles.kicker}>{isCustomer ? 'PAINEL DE AGENDAMENTOS' : (agendaView === 'team' ? 'AGENDA DA EQUIPE' : 'MINHA AGENDA')}</p>
                    <h1 className={Styles.title}>
                        {isCustomer
                            ? 'Acompanhe seus proximos cortes'
                            : (agendaView === 'team' ? 'Visualize os atendimentos da equipe' : 'Organize seus atendimentos')}
                    </h1>
                    <p className={Styles.subtitle}>
                        {agendaView === 'team'
                            ? 'Visão consolidada dos atendimentos da barbearia no dia selecionado.'
                            : 'Visualize status, horario e servicos de cada agendamento em um fluxo mais claro.'}
                    </p>
                </section>

                {offlineTransactionalNotice && (
                    <p className={`${Styles.empty} ca-state ca-state--error`}>
                        {offlineTransactionalNotice}
                    </p>
                )}

                {/* ── Stats bar ── */}
                {!isCustomer && (
                    <div className={Styles.statsBar}>
                        {[
                            { label: 'Hoje', value: stats.today, filter: 'TODAY', icon: <FiCalendar size={13} />, accent: '#c19006' },
                            { label: 'Ativos', value: stats.active, filter: 'SCHEDULED', icon: <FiClock size={13} />, accent: '#3b82f6' },
                            { label: 'Encaixe', value: stats.walkIn, filter: 'WALK_IN', icon: <FiScissors size={13} />, accent: '#7c3aed' },
                            { label: 'Pendentes', value: stats.pending, filter: 'PAYMENT_PENDING', icon: <FiClock size={13} />, accent: '#f97316' },
                            { label: 'Concluídos', value: stats.completed, filter: 'COMPLETED', icon: <FiCheckCircle size={13} />, accent: '#10b981' },
                            { label: 'Cancelados', value: stats.cancelled, filter: 'CANCELLED', icon: <FiXCircle size={13} />, accent: '#ef4444' },
                        ].map(item => {
                            const isToday = item.filter === 'TODAY';
                            const isActive = isToday
                                ? activeFilter === 'ALL' && (agendaView === 'team' ? teamDate : dateFilter) === todayStr
                                : activeFilter === item.filter;
                            return (
                                <button
                                    key={item.label}
                                    className={`${Styles.statCard} ${isActive ? Styles.statCardActive : ''}`}
                                    style={{ '--accent': item.accent }}
                                    onClick={() => {
                                        if (isToday) {
                                            setActiveFilter('ALL');
                                            setDateFilter(todayStr);
                                            setTeamDate(todayStr);
                                        } else {
                                            setActiveFilter(activeFilter === item.filter ? 'ALL' : item.filter);
                                        }
                                    }}
                                    type="button"
                                    title={isToday
                                        ? 'Ver agendamentos de hoje'
                                        : (isActive ? `Remover filtro "${item.label}"` : `Filtrar: ${item.label}`)}
                                >
                                    <span className={Styles.statIcon}>{item.icon}</span>
                                    <span className={Styles.statValue}>{item.value}</span>
                                    <span className={Styles.statLabel}>{item.label}</span>
                                </button>
                            );
                        })}
                    </div>
                )}

                {/* Badge indicando filtro de status ativo */}
                {!isCustomer && activeFilter !== 'ALL' && (
                    <button
                        type="button"
                        className={Styles.activeFilterBadge}
                        onClick={() => setActiveFilter('ALL')}
                        title="Clique para remover o filtro"
                    >
                        <FiXCircle size={13} />
                        Filtrando por: <strong>{{
                            SCHEDULED: 'Ativos',
                            WALK_IN: 'Encaixe',
                            PAYMENT_PENDING: 'Pendentes',
                            COMPLETED: 'Concluídos',
                            CANCELLED: 'Cancelados',
                        }[activeFilter] || activeFilter}</strong>
                        &nbsp;— clique para ver todos
                    </button>
                )}

                {/* ── Seletor mine/team + navegação de data ── */}
                {!isCustomer && (
                    <div className={Styles.controlsWrapper}>
                        {/* mine/equipe */}
                        {isOwner && (
                            <div className={Styles.viewToggle}>
                                <button
                                    className={agendaView === 'mine' ? Styles.viewToggleBtnActive : Styles.viewToggleBtn}
                                    onClick={() => { setAgendaView('mine'); navigate('/meus-agendamentos'); }}
                                    type="button"
                                >
                                    <FiScissors size={14} /> Minha Agenda
                                </button>
                                <button
                                    className={agendaView === 'team' ? Styles.viewToggleBtnActive : Styles.viewToggleBtn}
                                    onClick={() => { setAgendaView('team'); navigate('/meus-agendamentos?view=team'); }}
                                    type="button"
                                >
                                    <FiUsers size={14} /> Equipe
                                </button>
                            </div>
                        )}

                        {/* Seletor de período — Semana e Mês */}
                        <div
                            className={`${Styles.viewToggle} ${activeFilter !== 'ALL' ? Styles.viewToggleDisabled : ''}`}
                            title={activeFilter !== 'ALL' ? 'Filtro de data desativado: exibindo todo o histórico do status selecionado' : undefined}
                        >
                            {[{ key: 'week', label: 'Semana' }, { key: 'month', label: 'Mês' }].map(r => (
                                <button
                                    key={r.key}
                                    className={rangeMode === r.key ? Styles.viewToggleBtnActive : Styles.viewToggleBtn}
                                    onClick={() => { setRangeMode(r.key); if (activeFilter !== 'ALL') setActiveFilter('ALL'); }}
                                    type="button"
                                    disabled={activeFilter !== 'ALL'}
                                >
                                    {r.label}
                                </button>
                            ))}
                        </div>

                        {/* ── Linha 2: navegação de data + ações fixas na direita ── */}
                        <div className={`${Styles.dateNavRow} ${activeFilter !== 'ALL' ? Styles.dateNavRowDisabled : ''}`}>
                            {/* Navegação de data — lado esquerdo/centro */}
                            <button
                                className={Styles.dateNavBtn}
                                onClick={() => agendaView === 'team' ? shiftTeamDate(-1) : shiftDateFilter(-1)}
                                aria-label="Período anterior"
                                type="button"
                                disabled={activeFilter !== 'ALL'}
                            >
                                <FiChevronLeft size={18} />
                            </button>
                            <label
                                className={Styles.dateNavLabel}
                                title={activeFilter !== 'ALL'
                                    ? 'Filtro de data inativo — selecione "Todos" no filtro acima para navegar por data'
                                    : 'Clique para escolher data'}
                            >
                                {activeFilter !== 'ALL'
                                    ? 'Todo o histórico'
                                    : formatDateDisplay(agendaView === 'team' ? teamDate : dateFilter)}
                                {activeFilter === 'ALL' && (
                                    <input
                                        type={rangeMode === 'month' ? 'month' : 'date'}
                                        value={rangeMode === 'month'
                                            ? (agendaView === 'team' ? teamDate : dateFilter).slice(0, 7)
                                            : (agendaView === 'team' ? teamDate : dateFilter)}
                                        onChange={(e) => {
                                            if (agendaView === 'team') {
                                                setTeamDate(rangeMode === 'month' ? e.target.value + '-01' : e.target.value);
                                            } else if (rangeMode === 'month') {
                                                setDateFilter(e.target.value + '-01');
                                            } else {
                                                setDateFilter(e.target.value);
                                            }
                                        }}
                                        className={Styles.dateNavInputHidden}
                                        aria-label="Selecionar data"
                                    />
                                )}
                            </label>
                            <button
                                className={Styles.dateNavBtn}
                                onClick={() => agendaView === 'team' ? shiftTeamDate(1) : shiftDateFilter(1)}
                                aria-label="Próximo período"
                                type="button"
                                disabled={activeFilter !== 'ALL'}
                            >
                                <FiChevronRight size={18} />
                            </button>

                            {/* Ações fixas — sempre na extrema direita, não encolhem */}
                            <div className={Styles.dateNavActions}>
                                <button
                                    className={Styles.dateNavRefreshBtn}
                                    onClick={carregarAgendamentos}
                                    aria-label="Atualizar"
                                    type="button"
                                    title="Atualizar agendamentos"
                                >
                                    <FiRefreshCw size={14} />
                                </button>
                                <span className={Styles.dateNavDivider} aria-hidden="true" />
                                <div className={Styles.viewToggle}>
                                    <button
                                        className={viewMode === 'list' ? Styles.viewToggleBtnActive : Styles.viewToggleBtn}
                                        onClick={() => setViewMode('list')}
                                        type="button"
                                        title="Visão lista"
                                    >
                                        <FiList size={14} /> Lista
                                    </button>
                                    <button
                                        className={viewMode === 'timeline' ? Styles.viewToggleBtnActive : Styles.viewToggleBtn}
                                        onClick={() => setViewMode('timeline')}
                                        type="button"
                                        title="Visão timeline"
                                    >
                                        <FiBarChart2 size={14} /> Timeline
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>
                )}

                {/* ── Timeline view ── */}
                {!isCustomer && viewMode === 'timeline' && !loading && renderTimeline()}

                {/* ── Lista view ── */}
                {(isCustomer || viewMode === 'list') && loading && (
                    <div className={`${Styles.loadingState} ca-state ca-state--loading`}>Carregando agendamentos...</div>
                )}

                {(isCustomer || viewMode === 'list') && !loading && filteredAppointments.length === 0 && (
                    <div className={`${Styles.empty} ca-state ca-state--empty`}>
                        <h3>Nenhum agendamento neste filtro.</h3>
                        {isCustomer && <p>Que tal marcar um horário agora?</p>}
                    </div>
                )}

                {(isCustomer || viewMode === 'list') && !loading && filteredAppointments.length > 0 && (
                    <div>
                        <div className={Styles.list}>
                            {paginatedAppointments.map(app => {
                                const durationLabel = getDurationLabel(app);
                                const mainInfoText = isCustomer
                                    ? `Com: ${app.barberName} (${app.barbershopName})`
                                    : agendaView === 'team'
                                        ? `${app.barberName || 'Barbeiro'} → ${app.customerName || 'Cliente'}`
                                        : `Cliente: ${app.customerName}`;
                                return (
                                <div key={app.id} className={Styles.card}>
                                    <div className={Styles.info}>
                                        <span className={Styles.datePill}>
                                            <FiClock />
                                            {formatData(app.startTime)}
                                            {durationLabel && (
                                                <span className={Styles.durationBadge}>{durationLabel}</span>
                                            )}
                                        </span>

                                        <span className={Styles.mainInfo}>{mainInfoText}</span>

                                        <span className={Styles.details}>
                                            {toServiceSummary(app)}
                                        </span>

                                        <span className={`${Styles.statusChip} ${getStatusClass(app.status)}`}>
                                            {(app.status === 'SCHEDULED' || app.status === 'CONFIRMED') && <FiCalendar />}
                                            {(app.status === 'PAYMENT_PENDING' || app.status === 'EXPIRED') && <FiClock />}
                                            {app.status === 'COMPLETED' && <FiCheckCircle />}
                                            {app.status === 'CANCELLED' && <FiXCircle />}
                                            {app.status === 'WALK_IN' && <FiScissors />}
                                            {translateStatus(app.status)}
                                        </span>
                                    </div>

                                    {canInteractWithAppointment(app.status) && (
                                        <div className={Styles.cardActions}>
                                            {isCustomer && ['SCHEDULED', 'CONFIRMED'].includes(app.status) && (() => {
                                                const horasRestantes = (new Date(app.startTime) - new Date()) / 3600000;
                                                if (horasRestantes > 3) {
                                                    return (
                                                        <button
                                                            className={Styles.rescheduleButton}
                                                            onClick={() => handleOpenRescheduleModal(app)}
                                                            disabled={isSubmittingCancel || isSubmittingConclude || isSubmittingReschedule}
                                                        >
                                                            Reagendar
                                                        </button>
                                                    );
                                                }
                                                if (horasRestantes > 0) {
                                                    return (
                                                        <span className={Styles.rescheduleLockedHint} title="Reagendamento não permitido dentro de 3 horas do horário">
                                                            <FiClock size={12} /> Reagendamento indisponível
                                                        </span>
                                                    );
                                                }
                                                return null;
                                            })()}
                                            <button
                                                className={Styles.concludeButton}
                                                onClick={() => handleOpenConcludeModal(app.id)}
                                                disabled={isSubmittingCancel || isSubmittingConclude || isSubmittingReschedule}
                                            >
                                                Concluir
                                            </button>
                                            <button
                                                className={Styles.cancelButton}
                                                onClick={() => handleOpenCancelModal(app.id)}
                                                disabled={isSubmittingCancel || isSubmittingConclude || isSubmittingReschedule}
                                            >
                                                Cancelar
                                            </button>
                                        </div>
                                    )}

                                    {isCustomer && app.status === 'COMPLETED' && !app.hasReviewed && (
                                        <button
                                            className={Styles.reviewButton}
                                            onClick={() => handleOpenReviewModal(app)}
                                        >
                                            Avaliar
                                        </button>
                                    )}
                                </div>
                                );
                            })}
                        </div>

                        {totalPages > 1 && (
                            <div className={Styles.paginationRow}>
                                <button
                                    className={Styles.paginationButton}
                                    onClick={() => setCurrentPage((prev) => Math.max(prev - 1, 1))}
                                    disabled={currentPageSafe === 1}
                                >
                                    Anterior
                                </button>
                                <div className={Styles.paginationNumbers}>
                                    {Array.from({ length: totalPages }, (_, index) => {
                                        const page = index + 1;
                                        return (
                                            <button
                                                key={page}
                                                className={page === currentPageSafe ? Styles.pageNumberActive : Styles.pageNumber}
                                                onClick={() => setCurrentPage(page)}
                                            >
                                                {page}
                                            </button>
                                        );
                                    })}
                                </div>
                                <button
                                    className={Styles.paginationButton}
                                    onClick={() => setCurrentPage((prev) => Math.min(prev + 1, totalPages))}
                                    disabled={currentPageSafe === totalPages}
                                >
                                    Proxima
                                </button>
                            </div>
                        )}
                    </div>
                )}

                {isCancelModalOpen && (
                    <div className={Styles.modalBackdrop} onClick={handleCloseCancelModal}>
                        <div className={Styles.modalCard} onClick={(e) => e.stopPropagation()}>
                            <p className={Styles.modalKicker}>CONFIRMAR CANCELAMENTO</p>
                            <h3 className={Styles.modalTitle}>Deseja cancelar este agendamento?</h3>
                            <p className={Styles.modalSubtitle}>Essa acao altera o status para cancelado e nao pode ser desfeita.</p>

                            <div className={Styles.modalActions}>
                                <button
                                    type="button"
                                    className={Styles.modalSecondaryButton}
                                    onClick={handleCloseCancelModal}
                                    disabled={isSubmittingCancel}
                                >
                                    Voltar
                                </button>
                                <button
                                    type="button"
                                    className={Styles.modalDangerButton}
                                    onClick={handleConfirmCancel}
                                    disabled={isSubmittingCancel}
                                >
                                    {isSubmittingCancel ? 'Cancelando...' : 'Confirmar cancelamento'}
                                </button>
                            </div>
                        </div>
                    </div>
                )}

                {isReviewModalOpen && (
                    <div className={Styles.modalBackdrop} onClick={handleCloseReviewModal}>
                        <div className={Styles.modalCard} onClick={(e) => e.stopPropagation()}>
                            <p className={Styles.modalKicker}>AVALIAR BARBEARIA</p>
                            <h3 className={Styles.modalTitle}>Como foi seu atendimento?</h3>
                            <p className={Styles.modalSubtitle}>Sua opiniao ajuda outros clientes a escolher melhor.</p>

                            <div className={Styles.reviewFormGroup}>
                                <label className={Styles.reviewLabel}>Sua avaliação</label>
                                <div className={Styles.starsRow}>
                                    {[1, 2, 3, 4, 5].map((star) => (
                                        <button
                                            key={star}
                                            type="button"
                                            className={Styles.starBtn}
                                            onClick={() => setReviewRating(star)}
                                            onMouseEnter={() => setReviewHover(star)}
                                            onMouseLeave={() => setReviewHover(0)}
                                            aria-label={`${star} estrela${star > 1 ? 's' : ''}`}
                                        >
                                            <FiStar
                                                size={32}
                                                className={`${Styles.starIcon} ${(reviewHover || reviewRating) >= star ? Styles.starFilled : ''}`}
                                            />
                                        </button>
                                    ))}
                                </div>
                                <p className={Styles.ratingLabel}>
                                    {reviewRating === 1 && '😞 Péssimo'}
                                    {reviewRating === 2 && '😕 Ruim'}
                                    {reviewRating === 3 && '😐 Regular'}
                                    {reviewRating === 4 && '😊 Bom'}
                                    {reviewRating === 5 && '🤩 Excelente!'}
                                </p>
                            </div>

                            <div className={Styles.reviewFormGroup}>
                                <label className={Styles.reviewLabel} htmlFor="review-comment">Comentario (opcional)</label>
                                <textarea
                                    id="review-comment"
                                    className={Styles.reviewTextarea}
                                    value={reviewComment}
                                    onChange={(e) => setReviewComment(e.target.value)}
                                    maxLength={500}
                                    placeholder="Conte como foi sua experiencia"
                                />
                            </div>

                            <div className={Styles.modalActions}>
                                <button
                                    type="button"
                                    className={Styles.modalSecondaryButton}
                                    onClick={handleCloseReviewModal}
                                    disabled={isSubmittingReview}
                                >
                                    Voltar
                                </button>
                                <button
                                    type="button"
                                    className={Styles.modalPrimaryButton}
                                    onClick={handleSubmitReview}
                                    disabled={isSubmittingReview}
                                >
                                    {isSubmittingReview ? 'Enviando...' : 'Enviar avaliacao'}
                                </button>
                            </div>
                        </div>
                    </div>
                )}

                {isConcludeModalOpen && (
                    <div className={Styles.modalBackdrop} onClick={handleCloseConcludeModal}>
                        <div className={Styles.modalCard} onClick={(e) => e.stopPropagation()}>
                            <p className={Styles.modalKicker}>CONFIRMAR CONCLUSAO</p>
                            <h3 className={Styles.modalTitle}>Deseja concluir este agendamento?</h3>
                            <p className={Styles.modalSubtitle}>O status sera alterado para concluido.</p>

                            <div className={Styles.modalActions}>
                                <button
                                    type="button"
                                    className={Styles.modalSecondaryButton}
                                    onClick={handleCloseConcludeModal}
                                    disabled={isSubmittingConclude}
                                >
                                    Voltar
                                </button>
                                <button
                                    type="button"
                                    className={Styles.modalPrimaryButton}
                                    onClick={handleConfirmConclude}
                                    disabled={isSubmittingConclude}
                                >
                                    {isSubmittingConclude ? 'Concluindo...' : 'Confirmar conclusao'}
                                </button>
                            </div>
                        </div>
                    </div>
                )}

                {isRescheduleModalOpen && reschedulingAppointment && (
                    <RescheduleModal
                        appointment={reschedulingAppointment}
                        onClose={handleCloseRescheduleModal}
                        onConfirm={handleConfirmReschedule}
                        isSubmitting={isSubmittingReschedule}
                    />
                )}
            </div>

            {!isCustomer && (
                <BarberNavbar
                    activeTab="agenda"
                    onTabChange={handleBarberTabChange}
                    isOwner={isOwner}
                    barbershopId={barbershopId}
                />
            )}
        </div>
    );
};

export default MeusAgendamentosPage;
