import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import api from '../services/api';
import { logoutUser } from '../services/authService';
import { isCustomer, isOwnerUser } from '../services/userContext';
import { maskCpf, onlyDigits } from '../utils/inputMasks';
import BarberHeader from '../components/BarberPage/BarberHeader';
import BarberNavbar from '../components/BarberPage/BarberNavbar';
import styles from './CSS/BarberHomePage.module.css';

function BarberTeamPage() {
    const navigate = useNavigate();
    const [barber, setBarber] = useState(null);
    const [loading, setLoading] = useState(true);
    const [team, setTeam] = useState([]);
    const [activities, setActivities] = useState([]);
    const [selectedMember, setSelectedMember] = useState(null);
    const [removalPlan, setRemovalPlan] = useState(null);
    const [loadingConflicts, setLoadingConflicts] = useState(false);
    const [commissionForm, setCommissionForm] = useState({ activityId: '', percentage: '' });
    const [isInviteModalOpen, setIsInviteModalOpen] = useState(false);
    const [inviteCpf, setInviteCpf] = useState('');
    const [inviteError, setInviteError] = useState('');
    const [isSendingInvite, setIsSendingInvite] = useState(false);

    const selectedCommissions = useMemo(
        () => selectedMember?.commissions || [],
        [selectedMember],
    );

    useEffect(() => {
        if (isCustomer()) { navigate('/homepage', { replace: true }); return; }
        if (!isOwnerUser()) { navigate('/barberHome', { replace: true }); return; }
        const token = localStorage.getItem('token');
        if (!token) { navigate('/', { replace: true }); return; }

        api.get('/auth/me')
            .then(res => { setBarber(res.data); setLoading(false); })
            .catch(() => { setLoading(false); navigate('/'); });
    }, [navigate]);

    const loadTeam = async () => {
        if (!barber?.barbershopId) return;

        try {
            const [teamResponse, activitiesResponse] = await Promise.all([
                api.get('/barbershops/my-shop/team'),
                api.get(`/barbershops/${barber.barbershopId}/activities`),
            ]);
            const nextTeam = Array.isArray(teamResponse.data) ? teamResponse.data : [];
            setTeam(nextTeam);
            setActivities(Array.isArray(activitiesResponse.data) ? activitiesResponse.data : []);
            if (selectedMember) {
                setSelectedMember(nextTeam.find((member) => member.barberId === selectedMember.barberId) || null);
            }
        } catch (error) {
            console.error('Erro ao carregar time:', error);
            toast.error('Não foi possível carregar a equipe.');
        }
    };

    useEffect(() => {
        loadTeam();
    }, [barber?.barbershopId]);

    const handleOpenInviteModal = () => {
        setInviteError('');
        setInviteCpf('');
        setIsInviteModalOpen(true);
    };

    const handleCloseInviteModal = () => {
        if (isSendingInvite) return;
        setIsInviteModalOpen(false);
        setInviteError('');
    };

    const handleSubmitInvite = async (e) => {
        e.preventDefault();
        const normalizedCpf = onlyDigits(inviteCpf);
        if (normalizedCpf.length !== 11) {
            setInviteError('Informe um CPF válido com 11 números.');
            return;
        }
        try {
            setIsSendingInvite(true);
            await api.post('/barbershops/my-shop/invite-barber', { cpf: normalizedCpf });
            toast.success('Convite enviado! O barbeiro verá no perfil dele.');
            setIsInviteModalOpen(false);
            setInviteCpf('');
            setInviteError('');
        } catch (error) {
            const msg = error?.response?.data?.message || 'Erro ao enviar convite. Verifique o CPF e tente novamente.';
            setInviteError(msg);
        } finally {
            setIsSendingInvite(false);
        }
    };

    const handleSaveCommission = async (event) => {
        event.preventDefault();
        if (!selectedMember || !commissionForm.activityId || !commissionForm.percentage) return;

        try {
            await api.post(`/barbershops/my-shop/team/${selectedMember.barberId}/commissions`, {
                activityId: commissionForm.activityId,
                percentage: Number(commissionForm.percentage),
            });
            setCommissionForm({ activityId: '', percentage: '' });
            await loadTeam();
            toast.success('Comissão salva.');
        } catch (error) {
            console.error('Erro ao salvar comissão:', error);
            toast.error(error?.response?.data?.message || 'Não foi possível salvar a comissão.');
        }
    };

    const handleDeleteCommission = async (ruleId) => {
        if (!selectedMember) return;

        try {
            await api.delete(`/barbershops/my-shop/team/${selectedMember.barberId}/commissions/${ruleId}`);
            await loadTeam();
            toast.success('Comissão removida.');
        } catch (error) {
            console.error('Erro ao remover comissão:', error);
            toast.error('Não foi possível remover a comissão.');
        }
    };

    const handleOpenRemoval = async (member) => {
        if (member.isOwner) return;

        try {
            setLoadingConflicts(true);
            const response = await api.get(`/barbershops/my-shop/team/${member.barberId}/conflicts`);
            setRemovalPlan({
                member,
                conflicts: Array.isArray(response.data) ? response.data : [],
                action: 'CANCEL',
                redistributeToId: '',
            });
        } catch (error) {
            console.error('Erro ao consultar conflitos:', error);
            toast.error(error?.response?.data?.message || 'Não foi possível consultar conflitos.');
        } finally {
            setLoadingConflicts(false);
        }
    };

    const handleConfirmRemoval = async () => {
        if (!removalPlan?.member) return;

        try {
            const payload = {
                action: removalPlan.action,
                redistributeToId: removalPlan.action === 'REDISTRIBUTE' ? removalPlan.redistributeToId : null,
            };
            await api.delete(`/barbershops/my-shop/team/${removalPlan.member.barberId}`, { data: payload });
            if (selectedMember?.barberId === removalPlan.member.barberId) setSelectedMember(null);
            setRemovalPlan(null);
            await loadTeam();
            toast.success('Colaborador removido.');
        } catch (error) {
            console.error('Erro ao remover colaborador:', error);
            toast.error(error?.response?.data?.message || 'Não foi possível remover o colaborador.');
        }
    };

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
        else if (tab === 'dashboards') navigate('/barberHome/dashboard');
        else if (tab === 'gerenciar-barbearia') navigate('/barberHome/gerenciar-barbearia');
        else if (tab === 'agenda-equipe') navigate('/meus-agendamentos?view=team');
        else if (tab === 'novo-agendamento') navigate('/barberHome/novo-agendamento');
        else if (tab === 'indisponibilidade') navigate('/barber/indisponibilidade');
    };

    if (loading) return <div className={styles.loadingContainer}>Carregando...</div>;

    return (
        <div className={`${styles.pageContainer} ${styles.withNavbar}`} data-onboarding-id="owner-team-page">
            <div className={styles.contentWrapper}>
                <BarberHeader barber={barber} onLogout={handleLogout} activeTab="time" onTabChange={handleTabChange} isOwner={true} barbershopId={barber?.barbershopId} />

                <section className={styles.heroSection} data-onboarding-id="owner-team-hero">
                    <p className={styles.heroKicker}>MEU TIME</p>
                    <h1>Equipe, comissões e remoção segura</h1>
                    <p>Convide barbeiros, acompanhe membros vinculados e defina percentuais por serviço.</p>
                </section>

                <section className={`${styles.dashboardSection} ${styles.teamManagementGrid} ${styles.animateItem} ${styles.delay2}`}>
                    <article className={styles.teamPanel} data-onboarding-id="owner-team-members">
                        <div className={styles.teamPanelHeader}>
                            <h2>Equipe</h2>
                            <button onClick={handleOpenInviteModal} className={styles.teamInviteButtonInline}>
                                + Convidar
                            </button>
                        </div>

                        <div className={styles.teamList}>
                            {team.map((member) => (
                                <article key={member.barberId} className={styles.teamMemberCard}>
                                    <div className={styles.teamMemberMain}>
                                        {member.imageUrl ? (
                                            <img src={member.imageUrl} alt={member.name} className={styles.teamAvatar} />
                                        ) : (
                                            <span className={styles.teamAvatarFallback}>{member.name?.slice(0, 2)?.toUpperCase() || 'BR'}</span>
                                        )}
                                        <div>
                                            <h3>{member.name}</h3>
                                            <p>{member.email || 'E-mail indisponível'}</p>
                                            <span>{member.isOwner ? 'Owner' : 'Colaborador'}</span>
                                        </div>
                                    </div>
                                    <div className={styles.teamMemberActions}>
                                        <button type="button" onClick={() => setSelectedMember(member)}>Comissões</button>
                                        {!member.isOwner && (
                                            <button type="button" className={styles.teamDangerButton} onClick={() => handleOpenRemoval(member)} disabled={loadingConflicts}>
                                                {loadingConflicts ? 'Consultando...' : 'Remover'}
                                            </button>
                                        )}
                                    </div>
                                </article>
                            ))}
                        </div>
                    </article>

                    <aside className={styles.teamPanel} data-onboarding-id="owner-team-commissions">
                        <div className={styles.teamPanelHeader}>
                            <h2>Comissões</h2>
                        </div>

                        {selectedMember ? (
                            <>
                                <p className={styles.teamFlowText}>Regras de {selectedMember.name}</p>
                                <form className={styles.commissionForm} onSubmit={handleSaveCommission}>
                                    <select
                                        value={commissionForm.activityId}
                                        onChange={(event) => setCommissionForm((prev) => ({ ...prev, activityId: event.target.value }))}
                                    >
                                        <option value="">Serviço</option>
                                        {activities.map((activity) => (
                                            <option key={activity.id} value={activity.id}>{activity.activityName}</option>
                                        ))}
                                    </select>
                                    <input
                                        type="number"
                                        min="0"
                                        max="100"
                                        step="0.01"
                                        placeholder="%"
                                        value={commissionForm.percentage}
                                        onChange={(event) => setCommissionForm((prev) => ({ ...prev, percentage: event.target.value }))}
                                    />
                                    <button type="submit">Salvar</button>
                                </form>

                                <ul className={styles.commissionList}>
                                    {selectedCommissions.map((rule) => (
                                        <li key={rule.id}>
                                            <span>{rule.activityName}</span>
                                            <strong>{Number(rule.percentage || 0).toFixed(2)}%</strong>
                                            <button type="button" onClick={() => handleDeleteCommission(rule.id)}>Remover</button>
                                        </li>
                                    ))}
                                </ul>
                            </>
                        ) : (
                            <p className={styles.teamFlowText}>Selecione um membro para configurar percentuais por serviço.</p>
                        )}
                    </aside>
                </section>
            </div>
            <BarberNavbar activeTab="time" onTabChange={handleTabChange} isOwner={true} barbershopId={barber?.barbershopId} />

            {isInviteModalOpen && (
                <div className={styles.modalBackdrop} onClick={handleCloseInviteModal}>
                    <div className={styles.modalCard} onClick={(e) => e.stopPropagation()}>
                        <p className={styles.modalKicker}>CONVIDAR BARBEIRO</p>
                        <h3 className={styles.modalTitle}>Adicionar à equipe</h3>
                        <p className={styles.modalSubtitle}>
                            Informe o CPF do barbeiro já cadastrado na plataforma. Ele receberá o convite no perfil.
                        </p>

                        <form onSubmit={handleSubmitInvite} className={styles.modalForm}>
                            <label className={styles.modalLabel}>CPF</label>
                            <input
                                type="text"
                                inputMode="numeric"
                                placeholder="000.000.000-00"
                                value={inviteCpf}
                                onChange={(event) => {
                                    setInviteCpf(maskCpf(event.target.value));
                                    if (inviteError) setInviteError('');
                                }}
                                maxLength={14}
                                autoFocus
                                className={styles.modalInput}
                            />

                            {inviteError && <p className={styles.modalError}>{inviteError}</p>}

                            <div className={styles.modalActions}>
                                <button type="button" onClick={handleCloseInviteModal} className={styles.modalSecondaryButton}>
                                    Cancelar
                                </button>
                                <button type="submit" disabled={isSendingInvite} className={styles.modalPrimaryButton}>
                                    {isSendingInvite ? 'Enviando...' : 'Enviar convite'}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {removalPlan && (
                <div className={styles.modalBackdrop} onClick={() => setRemovalPlan(null)}>
                    <div className={styles.modalCard} onClick={(event) => event.stopPropagation()}>
                        <p className={styles.modalKicker}>REMOVER COLABORADOR</p>
                        <h3 className={styles.modalTitle}>{removalPlan.member.name}</h3>
                        <p className={styles.modalSubtitle}>
                            {removalPlan.conflicts.length
                                ? `${removalPlan.conflicts.length} agendamento(s) futuro(s) precisam de tratamento.`
                                : 'Nenhum agendamento futuro encontrado para este colaborador.'}
                        </p>

                        {removalPlan.conflicts.length > 0 && (
                            <ul className={styles.conflictList}>
                                {removalPlan.conflicts.slice(0, 5).map((conflict) => (
                                    <li key={conflict.id}>
                                        <span>{new Date(conflict.startTime).toLocaleString('pt-BR')}</span>
                                        <strong>{conflict.customerName || 'Cliente'}</strong>
                                        <small>{conflict.status}</small>
                                    </li>
                                ))}
                            </ul>
                        )}

                        <div className={styles.removalOptions}>
                            <label>
                                <input
                                    type="radio"
                                    name="remove-action"
                                    checked={removalPlan.action === 'CANCEL'}
                                    onChange={() => setRemovalPlan((prev) => ({ ...prev, action: 'CANCEL', redistributeToId: '' }))}
                                />
                                Cancelar e notificar
                            </label>
                            <label>
                                <input
                                    type="radio"
                                    name="remove-action"
                                    checked={removalPlan.action === 'REDISTRIBUTE'}
                                    onChange={() => setRemovalPlan((prev) => ({ ...prev, action: 'REDISTRIBUTE' }))}
                                />
                                Redistribuir
                            </label>
                        </div>

                        {removalPlan.action === 'REDISTRIBUTE' && (
                            <select
                                className={styles.modalInput}
                                value={removalPlan.redistributeToId}
                                onChange={(event) => setRemovalPlan((prev) => ({ ...prev, redistributeToId: event.target.value }))}
                            >
                                <option value="">Escolha o destino</option>
                                {team
                                    .filter((member) => member.barberId !== removalPlan.member.barberId)
                                    .map((member) => (
                                        <option key={member.barberId} value={member.barberId}>{member.name}</option>
                                    ))}
                            </select>
                        )}

                        <div className={styles.modalActions}>
                            <button type="button" onClick={() => setRemovalPlan(null)} className={styles.modalSecondaryButton}>
                                Voltar
                            </button>
                            <button
                                type="button"
                                onClick={handleConfirmRemoval}
                                className={styles.modalPrimaryButton}
                                disabled={removalPlan.action === 'REDISTRIBUTE' && !removalPlan.redistributeToId}
                            >
                                Confirmar remoção
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}

export default BarberTeamPage;
