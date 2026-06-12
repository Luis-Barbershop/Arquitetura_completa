import React, { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Bell } from '@phosphor-icons/react';
import { toast } from 'react-toastify';
import api from '../../services/api';
import { useNotificationStream } from '../../hooks/useNotificationStream';
import styles from './NotificationBell.module.css';

/**
 * NotificationBell — sino de notificações compartilhado (cliente e barbeiro).
 *
 * Props:
 *   userType: 'customer' | 'barber'  — determina rota de redirect ao clicar
 *   visibility: 'all' | 'desktop' | 'mobile' — evita conexões duplicadas quando há header e bottom bar
 *
 * Lógica de redirect por tipo:
 *   agendamentos/pagamentos → /meus-agendamentos
 *   pedido de entrada       → /barberHome/time
 *   convite/remocao        → /barberHome/perfil
 */
function NotificationBell({ userType = 'barber', visibility = 'all' }) {
    const navigate = useNavigate();
    const [unreadCount, setUnreadCount] = useState(0);
    const [notifications, setNotifications] = useState([]);
    const [open, setOpen] = useState(false);
    const [loading, setLoading] = useState(false);
    const [clearing, setClearing] = useState(false);
    const [dropdownPos, setDropdownPos] = useState({ top: 0, right: 0, maxHeight: 420 });
    const [isMobileViewport, setIsMobileViewport] = useState(() => (
        typeof window !== 'undefined' && window.matchMedia('(max-width: 760px)').matches
    ));
    const dropdownRef = useRef(null);
    const buttonRef = useRef(null);
    const openRef = useRef(false);
    const shouldRender = visibility === 'all' ||
        (visibility === 'mobile' && isMobileViewport) ||
        (visibility === 'desktop' && !isMobileViewport);

    useEffect(() => {
        openRef.current = open;
    }, [open]);

    useEffect(() => {
        const mediaQuery = window.matchMedia('(max-width: 760px)');
        const syncViewport = () => setIsMobileViewport(mediaQuery.matches);
        syncViewport();
        mediaQuery.addEventListener('change', syncViewport);
        return () => mediaQuery.removeEventListener('change', syncViewport);
    }, []);

    useEffect(() => {
        if (!shouldRender) {
            setOpen(false);
        }
    }, [shouldRender]);

    const upsertNotification = useCallback((notification) => {
        if (!notification?.id) return;
        setNotifications((prev) => {
            const exists = prev.some((item) => item.id === notification.id);
            if (exists) {
                return prev.map((item) => (item.id === notification.id ? notification : item));
            }
            return [notification, ...prev];
        });
    }, []);

    const fetchNotifications = useCallback(async () => {
        setLoading(true);
        try {
            const res = await api.get('/notifications/my-notifications');
            setNotifications(Array.isArray(res.data) ? res.data : []);
        } catch {
            // Mantém o estado existente — preserva notificações adicionadas via SSE
        } finally {
            setLoading(false);
        }
    }, []);

    // SSE — recebe contagem de não lidas em tempo real (sem polling)
    const handleUnreadCount = useCallback((count) => {
        setUnreadCount((previous) => {
            if (count > previous && openRef.current) {
                void fetchNotifications();
            }
            return count;
        });
    }, [fetchNotifications]);

    const handleNotificationCreated = useCallback((notification, count) => {
        setUnreadCount(count);
        upsertNotification(notification);
        if (notification?.title) {
            toast.info(`${notification.title}${notification.message ? `: ${notification.message}` : ''}`, {
                autoClose: 6000,
            });
        }
    }, [upsertNotification]);

    useNotificationStream(handleUnreadCount, handleNotificationCreated, shouldRender);

    // Busca contagem inicial no mount (antes do SSE conectar)
    useEffect(() => {
        if (!shouldRender) return;
        api.get('/notifications/unread-count')
            .then((res) => setUnreadCount(res.data?.unreadCount ?? 0))
            .catch(() => {});
    }, [shouldRender]);

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

        // Calcula posição do dropdown relativa ao viewport (escapa do stacking context do header)
        if (buttonRef.current) {
            const rect = buttonRef.current.getBoundingClientRect();
            const defaultMaxHeight = 420;
            const maxHeight = isMobileViewport
                ? Math.min(defaultMaxHeight, Math.max(220, rect.top - 18))
                : defaultMaxHeight;
            const top = isMobileViewport
                ? Math.max(12, rect.top - maxHeight - 8)
                : rect.bottom + 8;
            const right = isMobileViewport ? 12 : window.innerWidth - rect.right;

            setDropdownPos({ top, right, maxHeight });
        }

        setOpen(true);
        await fetchNotifications();
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

    const handleClearAll = async () => {
        if (clearing || notifications.length === 0) return;

        setClearing(true);
        try {
            await api.delete('/notifications/my-notifications');
            setNotifications([]);
            setUnreadCount(0);
            toast.success('Notificações limpas.');
        } catch {
            toast.error('Não foi possível limpar as notificações agora.');
        } finally {
            setClearing(false);
        }
    };

    const isOwner = () => (
        localStorage.getItem('isOwner') === 'true' ||
        String(localStorage.getItem('userRole') || '').toUpperCase().includes('OWNER')
    );

    const getAppointmentRoute = () => {
        if (userType === 'customer') {
            return '/meus-agendamentos';
        }
        return isOwner() ? '/meus-agendamentos?view=team' : '/meus-agendamentos';
    };

    const getRedirectPath = (n) => {
        const type = String(n.type || '').toUpperCase();

        if (
            type === 'APPOINTMENT_CREATED' ||
            type === 'APPOINTMENT_CANCELLED' ||
            type === 'APPOINTMENT_CONCLUDED' ||
            type === 'APPOINTMENT_RESCHEDULED' ||
            type === 'APPOINTMENT_REMINDER' ||
            type === 'PAYMENT_APPROVED'
        ) {
            return getAppointmentRoute();
        }

        if (type === 'JOIN_REQUEST_RECEIVED') {
            return userType === 'barber' ? '/barberHome/time' : null;
        }

        if (type === 'INVITE_RECEIVED' || type === 'BARBER_REMOVED') {
            return userType === 'barber' ? '/barberHome/perfil' : null;
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

    if (!shouldRender) {
        return null;
    }

    const keepDropdownInteractionInside = (event) => {
        event.stopPropagation();
    };

    return (
        <div
            className={styles.bellWrapper}
            ref={dropdownRef}
            onMouseDown={keepDropdownInteractionInside}
            onTouchStart={keepDropdownInteractionInside}
        >
            <button
                className={styles.bellButton}
                onClick={handleToggle}
                ref={buttonRef}
                aria-label="Notificações"
                title="Notificações"
            >
                <Bell size={22} weight="duotone" className={styles.bellVectorIcon} />
                {unreadCount > 0 && (
                    <span className={styles.badge}>{unreadCount > 99 ? '99+' : unreadCount}</span>
                )}
            </button>

            {open && (
                <div
                    className={styles.dropdown}
                    style={{ top: dropdownPos.top, right: dropdownPos.right, maxHeight: dropdownPos.maxHeight }}
                >
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

                    {notifications.length > 0 && (
                        <div className={styles.footer}>
                            <button
                                type="button"
                                className={styles.clearAllButton}
                                onClick={handleClearAll}
                                disabled={clearing}
                            >
                                {clearing ? 'Limpando...' : 'Limpar todas'}
                            </button>
                        </div>
                    )}
                </div>
            )}
        </div>
    );
}

export default NotificationBell;
