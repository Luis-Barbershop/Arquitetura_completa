import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const navigate = vi.fn();

vi.mock('react-router-dom', () => ({
  useNavigate: () => navigate,
}));

vi.mock('../../hooks/useAuthGuard', () => ({
  useAuthGuard: vi.fn(),
}));

vi.mock('../../services/api', () => ({
  default: {
    get: vi.fn(),
  },
}));

vi.mock('../../services/authService', () => ({
  logoutUser: vi.fn(),
}));

vi.mock('../../services/navigationService', () => ({
  navigateToBarberTab: vi.fn(),
}));

vi.mock('../../services/analyticsService', () => ({
  getFinancialOverview: vi.fn(),
  getMyShopBarberPerformance: vi.fn(),
  getStockHealthAlert: vi.fn(),
  getAgendaThermometer: vi.fn(),
  getBarberSkillMatrix: vi.fn(),
  getCustomerAcquisition: vi.fn(),
  getCustomerRetention: vi.fn(),
}));

vi.mock('../../services/barbershopService', () => ({
  getMyFixedExpenses: vi.fn(),
  createFixedExpense: vi.fn(),
  deleteFixedExpense: vi.fn(),
}));

vi.mock('../../components/BarberPage/BarberHeader', () => ({
  default: ({ onLogout, onTabChange }) => (
    <div>
      <button type="button" onClick={onLogout}>Header logout</button>
      <button type="button" onClick={() => onTabChange('estoque')}>Header estoque</button>
    </div>
  ),
}));

vi.mock('../../components/BarberPage/BarberNavbar', () => ({
  default: ({ onTabChange }) => (
    <button type="button" onClick={() => onTabChange('time')}>Navbar time</button>
  ),
}));

vi.mock('../../components/Dashboard/DashReportPanel', () => ({
  default: ({ title, dashContent, reportContent, onRefresh }) => (
    <section>
      <h2>{title}</h2>
      {onRefresh && <button type="button" onClick={onRefresh}>Refresh {title}</button>}
      <div>{dashContent}</div>
      <div>{reportContent}</div>
    </section>
  ),
}));

vi.mock('../../components/Dashboard/ExportPDFModal', () => ({
  ExportPDFModal: ({ barbershopName, onClose }) => (
    <div role="dialog" aria-label="Exportar PDF">
      <span>PDF {barbershopName}</span>
      <button type="button" onClick={onClose}>Fechar PDF</button>
    </div>
  ),
}));

vi.mock('../../components/Dashboard/panels/BarberPerformancePanel', () => ({
  BarberPerformancePanel: ({ data }) => <div>Performance dash {data.length}</div>,
  BarberPerformanceTable: ({ data }) => <div>Performance table {data.length}</div>,
}));

vi.mock('../../components/Dashboard/panels/StockHealthPanel', () => ({
  StockHealthPanel: ({ data }) => <div>Stock dash {data.length}</div>,
  StockHealthTable: ({ data }) => <div>Stock table {data.length}</div>,
}));

vi.mock('../../components/Dashboard/panels/AgendaThermometerPanel', () => ({
  AgendaThermometerPanel: ({ data }) => <div>Agenda dash {data.length}</div>,
  AgendaThermometerTable: ({ data }) => <div>Agenda table {data.length}</div>,
}));

vi.mock('../../components/Dashboard/panels/BarberSkillMatrixPanel', () => ({
  BarberSkillMatrixPanel: ({ data }) => <div>Skill dash {data.length}</div>,
  BarberSkillMatrixTable: ({ data }) => <div>Skill table {data.length}</div>,
}));

vi.mock('../../components/Dashboard/panels/CustomerAcquisitionPanel', () => ({
  CustomerAcquisitionPanel: ({ data }) => <div>Acq dash {data.length}</div>,
  CustomerAcquisitionTable: ({ data }) => <div>Acq table {data.length}</div>,
}));

vi.mock('../../components/Dashboard/panels/CustomerRetentionPanel', () => ({
  CustomerRetentionPanel: ({ data }) => <div>Ret dash {data.length}</div>,
  CustomerRetentionTable: ({ data }) => <div>Ret table {data.length}</div>,
}));

vi.mock('../../components/Dashboard/panels/FixedExpensesPanel', () => ({
  FixedExpensesPiePanel: ({ data }) => <div>Expenses pie {data.length}</div>,
  FixedExpensesTable: ({ data, onDelete }) => (
    <div>
      Expenses table {data.length}
      {data.map((item) => (
        <button key={item.id} type="button" onClick={() => onDelete(item.id)}>
          Delete expense {item.id}
        </button>
      ))}
    </div>
  ),
}));

import api from '../../services/api';
import { logoutUser } from '../../services/authService';
import { useAuthGuard } from '../../hooks/useAuthGuard';
import { navigateToBarberTab } from '../../services/navigationService';
import {
  getAgendaThermometer,
  getBarberSkillMatrix,
  getCustomerAcquisition,
  getCustomerRetention,
  getFinancialOverview,
  getMyShopBarberPerformance,
  getStockHealthAlert,
} from '../../services/analyticsService';
import {
  createFixedExpense,
  deleteFixedExpense,
  getMyFixedExpenses,
} from '../../services/barbershopService';
import BarberDashboardPage from '../BarberDashboardPage';

describe('BarberDashboardPage', () => {
  beforeEach(() => {
    navigate.mockReset();
    localStorage.clear();
    vi.mocked(api.get).mockReset();
    vi.mocked(logoutUser).mockReset();
    vi.mocked(useAuthGuard).mockReset();
    vi.mocked(navigateToBarberTab).mockReset();
    vi.mocked(getFinancialOverview).mockReset();
    vi.mocked(getMyShopBarberPerformance).mockReset();
    vi.mocked(getStockHealthAlert).mockReset();
    vi.mocked(getAgendaThermometer).mockReset();
    vi.mocked(getBarberSkillMatrix).mockReset();
    vi.mocked(getCustomerAcquisition).mockReset();
    vi.mocked(getCustomerRetention).mockReset();
    vi.mocked(getMyFixedExpenses).mockReset();
    vi.mocked(createFixedExpense).mockReset();
    vi.mocked(deleteFixedExpense).mockReset();
    vi.mocked(useAuthGuard).mockReturnValue({ isAuthorized: true });
    vi.mocked(api.get).mockResolvedValue({
      data: {
        id: 'owner-1',
        name: 'Dono Dashboard',
        barbershopId: 'shop-1',
        barbershopName: 'Barbearia Dashboard',
      },
    });
    vi.mocked(getFinancialOverview).mockResolvedValue({
      totalServiceRevenue: 1000,
      productExpenses: 150,
    });
    vi.mocked(getMyShopBarberPerformance).mockResolvedValue([{ barberName: 'Ana' }]);
    vi.mocked(getStockHealthAlert).mockResolvedValue([{ productName: 'Pomada' }]);
    vi.mocked(getAgendaThermometer).mockResolvedValue([{ day: 'Seg' }]);
    vi.mocked(getBarberSkillMatrix).mockResolvedValue([{ barberName: 'Ana' }]);
    vi.mocked(getCustomerAcquisition).mockResolvedValue([{ source: 'App' }]);
    vi.mocked(getCustomerRetention).mockResolvedValue([{ month: 'Maio' }]);
    vi.mocked(getMyFixedExpenses).mockResolvedValue([{ id: 'exp-1', amount: 200 }]);
  });

  it('loads dashboard data, exports PDF, refreshes, saves and deletes fixed expenses', async () => {
    vi.mocked(createFixedExpense).mockResolvedValueOnce({});
    vi.mocked(deleteFixedExpense).mockResolvedValueOnce({});

    render(<BarberDashboardPage />);

    expect(await screen.findByText(/análise da sua barbearia/i)).toBeInTheDocument();
    expect(await screen.findByText('Performance dash 1')).toBeInTheDocument();
    expect(screen.getByText('R$ 1000,00')).toBeInTheDocument();
    expect(screen.getByText('R$ 150,00')).toBeInTheDocument();
    expect(screen.getByText('R$ 200,00')).toBeInTheDocument();
    expect(screen.getByText('R$ 650,00')).toBeInTheDocument();
    expect(screen.getByText('Expenses table 1')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /exportar pdf/i }));
    expect(screen.getByRole('dialog', { name: /exportar pdf/i })).toHaveTextContent('PDF Barbearia Dashboard');
    fireEvent.click(screen.getByRole('button', { name: /fechar pdf/i }));
    expect(screen.queryByRole('dialog', { name: /exportar pdf/i })).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /atualizar dados/i }));
    await waitFor(() => expect(getFinancialOverview.mock.calls.length).toBeGreaterThanOrEqual(2));

    fireEvent.click(screen.getByRole('button', { name: /\+ adicionar gasto/i }));
    fireEvent.change(screen.getByPlaceholderText(/aluguel sala 2/i), {
      target: { value: 'Internet loja' },
    });
    fireEvent.change(screen.getAllByRole('spinbutton').at(-1), {
      target: { value: '123.45' },
    });
    fireEvent.click(screen.getByRole('radio', { name: /somente este mês/i }));
    fireEvent.click(screen.getByRole('button', { name: /^salvar$/i }));

    await waitFor(() => expect(createFixedExpense).toHaveBeenCalledWith(expect.objectContaining({
      category: 'OUTROS',
      customName: 'Internet loja',
      amount: 123.45,
      recurringMonthly: false,
    })));

    fireEvent.click(screen.getByRole('button', { name: /delete expense exp-1/i }));
    await waitFor(() => expect(deleteFixedExpense).toHaveBeenCalledWith('exp-1'));
  }, 10000);

  it('navigates tabs, logs out and redirects on auth failures', async () => {
    render(<BarberDashboardPage />);

    await screen.findByText(/análise da sua barbearia/i);
    fireEvent.click(screen.getByText('Header estoque'));
    expect(navigateToBarberTab).toHaveBeenCalledWith('estoque', navigate, {
      isOwner: true,
      currentPath: '/barberHome/dashboard',
    });
    fireEvent.click(screen.getByText('Navbar time'));
    expect(navigateToBarberTab).toHaveBeenCalledWith('time', navigate, {
      isOwner: true,
      currentPath: '/barberHome/dashboard',
    });

    fireEvent.click(screen.getByText('Header logout'));
    await waitFor(() => expect(logoutUser).toHaveBeenCalled());
    expect(navigate).toHaveBeenCalledWith('/');

    vi.mocked(api.get).mockRejectedValueOnce(new Error('auth'));
    render(<BarberDashboardPage />);
    await waitFor(() => expect(navigate).toHaveBeenCalledWith('/'));
  });

  it('keeps loading while the owner guard is pending', () => {
    vi.mocked(useAuthGuard).mockReturnValueOnce({ isAuthorized: false });

    render(<BarberDashboardPage />);

    expect(screen.getByText('Carregando...')).toBeInTheDocument();
    expect(api.get).not.toHaveBeenCalled();
  });
});
