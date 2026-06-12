import { render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import Invoicing from './Invoicing';

vi.mock('../../services/analyticsService', () => ({
  getBarberFinancialSummary: vi.fn(),
  getFinancialOverview: vi.fn(),
  getFinancialSeries: vi.fn(),
}));

import { getBarberFinancialSummary, getFinancialOverview, getFinancialSeries } from '../../services/analyticsService';

describe('Invoicing', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.clearAllMocks();
    vi.mocked(getFinancialOverview).mockResolvedValue({
      totalServiceRevenue: 320,
      serviceRevenue: 250,
      walkInRevenue: 70,
      productExpenses: 40,
      inventoryAssetValue: 900,
      operationalResult: 210,
      operationalResultWithWalkIn: 280,
    });
    vi.mocked(getFinancialSeries).mockResolvedValue({
      points: [
        { date: '2026-06-12', serviceRevenue: 250, walkInRevenue: 70, totalServiceRevenue: 320 },
      ],
    });
    vi.mocked(getBarberFinancialSummary).mockResolvedValue({
      grossTotalRevenue: 200,
      barberServiceCommission: 80,
      barberWalkInCommission: 20,
      barberTotalCommission: 100,
      barbershopTotalCommission: 100,
    });
  });

  it('carrega o resumo completo da barbearia para owner', async () => {
    render(<Invoicing barber={{ barbershopId: 'shop-1', isOwner: true }} />);

    expect(await screen.findByText('Faturamento Hoje:')).toBeInTheDocument();
    expect(await screen.findAllByText('R$ 320,00')).toHaveLength(2);
    expect(screen.getByText('Receita com transacao: R$ 250,00')).toBeInTheDocument();
    expect(screen.getByText('Receita de walk-in: R$ 70,00')).toBeInTheDocument();

    await waitFor(() => {
      expect(getFinancialOverview).toHaveBeenCalledWith('shop-1', expect.objectContaining({
        from: expect.any(String),
        to: expect.any(String),
      }));
    });
    expect(getFinancialSeries).toHaveBeenCalledWith('shop-1', expect.objectContaining({ groupBy: 'DAY' }));
    expect(getBarberFinancialSummary).not.toHaveBeenCalled();
  });

  it('carrega apenas o resumo do barbeiro para colaborador', async () => {
    render(<Invoicing barber={{ barbershopId: 'shop-1', isOwner: false, role: 'ROLE_BARBER' }} />);

    expect(await screen.findByText('Você recebe hoje:')).toBeInTheDocument();
    expect(await screen.findByText('R$ 100,00')).toBeInTheDocument();
    expect(screen.getByText('Valor bruto gerado: R$ 200,00')).toBeInTheDocument();
    expect(screen.getByText('Comissão com transacao: R$ 80,00')).toBeInTheDocument();
    expect(screen.getByText('Comissão de walk-in: R$ 20,00')).toBeInTheDocument();

    await waitFor(() => {
      expect(getBarberFinancialSummary).toHaveBeenCalledWith('shop-1', expect.objectContaining({
        from: expect.any(String),
        to: expect.any(String),
      }));
    });
    expect(getFinancialOverview).not.toHaveBeenCalled();
    expect(getFinancialSeries).not.toHaveBeenCalled();
  });

  it('nao deixa localStorage antigo de owner sobrescrever o perfil de colaborador', async () => {
    localStorage.setItem('isOwner', 'true');
    localStorage.setItem('userRole', 'ROLE_OWNER');

    render(<Invoicing barber={{ barbershopId: 'shop-1', isOwner: false, role: 'ROLE_BARBER' }} />);

    expect(await screen.findByText('Você recebe hoje:')).toBeInTheDocument();
    await waitFor(() => {
      expect(getBarberFinancialSummary).toHaveBeenCalledWith('shop-1', expect.any(Object));
    });
    expect(getFinancialOverview).not.toHaveBeenCalled();
    expect(getFinancialSeries).not.toHaveBeenCalled();
  });
});
