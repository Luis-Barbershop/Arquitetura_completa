import React, { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../../services/api';
import styles from './NotificationBell.module.css';

/**
 * NotificationBell — sino de notificações compartilhado (cliente e barbeiro).
 *
 * Props:
 *   userType: 'customer' | 'barber'  — determina rota de redirect ao clicar
 *
 * Lógica de redirect por palavras-chave na notificação:
 *   convite      → /barberHome/perfil (barbeiro) | ignorado (cliente)
 *   agendamento / horário / confirmado / cancelado
 *                → /meus-agendamentos (cliente) | /barberHome (barbeiro)
 *   pagamento / pago
 *                → /meus-agendamentos (cliente) | /barberHome (barbeiro)
 */
function NotificationBell({ userType = 'barber' }) {
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

    // Fecha dropdown ao clicar fora
    useEffect(() => {
        const handler = (e) => {
            if (dropdownRef.current && !dropdownRef.current.contains(e.target)) {
                setOpen(false);
            }
        };
        document.addEventListener('mousedown', handler);
        return () => document.removeEventListener('mousedown', handler);
    }, []);

    const handleToggle = async () => {
        if (open) { setOpen(false); return; }

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

    const getRedirectPath = (n) => {
        const text = `${n.title || ''} ${n.message || ''}`.toLowerCase();

        if (text.includes('convite')) {
            return userType === 'barber' ? '/barberHome/perfil' : null;
        }
        if (
            text.includes('agendamento') ||
            text.includes('horário') ||
            text.includes('confirmado') ||
            text.includes('cancelado') ||
            text.includes('concluído') ||
            text.includes('encaixe')
        ) {
            return userType === 'customer' ? '/meus-agendamentos' : '/barberHome';
        }
        if (text.includes('pagamento') || text.includes('pago')) {
            return userType === 'customer' ? '/meus-agendamentos' : '/barberHome';
        }
        return null;
    };

    const handleNotificationClick = async (n) => {
        if (!n.read) await handleMarkAsRead(n.id);
        setOpen(false);
        const path = getRedirectPath(n);
        if (path) navigate(path);
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
                <img
                    src="/Icons/bellicon.png"
                    alt="Sino de Notificação"
                    className={styles.bellIcon}
                    onError={() => setImgError(true)}
                    style={imgError ? { display: 'none' } : {}}
                />
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
                                    title={getRedirectPath(n) ? 'Clique para ver detalhes' : 'Clique para marcar como lida'}
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
