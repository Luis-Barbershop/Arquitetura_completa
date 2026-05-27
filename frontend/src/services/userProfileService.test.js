import { beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('./api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
  },
}));

import api from './api';
import {
  getMyProfile,
  updateCustomerProfile,
  uploadBarberProfilePhoto,
  uploadCustomerProfilePhoto,
} from './userProfileService';

describe('userProfileService', () => {
  beforeEach(() => {
    vi.mocked(api.get).mockReset();
    vi.mocked(api.post).mockReset();
    vi.mocked(api.put).mockReset();
  });

  it('gets and updates profile data', async () => {
    vi.mocked(api.get).mockResolvedValueOnce({ data: { id: 'u1' } });
    vi.mocked(api.put).mockResolvedValueOnce({ data: { name: 'Renan' } });

    await expect(getMyProfile()).resolves.toEqual({ id: 'u1' });
    await expect(updateCustomerProfile({ name: 'Renan' })).resolves.toEqual({ name: 'Renan' });

    expect(api.get).toHaveBeenCalledWith('/auth/me');
    expect(api.put).toHaveBeenCalledWith('/customers/me', { name: 'Renan' });
  });

  it('uploads customer and barber profile photos as FormData', async () => {
    vi.mocked(api.post).mockResolvedValue({ data: { photoUrl: 'photo.png' } });
    const file = new File(['img'], 'photo.png', { type: 'image/png' });

    await expect(uploadCustomerProfilePhoto(file)).resolves.toEqual({ photoUrl: 'photo.png' });
    await expect(uploadBarberProfilePhoto(file)).resolves.toEqual({ photoUrl: 'photo.png' });

    expect(api.post).toHaveBeenCalledWith('/customers/me/upload-photo', expect.any(FormData));
    expect(api.post).toHaveBeenCalledWith('/barbers/me/upload-photo', expect.any(FormData));
  });
});
