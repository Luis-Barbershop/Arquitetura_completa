import React from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import api from '../../services/api';
import NotificationBell from './NotificationBell';
import styles from '../../pages/CSS/BarberHomePage.module.css';

/** Tabs visíveis para TODOS os barbeiros, com ou sem barbearia vinculada */
const baseNavItems = [
    { id: 'home',    label: 'Home',       short: 'HM' },
    { id: 'perfil',  label: 'Meu Perfil', short: 'PF' },
];

/** Tabs que exigem barbearia vinculada (barbershopId presente) */
const linkedNavItems = [
    { id: 'agenda',            label: 'Minha Agenda',    short: 'AG' },
    { id: 'novo-agendamento',  label: 'Novo Walk-in',    short: '✂️' },
    { id: 'servicos',          label: 'Servicos',        short: 'SV' },
];

/** Tabs exclusivas para OWNER */
const ownerNavItems = [
    { id: 'dashboards', label: 'Dashboards', short: 'DB' },
    { id: 'estoque',    label: 'Estoque',    short: 'ES' },
    { id: 'time',       label: 'Meu Time',   short: 'TM' },
];

/**
 * @param {object}   barber      - dados do barbeiro logado
 * @param {Function} onLogout    - callback de logout
 * @param {string}   activeTab   - tab ativa
 * @param {Function} onTabChange - callback ao trocar tab
 * @param {string|number} barbershopId - ID da barbearia vinculada (undefined = sem barbearia)
 */
function BarberHeader({ barber, onLogout, activeTab, onTabChange, isOwner = false, barbershopId }) {
    const navigate = useNavigate();
    const canChangePassword = (localStorage.getItem('authProvider') || 'EMAIL').toUpperCase() === 'EMAIL';

    // Monta a lista de tabs respeitando a mesma lógica do BarberNavbar:
    // Agenda e Serviços só aparecem quando o barbeiro já tem barbearia vinculada
    const commonNavItems = barbershopId
        ? [...baseNavItems, ...linkedNavItems]
        : baseNavItems;

    const navItems = isOwner
        ? [...commonNavItems, ...ownerNavItems]
        : commonNavItems;

    const handleMpConnect = async () => {
        const barberId = barber?.id;
        if (!barberId) return;
        try {
            const response = await api.get(`/payments/mp-connect?state=${barberId}`);
            const authUrl = response.data?.authorizationUrl;
            if (authUrl) {
                window.location.href = authUrl;
            }
        } catch (err) {
            console.error('Erro ao conectar Mercado Pago:', err);
            toast.error('Não foi possível iniciar a vinculação com o Mercado Pago. Tente novamente.');
        }
    };

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
                    <NotificationBell />
                    {isOwner && (
                        <button
                            onClick={handleMpConnect}
                            className={styles.mpButton}
                            title="Vincular conta Mercado Pago para receber pagamentos online"
                        >
                            💳 Vincular MP
                        </button>
                    )}
                    {canChangePassword && (
                        <button
                            onClick={() => navigate('/change-password')}
                            className={styles.changePasswordButton}
                            title="Alterar senha"
                        >
                            🔒 Alterar senha
                        </button>
                    )}
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
