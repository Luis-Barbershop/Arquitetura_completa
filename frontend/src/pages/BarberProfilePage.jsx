import React, { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import api from '../services/api';
import { getMyInvites, acceptInvite, rejectInvite, getMyWorkSchedule, saveMyWorkSchedule } from '../services/barbershopService';
import { logoutUser } from '../services/authService';
import { isCustomer } from '../services/userContext';
import BarberHeader from '../components/BarberPage/BarberHeader';
import BarberNavbar from '../components/BarberPage/BarberNavbar';
import styles from './CSS/BarberHomePage.module.css';

const DAYS_OF_WEEK = [
    { key: 'MONDAY',    label: 'Seg' },
    { key: 'TUESDAY',   label: 'Ter' },
    { key: 'WEDNESDAY', label: 'Qua' },
    { key: 'THURSDAY',  label: 'Qui' },
    { key: 'FRIDAY',    label: 'Sex' },
    { key: 'SATURDAY',  label: 'Sáb' },
    { key: 'SUNDAY',    label: 'Dom' },
];

const EMPTY_BLOCK = { startTime: '', endTime: '' };

/* ── Componente seletor de horário HH:MM ────────────────────────────────── */
function TimePicker({ value, onChange, disabled }) {
    const safeValue = value && value.length >= 5 ? value.substring(0, 5) : '';
    return (
        <div style={{ display: 'flex', alignItems: 'center', gap: 4, background: 'rgba(255,255,255,0.04)', borderRadius: 10, padding: '6px 8px', border: '1px solid #2a2a2a' }}>
            <input
                type="time"
                step={60}
                value={safeValue}
                onChange={(e) => onChange(e.target.value)}
                disabled={disabled}
                style={timeInputStyle(disabled)}
            />
        </div>
    );
}

/**
 * Página de Perfil do Barbeiro — exibe e permite editar dados pessoais e horário de trabalho.
 * Disponível para: Barbeiro colaborador, Owner e qualquer barbeiro (com ou sem barbearia).
 */
function BarberProfilePage() {
    const navigate = useNavigate();
    const [barber, setBarber] = useState(null);
    const [loading, setLoading] = useState(true);

    // ── actAsBarber toggle ─────────────────────────────────────────────────────
    const [actAsBarber, setActAsBarber] = useState(true);
    const [savingActAsBarber, setSavingActAsBarber] = useState(false);

    // ── Horário de trabalho (multi-bloco por dia) ──────────────────────────────
    // weekSchedule: { MONDAY: [{startTime:'09:00', endTime:'12:00'}, ...], ... }
    const [weekSchedule, setWeekSchedule] = useState({});
    const [savingSchedule, setSavingSchedule] = useState(false);
    const [loadingSchedule, setLoadingSchedule] = useState(false);

    // ── Copiar horário de um dia para outros ───────────────────────────────────
    const [copySource, setCopySource] = useState(null);
    const [copyTargets, setCopyTargets] = useState([]);
    const [isCopyModalOpen, setIsCopyModalOpen] = useState(false);

    // ── Convites pendentes (barbeiro sem barbearia) ────────────────────────────
    const [pendingInvites, setPendingInvites] = useState([]);
    const [loadingInvites, setLoadingInvites] = useState(false);
    const [inviteActionLoading, setInviteActionLoading] = useState(null);

    useEffect(() => {
        if (isCustomer()) { navigate('/homepage', { replace: true }); return; }
        const token = localStorage.getItem('token');
        if (!token) { navigate('/', { replace: true }); return; }

        api.get('/auth/me')
            .then(res => {
                const data = res.data;
                setBarber(data);
                setActAsBarber(data?.actAsBarber ?? true);
                setLoading(false);
            })
            .catch(() => { setLoading(false); navigate('/'); });
    }, [navigate]);

    // ── Carrega a grade de horários multi-bloco ────────────────────────────────
    useEffect(() => {
        if (!barber) return;
        setLoadingSchedule(true);
        getMyWorkSchedule()
            .then(data => {
                // data = [{ dayOfWeek: 'MONDAY', blocks: [{ startTime: '09:00', endTime: '12:00' }, ...] }, ...]
                const map = {};
                (data || []).forEach(day => {
                    if (day.blocks && day.blocks.length > 0) {
                        map[day.dayOfWeek] = day.blocks.map(b => ({
                            startTime: b.startTime ? b.startTime.substring(0, 5) : '',
                            endTime:   b.endTime   ? b.endTime.substring(0, 5)   : '',
                        }));
                    }
                });
                setWeekSchedule(map);
            })
            .catch(() => {
                // Fallback: se o endpoint falhar, tenta montar a partir do barber legado
                if (barber.workStartTime && barber.workEndTime) {
                    const start = barber.workStartTime.substring(0, 5);
                    const end   = barber.workEndTime.substring(0, 5);
                    const fallback = {};
                    ['MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY'].forEach(d => {
                        fallback[d] = [{ startTime: start, endTime: end }];
                    });
                    setWeekSchedule(fallback);
                }
            })
            .finally(() => setLoadingSchedule(false));
    }, [barber]);

    // ── Funções auxiliares do schedule ──────────────────────────────────────────
    const toggleDay = useCallback((dayKey) => {
        setWeekSchedule(prev => {
            const copy = { ...prev };
            if (copy[dayKey]) {
                delete copy[dayKey];
            } else {
                copy[dayKey] = [{ ...EMPTY_BLOCK }];
            }
            return copy;
        });
    }, []);

    const addBlock = useCallback((dayKey) => {
        setWeekSchedule(prev => ({
            ...prev,
            [dayKey]: [...(prev[dayKey] || []), { ...EMPTY_BLOCK }],
        }));
    }, []);

    const removeBlock = useCallback((dayKey, blockIdx) => {
        setWeekSchedule(prev => {
            const blocks = [...(prev[dayKey] || [])];
            blocks.splice(blockIdx, 1);
            if (blocks.length === 0) {
                const copy = { ...prev };
                delete copy[dayKey];
                return copy;
            }
            return { ...prev, [dayKey]: blocks };
        });
    }, []);

    const updateBlock = useCallback((dayKey, blockIdx, field, value) => {
        setWeekSchedule(prev => {
            const blocks = [...(prev[dayKey] || [])];
            blocks[blockIdx] = { ...blocks[blockIdx], [field]: value };
            return { ...prev, [dayKey]: blocks };
        });
    }, []);

    // ── Copiar horário de um dia para outros ───────────────────────────────────
    const handleOpenCopyModal = (dayKey) => {
        setCopySource(dayKey);
        setCopyTargets([]);
        setIsCopyModalOpen(true);
    };
    const handleCopyConfirm = () => {
        if (!copySource || copyTargets.length === 0) return;
        const srcBlocks = weekSchedule[copySource];
        if (!srcBlocks || srcBlocks.length === 0) { toast.warn('O dia de origem não tem blocos.'); return; }
        setWeekSchedule(prev => {
            const copy = { ...prev };
            copyTargets.forEach(target => { copy[target] = srcBlocks.map(b => ({ ...b })); });
            return copy;
        });
        setIsCopyModalOpen(false);
        toast.success(`Horário copiado para ${copyTargets.length} dia(s)!`);
    };
    const toggleCopyTarget = (dayKey) => {
        setCopyTargets(prev => prev.includes(dayKey) ? prev.filter(k => k !== dayKey) : [...prev, dayKey]);
    };

    // ── Carrega convites pendentes quando barbeiro não está vinculado ───────
    useEffect(() => {
        if (!barber || barber.barbershopId) return;
        setLoadingInvites(true);
        getMyInvites()
            .then(data => setPendingInvites(data))
            .catch(() => setPendingInvites([]))
            .finally(() => setLoadingInvites(false));
    }, [barber]);

    const refreshBarberProfileAfterInvite = async () => {
        const maxAttempts = 5;
        for (let attempt = 1; attempt <= maxAttempts; attempt += 1) {
            const res = await api.get('/auth/me', { params: { t: Date.now() } });
            const data = res.data;
            setBarber(data);

            if (data?.barbershopId) {
                localStorage.setItem('barbershopId', String(data.barbershopId));
                return true;
            }

            if (attempt < maxAttempts) {
                await new Promise(resolve => setTimeout(resolve, 350));
            }
        }

        return false;
    };

    const handleAcceptInvite = async (requestId) => {
        setInviteActionLoading(requestId);
        try {
            await acceptInvite(requestId);
            toast.success('Convite aceito! Você foi vinculado à barbearia.');
            setPendingInvites(prev => prev.filter(inv => inv.requestId !== requestId));
            const linked = await refreshBarberProfileAfterInvite();

            if (!linked) {
                toast.warn('Convite aceito, mas o vínculo ainda está sincronizando. Atualize a página em alguns segundos.');
            }
        } catch (err) {
            toast.error(err?.response?.data?.message || 'Erro ao aceitar convite.');
        } finally {
            setInviteActionLoading(null);
        }
    };

    const handleRejectInvite = async (requestId) => {
        setInviteActionLoading(requestId);
        try {
            await rejectInvite(requestId);
            toast.info('Convite recusado.');
            setPendingInvites(prev => prev.filter(inv => inv.requestId !== requestId));
        } catch (err) {
            toast.error(err?.response?.data?.message || 'Erro ao recusar convite.');
        } finally {
            setInviteActionLoading(null);
        }
    };

    const handleLogout = async () => {
        await logoutUser();
        navigate('/');
    };

    // ── actAsBarber ────────────────────────────────────────────────────────────
    const handleActAsBarberToggle = async (newValue) => {
        setSavingActAsBarber(true);
        try {
            await api.put(`/barbers/${barber.id}`, { actAsBarber: newValue });
            setActAsBarber(newValue);
            toast.success(
                newValue
                    ? 'Você voltou a aparecer como barbeiro nos agendamentos.'
                    : 'Você não aparecerá como barbeiro para novos agendamentos.'
            );
        } catch {
            toast.error('Erro ao salvar configuração. Tente novamente.');
        } finally {
            setSavingActAsBarber(false);
        }
    };

    // ── Salvar horário de trabalho (multi-bloco) ─────────────────────────────
    const handleSaveSchedule = async (e) => {
        e.preventDefault();

        // Validação: pelo menos um dia selecionado
        const activeDays = Object.keys(weekSchedule);
        if (activeDays.length === 0) {
            toast.warn('Selecione pelo menos um dia de trabalho.');
            return;
        }

        // Validação: cada bloco deve ter início e fim, e início < fim
        for (const dayKey of activeDays) {
            const blocks = weekSchedule[dayKey];
            const dayLabel = DAYS_OF_WEEK.find(d => d.key === dayKey)?.label || dayKey;
            for (let i = 0; i < blocks.length; i++) {
                const b = blocks[i];
                if (!b.startTime || !b.endTime) {
                    toast.warn(`${dayLabel} — Bloco ${i + 1}: preencha início e fim.`);
                    return;
                }
                if (b.startTime >= b.endTime) {
                    toast.warn(`${dayLabel} — Bloco ${i + 1}: o início deve ser anterior ao fim.`);
                    return;
                }
            }
            // Validação: blocos não podem se sobrepor
            const sorted = [...blocks].sort((a, b) => a.startTime.localeCompare(b.startTime));
            for (let i = 1; i < sorted.length; i++) {
                if (sorted[i].startTime < sorted[i - 1].endTime) {
                    toast.warn(`${dayLabel}: os blocos de horário não podem se sobrepor.`);
                    return;
                }
            }
        }

        setSavingSchedule(true);
        try {
            // Monta payload: { schedule: [{ dayOfWeek: 'MONDAY', blocks: [...] }, ...] }
            const schedule = activeDays.map(dayKey => ({
                dayOfWeek: dayKey,
                blocks: weekSchedule[dayKey],
            }));
            await saveMyWorkSchedule({ schedule });
            toast.success('Horário de trabalho salvo com sucesso!');
        } catch {
            toast.error('Erro ao salvar horário. Tente novamente.');
        } finally {
            setSavingSchedule(false);
        }
    };

    const hasLinkedBarbershop = !!barber?.barbershopId;
    const phoneValue = barber?.tell || barber?.phone || barber?.phoneNumber || '—';
    const cpfValue = barber?.documentCPF || barber?.documentCpf || barber?.cpf || '—';

    const handleTabChange = (tab) => {
        if (tab === 'home')       navigate('/barberHome');
        else if (tab === 'agenda')    navigate('/meus-agendamentos');
        else if (tab === 'servicos')  navigate('/barberHome/servicos');
        else if (tab === 'estoque')   navigate('/barberHome/estoque');
        else if (tab === 'time')      navigate('/barberHome/time');
        else if (tab === 'dashboards') navigate('/barberHome/dashboard');
        else if (tab === 'novo-agendamento') navigate('/barberHome/novo-agendamento');
    };

    if (loading) return <div className={styles.loadingContainer}>Carregando perfil...</div>;

    return (
        <div className={`${styles.pageContainer} ${hasLinkedBarbershop ? styles.withNavbar : styles.withoutNavbar}`}>
            <div className={styles.contentWrapper}>
                <BarberHeader
                    barber={barber}
                    onLogout={handleLogout}
                    activeTab="perfil"
                    isOwner={barber?.isOwner === true}
                    barbershopId={barber?.barbershopId}
                    onTabChange={handleTabChange}
                />

                <section className={styles.heroSection}>
                    <p className={styles.heroKicker}>MEU PERFIL</p>
                    <h1>Suas informações</h1>
                </section>

                <section className={styles.dashboardSection}>
                    {barber && (
                        <div style={{ display: 'flex', flexDirection: 'column', gap: 20, maxWidth: 600 }}>

                            {/* ── Dados pessoais ───────────────────────────────── */}
                            <div style={cardStyle}>
                                {barber.imageUrl && (
                                    <img
                                        src={barber.imageUrl}
                                        alt="Foto de perfil"
                                        style={{ width: 80, height: 80, borderRadius: '50%', objectFit: 'cover', marginBottom: 4 }}
                                    />
                                )}
                                <div><strong>Nome:</strong> {barber.name}</div>
                                <div><strong>E-mail:</strong> {barber.email}</div>
                                <div><strong>Telefone:</strong> {phoneValue}</div>
                                <div><strong>CPF:</strong> {cpfValue}</div>
                                <div><strong>Barbearia:</strong> {barber.barbershopId ? 'Vinculado' : 'Sem barbearia'}</div>
                                <div>
                                    <strong>Função:</strong>{' '}
                                    {barber.isOwner ? 'Dono do estabelecimento' : 'Colaborador'}
                                </div>
                            </div>

                            {/* ── Convites pendentes (barbeiro sem barbearia) ─ */}
                            {!barber.barbershopId && (
                                <div style={{ ...cardStyle, background: 'rgba(108,99,255,0.06)', borderColor: '#3a3570' }}>
                                    <p style={{ ...sectionTitleStyle, color: '#6c63ff' }}>📩 Convites de Barbearias</p>
                                    <p style={{ fontSize: 13, color: 'rgba(255,255,255,0.45)', marginTop: -6, marginBottom: 8 }}>
                                        Quando um dono de barbearia convida você, o convite aparece aqui.
                                    </p>
                                    {loadingInvites ? (
                                        <p style={{ fontSize: 13, color: 'rgba(255,255,255,0.3)' }}>Carregando convites...</p>
                                    ) : pendingInvites.length === 0 ? (
                                        <p style={{ fontSize: 13, color: 'rgba(255,255,255,0.3)' }}>Nenhum convite pendente no momento.</p>
                                    ) : (
                                        pendingInvites.map(inv => (
                                            <div key={inv.requestId} style={{
                                                display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                                                background: 'rgba(255,255,255,0.04)', borderRadius: 10, padding: '12px 14px',
                                                marginBottom: 8, gap: 10
                                            }}>
                                                <div>
                                                    <p style={{ margin: 0, fontWeight: 600, fontSize: 14 }}>
                                                        {inv.barbershopName || 'Barbearia'}
                                                    </p>
                                                    <p style={{ margin: 0, fontSize: 12, color: 'rgba(255,255,255,0.45)' }}>
                                                        Convite recebido
                                                    </p>
                                                </div>
                                                <div style={{ display: 'flex', gap: 8 }}>
                                                    <button
                                                        onClick={() => handleAcceptInvite(inv.requestId)}
                                                        disabled={inviteActionLoading === inv.requestId}
                                                        style={{
                                                            background: '#276749', color: '#fff', border: 'none',
                                                            padding: '7px 16px', borderRadius: 8, cursor: 'pointer',
                                                            fontSize: 13, fontWeight: 600
                                                        }}
                                                    >
                                                        {inviteActionLoading === inv.requestId ? '...' : 'Aceitar'}
                                                    </button>
                                                    <button
                                                        onClick={() => handleRejectInvite(inv.requestId)}
                                                        disabled={inviteActionLoading === inv.requestId}
                                                        style={{
                                                            background: '#742a2a', color: '#fff', border: 'none',
                                                            padding: '7px 16px', borderRadius: 8, cursor: 'pointer',
                                                            fontSize: 13, fontWeight: 600
                                                        }}
                                                    >
                                                        {inviteActionLoading === inv.requestId ? '...' : 'Recusar'}
                                                    </button>
                                                </div>
                                            </div>
                                        ))
                                    )}
                                </div>
                            )}

                            {/* ── Horário de trabalho (multi-bloco) ──────── */}
                            <div style={cardStyle}>
                                <p style={sectionTitleStyle}>🕐 Horário de Trabalho</p>
                                <p style={{ fontSize: 13, color: 'rgba(255,255,255,0.45)', marginTop: -6, marginBottom: 2 }}>
                                    Selecione os dias e adicione blocos de horário. Ex.: 9h–12h e 13h–18h.
                                </p>

                                {loadingSchedule ? (
                                    <p style={{ fontSize: 13, color: 'rgba(255,255,255,0.3)' }}>Carregando horários...</p>
                                ) : (
                                    <form onSubmit={handleSaveSchedule} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>

                                        {/* Seletores de dia */}
                                        <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                                            <span style={labelTextStyle}>Dias de trabalho</span>
                                            <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                                                {DAYS_OF_WEEK.map(({ key, label }) => {
                                                    const active = !!weekSchedule[key];
                                                    return (
                                                        <button
                                                            key={key}
                                                            type="button"
                                                            onClick={() => toggleDay(key)}
                                                            style={{
                                                                width: 44, height: 44, borderRadius: 10,
                                                                border: active ? '1px solid #d4af37' : '1px solid #2f2f2f',
                                                                background: active ? 'rgba(212,175,55,0.18)' : 'rgba(255,255,255,0.03)',
                                                                color: active ? '#d4af37' : 'rgba(255,255,255,0.45)',
                                                                fontWeight: active ? 700 : 500,
                                                                fontSize: 12, cursor: 'pointer', transition: 'all 0.2s',
                                                            }}
                                                        >
                                                            {label}
                                                        </button>
                                                    );
                                                })}
                                            </div>
                                        </div>

                                        {/* Blocos por dia selecionado */}
                                        {DAYS_OF_WEEK.filter(({ key }) => !!weekSchedule[key]).map(({ key, label }) => (
                                            <div key={key} style={dayBlockContainerStyle}>
                                                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 6 }}>
                                                    <span style={{ fontSize: 13, fontWeight: 600, color: '#d4af37' }}>
                                                        {label}
                                                    </span>
                                                    <div style={{ display: 'flex', gap: 6 }}>
                                                        <button type="button" onClick={() => handleOpenCopyModal(key)}
                                                            style={{ ...addBlockBtnStyle, color: '#9b8ce6', borderColor: 'rgba(155,140,230,0.45)' }}
                                                            title="Copiar este horário para outros dias">
                                                            📋 Copiar
                                                        </button>
                                                        <button type="button" onClick={() => addBlock(key)} style={addBlockBtnStyle} title="Adicionar bloco de horário">
                                                            + Bloco
                                                        </button>
                                                    </div>
                                                </div>

                                                {(weekSchedule[key] || []).map((block, idx) => (
                                                    <div key={idx} style={{ ...blockRowStyle, flexWrap: 'wrap' }}>
                                                        <TimePicker value={block.startTime} onChange={v => updateBlock(key, idx, 'startTime', v)} disabled={savingSchedule} />
                                                        <span style={{ color: 'rgba(255,255,255,0.35)', fontSize: 13, userSelect: 'none' }}>até</span>
                                                        <TimePicker value={block.endTime} onChange={v => updateBlock(key, idx, 'endTime', v)} disabled={savingSchedule} />
                                                        {(weekSchedule[key] || []).length > 1 && (
                                                            <button type="button" onClick={() => removeBlock(key, idx)} style={removeBlockBtnStyle} title="Remover bloco">✕</button>
                                                        )}
                                                    </div>
                                                ))}
                                            </div>
                                        ))}

                                        {/* Resumo visual */}
                                        {Object.keys(weekSchedule).length > 0 && (
                                            <div style={scheduleSummaryStyle}>
                                                <span style={{ fontSize: 12, fontWeight: 600, color: 'rgba(255,255,255,0.55)', marginBottom: 4 }}>
                                                    Resumo
                                                </span>
                                                {DAYS_OF_WEEK.filter(({ key }) => !!weekSchedule[key]).map(({ key, label }) => (
                                                    <div key={key} style={{ fontSize: 12, color: 'rgba(255,255,255,0.5)', display: 'flex', gap: 6 }}>
                                                        <strong style={{ color: '#d4af37', minWidth: 28 }}>{label}:</strong>
                                                        <span>
                                                            {(weekSchedule[key] || []).map((b, i) => (
                                                                <span key={i}>
                                                                    {b.startTime || '??'}–{b.endTime || '??'}
                                                                    {i < weekSchedule[key].length - 1 ? '  /  ' : ''}
                                                                </span>
                                                            ))}
                                                        </span>
                                                    </div>
                                                ))}
                                            </div>
                                        )}

                                        <button
                                            type="submit"
                                            disabled={savingSchedule}
                                            style={saveButtonStyle(savingSchedule)}
                                        >
                                            {savingSchedule ? 'Salvando...' : 'Salvar horário'}
                                        </button>
                                    </form>
                                )}
                            </div>

                            {/* ── Configurações do Owner (actAsBarber) ─────────── */}
                            {barber.isOwner && (
                                <div style={{ ...cardStyle, background: 'rgba(193,144,6,0.08)', borderColor: '#4a3a17' }}>
                                    <p style={{ ...sectionTitleStyle, color: '#d4af37' }}>⚙️ Configurações da Barbearia</p>
                                    <label style={{
                                        display: 'flex', alignItems: 'flex-start', gap: 10,
                                        cursor: savingActAsBarber ? 'not-allowed' : 'pointer',
                                        opacity: savingActAsBarber ? 0.6 : 1
                                    }}>
                                        <input
                                            type="checkbox"
                                            checked={actAsBarber}
                                            disabled={savingActAsBarber}
                                            onChange={e => handleActAsBarberToggle(e.target.checked)}
                                            style={{ width: 18, height: 18, accentColor: '#d4af37', cursor: 'inherit', marginTop: 2, flexShrink: 0 }}
                                        />
                                        <span style={{ fontSize: 13, lineHeight: 1.5 }}>
                                            <strong>Atuar como barbeiro</strong> — aparecer na lista de profissionais disponíveis para agendamento
                                        </span>
                                    </label>
                                </div>
                            )}

                        </div>
                    )}
                </section>
            </div>

            {hasLinkedBarbershop && (
                <BarberNavbar
                    activeTab="perfil"
                    onTabChange={handleTabChange}
                    isOwner={barber?.isOwner === true}
                    barbershopId={barber?.barbershopId}
                />
            )}

            {/* ── Modal copiar horário ──────────────────────────────────── */}
            {isCopyModalOpen && (
                <div style={modalBackdropStyle} onClick={() => setIsCopyModalOpen(false)}>
                    <div style={modalCardStyle} onClick={e => e.stopPropagation()}>
                        <p style={{ fontWeight: 700, fontSize: 15, marginBottom: 4 }}>📋 Copiar horário</p>
                        <p style={{ fontSize: 13, color: 'rgba(255,255,255,0.5)', marginBottom: 12 }}>
                            Copiar de <strong style={{ color: '#d4af37' }}>{DAYS_OF_WEEK.find(d => d.key === copySource)?.label}</strong> para:
                        </p>
                        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: 16 }}>
                            {DAYS_OF_WEEK.filter(d => d.key !== copySource).map(({ key, label }) => {
                                const selected = copyTargets.includes(key);
                                return (
                                    <button key={key} type="button" onClick={() => toggleCopyTarget(key)}
                                        style={{
                                            width: 48, height: 40, borderRadius: 10, fontSize: 12, fontWeight: 600,
                                            border: selected ? '1px solid #9b8ce6' : '1px solid #2f2f2f',
                                            background: selected ? 'rgba(155,140,230,0.18)' : 'rgba(255,255,255,0.03)',
                                            color: selected ? '#9b8ce6' : 'rgba(255,255,255,0.45)',
                                            cursor: 'pointer', transition: 'all 0.2s',
                                        }}>
                                        {label}
                                    </button>
                                );
                            })}
                        </div>
                        <div style={{ display: 'flex', gap: 10 }}>
                            <button onClick={() => setIsCopyModalOpen(false)}
                                style={{ flex: 1, padding: '10px 0', borderRadius: 10, border: '1px solid #3a3a3a', background: 'transparent', color: 'rgba(255,255,255,0.6)', cursor: 'pointer', fontSize: 13, fontWeight: 600 }}>
                                Cancelar
                            </button>
                            <button onClick={handleCopyConfirm} disabled={copyTargets.length === 0}
                                style={{ flex: 1, padding: '10px 0', borderRadius: 10, border: 'none', background: copyTargets.length === 0 ? 'rgba(155,140,230,0.2)' : '#9b8ce6', color: copyTargets.length === 0 ? 'rgba(255,255,255,0.3)' : '#fff', cursor: copyTargets.length === 0 ? 'not-allowed' : 'pointer', fontSize: 13, fontWeight: 700 }}>
                                Copiar ({copyTargets.length})
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}

// ── Estilos inline compartilhados ──────────────────────────────────────────────
const timeInputStyle = (disabled) => ({
    width: 124,
    height: 34,
    borderRadius: 8,
    border: '1px solid #3a3a3a',
    background: disabled ? 'rgba(255,255,255,0.03)' : 'rgba(255,255,255,0.08)',
    color: disabled ? 'rgba(255,255,255,0.3)' : '#fff',
    fontSize: 13,
    fontWeight: 700,
    textAlign: 'center',
    outline: 'none',
    padding: '0 8px',
});

const cardStyle = {
    background: 'rgba(255,255,255,0.04)',
    border: '1px solid #2f2f2f',
    borderRadius: 16,
    padding: 24,
    display: 'flex',
    flexDirection: 'column',
    gap: 12,
};

const sectionTitleStyle = {
    fontWeight: 700,
    fontSize: 14,
    marginBottom: 4,
    color: 'rgba(255,255,255,0.85)',
};

const labelTextStyle = {
    fontSize: 13,
    color: 'rgba(255,255,255,0.6)',
    fontWeight: 500,
};

const dayBlockContainerStyle = {
    background: 'rgba(255,255,255,0.03)',
    border: '1px solid #2a2a2a',
    borderRadius: 12,
    padding: '12px 14px',
};

const blockRowStyle = {
    display: 'flex',
    alignItems: 'center',
    gap: 8,
    marginBottom: 6,
};

const addBlockBtnStyle = {
    background: 'none',
    border: '1px dashed rgba(212,175,55,0.45)',
    borderRadius: 8,
    color: '#d4af37',
    fontSize: 11,
    fontWeight: 600,
    padding: '4px 10px',
    cursor: 'pointer',
    transition: 'border-color 0.2s',
};

const removeBlockBtnStyle = {
    background: 'rgba(116,42,42,0.35)',
    border: 'none',
    borderRadius: 6,
    color: '#ff8888',
    fontSize: 13,
    fontWeight: 700,
    width: 28,
    height: 28,
    cursor: 'pointer',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    flexShrink: 0,
};

const scheduleSummaryStyle = {
    background: 'rgba(212,175,55,0.06)',
    border: '1px solid rgba(212,175,55,0.15)',
    borderRadius: 10,
    padding: '10px 14px',
    display: 'flex',
    flexDirection: 'column',
    gap: 3,
};

const saveButtonStyle = (disabled) => ({
    alignSelf: 'flex-start',
    padding: '10px 24px',
    borderRadius: 10,
    border: 'none',
    background: disabled ? 'rgba(212,175,55,0.25)' : '#d4af37',
    color: disabled ? 'rgba(255,255,255,0.35)' : '#1a1a1a',
    fontWeight: 700,
    fontSize: 14,
    cursor: disabled ? 'not-allowed' : 'pointer',
    transition: 'background 0.2s',
});

const modalBackdropStyle = {
    position: 'fixed', top: 0, left: 0, width: '100vw', height: '100vh',
    background: 'rgba(0,0,0,0.6)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000,
};
const modalCardStyle = {
    background: '#1a1a2e', borderRadius: 16, padding: 24, maxWidth: 380, width: '90%',
    border: '1px solid #2f2f2f', color: '#fff',
};

export default BarberProfilePage;
