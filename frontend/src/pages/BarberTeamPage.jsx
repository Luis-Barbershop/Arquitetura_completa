import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';
import { logoutUser } from '../services/authService';
import BarberHeader from '../components/BarberPage/BarberHeader';
import BarberNavbar from '../components/BarberPage/BarberNavbar';

/**
 * Página "Meu Time" — lista pedidos pendentes e barbeiros da barbearia.
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

    useEffect(() => {
        const token = localStorage.getItem('token');
        if (!token) {
            navigate('/identificacao', { state: { mode: 'login', role: 'barber' } });
            return;
        }
        api.get('/auth/me')
            .then(res => {
                setBarber(res.data);
                setLoading(false);
            })
            .catch(() => { setLoading(false); navigate('/identificacao'); });
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

    return (
        <div style={{ minHeight: '100vh', background: '#0f0f1a', color: '#fff' }}>
            <BarberHeader barber={barber} onLogout={handleLogout} activeTab="time" onTabChange={handleTabChange} />
            <main style={{ maxWidth: 680, margin: '40px auto', padding: '0 16px' }}>
                <h2 style={{ marginBottom: 8 }}>Meu Time</h2>
                <p style={{ color: 'rgba(255,255,255,0.5)', marginBottom: 24, fontSize: 14 }}>
                    Gerencie os pedidos de entrada de barbeiros na sua barbearia.
                </p>

                {feedback && (
                    <div style={{
                        background: feedback.type === 'success' ? '#276749' : '#742a2a',
                        color: '#fff', padding: '10px 16px', borderRadius: 8, marginBottom: 16, fontSize: 14
                    }}>
                        {feedback.msg}
                    </div>
                )}

                <h3 style={{ fontSize: 15, marginBottom: 12, color: 'rgba(255,255,255,0.7)' }}>
                    Pedidos Pendentes
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
            <BarberNavbar activeTab="time" onTabChange={handleTabChange} />
        </div>
    );
}

export default BarberTeamPage;
