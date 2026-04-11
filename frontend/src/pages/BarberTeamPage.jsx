import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import api from '../services/api';
import { logoutUser } from '../services/authService';
import { isCustomer, isOwnerUser } from '../services/userContext';
import BarberHeader from '../components/BarberPage/BarberHeader';
import BarberNavbar from '../components/BarberPage/BarberNavbar';

/**
 * Página "Meu Time" — convida barbeiros pelo CPF e lista pedidos pendentes.
 * Disponível apenas para donos (OWNER).
 */
function BarberTeamPage() {
    const navigate = useNavigate();
    const [barber, setBarber] = useState(null);
    const [loading, setLoading] = useState(true);
    const [pendingRequests, setPendingRequests] = useState([]);
    const [loadingRequests, setLoadingRequests] = useState(false);
    const [actionLoading, setActionLoading] = useState(null);
    const [feedback, setFeedback] = useState(null);

    // ── Convite por CPF ────────────────────────────────────────────────────
    const [isInviteModalOpen, setIsInviteModalOpen] = useState(false);
    const [inviteCpf, setInviteCpf] = useState('');
    const [inviteError, setInviteError] = useState('');
    const [isSendingInvite, setIsSendingInvite] = useState(false);

    const formatCpf = (value) => {
        const digits = value.replace(/\D/g, '').slice(0, 11);
        return digits
            .replace(/(\d{3})(\d)/, '$1.$2')
            .replace(/(\d{3})(\d)/, '$1.$2')
            .replace(/(\d{3})(\d{1,2})$/, '$1-$2');
    };

    useEffect(() => {
        if (isCustomer()) { navigate('/homepage', { replace: true }); return; }
        if (!isOwnerUser()) { navigate('/barberHome', { replace: true }); return; }
        const token = localStorage.getItem('token');
        if (!token) { navigate('/', { replace: true }); return; }
        api.get('/auth/me')
            .then(res => { setBarber(res.data); setLoading(false); })
            .catch(() => { setLoading(false); navigate('/'); });
    }, [navigate]);

    useEffect(() => {
        if (!barber?.barbershopId) return;
        setLoadingRequests(true);
        api.get('/barbershops/my-shop/pending-requests')
            .then(res => setPendingRequests(Array.isArray(res.data) ? res.data : []))
            .catch(() => setPendingRequests([]))
            .finally(() => setLoadingRequests(false));
    }, [barber]);

    const handleApprove = async (requestId) => {
        setActionLoading(requestId);
        try {
            await api.post(`/barbershops/my-shop/approve-request/${requestId}`);
            setPendingRequests(prev => prev.filter(r => r.requestId !== requestId));
            setFeedback({ type: 'success', msg: 'Barbeiro aprovado com sucesso!' });
        } catch (err) {
            setFeedback({ type: 'error', msg: err?.response?.data?.message || 'Erro ao aprovar pedido.' });
        } finally {
            setActionLoading(null);
            setTimeout(() => setFeedback(null), 4000);
        }
    };

    const handleReject = async (requestId) => {
        setActionLoading(requestId);
        try {
            await api.post(`/barbershops/my-shop/reject-request/${requestId}`);
            setPendingRequests(prev => prev.filter(r => r.requestId !== requestId));
            setFeedback({ type: 'success', msg: 'Pedido recusado.' });
        } catch (err) {
            setFeedback({ type: 'error', msg: err?.response?.data?.message || 'Erro ao recusar pedido.' });
        } finally {
            setActionLoading(null);
            setTimeout(() => setFeedback(null), 4000);
        }
    };

    // ── Convite por CPF handlers ───────────────────────────────────────────
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

    const handleInviteCpfChange = (e) => {
        setInviteCpf(formatCpf(e.target.value));
        if (inviteError) setInviteError('');
    };

    const handleSubmitInvite = async (e) => {
        e.preventDefault();
        const normalizedCpf = inviteCpf.replace(/\D/g, '');
        if (normalizedCpf.length !== 11) {
            setInviteError('Informe um CPF valido com 11 numeros.');
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
    };

    if (loading) return <p style={{ padding: 32, color: '#fff' }}>Carregando...</p>;

    const cardStyle = {
        background: 'rgba(255,255,255,0.05)', borderRadius: 10, padding: 16,
        marginBottom: 12, display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12
    };

    const modalBackdropStyle = {
        position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.6)', display: 'flex',
        alignItems: 'center', justifyContent: 'center', zIndex: 1000, padding: 16
    };

    const modalCardStyle = {
        background: '#1a1a2e', borderRadius: 16, padding: 28, maxWidth: 420,
        width: '100%', color: '#fff'
    };

    return (
        <div style={{ minHeight: '100vh', background: '#0f0f1a', color: '#fff' }}>
            <BarberHeader barber={barber} onLogout={handleLogout} activeTab="time" onTabChange={handleTabChange} isOwner={true} barbershopId={barber?.barbershopId} />
            <main style={{ maxWidth: 680, margin: '40px auto', padding: '0 16px' }}>
                <h2 style={{ marginBottom: 8 }}>Meu Time</h2>
                <p style={{ color: 'rgba(255,255,255,0.5)', marginBottom: 24, fontSize: 14 }}>
                    Convide barbeiros para sua equipe informando o CPF deles.
                </p>

                <button
                    onClick={handleOpenInviteModal}
                    style={{
                        background: '#6c63ff', color: '#fff', border: 'none',
                        padding: '12px 24px', borderRadius: 10, cursor: 'pointer',
                        fontSize: 14, fontWeight: 600, marginBottom: 28, width: '100%'
                    }}
                >
                    + Convidar barbeiro por CPF
                </button>

                {feedback && (
                    <div style={{
                        background: feedback.type === 'success' ? '#276749' : '#742a2a',
                        color: '#fff', padding: '10px 16px', borderRadius: 8, marginBottom: 16, fontSize: 14
                    }}>
                        {feedback.msg}
                    </div>
                )}

                <h3 style={{ fontSize: 15, marginBottom: 12, color: 'rgba(255,255,255,0.7)' }}>
                    Pedidos Pendentes (fluxo legado)
                </h3>

                {loadingRequests ? (
                    <p style={{ color: 'rgba(255,255,255,0.4)' }}>Carregando pedidos...</p>
                ) : pendingRequests.length === 0 ? (
                    <p style={{ color: 'rgba(255,255,255,0.4)', fontSize: 14 }}>Nenhum pedido de entrada pendente.</p>
                ) : (
                    pendingRequests.map(req => (
                        <div key={req.requestId} style={cardStyle}>
                            <div>
                                <p style={{ margin: 0, fontWeight: 600 }}>{req.barberName || 'Barbeiro'}</p>
                                <p style={{ margin: 0, fontSize: 12, color: 'rgba(255,255,255,0.5)' }}>{req.barberEmail}</p>
                            </div>
                            <div style={{ display: 'flex', gap: 8 }}>
                                <button
                                    onClick={() => handleApprove(req.requestId)}
                                    disabled={actionLoading === req.requestId}
                                    style={{
                                        background: '#276749', color: '#fff', border: 'none',
                                        padding: '7px 16px', borderRadius: 8, cursor: 'pointer', fontSize: 13, fontWeight: 600
                                    }}
                                >
                                    {actionLoading === req.requestId ? '...' : 'Aprovar'}
                                </button>
                                <button
                                    onClick={() => handleReject(req.requestId)}
                                    disabled={actionLoading === req.requestId}
                                    style={{
                                        background: '#742a2a', color: '#fff', border: 'none',
                                        padding: '7px 16px', borderRadius: 8, cursor: 'pointer', fontSize: 13, fontWeight: 600
                                    }}
                                >
                                    {actionLoading === req.requestId ? '...' : 'Recusar'}
                                </button>
                            </div>
                        </div>
                    ))
                )}
            </main>
            <BarberNavbar activeTab="time" onTabChange={handleTabChange} isOwner={true} barbershopId={barber?.barbershopId} />

            {isInviteModalOpen && (
                <div style={modalBackdropStyle} onClick={handleCloseInviteModal}>
                    <div style={modalCardStyle} onClick={(e) => e.stopPropagation()}>
                        <p style={{ fontSize: 11, textTransform: 'uppercase', color: '#6c63ff', letterSpacing: 1.5, marginBottom: 4 }}>CONVIDAR BARBEIRO</p>
                        <h3 style={{ margin: '0 0 6px', fontSize: 18 }}>Adicionar à equipe</h3>
                        <p style={{ color: 'rgba(255,255,255,0.5)', fontSize: 13, marginBottom: 20 }}>
                            Informe o CPF do barbeiro já cadastrado na plataforma. Ele receberá o convite no perfil.
                        </p>

                        <form onSubmit={handleSubmitInvite}>
                            <label style={{ fontSize: 12, color: 'rgba(255,255,255,0.6)', display: 'block', marginBottom: 6 }}>CPF</label>
                            <input
                                type="text"
                                inputMode="numeric"
                                placeholder="000.000.000-00"
                                value={inviteCpf}
                                onChange={handleInviteCpfChange}
                                maxLength={14}
                                autoFocus
                                style={{
                                    width: '100%', padding: '12px 14px', borderRadius: 10,
                                    border: '1px solid rgba(255,255,255,0.15)', background: 'rgba(255,255,255,0.07)',
                                    color: '#fff', fontSize: 15, outline: 'none', boxSizing: 'border-box'
                                }}
                            />

                            {inviteError && <p style={{ color: '#fc8181', fontSize: 13, marginTop: 8 }}>{inviteError}</p>}

                            <div style={{ display: 'flex', gap: 10, marginTop: 20 }}>
                                <button
                                    type="button"
                                    onClick={handleCloseInviteModal}
                                    style={{
                                        flex: 1, padding: '11px 0', borderRadius: 10, border: '1px solid rgba(255,255,255,0.15)',
                                        background: 'transparent', color: '#fff', cursor: 'pointer', fontSize: 14
                                    }}
                                >
                                    Cancelar
                                </button>
                                <button
                                    type="submit"
                                    disabled={isSendingInvite}
                                    style={{
                                        flex: 1, padding: '11px 0', borderRadius: 10, border: 'none',
                                        background: '#6c63ff', color: '#fff', cursor: 'pointer', fontSize: 14, fontWeight: 600
                                    }}
                                >
                                    {isSendingInvite ? 'Enviando...' : 'Enviar convite'}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}

export default BarberTeamPage;
