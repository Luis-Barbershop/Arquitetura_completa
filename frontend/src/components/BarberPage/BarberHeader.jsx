import React, { useRef, useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import {
    House,
    CalendarBlank,
    CalendarX,
    PlusCircle,
    Scissors,
    Users,
    ChartBar,
    Package,
    CaretDown,
    UserCircle,
    Lock,
    CreditCard,
    SignOut,
} from '@phosphor-icons/react';
import cortaAiLogo from '/CortaAiLogo.png';
import api from '../../services/api';
import NotificationBell from '../NotificationBell/NotificationBell';
import { isOwnerUser, getBarbershopId, getHomeRouteByRole } from '../../services/userContext';
import styles from './CSS/BarberHeader.module.css';

/**
 * BarberHeader — barra superior desktop (oculto em mobile, ver BarberNavbar)
 *
 * isOwner e barbershopId são lidos do localStorage via userContext — fonte única de verdade.
 * Props homônimas são ignoradas para evitar inconsistência entre páginas.
 *
 * Visibilidade por perfil:
 *   sem barbearia  → Home
 *   com barbearia  → + Agenda ▾ (Minha Agenda, Novo Encaixe) + Serviços
 *   owner          → + Meu Time + Gestão ▾ (Dashboard, Estoque)
 *
 * Avatar dropdown (todos):
 *   Meu Perfil | Alterar Senha (e-mail) | Vincular MP (owner) | Sair
 */
function BarberHeader({ barber, onLogout, activeTab, onTabChange }) {
    const navigate = useNavigate();

    // Fonte única de verdade — localStorage via userContext
    const isOwner   = isOwnerUser();
    const barbershopId = getBarbershopId();
    const hasShop   = Boolean(barbershopId);
    const activeBarbershopName = localStorage.getItem('barbershopName') || barber?.barbershopName || '';
    const canChangePassword = (localStorage.getItem('authProvider') || 'EMAIL').toUpperCase() === 'EMAIL';

    // Foto de perfil: usa o prop (mais recente) ou cai para o localStorage como fallback
    const profileImageUrl = barber?.imageUrl || localStorage.getItem('userProfileImage') || null;

    const [agendaOpen, setAgendaOpen] = useState(false);
    const [gestaoOpen, setGestaoOpen] = useState(false);
    const [avatarOpen, setAvatarOpen] = useState(false);
    const [mpStatusLoading, setMpStatusLoading] = useState(false);
    const [mpStatus, setMpStatus] = useState({ linked: false, mpUserIdMasked: null, hasPublicKey: false });

    const agendaRef = useRef(null);
    const gestaoRef = useRef(null);
    const avatarRef = useRef(null);

    useEffect(() => {
        const handler = (e) => {
            if (agendaRef.current && !agendaRef.current.contains(e.target)) setAgendaOpen(false);
            if (gestaoRef.current && !gestaoRef.current.contains(e.target)) setGestaoOpen(false);
            if (avatarRef.current && !avatarRef.current.contains(e.target)) setAvatarOpen(false);
        };
        document.addEventListener('mousedown', handler);
        return () => document.removeEventListener('mousedown', handler);
    }, []);

    useEffect(() => {
        const loadMpStatus = async () => {
            if (!isOwner) {
                setMpStatus({ linked: false, mpUserIdMasked: null, hasPublicKey: false });
                return;
            }
            try {
                setMpStatusLoading(true);
                const response = await api.get('/payments/mp-status');
                const data = response.data || {};
                setMpStatus({
                    linked: Boolean(data.linked),
                    mpUserIdMasked: data.mpUserIdMasked || null,
                    hasPublicKey: Boolean(data.hasPublicKey)
                });
            } catch {
                setMpStatus({ linked: false, mpUserIdMasked: null, hasPublicKey: false });
            } finally {
                setMpStatusLoading(false);
            }
        };

        loadMpStatus();
    }, [isOwner]);

    const agendaSubItems = [
        { id: 'agenda',           label: 'Minha Agenda',     icon: <CalendarBlank size={15} weight="duotone" /> },
        ...(hasShop             ? [
            { id: 'novo-agendamento', label: 'Novo Encaixe', icon: <PlusCircle size={15} weight="duotone" /> },
            { id: 'indisponibilidade', label: 'Indisponibilidade', icon: <CalendarX size={15} weight="duotone" /> },
        ] : []),
    ];

    const agendaActive  = ['agenda', 'novo-agendamento', 'indisponibilidade'].includes(activeTab);
    const gestaoActive  = ['dashboards', 'estoque'].includes(activeTab);

    const initials = barber?.name
        ? barber.name.split(' ').slice(0, 2).map(n => n[0]).join('').toUpperCase()
        : 'BC';

    const handleMpConnect = async () => {
        const barberId = barber?.id;
        if (!barberId) return;
        try {
            const response = await api.get(`/payments/mp-connect?state=${barberId}`);
            const authUrl = response.data?.authorizationUrl;
            if (authUrl) window.location.href = authUrl;
        } catch {
            toast.error('Não foi possível iniciar a vinculação com o Mercado Pago. Tente novamente.');
        }
    };

    const handleMpDisconnect = async () => {
        if (!window.confirm('Deseja desvincular a conta do Mercado Pago?')) return;
        try {
            await api.put('/payments/mp-disconnect');
            setMpStatus({ linked: false, mpUserIdMasked: null, hasPublicKey: false });
            toast.success('Conta Mercado Pago desvinculada com sucesso.');
        } catch {
            toast.error('Não foi possível desvincular a conta Mercado Pago.');
        }
    };

    return (
        <header className={styles.header}>
            {/* ── Brand ─── */}
            <div className={styles.brand}>
                <button
                    type="button"
                    className={styles.brandButton}
                    onClick={() => navigate(getHomeRouteByRole())}
                    aria-label="Ir para a página inicial"
                >
                    <img src={cortaAiLogo} alt="CortaAI" className={styles.brandLogo} />
                </button>
            </div>

            {/* ── Nav central ─── */}
            <nav className={styles.nav}>
                <button
                    className={activeTab === 'home' ? styles.navItemActive : styles.navItem}
                    onClick={() => onTabChange('home')}
                >
                    <House size={16} weight="duotone" /> Home
                </button>

                {hasShop && (
                    <div className={styles.navDropdownWrapper} ref={agendaRef}>
                        <button
                            className={agendaActive ? styles.navItemActive : styles.navItem}
                            onClick={() => { setAgendaOpen(o => !o); setGestaoOpen(false); }}
                        >
                            <CalendarBlank size={16} weight="duotone" />
                            Agenda
                            <CaretDown size={11} weight="bold" className={agendaOpen ? styles.caretOpen : styles.caret} />
                        </button>
                        {agendaOpen && (
                            <div className={styles.navDropdown}>
                                {agendaSubItems.map(item => (
                                    <button
                                        key={item.id}
                                        className={activeTab === item.id ? styles.navDropdownItemActive : styles.navDropdownItem}
                                        onClick={() => { onTabChange(item.id); setAgendaOpen(false); }}
                                    >
                                        {item.icon} {item.label}
                                    </button>
                                ))}
                            </div>
                        )}
                    </div>
                )}

                {isOwner && hasShop && (
                    <button
                        className={activeTab === 'servicos' ? styles.navItemActive : styles.navItem}
                        onClick={() => onTabChange('servicos')}
                    >
                        <Scissors size={16} weight="duotone" /> Serviços
                    </button>
                )}

                {isOwner && hasShop && (
                    <button
                        className={activeTab === 'time' ? styles.navItemActive : styles.navItem}
                        onClick={() => onTabChange('time')}
                    >
                        <Users size={16} weight="duotone" /> Meu Time
                    </button>
                )}

                {isOwner && hasShop && (
                    <div className={styles.navDropdownWrapper} ref={gestaoRef}>
                        <button
                            className={gestaoActive ? styles.navItemActive : styles.navItem}
                            onClick={() => { setGestaoOpen(o => !o); setAgendaOpen(false); }}
                        >
                            <ChartBar size={16} weight="duotone" />
                            Gestão
                            <CaretDown size={11} weight="bold" className={gestaoOpen ? styles.caretOpen : styles.caret} />
                        </button>
                        {gestaoOpen && (
                            <div className={styles.navDropdown}>
                                <button
                                    className={activeTab === 'dashboards' ? styles.navDropdownItemActive : styles.navDropdownItem}
                                    onClick={() => { onTabChange('dashboards'); setGestaoOpen(false); }}
                                >
                                    <ChartBar size={15} weight="duotone" /> Dashboard
                                </button>
                                <button
                                    className={activeTab === 'estoque' ? styles.navDropdownItemActive : styles.navDropdownItem}
                                    onClick={() => { onTabChange('estoque'); setGestaoOpen(false); }}
                                >
                                    <Package size={15} weight="duotone" /> Estoque
                                </button>
                            </div>
                        )}
                    </div>
                )}
            </nav>

            {/* ── Direita: bell + avatar ─── */}
            <div className={styles.headerRight}>
                {isOwner && hasShop && activeBarbershopName && (
                    <span className={styles.activeBarbershop} title={activeBarbershopName}>
                        {activeBarbershopName}
                    </span>
                )}
                <NotificationBell userType="barber" />

                <div className={styles.avatarWrapper} ref={avatarRef}>
                    <button
                        className={styles.avatarBtn}
                        onClick={() => setAvatarOpen(o => !o)}
                        aria-label="Menu do usuário"
                    >
                        {profileImageUrl ? (
                            <img src={profileImageUrl} alt="Foto de perfil" className={styles.avatarImage} />
                        ) : (
                            <span className={styles.avatarCircle}>{initials}</span>
                        )}
                        <CaretDown size={12} weight="bold" className={avatarOpen ? styles.caretOpen : styles.caret} />
                    </button>

                    {avatarOpen && (
                        <div className={styles.avatarDropdown}>
                            <div className={styles.avatarDropdownUser}>
                                <span className={styles.avatarDropdownName}>{barber?.name || 'Barbeiro'}</span>
                                <span className={styles.avatarDropdownRole}>{isOwner ? 'Owner' : 'Barbeiro'}</span>
                            </div>
                            <div className={styles.dropdownDivider} />
                            <button className={styles.dropdownItem} onClick={() => { onTabChange('perfil'); setAvatarOpen(false); }}>
                                <UserCircle size={15} weight="duotone" /> Meu Perfil
                            </button>
                            {canChangePassword && (
                                <button className={styles.dropdownItem} onClick={() => { navigate('/change-password'); setAvatarOpen(false); }}>
                                    <Lock size={15} weight="duotone" /> Alterar Senha
                                </button>
                            )}
                            {isOwner && (
                                <>
                                    <div className={styles.dropdownInfo}>
                                        {mpStatusLoading
                                            ? 'Mercado Pago: verificando...'
                                            : mpStatus.linked
                                                ? `Mercado Pago conectado (${mpStatus.mpUserIdMasked || 'conta vinculada'})`
                                                : 'Mercado Pago não conectado'}
                                    </div>

                                    {!mpStatus.linked ? (
                                        <button className={styles.dropdownItem} onClick={() => { handleMpConnect(); setAvatarOpen(false); }}>
                                            <CreditCard size={15} weight="duotone" /> Vincular Mercado Pago
                                        </button>
                                    ) : (
                                        <button className={styles.dropdownItem} onClick={() => { handleMpDisconnect(); setAvatarOpen(false); }}>
                                            <CreditCard size={15} weight="duotone" /> Desvincular Mercado Pago
                                        </button>
                                    )}
                                </>
                            )}
                            <div className={styles.dropdownDivider} />
                            <button className={`${styles.dropdownItem} ${styles.dropdownItemDanger}`} onClick={onLogout}>
                                <SignOut size={15} weight="duotone" /> Sair
                            </button>
                        </div>
                    )}
                </div>
            </div>
        </header>
    );
}

export default BarberHeader;
