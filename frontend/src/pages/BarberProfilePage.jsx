import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import api from '../services/api';
import { logoutUser } from '../services/authService';
import { isCustomer } from '../services/userContext';
import BarberHeader from '../components/BarberPage/BarberHeader';
import BarberNavbar from '../components/BarberPage/BarberNavbar';
import styles from './CSS/BarberHomePage.module.css';

const DAYS_OF_WEEK = [
    { key: 'MON', label: 'Seg' },
    { key: 'TUE', label: 'Ter' },
    { key: 'WED', label: 'Qua' },
    { key: 'THU', label: 'Qui' },
    { key: 'FRI', label: 'Sex' },
    { key: 'SAT', label: 'Sáb' },
    { key: 'SUN', label: 'Dom' },
];

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

    // ── Formulário de horário de trabalho ──────────────────────────────────────
    const [workStartTime, setWorkStartTime] = useState('');
    const [workEndTime, setWorkEndTime]     = useState('');
    const [selectedDays, setSelectedDays]   = useState(['MON','TUE','WED','THU','FRI']);
    const [savingSchedule, setSavingSchedule] = useState(false);

    useEffect(() => {
        if (isCustomer()) { navigate('/homepage', { replace: true }); return; }
        const token = localStorage.getItem('token');
        if (!token) { navigate('/', { replace: true }); return; }

        api.get('/auth/me')
            .then(res => {
                const data = res.data;
                setBarber(data);
                setActAsBarber(data?.actAsBarber ?? true);
                // Horários vêm como "HH:mm:ss" do backend — normaliza para "HH:mm"
                setWorkStartTime(data?.workStartTime ? data.workStartTime.substring(0, 5) : '');
                setWorkEndTime(data?.workEndTime   ? data.workEndTime.substring(0, 5)   : '');
                setLoading(false);
            })
            .catch(() => { setLoading(false); navigate('/'); });
    }, [navigate]);

    const toggleDay = (key) => {
        setSelectedDays(prev =>
            prev.includes(key) ? prev.filter(d => d !== key) : [...prev, key]
        );
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

    // ── Salvar horário de trabalho ─────────────────────────────────────────────
    const handleSaveSchedule = async (e) => {
        e.preventDefault();
        if (!workStartTime || !workEndTime) {
            toast.warn('Informe os dois horários antes de salvar.');
            return;
        }
        if (workStartTime >= workEndTime) {
            toast.warn('O horário de início deve ser anterior ao horário de término.');
            return;
        }
        setSavingSchedule(true);
        try {
            const updated = await api.put(`/barbers/${barber.id}`, {
                workStartTime,
                workEndTime,
            });
            // Atualiza state local com os dados retornados
            setBarber(prev => ({ ...prev, ...updated.data }));
            toast.success('Horário de trabalho salvo com sucesso!');
        } catch {
            toast.error('Erro ao salvar horário. Tente novamente.');
        } finally {
            setSavingSchedule(false);
        }
    };

    const hasLinkedBarbershop = !!barber?.barbershopId;

    const handleTabChange = (tab) => {
        if (tab === 'home')       navigate('/barberHome');
        else if (tab === 'agenda')    navigate('/meus-agendamentos');
        else if (tab === 'servicos')  navigate('/barberHome/servicos');
        else if (tab === 'estoque')   navigate('/barberHome/estoque');
        else if (tab === 'time')      navigate('/barberHome/time');
        else if (tab === 'dashboards') navigate('/barberHome/dashboard');
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
                                <div><strong>Telefone:</strong> {barber.tell || '—'}</div>
                                <div><strong>CPF:</strong> {barber.documentCPF || '—'}</div>
                                <div>
                                    <strong>Barbearia:</strong>{' '}
                                    {barber.barbershopId ? `Vinculado (ID: ${barber.barbershopId})` : 'Sem barbearia'}
                                </div>
                                <div>
                                    <strong>Função:</strong>{' '}
                                    {barber.isOwner ? 'Dono do estabelecimento' : 'Colaborador'}
                                </div>
                                <p style={{ fontSize: 12, color: 'rgba(255,255,255,0.4)', marginTop: 4 }}>
                                    Para atualizar sua foto, use o botão de upload nas configurações.
                                </p>
                            </div>

                            {/* ── Horário de trabalho ──────────────────────────── */}
                            <div style={cardStyle}>
                                <p style={sectionTitleStyle}>🕐 Horário de Trabalho</p>
                                <p style={{ fontSize: 13, color: 'rgba(255,255,255,0.45)', marginTop: -6, marginBottom: 2 }}>
                                    Defina seu expediente — disponível para owner, colaborador e barbeiros sem barbearia.
                                </p>

                                <form onSubmit={handleSaveSchedule} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>

                                    {/* Dias da semana */}
                                    <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                                        <span style={labelTextStyle}>Dias de trabalho</span>
                                        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                                            {DAYS_OF_WEEK.map(({ key, label }) => {
                                                const active = selectedDays.includes(key);
                                                return (
                                                    <button
                                                        key={key}
                                                        type="button"
                                                        onClick={() => toggleDay(key)}
                                                        style={{
                                                            width: 44,
                                                            height: 44,
                                                            borderRadius: 10,
                                                            border: active ? '1px solid #d4af37' : '1px solid #2f2f2f',
                                                            background: active ? 'rgba(212,175,55,0.18)' : 'rgba(255,255,255,0.03)',
                                                            color: active ? '#d4af37' : 'rgba(255,255,255,0.45)',
                                                            fontWeight: active ? 700 : 500,
                                                            fontSize: 12,
                                                            cursor: 'pointer',
                                                            transition: 'all 0.2s',
                                                        }}
                                                    >
                                                        {label}
                                                    </button>
                                                );
                                            })}
                                        </div>
                                    </div>

                                    {/* Horários */}
                                    <div style={{ display: 'flex', gap: 16, flexWrap: 'wrap' }}>
                                        <label style={labelStyle}>
                                            <span style={labelTextStyle}>Início do expediente</span>
                                            <input
                                                type="time"
                                                value={workStartTime}
                                                onChange={e => setWorkStartTime(e.target.value)}
                                                disabled={savingSchedule}
                                                style={inputStyle(savingSchedule)}
                                                required
                                            />
                                        </label>
                                        <label style={labelStyle}>
                                            <span style={labelTextStyle}>Fim do expediente</span>
                                            <input
                                                type="time"
                                                value={workEndTime}
                                                onChange={e => setWorkEndTime(e.target.value)}
                                                disabled={savingSchedule}
                                                style={inputStyle(savingSchedule)}
                                                required
                                            />
                                        </label>
                                    </div>

                                    {/* Resumo visual do horário atual salvo */}
                                    {(barber.workStartTime || barber.workEndTime) && (
                                        <p style={{ fontSize: 12, color: 'rgba(255,255,255,0.45)', marginTop: -6 }}>
                                            Horário atual salvo:{' '}
                                            <strong style={{ color: 'rgba(255,255,255,0.7)' }}>
                                                {barber.workStartTime
                                                    ? String(barber.workStartTime).substring(0, 5)
                                                    : '—'}
                                                {' às '}
                                                {barber.workEndTime
                                                    ? String(barber.workEndTime).substring(0, 5)
                                                    : '—'}
                                            </strong>
                                        </p>
                                    )}

                                    <button
                                        type="submit"
                                        disabled={savingSchedule}
                                        style={saveButtonStyle(savingSchedule)}
                                    >
                                        {savingSchedule ? 'Salvando...' : 'Salvar horário'}
                                    </button>
                                </form>
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
        </div>
    );
}

// ── Estilos inline compartilhados ──────────────────────────────────────────────
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

const labelStyle = {
    display: 'flex',
    flexDirection: 'column',
    gap: 6,
    flex: 1,
    minWidth: 140,
};

const labelTextStyle = {
    fontSize: 13,
    color: 'rgba(255,255,255,0.6)',
    fontWeight: 500,
};

const inputStyle = (disabled) => ({
    padding: '9px 12px',
    borderRadius: 8,
    border: `1px solid ${disabled ? '#2a2a2a' : '#3a3a3a'}`,
    background: disabled ? 'rgba(255,255,255,0.03)' : 'rgba(255,255,255,0.06)',
    color: disabled ? 'rgba(255,255,255,0.3)' : '#fff',
    fontSize: 14,
    outline: 'none',
    cursor: disabled ? 'not-allowed' : 'default',
    colorScheme: 'dark',
});

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

export default BarberProfilePage;
