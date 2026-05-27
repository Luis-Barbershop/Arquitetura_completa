import { describe, expect, it, vi } from 'vitest';

vi.mock('./api', () => ({
  default: {
    post: vi.fn(),
  },
}));

import api from './api';
import { sendMessage } from './gustaveService';

describe('gustaveService', () => {
  it('sends chat messages with the selected mode', () => {
    sendMessage('Quais horários tenho hoje?', 'PREVIEW');

    expect(api.post).toHaveBeenCalledWith('/schedule/ai/chat', {
      message: 'Quais horários tenho hoje?',
      mode: 'PREVIEW',
    });
  });
});
