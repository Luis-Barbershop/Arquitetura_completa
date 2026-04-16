import React, { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../../services/api';
import styles from './NotificationBell.module.css';

/**
 * Sininho de notificações — exibe badge com contagem de não lidas
 * e dropdown com a lista de notificações ao clicar.
 *
 * Usado no BarberHeader.jsx para donos e barbeiros.
 */
function NotificationBell() {
    const navigate = useNavigate();
    const [unreadCount, setUnreadCount] = useState(0);
    const [notifications, setNotifications] = useState([]);
    const [open, setOpen] = useState(false);
    const [loading, setLoading] = useState(false);
    const [imgError, setImgError] = useState(false);
    const dropdownRef = useRef(null);

    // Busca contagem de não lidas periodicamente (a cada 30s)
    useEffect(() => {
        const fetchCount = async () => {
            try {
                const res = await api.get('/notifications/unread-count');
                setUnreadCount(res.data?.unreadCount ?? 0);
            } catch {
                // silencia erros de rede — não crítico
            }
        };

        fetchCount();
        const interval = setInterval(fetchCount, 30_000);
        return () => clearInterval(interval);
    }, []);

    // Fecha o dropdown ao clicar fora
    useEffect(() => {
        const handleClickOutside = (e) => {
            if (dropdownRef.current && !dropdownRef.current.contains(e.target)) {
                setOpen(false);
            }
        };
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    const handleToggle = async () => {
        if (open) {
            setOpen(false);
            return;
        }

        setOpen(true);
        setLoading(true);
        try {
            const res = await api.get('/notifications/my-notifications');
            setNotifications(Array.isArray(res.data) ? res.data : []);
        } catch {
            setNotifications([]);
        } finally {
            setLoading(false);
        }
    };

    const handleMarkAsRead = async (id) => {
        try {
            await api.put(`/notifications/${id}/read`);
            setNotifications((prev) =>
                prev.map((n) => (n.id === id ? { ...n, read: true } : n))
            );
            setUnreadCount((prev) => Math.max(0, prev - 1));
        } catch {
            // silencia
        }
    };

    const handleNotificationClick = async (n) => {
        if (!n.read) await handleMarkAsRead(n.id);
        setOpen(false);
        // Convites redirecionam para a aba de perfil onde ficam os convites pendentes
        const isInvite = n.title?.toLowerCase().includes('convite') ||
                         n.message?.toLowerCase().includes('convite') ||
                         n.message?.toLowerCase().includes('convidou');
        if (isInvite) {
            navigate('/barberHome/perfil');
        }
    };

    const formatDate = (dateStr) => {
        if (!dateStr) return '';
        return new Date(dateStr).toLocaleString('pt-BR', {
            day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit',
        });
    };

    return (
        <div className={styles.bellWrapper} ref={dropdownRef}>
            <button
                className={styles.bellButton}
                onClick={handleToggle}
                aria-label="Notificações"
                title="Notificações"
            >
                <img src="/Icons/bellicon.png" alt="Sino de Notificação" className={styles.bellIcon}
                    onError={() => setImgError(true)} style={imgError ? { display: 'none' } : {}} />
                {imgError && <span style={{ fontSize: 20 }}>🔔</span>}
                {unreadCount > 0 && (
                    <span className={styles.badge}>{unreadCount > 99 ? '99+' : unreadCount}</span>
                )}
            </button>

            {open && (
                <div className={styles.dropdown}>
                    <div className={styles.dropdownHeader}>
                        <span>Notificações</span>
                        {unreadCount > 0 && (
                            <span className={styles.unreadLabel}>{unreadCount} não lida(s)</span>
                        )}
                    </div>

                    {loading ? (
                        <p className={styles.empty}>Carregando...</p>
                    ) : notifications.length === 0 ? (
                        <p className={styles.empty}>Nenhuma notificação.</p>
                    ) : (
                        <ul className={styles.list}>
                            {notifications.map((n) => (
                                <li
                                    key={n.id}
                                    className={`${styles.item} ${n.read ? styles.read : styles.unread}`}
                                    onClick={() => handleNotificationClick(n)}
                                    title={n.read ? 'Clique para ver detalhes' : 'Clique para marcar como lida'}
                                    style={{ cursor: 'pointer' }}
                                >
                                    <p className={styles.itemTitle}>{n.title}</p>
                                    <p className={styles.itemMessage}>{n.message}</p>
                                    <span className={styles.itemDate}>{formatDate(n.createdAt)}</span>
                                    {!n.read && <span className={styles.dot} aria-label="não lida" />}
                                </li>
                            ))}
                        </ul>
                    )}
                </div>
            )}
        </div>
    );
}

export default NotificationBell;
