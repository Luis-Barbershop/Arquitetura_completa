import React from 'react';
import styles from './CSS/BarberNavbar.module.css';

const NAV_PATHS = {
    home: '/barberHome',
    agenda: '/meus-agendamentos',
    'agenda-barbearia': '/barberHome/agenda-barbearia',
    servicos: '/barberHome/servicos',
    perfil: '/barberHome/perfil',
    estoque: '/barberHome/estoque',
    time: '/barberHome/time',
    dashboards: '/barberHome/dashboard',
    'novo-agendamento': '/barberHome/novo-agendamento',
};

/** Tabs visíveis para TODOS os barbeiros com barbearia vinculada */
const linkedNavItems = [
    { id: 'home',              label: 'Home',    short: 'HM' },
    { id: 'agenda',            label: 'Agenda',  short: 'AG' },
    { id: 'novo-agendamento',  label: 'Novo',    short: '✂️' },
    { id: 'servicos',          label: 'Servicos',short: 'SV' },
    { id: 'perfil',            label: 'Perfil',  short: 'PF' },
];

/** Tabs visíveis para barbeiros SEM barbearia vinculada */
const unlinkedNavItems = [
    { id: 'home',    label: 'Home',    short: 'HM' },
    { id: 'perfil',  label: 'Perfil',  short: 'PF' },
];

/** Tabs exclusivas para OWNER */
const ownerNavItems = [
    { id: 'agenda-barbearia', label: 'Barbearia', short: '📋' },
    { id: 'dashboards', label: 'Dash',    short: 'DB' },
    { id: 'estoque',    label: 'Estoque', short: 'ES' },
    { id: 'time',       label: 'Time',    short: 'TM' },
];

/**
 * @param {string}        activeTab   - tab ativa no momento
 * @param {boolean}       isOwner     - true = dono do estabelecimento
 * @param {string|number|null} barbershopId - ID da barbearia vinculada.
 *   - undefined (não passado): assume que há barbearia (retro-compatibilidade)
 *   - null / false / 0 / "": sem barbearia → oculta Agenda e Serviços
 */
function BarberNavbar({ activeTab, isOwner = false, barbershopId, onTabChange }) {
    // Se não foi passado explicitamente, assume que há barbearia (retro-compatibilidade)
    const hasShop = barbershopId === undefined ? true : Boolean(barbershopId);
    const baseItems = hasShop ? linkedNavItems : unlinkedNavItems;
    const navItems  = (isOwner && hasShop)
        ? [...baseItems, ...ownerNavItems]
        : baseItems;

    const handleItemClick = (itemId) => {
        if (typeof onTabChange === 'function') {
            onTabChange(itemId);
            return;
        }

        const target = NAV_PATHS[itemId];
        if (target && window.location.pathname !== target) {
            window.location.assign(target);
        }
    };

    return (
        <nav className={styles.navbarContainer}>
            <ul className={styles.navbarList}>
                {navItems.map(item => (
                    <li key={item.id}>
                        <button
                            type="button"
                            onClick={() => handleItemClick(item.id)}
                            className={activeTab === item.id ? styles.navItemActive : styles.navItem}
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
