import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { FiArrowLeft, FiCalendar, FiCheckCircle, FiClock, FiRefreshCw, FiScissors, FiXCircle } from 'react-icons/fi';
import Styles from './CSS/MeusAgendamentos.module.css';
import { getMyAppointments, cancelAppointment } from '../services/appointmentService';
import { createBarbershopReview } from '../services/barbershopService';
import BarberHeader from '../components/BarberPage/BarberHeader';
import BarberNavbar from '../components/BarberPage/BarberNavbar';
import { logoutUser } from '../services/authService';

const MeusAgendamentosPage = () => {
    const navigate = useNavigate();
    const [appointments, setAppointments] = useState([]);
    const [loading, setLoading] = useState(true);
    const [activeFilter, setActiveFilter] = useState('ALL');
    const [currentPage, setCurrentPage] = useState(1);
    const [cancelingAppointmentId, setCancelingAppointmentId] = useState(null);
    const [isCancelModalOpen, setIsCancelModalOpen] = useState(false);
    const [isSubmittingCancel, setIsSubmittingCancel] = useState(false);
    const [reviewingAppointment, setReviewingAppointment] = useState(null);
    const [isReviewModalOpen, setIsReviewModalOpen] = useState(false);
    const [isSubmittingReview, setIsSubmittingReview] = useState(false);
    const [reviewRating, setReviewRating] = useState(5);
    const [reviewComment, setReviewComment] = useState('');
    
    // Identificar o papel para ajustar os textos
    const role = localStorage.getItem('role'); 
    const isCustomer = role === 'ROLE_CUSTOMER';
    const userName = localStorage.getItem('userName') || (isCustomer ? 'Cliente' : 'Profissional');
    const firstName = userName.split(' ')[0];

    useEffect(() => {
        carregarAgendamentos();
    }, []);

    useEffect(() => {
        setCurrentPage(1);
    }, [activeFilter]);

    const carregarAgendamentos = async () => {
        try {
            const data = await getMyAppointments();
            // Ordenar: Mais recentes primeiro
            const sorted = data.sort((a, b) => new Date(b.startTime) - new Date(a.startTime));
            setAppointments(sorted);
        } catch (error) {
            console.error("Erro ao buscar agendamentos:", error);
            // alert("Não foi possível carregar sua agenda.");
        } finally {
            setLoading(false);
        }
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
            carregarAgendamentos();
        } catch (error) {
            alert("Erro ao cancelar. Tente novamente.");
        } finally {
            setIsSubmittingCancel(false);
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
            alert('Nao foi possivel identificar a barbearia deste atendimento.');
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
            alert('Avaliacao enviada com sucesso!');
        } catch (error) {
            if (error?.response?.status === 409) {
                alert('Voce ja avaliou esta barbearia.');
            } else {
                alert('Nao foi possivel enviar sua avaliacao. Tente novamente.');
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
            'CANCELLED': 'Cancelado',
            'COMPLETED': 'Concluído'
        };
        return map[status] || status;
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
        if (status === 'CANCELLED') return Styles.statusCancelled;
        if (status === 'COMPLETED') return Styles.statusCompleted;
        return '';
    };

    const filterItems = [
        { key: 'ALL', label: 'Todos' },
        { key: 'SCHEDULED', label: 'Agendados' },
        { key: 'COMPLETED', label: 'Concluidos' },
        { key: 'CANCELLED', label: 'Cancelados' },
    ];

    const handleBarberTabChange = (tab) => {
        if (tab === 'agenda') return;

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

        navigate('/barberHome', { state: { activeTab: tab } });
    };

    const handleBarberLogout = () => {
        logoutUser();
        navigate('/identificacao', { state: { mode: 'login', role: 'barber' } });
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
                    />
                )}

                {isCustomer && <header className={Styles.topMenu}>
                    <div className={Styles.brandBlock}>
                        <div className={Styles.brandBadge}>CA</div>
                        <div>
                            <h3>CortaAI</h3>
                            <p>{isCustomer ? `Agenda de ${firstName}` : `Painel de ${firstName}`}</p>
                        </div>
                    </div>

                    <nav className={Styles.menuCenter} aria-label="Navegacao de agendamentos">
                        <button onClick={() => navigate(isCustomer ? '/homepage' : '/barberHome')}>
                            <FiScissors />
                            <span>Home</span>
                        </button>
                        <button className={Styles.menuItemActive}>
                            <FiCalendar />
                            <span>Meus agendamentos</span>
                        </button>
                    </nav>

                    <div className={Styles.menuActions}>
                        <button className={Styles.secondaryAction} onClick={carregarAgendamentos}>
                            <FiRefreshCw />
                            <span>Atualizar</span>
                        </button>
                        <button className={Styles.ghostAction} onClick={() => navigate(-1)}>
                            <FiArrowLeft />
                            <span>Voltar</span>
                        </button>
                    </div>
                </header>}

                <section className={Styles.heroBlock}>
                    <p className={Styles.kicker}>{isCustomer ? 'PAINEL DE AGENDAMENTOS' : 'MINHA AGENDA'}</p>
                    <h1 className={Styles.title}>{isCustomer ? 'Acompanhe seus proximos cortes' : 'Organize seus atendimentos'}</h1>
                    <p className={Styles.subtitle}>Visualize status, horario e servicos de cada agendamento em um fluxo mais claro.</p>
                </section>

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
                    <div className={Styles.loadingState}>Carregando agendamentos...</div>
                ) : filteredAppointments.length === 0 ? (
                    <div className={Styles.empty}>
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
                                                : `Cliente: ${app.customerName}`
                                            }
                                        </span>

                                        {/* Lista de serviços (caso seu DTO retorne activityNames como lista) */}
                                        <span className={Styles.details}>
                                            {app.activityNames ? app.activityNames.join(", ") : "Servico"}
                                        </span>

                                        <span className={`${Styles.statusChip} ${getStatusClass(app.status)}`}>
                                            {app.status === 'SCHEDULED' && <FiCalendar />}
                                            {app.status === 'COMPLETED' && <FiCheckCircle />}
                                            {app.status === 'CANCELLED' && <FiXCircle />}
                                            {translateStatus(app.status)}
                                        </span>
                                    </div>

                                    {/* Botão Cancelar apenas se estiver Agendado */}
                                    {app.status === 'SCHEDULED' && (
                                        <button 
                                            className={Styles.cancelButton}
                                            onClick={() => handleOpenCancelModal(app.id)}
                                        >
                                            Cancelar
                                        </button>
                                    )}

                                    {isCustomer && app.status === 'COMPLETED' && (
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
            </div>

            {!isCustomer && (
                <BarberNavbar activeTab="agenda" onTabChange={handleBarberTabChange} />
            )}
        </div>
    );
};

export default MeusAgendamentosPage;