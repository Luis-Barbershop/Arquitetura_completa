import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
    House,
    CalendarBlank,
    DotsThreeOutline,
    X,
    UserCircle,
    Lock,
    SignOut,
} from '@phosphor-icons/react';
import styles from './CSS/CustomerNavbar.module.css';

/**
 * CustomerNavbar — bottom bar do cliente (mobile ≤760px).
 * Mesmo padrão visual do BarberNavbar.
 *
 * Props:
 *   activeTab: 'home' | 'agendamentos' | 'perfil'
 *   onLogout: () => void
 */
function CustomerNavbar({ activeTab = 'home', onLogout }) {
    const navigate = useNavigate();
    const canChangePassword = (localStorage.getItem('authProvider') || 'EMAIL').toUpperCase() === 'EMAIL';
    const [drawerOpen, setDrawerOpen] = useState(false);

    const mainItems = [
        { id: 'home',         label: 'Home',        Icon: House,         path: '/homepage' },
        { id: 'agendamentos', label: 'Agendamentos', Icon: CalendarBlank, path: '/meus-agendamentos' },
    ];

    const drawerItems = [
        { id: 'perfil', label: 'Meu Perfil', Icon: UserCircle, path: '/homepage/perfil' },
        ...(canChangePassword
            ? [{ id: 'senha',  label: 'Alterar Senha', Icon: Lock,        path: '/change-password' }]
            : []),
        { id: 'sair', label: 'Sair', Icon: SignOut, danger: true },
    ];

    const drawerActive = drawerItems.some(i => i.id === activeTab);

    const handleMain = (path) => navigate(path);

    const handleDrawer = (item) => {
        setDrawerOpen(false);
        if (item.id === 'sair') {
            if (typeof onLogout === 'function') onLogout();
        } else if (item.path) {
            navigate(item.path);
        }
    };

    return (
        <>
            <nav className={styles.navbarContainer}>
                <ul className={styles.navbarList}>
                    {mainItems.map(({ id, label, Icon, path }) => (
                        <li key={id}>
                            <button
                                type="button"
                                onClick={() => handleMain(path)}
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
                </ul>
            </nav>

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
                            {drawerItems.map((item) => (
                                <li key={item.id}>
                                    <button
                                        className={
                                            activeTab === item.id
                                                ? styles.drawerItemActive
                                                : item.danger
                                                    ? styles.drawerItemDanger
                                                    : styles.drawerItem
                                        }
                                        onClick={() => handleDrawer(item)}
                                    >
                                        <item.Icon size={20} weight={activeTab === item.id ? 'duotone' : 'regular'} />
                                        {item.label}
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

export default CustomerNavbar;
