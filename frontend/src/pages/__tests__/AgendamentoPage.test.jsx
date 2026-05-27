vi.mock('react-router-dom', () => ({ useParams: () => ({ barbershopId: '123' }), useNavigate: () => vi.fn() }));

vi.mock('../components/HomePage/CustomerHeader', () => ({ default: () => <div>CustomerHeader</div> }));
vi.mock('../components/HomePage/CustomerNavbar', () => ({ default: () => <div>CustomerNavbar</div> }));
vi.mock('../components/AgendamentoPage/ServicesAgendamento', () => ({ default: ({ data }) => <div data-testid={`service-${data.id}`}>{data.name}</div> }));
vi.mock('../components/BarbershopMap/BarbershopMap', () => ({ default: () => <div>BarbershopMap</div> }));

vi.mock('../services/barbershopService', () => ({
  getShopServices: () => Promise.resolve([{ id: 's1', name: 'Corte', durationMinutes: 30, price: 30 }]),
  getShopBarbers: () => Promise.resolve([{ id: 'b1', name: 'Joao', assignedActivityIds: ['s1'] }]),
  getBarbershopById: () => Promise.resolve({ id: '123', name: 'Barbearia Teste', ownerId: 'owner1', latitude: null, longitude: null }),
}));

vi.mock('../services/api', () => ({ default: { get: () => Promise.resolve({ data: {} }) } }));
vi.mock('../services/appointmentAvailabilityService', () => ({
  createDateOptionsBase: () => [],
  formatDateToApi: d => d,
  formatCompactDate: d => d,
  getRelativeDateLabel: () => 'hoje',
  hydrateDateOptionsWithAvailability: () => Promise.resolve([]),
  fetchAvailabilitySlots: () => Promise.resolve([]),
  clearAvailabilitySlotsCache: () => {},
}));

import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import AgendamentoPage from '../AgendamentoPage';

describe('AgendamentoPage', () => {
  beforeEach(() => {
    localStorage.setItem('token', 'tok');
    localStorage.setItem('userRole', 'ROLE_CUSTOMER');
    localStorage.setItem('isOwner', 'false');
  });

  afterEach(() => {
    localStorage.clear();
    vi.resetAllMocks();
  });

  it('renders hero and services list', async () => {
    render(<AgendamentoPage />);

    await waitFor(() => expect(screen.getByText(/AGENDAMENTO ONLINE/i)).toBeInTheDocument());
    expect(screen.getByText(/Monte seu horário/i)).toBeInTheDocument();
  });
});
