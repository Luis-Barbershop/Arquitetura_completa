import { beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('./api', () => ({
  default: {
    get: vi.fn(),
  },
}));

import api from './api';
import {
  getAgendaThermometer,
  getBarberFinancialSummary,
  getBarberPerformance,
  getBarberSkillMatrix,
  getCustomerAcquisition,
  getCustomerRetention,
  getFinancialOverview,
  getFinancialSeries,
  getMyShopBarberPerformance,
  getStockHealthAlert,
} from './analyticsService';

describe('analyticsService', () => {
  beforeEach(() => {
    vi.mocked(api.get).mockReset();
    vi.mocked(api.get).mockResolvedValue({ data: { ok: true } });
  });

  it('calls payment analytics endpoints with shop filters', async () => {
    await getBarberPerformance('s1');
    await getMyShopBarberPerformance('s1', { month: 5 });
    await getFinancialOverview('s1', { year: 2026 });
    await getFinancialSeries('s1', { range: 'month' });
    await getBarberFinancialSummary('s1', { barberId: 'b1' });

    expect(api.get).toHaveBeenCalledWith('/payments/analytics/barber-performance', { params: { barbershopId: 's1' } });
    expect(api.get).toHaveBeenCalledWith('/payments/my-shop/barber-performance', { params: { barbershopId: 's1', month: 5 } });
    expect(api.get).toHaveBeenCalledWith('/payments/my-shop/overview', { params: { barbershopId: 's1', year: 2026 } });
    expect(api.get).toHaveBeenCalledWith('/payments/my-shop/series', { params: { barbershopId: 's1', range: 'month' } });
    expect(api.get).toHaveBeenCalledWith('/payments/my-shop/barber-summary', { params: { barbershopId: 's1', barberId: 'b1' } });
  });

  it('calls stock, agenda, skill and customer analytics endpoints', async () => {
    await getStockHealthAlert('s1');
    await getAgendaThermometer('s1');
    await getBarberSkillMatrix('s1');
    await getCustomerAcquisition();
    await getCustomerRetention();

    expect(api.get).toHaveBeenCalledWith('/products/analytics/stock-health', { params: { barbershopId: 's1' } });
    expect(api.get).toHaveBeenCalledWith('/appointments/analytics/agenda-thermometer', { params: { barbershopId: 's1' } });
    expect(api.get).toHaveBeenCalledWith('/appointments/analytics/barber-skill-matrix', { params: { barbershopId: 's1' } });
    expect(api.get).toHaveBeenCalledWith('/users/analytics/customer-acquisition');
    expect(api.get).toHaveBeenCalledWith('/users/analytics/customer-retention');
  });
});
