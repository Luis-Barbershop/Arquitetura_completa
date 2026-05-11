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
    Storefront,
    ChartLineUp,
    X,
    SignOut,
} from '@phosphor-icons/react';
import { isOwnerUser, getBarbershopId } from '../../services/userContext';
import { logoutUser } from '../../services/authService';
import styles from './CSS/BarberNavbar.module.css';

/**
 * BarberNavbar — bottom bar fixo (mobile ≤760px)
 *
 * isOwner e barbershopId são lidos do localStorage via userContext — fonte única de verdade.
 * Props homônimas são ignoradas para evitar inconsistência entre páginas.
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

    // ── Bottom bar ────────────────────────────────────────────────────────────
    // Owner com barbearia: Home | Agenda | Serviços | Meu Time | Gestão
    // Colaborador com barbearia: Home | Agenda | Habilidades | Mais
    // Sem barbearia: Home | Habilidades | Perfil
    const mainItems = hasShop
        ? isOwner
            ? [
                { id: 'home',    label: 'Home',     Icon: House },
                { id: 'agenda',  label: 'Agenda',   Icon: CalendarBlank },
                { id: 'servicos',label: 'Serviços',  Icon: Scissors },
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

    // ── Drawer items ──────────────────────────────────────────────────────────
    // Owner → drawer chamado "Gestão"
    // Colaborador / sem barbearia → drawer chamado "Mais"
    const drawerLabel = isOwner && hasShop ? 'Gestão' : 'Mais';
    const DrawerTriggerIcon = isOwner && hasShop ? ChartLineUp : Package;

    const drawerItems = isOwner && hasShop
        ? [
            { id: 'gerenciar-barbearia', label: 'Gerenciar Barbearia', Icon: Storefront },
            { id: 'dashboards',          label: 'Dashboard',           Icon: ChartBar },
            { id: 'estoque',             label: 'Estoque',             Icon: Package },
            { id: 'novo-agendamento',    label: 'Novo Encaixe',        Icon: PlusCircle },
            { id: 'indisponibilidade',   label: 'Indisponibilidade',   Icon: CalendarX },
            { id: 'perfil',              label: 'Meu Perfil',          Icon: UserCircle },
            { id: 'logout',              label: 'Sair',                Icon: SignOut, danger: true },
        ]
        : [
            ...(hasShop ? [{ id: 'novo-agendamento',  label: 'Novo Encaixe',      Icon: PlusCircle }] : []),
            ...(hasShop ? [{ id: 'indisponibilidade', label: 'Indisponibilidade', Icon: CalendarX }] : []),
            ...(!mainItems.some(i => i.id === 'perfil') ? [{ id: 'perfil', label: 'Meu Perfil', Icon: UserCircle }] : []),
            { id: 'logout', label: 'Sair', Icon: SignOut, danger: true },
        ];

    const drawerActive = drawerItems.some(i => i.id === activeTab);

    return (
        <>
            <nav className={styles.navbarContainer}>
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

                    {/* Botão Gestão (owner) ou Mais (colaborador) */}
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
                                        onClick={() => (id === 'logout' ? handleLogout() : handleTab(id))}
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

import { isOwnerUser, getBarbershopId } from '../../services/userContext';
import { logoutUser } from '../../services/authService';
import styles from './CSS/BarberNavbar.module.css';

/**
 * BarberNavbar — bottom bar fixo (mobile ≤760px)
 *
 * isOwner e barbershopId são lidos do localStorage via userContext — fonte única de verdade.
 * Props homônimas são ignoradas para evitar inconsistência entre páginas.
 */
function BarberNavbar({ activeTab, onTabChange, onLogout }) {
    // Fonte única de verdade — localStorage via userContext
    const isOwner  = isOwnerUser();
    const hasShop  = Boolean(getBarbershopId());
    const navigate = useNavigate();
    const [drawerOpen, setDrawerOpen] = useState(false);

    const handleTab = (id) => {
        setDrawerOpen(false);
        if (typeof onTabChange === 'function') onTabChange(id);
    };

    // Itens fixos da bottom bar
    const mainItems = hasShop
        ? [
            { id: 'home',     label: 'Home',    Icon: House },
            { id: 'agenda',   label: 'Agenda',  Icon: CalendarBlank },
            { id: 'servicos', label: isOwner ? 'Serviços' : 'Habilidades', Icon: Scissors },
            ...(isOwner ? [{ id: 'time', label: 'Time', Icon: Users }] : []),
        ]
        : [
            { id: 'home',     label: 'Home',    Icon: House },
            { id: 'servicos', label: 'Habilidades', Icon: Scissors },
            { id: 'perfil',   label: 'Perfil',  Icon: UserCircle },
        ];

    // Itens do drawer "Mais"
    const drawerItems = [
        ...(hasShop ? [{ id: 'novo-agendamento', label: 'Novo Encaixe', Icon: PlusCircle }] : []),
        ...(hasShop ? [{ id: 'indisponibilidade', label: 'Indisponibilidade', Icon: CalendarX }] : []),
        ...(hasShop && isOwner ? [{ id: 'dashboards', label: 'Dashboard', Icon: ChartBar }] : []),
        ...(hasShop && isOwner ? [{ id: 'estoque', label: 'Estoque', Icon: Package }] : []),
        ...(hasShop && isOwner ? [{ id: 'gerenciar-barbearia', label: 'Gerenciar Barbearia', Icon: Storefront }] : []),
        ...(!mainItems.some((item) => item.id === 'perfil') ? [{ id: 'perfil', label: 'Meu Perfil', Icon: UserCircle }] : []),
        { id: 'logout', label: 'Sair', Icon: SignOut, danger: true },
    ];

    const handleLogout = () => {
        setDrawerOpen(false);

        if (typeof onLogout === 'function') {
            onLogout();
            return;
        }

        logoutUser();
        navigate('/');
    };

    const hasDrawer = drawerItems.length > 0;
    const drawerActive = drawerItems.some(i => i.id === activeTab);

    return (
        <>
            <nav className={styles.navbarContainer}>
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

                    {hasDrawer && (
                        <li>
                            <button
                                type="button"
                                onClick={() => setDrawerOpen(o => !o)}
                                className={(drawerActive || drawerOpen) ? styles.navItemActive : styles.navItem}
                                aria-label="Mais opções"
                            >
                                <DotsThreeOutline
                                    size={22}
                                    weight={(drawerActive || drawerOpen) ? 'duotone' : 'regular'}
                                    className={styles.navIcon}
                                />
                                <span className={styles.navLabel}>Mais</span>
                            </button>
                        </li>
                    )}
                </ul>
            </nav>

            {/* Drawer */}
            {drawerOpen && (
                <div className={styles.drawerOverlay} onClick={() => setDrawerOpen(false)}>
                    <div className={styles.drawer} onClick={e => e.stopPropagation()}>
                        <div className={styles.drawerHandle} />
                        <div className={styles.drawerHeader}>
                            <span className={styles.drawerTitle}>Mais opções</span>
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
                                        onClick={() => (id === 'logout' ? handleLogout() : handleTab(id))}
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
