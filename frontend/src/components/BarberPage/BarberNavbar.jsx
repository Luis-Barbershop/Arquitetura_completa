import React, { useState } from 'react';
import {
    House,
    CalendarBlank,
    Scissors,
    Users,
    ChartBar,
    Package,
    PlusCircle,
    UserCircle,
    DotsThreeOutline,
    X,
} from '@phosphor-icons/react';
import { isOwnerUser, getBarbershopId } from '../../services/userContext';
import styles from './CSS/BarberNavbar.module.css';

/**
 * BarberNavbar — bottom bar fixo (mobile ≤760px)
 *
 * isOwner e barbershopId são lidos do localStorage via userContext — fonte única de verdade.
 * Props homônimas são ignoradas para evitar inconsistência entre páginas.
 */
function BarberNavbar({ activeTab, onTabChange }) {
    // Fonte única de verdade — localStorage via userContext
    const isOwner  = isOwnerUser();
    const hasShop  = Boolean(getBarbershopId());
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
            { id: 'servicos', label: 'Serviços', Icon: Scissors },
            ...(isOwner ? [{ id: 'time', label: 'Time', Icon: Users }] : []),
        ]
        : [
            { id: 'home',   label: 'Home',   Icon: House },
            { id: 'perfil', label: 'Perfil', Icon: UserCircle },
        ];

    // Itens do drawer "Mais"
    const drawerItems = hasShop
        ? [
            { id: 'novo-agendamento', label: 'Novo Encaixe',   Icon: PlusCircle },
            ...(isOwner ? [{ id: 'dashboards', label: 'Dashboard', Icon: ChartBar }] : []),
            ...(isOwner ? [{ id: 'estoque', label: 'Estoque', Icon: Package }] : []),
            { id: 'perfil', label: 'Meu Perfil', Icon: UserCircle },
        ]
        : [];

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
                            {drawerItems.map(({ id, label, Icon }) => (
                                <li key={id}>
                                    <button
                                        className={activeTab === id ? styles.drawerItemActive : styles.drawerItem}
                                        onClick={() => handleTab(id)}
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
