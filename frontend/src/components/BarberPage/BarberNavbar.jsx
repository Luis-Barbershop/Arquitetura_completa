import React from 'react';
import styles from './CSS/BarberNavbar.module.css';

/** Tabs visíveis para TODOS os barbeiros com barbearia vinculada */
const linkedNavItems = [
    { id: 'home',    label: 'Home',    short: 'HM' },
    { id: 'agenda',  label: 'Agenda',  short: 'AG' },
    { id: 'servicos',label: 'Servicos',short: 'SV' },
    { id: 'perfil',  label: 'Perfil',  short: 'PF' },
];

/** Tabs visíveis para barbeiros SEM barbearia vinculada */
const unlinkedNavItems = [
    { id: 'home',    label: 'Home',    short: 'HM' },
    { id: 'perfil',  label: 'Perfil',  short: 'PF' },
];

/** Tabs exclusivas para OWNER */
const ownerNavItems = [
    { id: 'dashboards', label: 'Dash',    short: 'DB' },
    { id: 'estoque',    label: 'Estoque', short: 'ES' },
    { id: 'time',       label: 'Time',    short: 'TM' },
];

/**
 * @param {string}        activeTab   - tab ativa no momento
 * @param {Function}      onTabChange - callback ao trocar tab
 * @param {boolean}       isOwner     - true = dono do estabelecimento
 * @param {string|number|null} barbershopId - ID da barbearia vinculada.
 *   - undefined (não passado): assume que há barbearia (retro-compatibilidade)
 *   - null / false / 0 / "": sem barbearia → oculta Agenda e Serviços
 */
function BarberNavbar({ activeTab, onTabChange, isOwner = false, barbershopId }) {
    // Se não foi passado explicitamente, assume que há barbearia (retro-compatibilidade)
    const hasShop = barbershopId === undefined ? true : Boolean(barbershopId);
    const baseItems = hasShop ? linkedNavItems : unlinkedNavItems;
    const navItems  = (isOwner && hasShop)
        ? [...baseItems, ...ownerNavItems]
        : baseItems;

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
