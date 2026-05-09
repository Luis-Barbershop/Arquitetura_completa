import api from './api';

/**
 * Envia uma mensagem para o gustave.
 * @param {string} message
 * @param {'PREVIEW'|'CONSOLIDATED'} mode
 */
export const sendMessage = (message, mode) =>
    api.post('/schedule/ai/chat', { message, mode });
