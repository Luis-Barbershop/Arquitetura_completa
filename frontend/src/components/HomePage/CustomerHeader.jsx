import React, { useRef, useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
    House,
    CalendarBlank,
    Scissors,
    CaretDown,
    Bell,
    Lock,
    SignOut,
} from '@phosphor-icons/react';
import styles from './CSS/CustomerHeader.module.css';

/**
 * CustomerHeader — barra superior do cliente (desktop).
 * Mesma estrutura visual do BarberHeader.
 *
 * Nav central: Home | Meus Agendamentos
 * Avatar dropdown: Alterar Senha (email) | Sair
 *
 * Props:
 *   activeTab: 'home' | 'agendamentos'
 *   onLogout: () => void
 */
function CustomerHeader({ activeTab = 'home', onLogout }) {
    const navigate = useNavigate();
    const canChangePassword = (localStorage.getItem('authProvider') || 'EMAIL').toUpperCase() === 'EMAIL';
    const userName = localStorage.getItem('userName') || 'Cliente';

    const [avatarOpen, setAvatarOpen] = useState(false);
    const [bellOpen, setBellOpen] = useState(false);
    const avatarRef = useRef(null);
    const bellRef = useRef(null);

    useEffect(() => {
        const handler = (e) => {
            if (avatarRef.current && !avatarRef.current.contains(e.target)) setAvatarOpen(false);
            if (bellRef.current && !bellRef.current.contains(e.target)) setBellOpen(false);
        };
        document.addEventListener('mousedown', handler);
        return () => document.removeEventListener('mousedown', handler);
    }, []);

    const initials = userName
        .split(' ')
        .slice(0, 2)
        .map(n => n[0])
        .join('')
        .toUpperCase();

    return (
        <header className={styles.header}>
            {/* ── Brand ─── */}
            <div className={styles.brand}>
                <div className={styles.brandBadge}>
                    <Scissors size={20} weight="duotone" />
                </div>
                <div className={styles.brandText}>
                    <span className={styles.brandName}>CortaAI</span>
                    <span className={styles.brandSub}>Painel do cliente</span>
                </div>
            </div>

            {/* ── Nav central ─── */}
            <nav className={styles.nav}>
                <button
                    className={activeTab === 'home' ? styles.navItemActive : styles.navItem}
                    onClick={() => navigate('/homepage')}
                >
                    <House size={16} weight="duotone" /> Home
                </button>
                <button
                    className={activeTab === 'agendamentos' ? styles.navItemActive : styles.navItem}
                    onClick={() => navigate('/meus-agendamentos')}
                >
                    <CalendarBlank size={16} weight="duotone" /> Meus Agendamentos
                </button>
            </nav>

            {/* ── Direita: sino + avatar ─── */}
            <div className={styles.headerRight}>

                {/* Sino */}
                <div className={styles.bellWrapper} ref={bellRef}>
                    <button
                        className={styles.bellBtn}
                        onClick={() => setBellOpen(o => !o)}
                        aria-label="Notificações"
                    >
                        <Bell size={20} weight={bellOpen ? 'duotone' : 'regular'} />
                    </button>

                    {bellOpen && (
                        <div className={styles.bellDropdown}>
                            <div className={styles.bellDropdownHeader}>
                                <span className={styles.bellDropdownTitle}>Notificações</span>
                            </div>
                            <div className={styles.bellEmpty}>
                                <Bell size={28} weight="duotone" className={styles.bellEmptyIcon} />
                                <span>Nenhuma notificação</span>
                            </div>
                        </div>
                    )}
                </div>

                {/* Avatar */}
                <div className={styles.avatarWrapper} ref={avatarRef}>
                    <button
                        className={styles.avatarBtn}
                        onClick={() => setAvatarOpen(o => !o)}
                        aria-label="Menu do usuário"
                    >
                        <span className={styles.avatarCircle}>{initials}</span>
                        <CaretDown size={12} weight="bold" className={avatarOpen ? styles.caretOpen : styles.caret} />
                    </button>

                    {avatarOpen && (
                        <div className={styles.avatarDropdown}>
                            <div className={styles.avatarDropdownUser}>
                                <span className={styles.avatarDropdownName}>{userName}</span>
                                <span className={styles.avatarDropdownRole}>Cliente</span>
                            </div>
                            <div className={styles.dropdownDivider} />
                            {canChangePassword && (
                                <button
                                    className={styles.dropdownItem}
                                    onClick={() => { navigate('/change-password'); setAvatarOpen(false); }}
                                >
                                    <Lock size={15} weight="duotone" /> Alterar Senha
                                </button>
                            )}
                            <div className={styles.dropdownDivider} />
                            <button
                                className={`${styles.dropdownItem} ${styles.dropdownItemDanger}`}
                                onClick={onLogout}
                            >
                                <SignOut size={15} weight="duotone" /> Sair
                            </button>
                        </div>
                    )}
                </div>
            </div>
        </header>
    );
}

export default CustomerHeader;
