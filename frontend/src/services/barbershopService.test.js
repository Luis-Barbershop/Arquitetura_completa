import { beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('./api', () => ({
  default: {
    delete: vi.fn(),
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
  },
}));

vi.unmock('./barbershopService');

import api from './api';
import {
  acceptInvite,
  addFavoriteBarbershop,
  assignActivities,
  createBarbershopReview,
  createFixedExpense,
  createService,
  deleteFixedExpense,
  deleteService,
  getAllBarbershops,
  getBarbershopById,
  getBarbershops,
  getMyAssignedActivities,
  getMyFavoriteBarbershopsIds,
  getMyFixedExpenses,
  getMyInvites,
  getMyServices,
  getMyWorkSchedule,
  getShopBarbers,
  hasReviewedBarbershop,
  inviteBarberByCpf,
  leaveShop,
  rejectInvite,
  removeFavoriteBarbershop,
  saveMyWorkSchedule,
  updateMyBarbershop,
} from './barbershopService';

describe('barbershopService', () => {
  beforeEach(() => {
    vi.mocked(api.delete).mockReset();
    vi.mocked(api.get).mockReset();
    vi.mocked(api.post).mockReset();
    vi.mocked(api.put).mockReset();
  });

  it('returns public barbershops and null/empty fallbacks on API errors', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => {});
    vi.mocked(api.get)
      .mockResolvedValueOnce({ data: [{ id: 's1' }] })
      .mockRejectedValueOnce(new Error('not found'));

    await expect(getAllBarbershops()).resolves.toEqual([{ id: 's1' }]);
    await expect(getBarbershopById('missing')).resolves.toBeNull();
  });

  it('normalizes barber payloads and uses fallback route when needed', async () => {
    vi.spyOn(console, 'warn').mockImplementation(() => {});
    vi.mocked(api.get)
      .mockResolvedValueOnce({ data: { content: [{ id: 'b1' }] } })
      .mockRejectedValueOnce(new Error('primary failed'))
      .mockResolvedValueOnce({ data: { barbers: [{ id: 'b2' }] } });

    await expect(getShopBarbers('s1')).resolves.toEqual([{ id: 'b1' }]);
    await expect(getShopBarbers('s2')).resolves.toEqual([{ id: 'b2' }]);
    await expect(getShopBarbers()).resolves.toEqual([]);
  });

  it('handles favorites, reviews and review status', async () => {
    vi.mocked(api.get)
      .mockResolvedValueOnce({ data: ['s1'] })
      .mockResolvedValueOnce({ data: { reviewed: true } });
    vi.mocked(api.post).mockResolvedValue({});
    vi.mocked(api.delete).mockResolvedValue({});

    await expect(getMyFavoriteBarbershopsIds()).resolves.toEqual(['s1']);
    await addFavoriteBarbershop('s1');
    await removeFavoriteBarbershop('s1');
    await createBarbershopReview('s1', { rating: 5 });
    await expect(hasReviewedBarbershop('s1')).resolves.toBe(true);

    expect(api.post).toHaveBeenCalledWith('/customers/me/favorites/s1');
    expect(api.delete).toHaveBeenCalledWith('/customers/me/favorites/s1');
    expect(api.post).toHaveBeenCalledWith('/barbershops/s1/reviews', { rating: 5 });
  });

  it('manages services and assigned activities', async () => {
    vi.mocked(api.get)
      .mockResolvedValueOnce({ data: { barbershopId: 's1' } })
      .mockResolvedValueOnce({ data: [{ id: 'svc1' }] })
      .mockResolvedValueOnce({ data: ['a1', { id: 2 }, null] });
    vi.mocked(api.post).mockResolvedValue({ data: { ok: true } });
    vi.mocked(api.delete).mockResolvedValue({});

    await expect(getMyServices()).resolves.toEqual([{ id: 'svc1' }]);
    await expect(createService({ activityName: 'Corte' })).resolves.toEqual({ ok: true });
    await deleteService('svc1');
    await expect(getMyAssignedActivities()).resolves.toEqual(['a1', '2']);
    await expect(assignActivities(['a1'])).resolves.toEqual({ ok: true });

    expect(api.post).toHaveBeenCalledWith('/barbershops/my-shop/activities', { activityName: 'Corte' });
    expect(api.delete).toHaveBeenCalledWith('/barbershops/my-shop/activities/svc1');
    expect(api.post).toHaveBeenCalledWith('/barbers/me/assign-activities', { activityIds: ['a1'] });
  });

  it('covers invite, schedule, fixed expense and search endpoints', async () => {
    vi.mocked(api.get)
      .mockResolvedValueOnce({ data: { data: [{ id: 'invite-1' }] } })
      .mockResolvedValueOnce({ data: [{ dayOfWeek: 'MONDAY' }] })
      .mockResolvedValueOnce({ data: [{ id: 'expense-1' }] })
      .mockResolvedValueOnce({ data: [{ id: 'nearby' }] });
    vi.mocked(api.post).mockResolvedValue({ data: { ok: true } });
    vi.mocked(api.put).mockResolvedValue({ data: { ok: true } });
    vi.mocked(api.delete).mockResolvedValue({});

    await expect(inviteBarberByCpf('123')).resolves.toEqual({ ok: true });
    await expect(getMyInvites()).resolves.toEqual([{ id: 'invite-1' }]);
    await expect(acceptInvite('invite-1')).resolves.toEqual({ ok: true });
    await expect(rejectInvite('invite-1')).resolves.toEqual({ ok: true });
    await expect(leaveShop()).resolves.toEqual({ ok: true });
    await expect(getMyWorkSchedule()).resolves.toEqual([{ dayOfWeek: 'MONDAY' }]);
    await expect(saveMyWorkSchedule([{ dayOfWeek: 'MONDAY' }])).resolves.toEqual({ ok: true });
    await expect(getMyFixedExpenses(5, 2026)).resolves.toEqual([{ id: 'expense-1' }]);
    await expect(createFixedExpense({ name: 'Aluguel' })).resolves.toEqual({ ok: true });
    await deleteFixedExpense('expense-1');
    await expect(getBarbershops({ lat: -23, lng: -46 })).resolves.toEqual([{ id: 'nearby' }]);

    expect(api.post).toHaveBeenCalledWith('/barbershops/my-shop/invite-barber', { cpf: '123' });
    expect(api.put).toHaveBeenCalledWith('/barbers/me/work-schedule', [{ dayOfWeek: 'MONDAY' }]);
    expect(api.get).toHaveBeenCalledWith('/barbershops/my-shop/fixed-expenses', { params: { month: 5, year: 2026 } });
    expect(api.delete).toHaveBeenCalledWith('/barbershops/my-shop/fixed-expenses/expense-1');
    expect(api.get).toHaveBeenCalledWith('/barbershops', { params: { lat: -23, lng: -46, radiusKm: 10 } });
  });

  it('updates my shop profile', async () => {
    vi.mocked(api.put).mockResolvedValueOnce({ data: { name: 'Nova Barbearia' } });

    await expect(updateMyBarbershop({ name: 'Nova Barbearia' })).resolves.toEqual({ name: 'Nova Barbearia' });
    expect(api.put).toHaveBeenCalledWith('/barbershops/my-shop', { name: 'Nova Barbearia' });
  });
});
