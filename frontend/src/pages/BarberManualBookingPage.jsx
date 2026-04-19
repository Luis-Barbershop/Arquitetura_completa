import React, { useEffect, useState, useCallback, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import { FiChevronUp, FiChevronDown } from 'react-icons/fi';
import api from '../services/api';
import { logoutUser } from '../services/authService';
import { isCustomer, isOwnerUser } from '../services/userContext';
import { getMyAssignedActivities } from '../services/barbershopService';
import BarberHeader from '../components/BarberPage/BarberHeader';
import BarberNavbar from '../components/BarberPage/BarberNavbar';
import styles from './CSS/BarberHomePage.module.css';
import bookingStyles from './CSS/BarberManualBooking.module.css';

/**
 * Página de encaixe (atendimento imediato) pelo barbeiro.
 * Data e horário seguem o mesmo padrão da AgendamentoPage:
 * cards de 14 dias com slots de disponibilidade da API.
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

    // Seleção de data/horário (padrão AgendamentoPage)
    const [dateOptions, setDateOptions] = useState([]);
    const [isLoadingDates, setIsLoadingDates] = useState(false);
    const [selectedDate, setSelectedDate] = useState(null);
    const [selectedTime, setSelectedTime] = useState('');
    const [expandedPeriods, setExpandedPeriods] = useState({ morning: true, afternoon: true });

    // Resumo de preço/duração calculado
    const [summary, setSummary] = useState({ totalPrice: 0, totalDuration: 0 });

    // ── Helpers de data ───────────────────────────────────────────────────
    const WEEK_SHORT = ['DOM', 'SEG', 'TER', 'QUA', 'QUI', 'SEX', 'SAB'];

    const formatDateToApi = (dateObj) => {
        const y = dateObj.getFullYear();
        const m = String(dateObj.getMonth() + 1).padStart(2, '0');
        const d = String(dateObj.getDate()).padStart(2, '0');
        return `${y}-${m}-${d}`;
    };

    const formatCompact = (dateObj) =>
        `${String(dateObj.getDate()).padStart(2, '0')}/${String(dateObj.getMonth() + 1).padStart(2, '0')}`;

    const getRelativeLabel = (dateObj, idx) => {
        if (idx === 0) return 'Hoje';
        if (idx === 1) return 'Amanhã';
        return WEEK_SHORT[dateObj.getDay()];
    };

    const buildWindow = (days = 14) => {
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        return Array.from({ length: days }, (_, i) => {
            const d = new Date(today);
            d.setDate(today.getDate() + i);
            return d;
        });
    };

    const fetchSlots = async (barberId, dateObj, durationMinutes) => {
        const res = await api.get('/appointments/availability', {
            params: { barberId, date: formatDateToApi(dateObj), duration: durationMinutes || 30 },
        });
        const data = Array.isArray(res.data) ? res.data : [];
        return data
            .filter(s => s.available)
            .map(s => {
                const raw = s.startTime;
                if (!raw) return null;
                const part = raw.includes('T') ? raw.split('T')[1] : raw;
                return part.substring(0, 5);
            })
            .filter(Boolean);
    };

    // ── Auth guard + carrega barbeiro ──────────────────────────────────────
    useEffect(() => {
        if (isCustomer()) { navigate('/homepage', { replace: true }); return; }
        const token = localStorage.getItem('token');
        if (!token) { navigate('/', { replace: true }); return; }

        api.get('/auth/me')
            .then(res => {
                setBarber(res.data);
                return res.data;
            })
            .then(barberData => {
                const shopId = barberData?.barbershopId;
                if (!shopId) return;
                return Promise.all([
                    api.get(`/barbershops/${shopId}/activities`),
                    getMyAssignedActivities(),
                ]).then(([shopRes, assignedActivities]) => {
                    const all = Array.isArray(shopRes?.data) ? shopRes.data : [];
                    const assignedIds = new Set((assignedActivities || []).map(String));
                    setActivities(all.filter(a => assignedIds.has(String(a.id))));
                });
            })
            .catch(() => navigate('/'))
            .finally(() => setLoading(false));
    }, [navigate]);

    // ── Recalcula resumo quando seleção muda ──────────────────────────────
    useEffect(() => {
        const selected = activities.filter(a => selectedActivityIds.includes(a.id));
        const totalPrice = selected.reduce((s, a) => s + (parseFloat(a.price) || 0), 0);
        const totalDuration = selected.reduce((s, a) => s + (parseInt(a.durationMinutes, 10) || 0), 0);
        setSummary({ totalPrice, totalDuration });
    }, [selectedActivityIds, activities]);

    // ── Busca disponibilidade dos 14 dias quando duração muda ────────────
    useEffect(() => {
        if (!barber?.id || summary.totalDuration <= 0) {
            setDateOptions([]);
            setSelectedDate(null);
            setSelectedTime('');
            return;
        }

        const load = async () => {
            setIsLoadingDates(true);
            try {
                const window = buildWindow(14);
                const base = window.map((d, i) => ({
                    key: formatDateToApi(d),
                    date: d,
                    label: getRelativeLabel(d, i),
                    compact: formatCompact(d),
                    slots: [],
                    isAvailable: false,
                }));

                const results = await Promise.allSettled(
                    base.map(o => fetchSlots(barber.id, o.date, summary.totalDuration))
                );

                const hydrated = base.map((o, i) => {
                    const slots = results[i].status === 'fulfilled' ? results[i].value : [];
                    return { ...o, slots, isAvailable: slots.length > 0 };
                });

                setDateOptions(hydrated);

                const first = hydrated.find(o => o.isAvailable);
                if (first) {
                    setSelectedDate(first.date);
                    setSelectedTime(first.slots[0] || '');
                } else {
                    setSelectedDate(hydrated[0]?.date || null);
                    setSelectedTime('');
                }
            } catch {
                setDateOptions([]);
            } finally {
                setIsLoadingDates(false);
            }
        };

        load();
    }, [barber?.id, summary.totalDuration]);

    // ── Slots do dia selecionado ──────────────────────────────────────────
    const currentSlots = useMemo(() => {
        if (!selectedDate) return [];
        const key = formatDateToApi(selectedDate);
        return dateOptions.find(o => o.key === key)?.slots || [];
    }, [selectedDate, dateOptions]);

    const groupedSlots = useMemo(() => {
        const morning = [], afternoon = [];
        currentSlots.forEach(slot => {
            const hour = Number(slot.split(':')[0]);
            (hour < 12 ? morning : afternoon).push(slot);
        });
        return { morning, afternoon };
    }, [currentSlots]);

    const selectedDateLabel = useMemo(() => {
        if (!selectedDate) return 'Selecione um dia';
        return selectedDate.toLocaleDateString('pt-BR', {
            weekday: 'long', day: '2-digit', month: '2-digit', year: 'numeric',
        });
    }, [selectedDate]);

    const handleLogout = async () => { await logoutUser(); navigate('/'); };

    const handleTabChange = (tab) => {
        if (tab === 'novo-agendamento') return;
        const routes = {
            home: '/barberHome', agenda: '/meus-agendamentos',
            servicos: '/barberHome/servicos', estoque: '/barberHome/estoque',
            perfil: '/barberHome/perfil', time: '/barberHome/time',
            dashboards: '/barberHome/dashboard', 'agenda-equipe': '/barberHome/agenda-equipe',
        };
        if (routes[tab]) navigate(routes[tab]);
    };

    const toggleActivity = useCallback((id) => {
        setSelectedActivityIds(prev =>
            prev.includes(id) ? prev.filter(x => x !== id) : [...prev, id]
        );
    }, []);

    const handleSelectDate = (option) => {
        if (!option.isAvailable) return;
        setSelectedDate(option.date);
        setSelectedTime(option.slots[0] || '');
    };

    // ── Submissão ─────────────────────────────────────────────────────────
    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!clientName.trim()) { toast.warn('Informe o nome do cliente.'); return; }
        if (selectedActivityIds.length === 0) { toast.warn('Selecione ao menos um serviço.'); return; }
        if (!selectedDate || !selectedTime) { toast.warn('Selecione a data e horário do atendimento.'); return; }
        if (clientPhone && clientPhone.length !== 11) { toast.warn('Telefone deve conter exatamente 11 dígitos.'); return; }

        const shopId = barber?.barbershopId;
        if (!shopId) { toast.error('Você não está vinculado a nenhuma barbearia.'); return; }

        const payload = {
            barbershopId: shopId,
            activityIds: selectedActivityIds,
            startTime: `${formatDateToApi(selectedDate)}T${selectedTime}:00`,
            clientName: clientName.trim(),
            clientPhone: clientPhone || null,
        };

        setSubmitting(true);
        try {
            await api.post('/appointments/barber-booking', payload);
            toast.success('Agendamento registrado com sucesso!');
            setClientName('');
            setClientPhone('');
            setSelectedActivityIds([]);
            setSelectedTime('');
        } catch (err) {
            const msg = err.response?.data?.message || 'Erro ao criar agendamento. Tente novamente.';
            err.response?.status === 409 ? toast.warn(msg) : toast.error(msg);
        } finally {
            setSubmitting(false);
        }
    };

    // ── Estilos inline remanescentes (estrutura do formulário) ─────────────
    const inputStyle = {
        width: '100%', background: 'rgba(255,255,255,0.07)', border: '1px solid rgba(255,255,255,0.15)',
        borderRadius: 8, padding: '10px 14px', color: '#fff', fontSize: 15,
        boxSizing: 'border-box', outline: 'none',
    };
    const labelStyle = { display: 'block', marginBottom: 6, fontSize: 13, color: 'rgba(255,255,255,0.6)' };
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

    if (loading) return <div className={styles.loadingContainer}>Carregando...</div>;

    const hasLinkedBarbershop = Boolean(barber?.barbershopId);

    return (
        <div className={`${styles.pageContainer} ${hasLinkedBarbershop ? styles.withNavbar : styles.withoutNavbar}`}>
            <div className={styles.contentWrapper}>
                <BarberHeader
                    barber={barber}
                    onLogout={handleLogout}
                    activeTab="novo-agendamento"
                    isOwner={barber?.isOwner === true}
                    barbershopId={barber?.barbershopId}
                    onTabChange={handleTabChange}
                />

                <section className={styles.heroSection}>
                    <p className={styles.heroKicker}>NOVO AGENDAMENTO</p>
                    <h1>Encaixe</h1>
                    <p>Registre um atendimento presencial sem precisar que o cliente tenha conta no app.</p>
                </section>

                <section className={styles.dashboardSection}>
                    <form onSubmit={handleSubmit} style={{ maxWidth: 700 }}>

                        {/* ── Dados do cliente ── */}
                        <div style={cardStyle}>
                            <h3 style={{ margin: '0 0 16px', fontSize: 16, color: 'rgba(255,255,255,0.8)' }}>
                                👤 Dados do cliente
                            </h3>
                            <div style={{ marginBottom: 14 }}>
                                <label style={labelStyle}>Nome *</label>
                                <input
                                    style={inputStyle} type="text" placeholder="Ex: João Silva"
                                    value={clientName} onChange={e => setClientName(e.target.value)}
                                    maxLength={70} required
                                />
                            </div>
                            <div>
                                <label style={labelStyle}>Telefone (opcional)</label>
                                <input
                                    style={inputStyle} type="tel" placeholder="Ex: 11999999999"
                                    value={clientPhone}
                                    onChange={e => setClientPhone(e.target.value.replace(/\D/g, '').slice(0, 11))}
                                    maxLength={11}
                                />
                            </div>
                        </div>

                        {/* ── Serviços ── */}
                        <div style={cardStyle}>
                            <h3 style={{ margin: '0 0 16px', fontSize: 16, color: 'rgba(255,255,255,0.8)' }}>
                                ✂️ Serviços
                            </h3>
                            {activities.length === 0 ? (
                                <p style={{ color: 'rgba(255,255,255,0.4)', fontSize: 14 }}>
                                    Nenhum serviço disponível. Verifique se você vinculou suas habilidades.
                                </p>
                            ) : (
                                <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                                    {activities.map(act => {
                                        const isSelected = selectedActivityIds.includes(act.id);
                                        return (
                                            <div key={act.id} style={activityCardStyle(isSelected)}
                                                onClick={() => toggleActivity(act.id)}
                                                role="checkbox" aria-checked={isSelected}
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
                                    <span style={{ color: 'rgba(255,255,255,0.6)' }}>⏱ {summary.totalDuration} min</span>
                                    <span style={{ color: '#ffd700', fontWeight: 600 }}>Total: R$ {summary.totalPrice.toFixed(2)}</span>
                                </div>
                            )}
                        </div>

                        {/* ── Data e horário ── */}
                        {summary.totalDuration > 0 && (
                            <div style={cardStyle}>
                                <h3 style={{ margin: '0 0 16px', fontSize: 16, color: 'rgba(255,255,255,0.8)' }}>
                                    🗓 Data e horário
                                </h3>

                                {isLoadingDates ? (
                                    <p className={bookingStyles.loadingSlots}>Buscando disponibilidade...</p>
                                ) : (
                                    <div className={bookingStyles.dateModule}>

                                        {/* Painel info data selecionada */}
                                        <div className={bookingStyles.dateInfoPanel}>
                                            <p className={bookingStyles.dateInfoKicker}>Data selecionada</p>
                                            <p className={bookingStyles.dateInfoValue}>{selectedDateLabel}</p>
                                            {selectedTime && (
                                                <p className={bookingStyles.dateInfoValue} style={{ marginTop: 8, color: '#c19006' }}>
                                                    ⏰ {selectedTime}
                                                </p>
                                            )}
                                            {!dateOptions.some(o => o.isAvailable) && (
                                                <p className={bookingStyles.dateInfoHint}>
                                                    Nenhum horário disponível nos próximos 14 dias.
                                                </p>
                                            )}
                                        </div>

                                        {/* Cards de data + slots */}
                                        <div>
                                            <div className={bookingStyles.dateRail}>
                                                {dateOptions.map(option => (
                                                    <button
                                                        key={option.key}
                                                        type="button"
                                                        disabled={!option.isAvailable}
                                                        className={
                                                            selectedDate && formatDateToApi(selectedDate) === option.key
                                                                ? bookingStyles.dateChipSelected
                                                                : bookingStyles.dateChip
                                                        }
                                                        onClick={() => handleSelectDate(option)}
                                                    >
                                                        <span className={bookingStyles.dateChipLabel}>{option.label}</span>
                                                        <span className={bookingStyles.dateChipValue}>{option.compact}</span>
                                                        <span className={bookingStyles.dateChipMeta}>
                                                            {option.isAvailable ? `${option.slots.length} horário(s)` : 'Indisponível'}
                                                        </span>
                                                    </button>
                                                ))}
                                            </div>

                                            {/* Slots do dia selecionado */}
                                            {selectedDate && currentSlots.length > 0 && (
                                                <div className={bookingStyles.slotsPeriods} style={{ marginTop: 14 }}>
                                                    {[
                                                        { key: 'morning', label: '🌅 Manhã', slots: groupedSlots.morning },
                                                        { key: 'afternoon', label: '☀️ Tarde / Noite', slots: groupedSlots.afternoon },
                                                    ].map(({ key, label, slots }) =>
                                                        slots.length > 0 ? (
                                                            <div key={key} className={bookingStyles.periodCard}>
                                                                <button
                                                                    type="button"
                                                                    className={bookingStyles.periodToggle}
                                                                    onClick={() => setExpandedPeriods(p => ({ ...p, [key]: !p[key] }))}
                                                                >
                                                                    <span>{label}</span>
                                                                    <span className={bookingStyles.periodMeta}>
                                                                        {slots.length} horário(s)&nbsp;
                                                                        {expandedPeriods[key] ? <FiChevronUp /> : <FiChevronDown />}
                                                                    </span>
                                                                </button>
                                                                {expandedPeriods[key] && (
                                                                    <div className={bookingStyles.slotsGrid}>
                                                                        {slots.map(slot => (
                                                                            <button
                                                                                key={slot}
                                                                                type="button"
                                                                                className={`${bookingStyles.slotBtn} ${selectedTime === slot ? bookingStyles.slotSelected : ''}`}
                                                                                onClick={() => setSelectedTime(slot)}
                                                                            >
                                                                                {slot}
                                                                            </button>
                                                                        ))}
                                                                    </div>
                                                                )}
                                                            </div>
                                                        ) : null
                                                    )}
                                                </div>
                                            )}

                                            {selectedDate && currentSlots.length === 0 && (
                                                <p className={bookingStyles.noSlots} style={{ marginTop: 12 }}>
                                                    Nenhum horário disponível para este dia.
                                                </p>
                                            )}
                                        </div>
                                    </div>
                                )}
                            </div>
                        )}

                        {/* ── Botão ── */}
                        <button
                            type="submit"
                            disabled={submitting}
                            style={{
                                width: '100%', padding: '14px 0',
                                background: submitting ? 'rgba(255,215,0,0.4)' : '#ffd700',
                                color: '#111', fontWeight: 700, fontSize: 16,
                                border: 'none', borderRadius: 10,
                                cursor: submitting ? 'not-allowed' : 'pointer',
                                transition: 'background 0.2s',
                            }}
                        >
                            {submitting ? 'Registrando...' : 'Registrar atendimento'}
                        </button>
                    </form>
                </section>
            </div>

            {hasLinkedBarbershop && (
                <BarberNavbar
                    activeTab="novo-agendamento"
                    onTabChange={handleTabChange}
                    isOwner={isOwnerUser()}
                    barbershopId={barber?.barbershopId}
                />
            )}
        </div>
    );
}

export default BarberManualBookingPage;
