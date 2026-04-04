import React from 'react';
import { useNavigate } from 'react-router-dom';
import styles from '../../pages/CSS/BarberHomePage.module.css';

const navItems = [
    { id: 'home', label: 'Home', short: 'HM' },
    { id: 'agenda', label: 'Minha Agenda', short: 'AG' },
    { id: 'dashboards', label: 'Dashboards', short: 'DB' },
    { id: 'estoque', label: 'Estoque', short: 'ES' },
    { id: 'servicos', label: 'Servicos', short: 'SV' },
    { id: 'perfil', label: 'Meu Perfil', short: 'PF' },
    { id: 'time', label: 'Meu Time', short: 'TM' },
];

function BarberHeader({ barber, onLogout, activeTab, onTabChange }) {
    const navigate = useNavigate();

    return (
        <header className={styles.header}>
            <div className={styles.headerTopRow}>
                <div className={styles.headerleft}>
                    <div className={styles.headerBrandBadge}>CA</div>
                    <div className={styles.headerBrandText}>
                        <h2 className={styles.headerTitle}>Corta AI</h2>
                        <p className={styles.headerWelcome}>Painel profissional de {barber?.name}</p>
                    </div>
                </div>

                <div className={styles.headerRight}>
                    <button className={styles.notificationButton}>
                        <img src="/Icons/bellicon.png" alt="Sino de Notificacao" />
                    </button>
                    <button
                        onClick={() => navigate('/change-password')}
                        className={styles.changePasswordButton}
                        title="Alterar senha"
                    >
                        🔒 Alterar senha
                    </button>
                    <button onClick={onLogout} className={styles.logoutButton}>
                        Sair
                    </button>
                </div>
            </div>

            <nav className={styles.headerNav} aria-label="Navegacao principal do barbeiro">
                {navItems.map((item) => (
                    <button
                        key={item.id}
                        type="button"
                        className={activeTab === item.id ? styles.headerNavItemActive : styles.headerNavItem}
                        onClick={() => onTabChange(item.id)}
                        aria-label={item.label}
                    >
                        <span className={styles.headerNavChip}>{item.short}</span>
                        {item.label}
                    </button>
                ))}
            </nav>
        </header>
    );
}

export default BarberHeader;
