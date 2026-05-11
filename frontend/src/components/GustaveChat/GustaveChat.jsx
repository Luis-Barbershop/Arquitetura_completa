import React, { useEffect, useRef, useState } from 'react';
import { sendMessage } from '../../services/gustaveService';
import styles from './GustaveChat.module.css';

const AVATAR = '✂️';

// Detecta o modo pelo conteúdo da mensagem:
// CONSOLIDATED → perguntas sobre histórico, relatórios, desempenho passado
// PREVIEW      → padrão (agenda futura, próximos clientes, previsões)
function detectMode(text) {
    const lower = text.toLowerCase();
    const consolidatedKeywords = [
        'semana passada', 'mês passado', 'ontem', 'histórico', 'relatório',
        'faturamento', 'receita', 'atendimentos realizados', 'concluídos',
        'desempenho', 'balanço', 'resumo do mês', 'últimos dias',
    ];
    return consolidatedKeywords.some(k => lower.includes(k)) ? 'CONSOLIDATED' : 'PREVIEW';
}

function GustaveChat() {
    const [userRole, setUserRole] = useState(() => localStorage.getItem('userRole'));

    useEffect(() => {
        const sync = () => setUserRole(localStorage.getItem('userRole'));
        window.addEventListener('cortaai:login-success', sync);
        window.addEventListener('cortaai:logout', sync);
        window.addEventListener('storage', sync);
        return () => {
            window.removeEventListener('cortaai:login-success', sync);
            window.removeEventListener('cortaai:logout', sync);
            window.removeEventListener('storage', sync);
        };
    }, []);

    const isBarberRole = (userRole === 'ROLE_BARBER' || userRole === 'ROLE_OWNER')
        && !!localStorage.getItem('token');

    const [open, setOpen]         = useState(false);
    const [messages, setMessages] = useState([
        { role: 'assistant', text: 'Olá! Sou o Gustavo, assistente de gestão do CortaAi. Como posso ajudar?' },
    ]);
    const [input, setInput]   = useState('');
    const [typing, setTyping] = useState(false);
    const [offline, setOffline] = useState(false);
    const bottomRef = useRef(null);

    useEffect(() => {
        if (open) bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, [messages, open]);

    // Fecha o chat automaticamente se o usuário sair ou mudar de role
    useEffect(() => {
        if (!isBarberRole) setOpen(false);
    }, [isBarberRole]);

    if (!isBarberRole) return null;

    const handleSend = async () => {
        const text = input.trim();
        if (!text || typing) return;

        const mode = detectMode(text);

        setMessages(prev => [...prev, { role: 'user', text }]);
        setInput('');
        setTyping(true);
        setOffline(false);

        try {
            const res = await sendMessage(text, mode);
            const data = res.data;
            const isFallback = data.source === 'fallback';
            setOffline(isFallback);
            setMessages(prev => [...prev, { role: 'assistant', text: data.message, source: data.source }]);
        } catch {
            setOffline(true);
            setMessages(prev => [...prev, {
                role: 'assistant',
                text: 'Não consegui me conectar. Tente novamente em alguns instantes.',
                source: 'fallback',
            }]);
        } finally {
            setTyping(false);
        }
    };

    const handleKeyDown = (e) => {
        if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); handleSend(); }
    };

    return (
        <div className={styles.wrapper}>
            {/* Janela de chat */}
            {open && (
                <div className={styles.window}>
                    <div className={styles.header}>
                        <span className={styles.avatar}>{AVATAR}</span>
                        <div className={styles.headerInfo}>
                            <span className={styles.name}>Gustavo</span>
                            <span className={`${styles.status} ${offline ? styles.offline : styles.online}`}>
                                {offline ? 'offline' : 'online'}
                            </span>
                        </div>
                        <button className={styles.closeBtn} onClick={() => setOpen(false)} aria-label="Fechar chat" type="button">✕</button>
                    </div>

                    {/* Histórico */}
                    <div className={styles.messages}>
                        {messages.map((msg, i) => (
                            <div key={i} className={msg.role === 'user' ? styles.msgUser : styles.msgAssistant}>
                                {msg.role === 'assistant' && <span className={styles.msgAvatar}>{AVATAR}</span>}
                                <span className={styles.msgText}>{msg.text}</span>
                            </div>
                        ))}
                        {typing && (
                            <div className={styles.msgAssistant}>
                                <span className={styles.msgAvatar}>{AVATAR}</span>
                                <span className={styles.typing}>
                                    <span /><span /><span />
                                </span>
                            </div>
                        )}
                        <div ref={bottomRef} />
                    </div>

                    {/* Input */}
                    <div className={styles.inputRow}>
                        <textarea
                            className={styles.input}
                            placeholder="Pergunte ao Gustavo..."
                            value={input}
                            onChange={(e) => setInput(e.target.value)}
                            onKeyDown={handleKeyDown}
                            rows={1}
                            disabled={typing}
                        />
                        <button
                            className={styles.sendBtn}
                            onClick={handleSend}
                            disabled={typing || !input.trim()}
                            type="button"
                            aria-label="Enviar"
                        >
                            ➤
                        </button>
                    </div>
                </div>
            )}

            {/* Botão flutuante */}
            <button
                className={`${styles.fab} ${open ? styles.fabOpen : ''}`}
                onClick={() => setOpen(v => !v)}
                aria-label={open ? 'Fechar Gustavo' : 'Abrir Gustavo'}
                type="button"
            >
                {open ? '✕' : '✂️'}
            </button>
        </div>
    );
}

export default GustaveChat;
