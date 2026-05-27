import { beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('./api', () => ({
  default: {
    delete: vi.fn(),
    get: vi.fn(),
    post: vi.fn(),
  },
}));

import api from './api';
import { createBarberBlock, deleteBarberBlock, getBarberBlocks } from './barberBlockService';

describe('barberBlockService', () => {
  beforeEach(() => {
    vi.mocked(api.delete).mockReset();
    vi.mocked(api.get).mockReset();
    vi.mocked(api.post).mockReset();
  });

  it('creates a block trimming blank reasons', async () => {
    vi.mocked(api.post).mockResolvedValueOnce({ data: { id: 'block-1' } });

    await expect(createBarberBlock({
      barberId: 'b1',
      startTime: '09:00',
      endTime: '10:00',
      reason: '  almoco  ',
    })).resolves.toEqual({ id: 'block-1' });

    expect(api.post).toHaveBeenCalledWith('/appointments/barber-blocks', {
      barberId: 'b1',
      startTime: '09:00',
      endTime: '10:00',
      reason: 'almoco',
    });
  });

  it('lists and deletes barber blocks', async () => {
    vi.mocked(api.get).mockResolvedValueOnce({ data: [{ id: 'block-1' }] });
    vi.mocked(api.delete).mockResolvedValueOnce({});

    await expect(getBarberBlocks({ barberId: 'b1', date: '2026-05-22' })).resolves.toEqual([{ id: 'block-1' }]);
    await deleteBarberBlock('block-1');

    expect(api.get).toHaveBeenCalledWith('/appointments/barber-blocks', {
      params: { barberId: 'b1', date: '2026-05-22' },
    });
    expect(api.delete).toHaveBeenCalledWith('/appointments/barber-blocks/block-1');
  });
});
