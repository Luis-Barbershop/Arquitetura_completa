vi.mock('react-router-dom', () => ({
  useLocation: () => ({ state: { role: 'barber' } }),
  useNavigate: () => vi.fn(),
  Link: (props) => props.children,
}));

vi.mock('../components/Login/Login_Inputs', () => ({ default: () => 'LOGIN_INPUTS' }));

import React from 'react';
import { render, screen } from '@testing-library/react';
import LoginPage from '../LoginPage';

describe('LoginPage', () => {
  afterEach(() => vi.resetAllMocks());

  it('renders with barber role label', () => {
    render(<LoginPage />);
    expect(screen.getByText(/Perfil:/i)).toBeInTheDocument();
    expect(screen.getAllByText(/Barbeiro/i).length).toBeGreaterThan(0);
    // Ensure the page renders the login form and role tag
    expect(screen.getByText(/Entrar na conta/i)).toBeInTheDocument();
  });
});
