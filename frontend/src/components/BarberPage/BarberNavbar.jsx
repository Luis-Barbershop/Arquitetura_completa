import React from 'react';
import styles from './CSS/BarberNavbar.module.css';

const navItems = [
    { id: 'home', label: 'Home', short: 'HM' },
    { id: 'agenda', label: 'Agenda', short: 'AG' },
    { id: 'dashboards', label: 'Dash', short: 'DB' },
    { id: 'estoque', label: 'Estoque', short: 'ES' },
    { id: 'servicos', label: 'Servicos', short: 'SV' },
    { id: 'perfil', label: 'Perfil', short: 'PF' },
    { id: 'time', label: 'Time', short: 'TM' },
];

function BarberNavbar({ activeTab, onTabChange }) {
    return (
        <nav className={styles.navbarContainer}>
            <ul className={styles.navbarList}>
                {navItems.map(item => (
                    <li key={item.id}>
                        <button
                            type="button"
                            className={activeTab === item.id ? styles.navItemActive : styles.navItem}
                            onClick={() => onTabChange(item.id)}
                            aria-label={item.label}
                        >
                            <span className={styles.navIcon}>{item.short}</span>
                            <span className={styles.navLabel}>{item.label}</span>
                        </button>
                    </li>
                ))}
            </ul>
        </nav>
    );
}

export default BarberNavbar;
