import React, { useEffect, useRef, useState } from 'react';
import { sendMessage } from '../../services/gustaveService';
import styles from './GustaveChat.module.css';

const MODES = [
    { key: 'PREVIEW',      label: 'Previsão' },
    { key: 'CONSOLIDATED', label: 'Consolidado' },
];

const AVATAR = '✂️';

function GustaveChat() {
    const userRole = localStorage.getItem('userRole');

    const [open, setOpen]     = useState(false);
    const [mode, setMode]     = useState('PREVIEW');
    const [messages, setMessages] = useState([
        { role: 'assistant', text: 'Olá! Sou o **gustave**, seu assistente de agenda. Escolha o modo e me pergunte o que quiser.' },
    ]);
    const [input, setInput]   = useState('');
    const [typing, setTyping] = useState(false);
    const [offline, setOffline] = useState(false);
    const bottomRef = useRef(null);

    useEffect(() => {
        if (open) bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, [messages, open]);

    if (userRole !== 'ROLE_BARBER') return null;

    const handleSend = async () => {
        const text = input.trim();
        if (!text || typing) return;

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
                            <span className={styles.name}>gustave</span>
                            <span className={`${styles.status} ${offline ? styles.offline : styles.online}`}>
                                {offline ? 'offline' : 'online'}
                            </span>
                        </div>
                        <button className={styles.closeBtn} onClick={() => setOpen(false)} aria-label="Fechar chat" type="button">✕</button>
                    </div>

                    {/* Toggle de modo */}
                    <div className={styles.modeBar}>
                        {MODES.map(m => (
                            <button
                                key={m.key}
                                className={mode === m.key ? styles.modeActive : styles.modeBtn}
                                onClick={() => setMode(m.key)}
                                type="button"
                            >
                                {m.label}
                            </button>
                        ))}
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
                            placeholder="Pergunte ao gustave..."
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
                aria-label={open ? 'Fechar gustave' : 'Abrir gustave'}
                type="button"
            >
                {open ? '✕' : '✂️'}
            </button>
        </div>
    );
}

export default GustaveChat;
