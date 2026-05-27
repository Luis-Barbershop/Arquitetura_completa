import React, { useRef, useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
    House,
    CalendarBlank,
    CaretDown,
    UserCircle,
    Lock,
    SignOut,
} from '@phosphor-icons/react';
import cortaAiLogo from '/CortaAiLogo.png';
import NotificationBell from '../NotificationBell/NotificationBell';
import { getHomeRouteByRole } from '../../services/userContext';
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

    // Reativo ao login (cortaai:login-success) e storage — mesmo padrão do GustaveChat
    const [userName, setUserName] = useState(() => localStorage.getItem('userName') || 'Cliente');
    const [userProfileImage, setUserProfileImage] = useState(() => localStorage.getItem('userProfileImage') || '');

    useEffect(() => {
        const sync = () => {
            setUserName(localStorage.getItem('userName') || 'Cliente');
            setUserProfileImage(localStorage.getItem('userProfileImage') || '');
        };
        window.addEventListener('cortaai:login-success', sync);
        window.addEventListener('storage', sync);
        return () => {
            window.removeEventListener('cortaai:login-success', sync);
            window.removeEventListener('storage', sync);
        };
    }, []);

    const [avatarOpen, setAvatarOpen] = useState(false);
    const avatarRef = useRef(null);

    useEffect(() => {
        const handler = (e) => {
            if (avatarRef.current && !avatarRef.current.contains(e.target)) setAvatarOpen(false);
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
                <button
                    type="button"
                    className={styles.brandButton}
                    onClick={() => navigate(getHomeRouteByRole())}
                    aria-label="Ir para a página inicial"
                >
                    <img src={cortaAiLogo} alt="CortaAI" className={styles.brandLogo} />
                </button>
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
                <NotificationBell userType="customer" visibility="desktop" />

                {/* Avatar */}
                <div className={styles.avatarWrapper} ref={avatarRef}>
                    <button
                        className={styles.avatarBtn}
                        onClick={() => setAvatarOpen(o => !o)}
                        aria-label="Menu do usuário"
                    >
                        {userProfileImage ? (
                            <img src={userProfileImage} alt="Foto de perfil" className={styles.avatarImage} />
                        ) : (
                            <span className={styles.avatarCircle}>{initials}</span>
                        )}
                        <CaretDown size={12} weight="bold" className={avatarOpen ? styles.caretOpen : styles.caret} />
                    </button>

                    {avatarOpen && (
                        <div className={styles.avatarDropdown}>
                            <div className={styles.avatarDropdownUser}>
                                <span className={styles.avatarDropdownName}>{userName}</span>
                                <span className={styles.avatarDropdownRole}>Cliente</span>
                            </div>
                            <div className={styles.dropdownDivider} />
                            <button
                                className={styles.dropdownItem}
                                onClick={() => { navigate('/homepage/perfil'); setAvatarOpen(false); }}
                            >
                                <UserCircle size={15} weight="duotone" /> Meu Perfil
                            </button>
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
