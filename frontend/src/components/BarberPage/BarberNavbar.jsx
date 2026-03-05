import React, { useState } from 'react';
import styles from './CSS/BarberNavbar.module.css';

const navItems = [
    { id: 'inicio', label: 'Início', icon: '🏠' },
    { id: 'agendamentos', label: 'Agendamentos', icon: '📅' },
    { id: 'servicos', label: 'Serviços', icon: '✂️' },
    { id: 'estoque', label: 'Estoque', icon: '📦' },
    { id: 'dashboard', label: 'Dashboard', icon: '📊' },
    { id: 'perfil', label: 'Meu Perfil', icon: '👤' },
];

function BarberNavbar({ activeTab, onTabChange }) {
    return (
        <nav className={styles.navbarContainer}>
            <ul className={styles.navbarList}>
                {navItems.map(item => (
                    <li key={item.id}>
                        <button
                            className={activeTab === item.id ? styles.navItemActive : styles.navItem}
                            onClick={() => onTabChange(item.id)}
                        >
                            <span className={styles.navIcon}>{item.icon}</span>
                            <span className={styles.navLabel}>{item.label}</span>
                        </button>
                    </li>
                ))}
            </ul>
        </nav>
    );
}

export default BarberNavbar;
