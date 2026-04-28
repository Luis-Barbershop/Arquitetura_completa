import React, { useCallback, useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { FiArrowLeft, FiCalendar, FiCheckCircle, FiClock, FiRefreshCw, FiScissors, FiXCircle } from 'react-icons/fi';
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

const MeusAgendamentosPage = () => {
    const navigate = useNavigate();
    const location = useLocation();
    const [appointments, setAppointments] = useState([]);
    const [loading, setLoading] = useState(true);
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
    const [isRescheduleModalOpen, setIsRescheduleModalOpen] = useState(false);
    const [isSubmittingReschedule, setIsSubmittingReschedule] = useState(false);
    const [rescheduleDateTime, setRescheduleDateTime] = useState('');
    const [reviewingAppointment, setReviewingAppointment] = useState(null);
    const [isReviewModalOpen, setIsReviewModalOpen] = useState(false);
    const [isSubmittingReview, setIsSubmittingReview] = useState(false);
    const [reviewRating, setReviewRating] = useState(5);
    const [reviewComment, setReviewComment] = useState('');
    const [offlineTransactionalNotice, setOfflineTransactionalNotice] = useState('');

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

                setAppointments(sorted.map((item) => ({
                    ...item,
                    hasReviewed: Boolean(reviewedByShop[item.barbershopId]),
                })));
                return;
            }

            setAppointments(sorted);
        } catch (error) {
            console.error("Erro ao buscar agendamentos:", error);
            if (isOfflineTransactionalError(error)) {
                setOfflineTransactionalNotice(getOfflineTransactionalMessage(error));
            } else {
                toast.error('Nao foi possivel carregar seus agendamentos.');
            }
            setAppointments([]);
        } finally {
            setLoading(false);
        }
    }, [agendaView, barbershopId, isCustomer, isOwner, teamDate]);

    useEffect(() => {
        carregarAgendamentos();
    }, [carregarAgendamentos]);

    useEffect(() => {
        setCurrentPage(1);
    }, [activeFilter]);

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

    const toDateTimeLocalValue = (isoString) => {
        const date = new Date(isoString);
        if (Number.isNaN(date.getTime())) return '';

        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        const hours = String(date.getHours()).padStart(2, '0');
        const minutes = String(date.getMinutes()).padStart(2, '0');

        return `${year}-${month}-${day}T${hours}:${minutes}`;
    };

    const toApiLocalDateTime = (dateTimeLocal) => {
        if (!dateTimeLocal) return '';
        if (dateTimeLocal.length === 16) {
            return `${dateTimeLocal}:00`;
        }
        return dateTimeLocal;
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
        setRescheduleDateTime(toDateTimeLocalValue(appointment.startTime));
        setIsRescheduleModalOpen(true);
    };

    const handleCloseRescheduleModal = () => {
        if (isSubmittingReschedule) return;
        setIsRescheduleModalOpen(false);
        setReschedulingAppointmentId(null);
        setRescheduleDateTime('');
    };

    const handleConfirmReschedule = async () => {
        if (!reschedulingAppointmentId) return;

        const normalizedDateTime = toApiLocalDateTime(rescheduleDateTime);
        if (!normalizedDateTime) {
            toast.warn('Informe o novo horario do agendamento.');
            return;
        }

        try {
            setIsSubmittingReschedule(true);
            await rescheduleAppointment(reschedulingAppointmentId, normalizedDateTime);
            setIsRescheduleModalOpen(false);
            setReschedulingAppointmentId(null);
            setRescheduleDateTime('');
            await carregarAgendamentos();
            toast.success('Agendamento reagendado com sucesso.');
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
        if (activeFilter === 'ALL') return true;
        return app.status === activeFilter;
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
        if (status === 'CANCELLED') return Styles.statusCancelled;
        if (status === 'COMPLETED') return Styles.statusCompleted;
        return '';
    };

    const filterItems = [
        { key: 'ALL', label: 'Todos' },
        { key: 'SCHEDULED', label: 'Agendados' },
        { key: 'PAYMENT_PENDING', label: 'Pendentes' },
        { key: 'WALK_IN', label: 'Encaixe' },
        { key: 'COMPLETED', label: 'Concluidos' },
        { key: 'CANCELLED', label: 'Cancelados' },
    ];

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

                {!isCustomer && (
                    <div className={Styles.filtersRow}>
                        {isOwner && (
                            <>
                                <button
                                    className={agendaView === 'mine' ? Styles.filterButtonActive : Styles.filterButton}
                                    onClick={() => {
                                        setAgendaView('mine');
                                        navigate('/meus-agendamentos');
                                    }}
                                >
                                    Minha Agenda
                                </button>
                                <button
                                    className={agendaView === 'team' ? Styles.filterButtonActive : Styles.filterButton}
                                    onClick={() => {
                                        setAgendaView('team');
                                        navigate('/meus-agendamentos?view=team');
                                    }}
                                >
                                    Agenda da Equipe
                                </button>

                                {agendaView === 'team' && (
                                    <>
                                        <input
                                            type="date"
                                            value={teamDate}
                                            onChange={(e) => setTeamDate(e.target.value)}
                                            className={Styles.filterButton}
                                            style={{ cursor: 'pointer' }}
                                        />
                                        <button className={Styles.filterButton} onClick={() => setTeamDate(new Date().toLocaleDateString('en-CA'))}>
                                            Hoje
                                        </button>
                                    </>
                                )}
                            </>
                        )}
                    </div>
                )}

                <div className={Styles.filtersRow}>
                    {filterItems.map((filter) => (
                        <button
                            key={filter.key}
                            className={activeFilter === filter.key ? Styles.filterButtonActive : Styles.filterButton}
                            onClick={() => setActiveFilter(filter.key)}
                        >
                            {filter.label}
                        </button>
                    ))}
                </div>

                {loading ? (
                    <div className={`${Styles.loadingState} ca-state ca-state--loading`}>Carregando agendamentos...</div>
                ) : filteredAppointments.length === 0 ? (
                    <div className={`${Styles.empty} ca-state ca-state--empty`}>
                        <h3>Nenhum agendamento neste filtro.</h3>
                        {isCustomer && <p>Que tal marcar um horário agora?</p>}
                    </div>
                ) : (
                    <div>
                        <div className={Styles.list}>
                            {paginatedAppointments.map(app => (
                                <div key={app.id} className={Styles.card}>
                                    
                                    <div className={Styles.info}>
                                        <span className={Styles.datePill}>
                                            <FiClock />
                                            {formatData(app.startTime)}
                                        </span>
                                        
                                        {/* Se sou Cliente, mostro o Barbeiro. Se sou Barbeiro, mostro o Cliente */}
                                        <span className={Styles.mainInfo}>
                                            {isCustomer 
                                                ? `Com: ${app.barberName} (${app.barbershopName})`
                                                : (agendaView === 'team'
                                                    ? `Barbeiro: ${app.barberName || 'Barbeiro'} • Cliente: ${app.customerName || 'Cliente'}`
                                                    : `Cliente: ${app.customerName}`)
                                            }
                                        </span>

                                        {/* Lista de serviços (caso seu DTO retorne activityNames como lista) */}
                                        <span className={Styles.details}>
                                            {toServiceSummary(app)}
                                        </span>

                                        <span className={`${Styles.statusChip} ${getStatusClass(app.status)}`}>
                                            {app.status === 'SCHEDULED' && <FiCalendar />}
                                            {app.status === 'CONFIRMED' && <FiCalendar />}
                                            {app.status === 'PAYMENT_PENDING' && <FiClock />}
                                            {app.status === 'COMPLETED' && <FiCheckCircle />}
                                            {app.status === 'CANCELLED' && <FiXCircle />}
                                            {app.status === 'WALK_IN' && <FiScissors />}
                                            {translateStatus(app.status)}
                                        </span>
                                    </div>

                                    {canInteractWithAppointment(app.status) && (
                                        <div className={Styles.cardActions}>
                                            <button
                                                className={Styles.rescheduleButton}
                                                onClick={() => handleOpenRescheduleModal(app)}
                                                disabled={isSubmittingCancel || isSubmittingConclude || isSubmittingReschedule}
                                            >
                                                Reagendar
                                            </button>
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
                            ))}
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
                                <label className={Styles.reviewLabel} htmlFor="review-rating">Nota (1 a 5)</label>
                                <select
                                    id="review-rating"
                                    className={Styles.reviewSelect}
                                    value={reviewRating}
                                    onChange={(e) => setReviewRating(e.target.value)}
                                >
                                    <option value={1}>1</option>
                                    <option value={2}>2</option>
                                    <option value={3}>3</option>
                                    <option value={4}>4</option>
                                    <option value={5}>5</option>
                                </select>
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

                {isRescheduleModalOpen && (
                    <div className={Styles.modalBackdrop} onClick={handleCloseRescheduleModal}>
                        <div className={Styles.modalCard} onClick={(e) => e.stopPropagation()}>
                            <p className={Styles.modalKicker}>REAGENDAR ATENDIMENTO</p>
                            <h3 className={Styles.modalTitle}>Escolha o novo horario</h3>
                            <p className={Styles.modalSubtitle}>Atualize data e hora para reagendar o atendimento.</p>

                            <div className={Styles.reviewFormGroup}>
                                <label className={Styles.reviewLabel} htmlFor="reschedule-date-time">Novo horario</label>
                                <input
                                    id="reschedule-date-time"
                                    type="datetime-local"
                                    className={Styles.reviewSelect}
                                    value={rescheduleDateTime}
                                    onChange={(e) => setRescheduleDateTime(e.target.value)}
                                />
                            </div>

                            <div className={Styles.modalActions}>
                                <button
                                    type="button"
                                    className={Styles.modalSecondaryButton}
                                    onClick={handleCloseRescheduleModal}
                                    disabled={isSubmittingReschedule}
                                >
                                    Voltar
                                </button>
                                <button
                                    type="button"
                                    className={Styles.modalPrimaryButton}
                                    onClick={handleConfirmReschedule}
                                    disabled={isSubmittingReschedule}
                                >
                                    {isSubmittingReschedule ? 'Salvando...' : 'Confirmar reagendamento'}
                                </button>
                            </div>
                        </div>
                    </div>
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
