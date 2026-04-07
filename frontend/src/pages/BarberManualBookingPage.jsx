import React, { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import api from '../services/api';
import { logoutUser } from '../services/authService';
import { isCustomer, isOwnerUser } from '../services/userContext';
import BarberHeader from '../components/BarberPage/BarberHeader';
import BarberNavbar from '../components/BarberPage/BarberNavbar';

/**
 * Página de agendamento manual (walk-in) pelo barbeiro.
 * Permite cadastrar um atendimento presencial sem exigir
 * que o cliente tenha conta no sistema.
 */
function BarberManualBookingPage() {
    const navigate = useNavigate();
    const [barber, setBarber] = useState(null);
    const [loading, setLoading] = useState(true);
    const [submitting, setSubmitting] = useState(false);

    // Serviços disponíveis na barbearia
    const [activities, setActivities] = useState([]);
    const [selectedActivityIds, setSelectedActivityIds] = useState([]);

    // Campos do formulário
    const [clientName, setClientName] = useState('');
    const [clientPhone, setClientPhone] = useState('');
    const [startDate, setStartDate] = useState('');
    const [startTime, setStartTime] = useState('');

    // Resumo de preço/duração calculado
    const [summary, setSummary] = useState({ totalPrice: 0, totalDuration: 0 });

    // ── Auth guard + carrega barbeiro ──────────────────────────────────────
    useEffect(() => {
        // Guard: cliente não pode acessar páginas de barbeiro
        if (isCustomer()) {
            navigate('/homepage', { replace: true });
            return;
        }
        const token = localStorage.getItem('token');
        if (!token) {
            navigate('/', { replace: true });
            return;
        }
        api.get('/auth/me')
            .then(res => {
                setBarber(res.data);
                return res.data;
            })
            .then(barberData => {
                const shopId = barberData?.barbershopId;
                if (!shopId) return;
                return api.get(`/barbershops/${shopId}/activities`);
            })
            .then(res => {
                if (res?.data) setActivities(res.data);
            })
            .catch(() => {
                navigate('/');
            })
            .finally(() => setLoading(false));
    }, [navigate]);

    // ── Recalcula resumo quando seleção muda ──────────────────────────────
    useEffect(() => {
        const selected = activities.filter(a => selectedActivityIds.includes(a.id));
        const totalPrice = selected.reduce((sum, a) => sum + (parseFloat(a.price) || 0), 0);
        const totalDuration = selected.reduce((sum, a) => sum + (parseInt(a.durationMinutes, 10) || 0), 0);
        setSummary({ totalPrice, totalDuration });
    }, [selectedActivityIds, activities]);

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
        else if (tab === 'time') navigate('/barberHome/time');
        else if (tab === 'dashboards') navigate('/barberHome/dashboard');
    };

    // ── Toggle de atividade na seleção ────────────────────────────────────
    const toggleActivity = useCallback((id) => {
        setSelectedActivityIds(prev =>
            prev.includes(id) ? prev.filter(x => x !== id) : [...prev, id]
        );
    }, []);

    // ── Submissão ─────────────────────────────────────────────────────────
    const handleSubmit = async (e) => {
        e.preventDefault();

        if (!clientName.trim()) {
            toast.warn('Informe o nome do cliente.');
            return;
        }
        if (selectedActivityIds.length === 0) {
            toast.warn('Selecione ao menos um serviço.');
            return;
        }
        if (!startDate || !startTime) {
            toast.warn('Informe a data e horário do atendimento.');
            return;
        }

        const shopId = barber?.barbershopId;
        if (!shopId) {
            toast.error('Você não está vinculado a nenhuma barbearia.');
            return;
        }

        const startTimeISO = `${startDate}T${startTime}:00`;

        const payload = {
            barbershopId: shopId,
            activityIds: selectedActivityIds,
            startTime: startTimeISO,
            clientName: clientName.trim(),
            clientPhone: clientPhone.trim() || null,
        };

        setSubmitting(true);
        try {
            await api.post('/appointments/barber-booking', payload);
            toast.success('Agendamento registrado com sucesso!');
            // Limpar formulário
            setClientName('');
            setClientPhone('');
            setStartDate('');
            setStartTime('');
            setSelectedActivityIds([]);
        } catch (err) {
            const msg = err.response?.data?.message || 'Erro ao criar agendamento. Tente novamente.';
            if (err.response?.status === 409) {
                toast.warn(msg);
            } else {
                toast.error(msg);
            }
        } finally {
            setSubmitting(false);
        }
    };

    // ── Estilos ───────────────────────────────────────────────────────────
    const inputStyle = {
        width: '100%', background: 'rgba(255,255,255,0.07)', border: '1px solid rgba(255,255,255,0.15)',
        borderRadius: 8, padding: '10px 14px', color: '#fff', fontSize: 15,
        boxSizing: 'border-box', outline: 'none',
    };

    const labelStyle = {
        display: 'block', marginBottom: 6, fontSize: 13, color: 'rgba(255,255,255,0.6)',
    };

    const cardStyle = {
        background: 'rgba(255,255,255,0.05)', borderRadius: 12, padding: '16px 18px',
        marginBottom: 20, border: '1px solid rgba(255,255,255,0.08)',
    };

    const activityCardStyle = (selected) => ({
        background: selected ? 'rgba(255,215,0,0.12)' : 'rgba(255,255,255,0.04)',
        border: selected ? '1.5px solid #ffd700' : '1px solid rgba(255,255,255,0.1)',
        borderRadius: 10, padding: '12px 14px', cursor: 'pointer',
        display: 'flex', justifyContent: 'space-between', alignItems: 'center',
        transition: 'all 0.15s ease',
    });

    if (loading) return <p style={{ padding: 32, color: '#fff' }}>Carregando...</p>;

    return (
        <div style={{ minHeight: '100vh', background: '#0f0f1a', color: '#fff' }}>
            <BarberHeader
                barber={barber}
                onLogout={handleLogout}
                activeTab="agenda"
                isOwner={barber?.isOwner === true}
                barbershopId={barber?.barbershopId}
                onTabChange={handleTabChange}
            />

            <main style={{ maxWidth: 640, margin: '40px auto', padding: '0 16px 60px' }}>
                <h2 style={{ marginBottom: 4 }}>Novo Agendamento (Walk-in)</h2>
                <p style={{ color: 'rgba(255,255,255,0.45)', marginBottom: 28, fontSize: 14 }}>
                    Registre um atendimento presencial sem precisar que o cliente tenha conta no app.
                </p>

                <form onSubmit={handleSubmit}>

                    {/* ── Dados do cliente ── */}
                    <div style={cardStyle}>
                        <h3 style={{ margin: '0 0 16px', fontSize: 16, color: 'rgba(255,255,255,0.8)' }}>
                            👤 Dados do cliente
                        </h3>
                        <div style={{ marginBottom: 14 }}>
                            <label style={labelStyle}>Nome *</label>
                            <input
                                style={inputStyle}
                                type="text"
                                placeholder="Ex: João Silva"
                                value={clientName}
                                onChange={e => setClientName(e.target.value)}
                                maxLength={70}
                                required
                            />
                        </div>
                        <div>
                            <label style={labelStyle}>Telefone (opcional)</label>
                            <input
                                style={inputStyle}
                                type="tel"
                                placeholder="Ex: (11) 99999-9999"
                                value={clientPhone}
                                onChange={e => setClientPhone(e.target.value)}
                                maxLength={20}
                            />
                        </div>
                    </div>

                    {/* ── Data e hora ── */}
                    <div style={cardStyle}>
                        <h3 style={{ margin: '0 0 16px', fontSize: 16, color: 'rgba(255,255,255,0.8)' }}>
                            🕐 Data e horário
                        </h3>
                        <div style={{ display: 'flex', gap: 14 }}>
                            <div style={{ flex: 1 }}>
                                <label style={labelStyle}>Data *</label>
                                <input
                                    style={inputStyle}
                                    type="date"
                                    value={startDate}
                                    onChange={e => setStartDate(e.target.value)}
                                    required
                                />
                            </div>
                            <div style={{ flex: 1 }}>
                                <label style={labelStyle}>Horário *</label>
                                <input
                                    style={inputStyle}
                                    type="time"
                                    value={startTime}
                                    onChange={e => setStartTime(e.target.value)}
                                    required
                                />
                            </div>
                        </div>
                    </div>

                    {/* ── Serviços ── */}
                    <div style={cardStyle}>
                        <h3 style={{ margin: '0 0 16px', fontSize: 16, color: 'rgba(255,255,255,0.8)' }}>
                            ✂️ Serviços
                        </h3>

                        {activities.length === 0 ? (
                            <p style={{ color: 'rgba(255,255,255,0.4)', fontSize: 14 }}>
                                Nenhum serviço cadastrado na sua barbearia.
                            </p>
                        ) : (
                            <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                                {activities.map(act => {
                                    const isSelected = selectedActivityIds.includes(act.id);
                                    return (
                                        <div
                                            key={act.id}
                                            style={activityCardStyle(isSelected)}
                                            onClick={() => toggleActivity(act.id)}
                                            role="checkbox"
                                            aria-checked={isSelected}
                                        >
                                            <span style={{ fontWeight: 500, fontSize: 15 }}>
                                                {isSelected ? '✅ ' : ''}{act.activityName}
                                            </span>
                                            <span style={{ fontSize: 13, color: 'rgba(255,255,255,0.55)' }}>
                                                {act.durationMinutes} min &nbsp;|&nbsp; R$ {parseFloat(act.price).toFixed(2)}
                                            </span>
                                        </div>
                                    );
                                })}
                            </div>
                        )}

                        {selectedActivityIds.length > 0 && (
                            <div style={{
                                marginTop: 16, padding: '10px 14px',
                                background: 'rgba(255,215,0,0.07)', borderRadius: 8,
                                display: 'flex', justifyContent: 'space-between', fontSize: 14,
                            }}>
                                <span style={{ color: 'rgba(255,255,255,0.6)' }}>
                                    ⏱ {summary.totalDuration} min
                                </span>
                                <span style={{ color: '#ffd700', fontWeight: 600 }}>
                                    Total: R$ {summary.totalPrice.toFixed(2)}
                                </span>
                            </div>
                        )}
                    </div>

                    {/* ── Botão ── */}
                    <button
                        type="submit"
                        disabled={submitting}
                        style={{
                            width: '100%', padding: '14px 0',
                            background: submitting ? 'rgba(255,215,0,0.4)' : '#ffd700',
                            color: '#111', fontWeight: 700, fontSize: 16,
                            border: 'none', borderRadius: 10, cursor: submitting ? 'not-allowed' : 'pointer',
                            transition: 'background 0.2s',
                        }}
                    >
                        {submitting ? 'Registrando...' : 'Registrar atendimento'}
                    </button>
                </form>
            </main>

            <BarberNavbar
                activeTab="agenda"
                onTabChange={handleTabChange}
                isOwner={isOwnerUser()}
                barbershopId={barber?.barbershopId}
            />
        </div>
    );
}

export default BarberManualBookingPage;
