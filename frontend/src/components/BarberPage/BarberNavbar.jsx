import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
    House,
    CalendarBlank,
    CalendarX,
    Scissors,
    Users,
    ChartBar,
    Package,
    PlusCircle,
    UserCircle,
    Question,
    Storefront,
    ChartLineUp,
    X,
    SignOut,
} from '@phosphor-icons/react';
import { isOwnerUser, getBarbershopId } from '../../services/userContext';
import { logoutUser } from '../../services/authService';
import { requestOnboardingReplay } from '../../services/onboardingService';
import NotificationBell from '../NotificationBell/NotificationBell';
import styles from './CSS/BarberNavbar.module.css';

/**
 * BarberNavbar â€” bottom bar fixo (mobile â‰¤760px)
 *
 * isOwner e barbershopId sÃ£o lidos do localStorage via userContext â€” fonte Ãºnica de verdade.
 * Props homÃ´nimas sÃ£o ignoradas para evitar inconsistÃªncia entre pÃ¡ginas.
 */
function BarberNavbar({ activeTab, onTabChange, onLogout }) {
    const isOwner  = isOwnerUser();
    const hasShop  = Boolean(getBarbershopId());
    const navigate = useNavigate();
    const [drawerOpen, setDrawerOpen] = useState(false);

    const handleTab = (id) => {
        setDrawerOpen(false);
        if (typeof onTabChange === 'function') onTabChange(id);
    };

    const handleLogout = () => {
        setDrawerOpen(false);
        if (typeof onLogout === 'function') { onLogout(); return; }
        logoutUser();
        navigate('/');
    };

    // â”€â”€ Bottom bar â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // Owner com barbearia: Home | Agenda | ServiÃ§os | Meu Time | GestÃ£o
    // Colaborador com barbearia: Home | Agenda | Habilidades | Mais
    // Sem barbearia: Home | Habilidades | Perfil
    const mainItems = hasShop
        ? isOwner
            ? [
                { id: 'home',    label: 'Home',     Icon: House },
                { id: 'agenda',  label: 'Agenda',   Icon: CalendarBlank },
                { id: 'servicos',label: 'ServiÃ§os',  Icon: Scissors },
                { id: 'time',    label: 'Meu Time',  Icon: Users },
            ]
            : [
                { id: 'home',    label: 'Home',       Icon: House },
                { id: 'agenda',  label: 'Agenda',     Icon: CalendarBlank },
                { id: 'servicos',label: 'Habilidades', Icon: Scissors },
            ]
        : [
            { id: 'home',    label: 'Home',       Icon: House },
            { id: 'servicos',label: 'Habilidades', Icon: Scissors },
            { id: 'perfil',  label: 'Perfil',     Icon: UserCircle },
        ];

    // â”€â”€ Drawer items â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // Owner â†’ drawer chamado "GestÃ£o"
    // Colaborador / sem barbearia â†’ drawer chamado "Mais"
    const drawerLabel = isOwner && hasShop ? 'GestÃ£o' : 'Mais';
    const DrawerTriggerIcon = isOwner && hasShop ? ChartLineUp : Package;

    const drawerItems = isOwner && hasShop
        ? [
            { id: 'gerenciar-barbearia', label: 'Gerenciar Barbearia', Icon: Storefront },
            { id: 'dashboards',          label: 'Dashboard',           Icon: ChartBar },
            { id: 'estoque',             label: 'Estoque',             Icon: Package },
            { id: 'novo-agendamento',    label: 'Novo Encaixe',        Icon: PlusCircle },
            { id: 'indisponibilidade',   label: 'Indisponibilidade',   Icon: CalendarX },
            { id: 'perfil',              label: 'Meu Perfil',          Icon: UserCircle },
            { id: 'onboarding',          label: 'Rever onboarding',    Icon: Question, action: requestOnboardingReplay },
            { id: 'logout',              label: 'Sair',                Icon: SignOut, danger: true },
        ]
        : [
            ...(hasShop ? [{ id: 'novo-agendamento',  label: 'Novo Encaixe',      Icon: PlusCircle }] : []),
            ...(hasShop ? [{ id: 'indisponibilidade', label: 'Indisponibilidade', Icon: CalendarX }] : []),
            ...(!mainItems.some(i => i.id === 'perfil') ? [{ id: 'perfil', label: 'Meu Perfil', Icon: UserCircle }] : []),
            { id: 'onboarding', label: 'Rever onboarding', Icon: Question, action: requestOnboardingReplay },
            { id: 'logout', label: 'Sair', Icon: SignOut, danger: true },
        ];

    const drawerActive = drawerItems.some(i => i.id === activeTab);

    return (
        <>
            <nav className={styles.navbarContainer} data-onboarding-id="barber-main-nav">
                <ul className={styles.navbarList}>
                    {mainItems.map(({ id, label, Icon }) => (
                        <li key={id}>
                            <button
                                type="button"
                                onClick={() => handleTab(id)}
                                className={activeTab === id ? styles.navItemActive : styles.navItem}
                                aria-label={label}
                            >
                                {React.createElement(Icon, {
                                    size: 22,
                                    weight: activeTab === id ? 'duotone' : 'regular',
                                    className: styles.navIcon,
                                })}
                                <span className={styles.navLabel}>{label}</span>
                            </button>
                        </li>
                    ))}

                    <li>
                        <div className={styles.notificationNavItem}>
                            <NotificationBell userType="barber" visibility="mobile" />
                            <span className={styles.navLabel}>Avisos</span>
                        </div>
                    </li>

                    {/* BotÃ£o GestÃ£o (owner) ou Mais (colaborador) */}
                    <li>
                        <button
                            type="button"
                            onClick={() => setDrawerOpen(o => !o)}
                            className={(drawerActive || drawerOpen) ? styles.navItemActive : styles.navItem}
                            aria-label={drawerLabel}
                        >
                            {React.createElement(DrawerTriggerIcon, {
                                size: 22,
                                weight: (drawerActive || drawerOpen) ? 'duotone' : 'regular',
                                className: styles.navIcon,
                            })}
                            <span className={styles.navLabel}>{drawerLabel}</span>
                        </button>
                    </li>
                </ul>
            </nav>

            {/* Drawer */}
            {drawerOpen && (
                <div className={styles.drawerOverlay} onClick={() => setDrawerOpen(false)}>
                    <div className={styles.drawer} onClick={e => e.stopPropagation()}>
                        <div className={styles.drawerHandle} />
                        <div className={styles.drawerHeader}>
                            <span className={styles.drawerTitle}>{drawerLabel}</span>
                            <button className={styles.drawerClose} onClick={() => setDrawerOpen(false)}>
                                <X size={18} />
                            </button>
                        </div>
                        <ul className={styles.drawerList}>
                            {drawerItems.map(({ id, label, Icon, danger }) => (
                                <li key={id}>
                                    <button
                                        className={
                                            danger
                                                ? styles.drawerItemDanger
                                                : activeTab === id
                                                    ? styles.drawerItemActive
                                                    : styles.drawerItem
                                        }
                                        onClick={() => {
                                            const item = drawerItems.find((drawerItem) => drawerItem.id === id);
                                            if (id === 'logout') {
                                                handleLogout();
                                                return;
                                            }
                                            if (typeof item?.action === 'function') {
                                                setDrawerOpen(false);
                                                item.action();
                                                return;
                                            }
                                            handleTab(id);
                                        }}
                                    >
                                        {React.createElement(Icon, {
                                            size: 20,
                                            weight: activeTab === id ? 'duotone' : 'regular',
                                        })}
                                        {label}
                                    </button>
                                </li>
                            ))}
                        </ul>
                    </div>
                </div>
            )}
        </>
    );
}

export default BarberNavbar;
