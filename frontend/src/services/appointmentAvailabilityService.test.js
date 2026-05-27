import { beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('./api', () => ({
  default: {
    get: vi.fn(),
  },
}));

import api from './api';
import {
  buildDateWindow,
  clearAvailabilitySlotsCache,
  createDateOptionsBase,
  fetchAvailabilitySlots,
  formatCompactDate,
  formatDateToApi,
  getRelativeDateLabel,
  hydrateDateOptionsWithAvailability,
  normalizeAvailabilitySlots,
} from './appointmentAvailabilityService';

describe('appointmentAvailabilityService', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date(2026, 4, 21, 8, 30, 0));
    clearAvailabilitySlotsCache();
    vi.mocked(api.get).mockReset();
  });

  it('formats dates and builds the date window', () => {
    const baseDate = new Date(2026, 4, 21, 14, 15, 0);

    expect(formatDateToApi(baseDate)).toBe('2026-05-21');
    expect(formatCompactDate(baseDate)).toBe('21/05');
    expect(getRelativeDateLabel(baseDate, 0)).toBe('Hoje');
    expect(getRelativeDateLabel(baseDate, 1)).toBe('Amanhã');
    expect(getRelativeDateLabel(new Date(2026, 4, 23), 2)).toBe('SAB');

    const window = buildDateWindow(3);
    expect(window).toHaveLength(3);
    expect(window[0].getHours()).toBe(0);
    expect(window[0].getMinutes()).toBe(0);
    expect(window[0].getSeconds()).toBe(0);
    expect(window[1].getDate()).toBe(22);
  });

  it('normalizes only the available slots with valid start times', () => {
    expect(
      normalizeAvailabilitySlots([
        { available: true, startTime: '2026-05-21T09:30:00' },
        { available: false, startTime: '2026-05-21T10:00:00' },
        { available: true, startTime: '11:15:00' },
        { available: true, startTime: null },
      ]),
    ).toEqual(['09:30', '11:15']);
  });

  it('caches availability requests and deduplicates in-flight calls', async () => {
    vi.mocked(api.get).mockResolvedValue({
      data: [
        { available: true, startTime: '2026-05-21T10:00:00' },
        { available: true, startTime: '2026-05-21T11:00:00' },
      ],
    });

    const first = await fetchAvailabilitySlots({
      barberId: 10,
      dateObj: new Date(2026, 4, 21),
      durationMinutes: 30,
    });

    const second = await fetchAvailabilitySlots({
      barberId: 10,
      dateObj: new Date(2026, 4, 21),
      durationMinutes: 30,
    });

    expect(first).toEqual(['10:00', '11:00']);
    expect(second).toEqual(['10:00', '11:00']);
    expect(api.get).toHaveBeenCalledTimes(1);
  });

  it('reuses an in-flight request for the same cache key', async () => {
    let resolveRequest;
    const requestPromise = new Promise((resolve) => {
      resolveRequest = resolve;
    });

    vi.mocked(api.get).mockReturnValueOnce(requestPromise);

    const firstCall = fetchAvailabilitySlots({
      barberId: 33,
      dateObj: new Date(2026, 4, 21),
      durationMinutes: 45,
    });

    const secondCall = fetchAvailabilitySlots({
      barberId: 33,
      dateObj: new Date(2026, 4, 21),
      durationMinutes: 45,
    });

    resolveRequest({
      data: [{ available: true, startTime: '2026-05-21T13:00:00' }],
    });

    await expect(firstCall).resolves.toEqual(['13:00']);
    await expect(secondCall).resolves.toEqual(['13:00']);
    expect(api.get).toHaveBeenCalledTimes(1);
  });

  it('hydrates date options and filters slots by minimum advance time', async () => {
    vi.mocked(api.get)
      .mockResolvedValueOnce({
        data: [{ available: true, startTime: '2026-05-21T10:00:00' }],
      })
      .mockResolvedValueOnce({
        data: [{ available: true, startTime: '2026-05-22T12:00:00' }],
      });

    const dateOptions = createDateOptionsBase(2);
    const hydrated = await hydrateDateOptionsWithAvailability({
      barberId: 7,
      durationMinutes: 30,
      dateOptions,
      minAdvanceHours: 2,
    });

    expect(hydrated[0].slots).toEqual([]);
    expect(hydrated[0].isAvailable).toBe(false);
    expect(hydrated[1].slots).toEqual(['12:00']);
    expect(hydrated[1].isAvailable).toBe(true);
  });
});