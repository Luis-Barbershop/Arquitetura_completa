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
  cancelAppointment,
  concludeAppointment,
  createAppointment,
  getBarbershopSchedule,
  getMyAppointments,
  rescheduleAppointment,
} from './appointmentService';

describe('appointmentService', () => {
  beforeEach(() => {
    vi.mocked(api.get).mockReset();
    vi.mocked(api.post).mockReset();
    vi.mocked(api.put).mockReset();
  });

  it('creates, cancels, concludes and reschedules appointments through the API', async () => {
    vi.mocked(api.post).mockResolvedValueOnce({ data: { id: 'a1' } });
    vi.mocked(api.put)
      .mockResolvedValueOnce({ data: { status: 'CANCELLED' } })
      .mockResolvedValueOnce({ data: { status: 'CONCLUDED' } })
      .mockResolvedValueOnce({ data: { startTime: '2026-05-22T10:00:00' } });

    await expect(createAppointment({ barberId: 'b1' })).resolves.toEqual({ id: 'a1' });
    await expect(cancelAppointment('a1')).resolves.toEqual({ status: 'CANCELLED' });
    await expect(concludeAppointment('a1')).resolves.toEqual({ status: 'CONCLUDED' });
    await expect(rescheduleAppointment('a1', '2026-05-22T10:00:00', 'b2')).resolves.toEqual({
      startTime: '2026-05-22T10:00:00',
    });

    expect(api.post).toHaveBeenCalledWith('/appointments', { barberId: 'b1' });
    expect(api.put).toHaveBeenCalledWith('/appointments/a1/cancel');
    expect(api.put).toHaveBeenCalledWith('/appointments/a1/conclude');
    expect(api.put).toHaveBeenCalledWith('/appointments/a1/reschedule', {
      newStartTime: '2026-05-22T10:00:00',
      barberId: 'b2',
    });
  });

  it('hydrates my appointments with barber, customer, shop and activity names', async () => {
    vi.mocked(api.get).mockImplementation((url) => {
      const responses = {
        '/appointments/my-appointments': [
          {
            id: 'a1',
            barberId: 'b1',
            customerId: 'c1',
            barbershopId: 's1',
            activities: [{ activityName: 'Corte' }, { activityName: null }],
          },
        ],
        '/barbershops': [{ id: 's1', name: 'Barbearia Central' }],
        '/barbers/b1': { name: 'Joao' },
        '/customers/c1': { name: 'Maria' },
      };
      return Promise.resolve({ data: responses[url] });
    });

    await expect(getMyAppointments()).resolves.toEqual([
      expect.objectContaining({
        id: 'a1',
        barberName: 'Joao',
        customerName: 'Maria',
        barbershopName: 'Barbearia Central',
        activityNames: ['Corte'],
      }),
    ]);
  });

  it('returns empty/fallback values when appointment lookups fail', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => {});
    vi.mocked(api.get)
      .mockResolvedValueOnce({ data: [{ id: 'a1', barberId: 'b1', customerId: 'walk', barbershopId: 's1' }] })
      .mockRejectedValueOnce(new Error('shops failed'))
      .mockRejectedValueOnce(new Error('barber failed'));

    const result = await getMyAppointments();

    expect(result[0]).toEqual(expect.objectContaining({
      barberName: 'Barbeiro',
      customerName: 'Cliente',
      barbershopName: 'Barbearia',
      activityNames: [],
    }));
  });

  it('normalizes barbershop schedule params and activities', async () => {
    vi.mocked(api.get).mockResolvedValueOnce({
      data: [{ id: 'a1', activityNames: ['Barba'] }],
    });

    await expect(getBarbershopSchedule('s1', '2026-05-22')).resolves.toEqual([
      expect.objectContaining({ activityNames: ['Barba'] }),
    ]);

    expect(api.get).toHaveBeenCalledWith('/appointments/barbershop/s1', {
      params: { date: '2026-05-22' },
    });
  });
});
