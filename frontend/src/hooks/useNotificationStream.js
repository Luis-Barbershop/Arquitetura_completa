import { useEffect, useRef } from 'react';

const SSE_RECONNECT_DELAY_MS = 10_000;

/**
 * Hook que abre uma conexão SSE com o servidor para receber atualizações
 * de notificações em tempo real, sem polling.
 *
 * @param {function} onUnreadCount  Callback chamado com o número de não lidas (number)
 * @param {function} onNotificationCreated  Callback chamado com a notificação criada
 *
 * Fluxo:
 *  1. Abre EventSource para GET /api/notifications/stream?token=<firebase_token>
 *  2. Ao receber evento "unread-count", chama onUnreadCount(n)
 *  3. Ao receber evento "notification-created", chama onNotificationCreated(notification, unreadCount)
 *  4. Em caso de erro, fecha e reconecta após SSE_RECONNECT_DELAY_MS
 *  5. Cleanup ao desmontar fecha a conexão e cancela o timeout de reconexão
 */
export function useNotificationStream(onUnreadCount, onNotificationCreated) {
    const esRef = useRef(null);
    const reconnectTimerRef = useRef(null);
    const activeRef = useRef(true);

    useEffect(() => {
        activeRef.current = true;

        const connect = () => {
            const token = localStorage.getItem('token');
            if (!token || !activeRef.current) return;

            const rawBaseUrl = import.meta.env.VITE_API_BASE_URL || 'https://api.cortaai.shop/api';
            const baseUrl = rawBaseUrl.endsWith('/') ? rawBaseUrl.slice(0, -1) : rawBaseUrl;
            const url = `${baseUrl}/notifications/stream?token=${encodeURIComponent(token)}`;

            const es = new EventSource(url);
            esRef.current = es;

            es.addEventListener('unread-count', (e) => {
                if (!activeRef.current) return;
                try {
                    const { unreadCount } = JSON.parse(e.data);
                    onUnreadCount(Number(unreadCount) || 0);
                } catch {
                    // payload malformado — ignora
                }
            });

            es.addEventListener('notification-created', (e) => {
                if (!activeRef.current || typeof onNotificationCreated !== 'function') return;
                try {
                    const { notification, unreadCount } = JSON.parse(e.data);
                    onNotificationCreated(notification, Number(unreadCount) || 0);
                } catch {
                    // payload malformado — ignora
                }
            });

            es.onerror = () => {
                es.close();
                esRef.current = null;
                if (activeRef.current) {
                    reconnectTimerRef.current = setTimeout(connect, SSE_RECONNECT_DELAY_MS);
                }
            };
        };

        connect();

        return () => {
            activeRef.current = false;
            clearTimeout(reconnectTimerRef.current);
            esRef.current?.close();
            esRef.current = null;
        };
    // onUnreadCount é passado como prop — envolver em useCallback no componente pai
    // para evitar reconexão desnecessária ao re-render
    }, [onUnreadCount, onNotificationCreated]);
}
