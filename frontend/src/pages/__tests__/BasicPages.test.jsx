import { fireEvent, render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const navigate = vi.fn();
let locationState = {};

vi.mock('react-router-dom', () => ({
  Link: ({ children, to, state, ...props }) => (
    <a href={typeof to === 'string' ? to : '#'} data-state={JSON.stringify(state || {})} {...props}>
      {children}
    </a>
  ),
  useLocation: () => ({ state: locationState, search: '' }),
  useNavigate: () => navigate,
}));

vi.mock('../../components/Sign_In/SignIn_inputs', () => ({
  default: () => <div data-testid="signin-inputs">SignIn inputs</div>,
}));

vi.mock('../../components/Site/Header', () => ({ default: () => <header>Header site</header> }));
vi.mock('../../components/Site/Banner', () => ({ default: () => <section>Banner site</section> }));
vi.mock('../../components/Site/Services', () => ({ default: () => <section>Services site</section> }));
vi.mock('../../components/Site/AboutUs', () => ({ default: () => <section>About site</section> }));
vi.mock('../../components/Site/Tutorial', () => ({ default: () => <section>Tutorial site</section> }));
vi.mock('../../components/Site/Mockup', () => ({ default: () => <section>Mockup site</section> }));
vi.mock('../../components/Site/Faq', () => ({ default: () => <section>Faq site</section> }));
vi.mock('../../components/Site/CTAStats', () => ({ default: () => <section>CTA site</section> }));
vi.mock('../../components/Site/Footer', () => ({ default: () => <footer>Footer site</footer> }));

import RedirectionPage from '../RedirectionPage';
import SignInPage from '../SignInPage';
import Site from '../Site';
import StartPage from '../StartPage';

describe('basic pages', () => {
  beforeEach(() => {
    navigate.mockReset();
    locationState = {};
  });

  it('navigates from start page to login and register identification flows', () => {
    render(<StartPage />);

    fireEvent.click(screen.getByRole('button', { name: /login/i }));
    fireEvent.click(screen.getByRole('button', { name: /cadastre-se/i }));

    expect(navigate).toHaveBeenCalledWith('/identificacao', { state: { mode: 'login' } });
    expect(navigate).toHaveBeenCalledWith('/identificacao', { state: { mode: 'register' } });
  });

  it('selects customer/barber profile according to redirection mode', () => {
    locationState = { mode: 'register' };
    render(<RedirectionPage />);

    expect(screen.getByText(/vamos afiar/i)).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /cadastrar como cliente/i }));
    fireEvent.click(screen.getByRole('button', { name: /cadastrar como barbeiro/i }));
    fireEvent.click(screen.getByRole('button', { name: /voltar para a pagina inicial/i }));

    expect(navigate).toHaveBeenCalledWith('/signin', { state: { role: 'customer' } });
    expect(navigate).toHaveBeenCalledWith('/signin', { state: { role: 'barber' } });
    expect(navigate).toHaveBeenCalledWith('/');
  });

  it('renders sign in page in complete profile barber mode', () => {
    locationState = { role: 'barber', mode: 'complete-profile' };

    render(<SignInPage />);

    expect(screen.getAllByText(/completar perfil/i)).toHaveLength(2);
    expect(screen.getByText(/perfil:/i)).toHaveTextContent('Barbeiro');
    expect(screen.getByTestId('signin-inputs')).toBeInTheDocument();
  });

  it('renders the institutional site page composition', () => {
    render(<Site />);

    expect(screen.getByText('Header site')).toBeInTheDocument();
    expect(screen.getByText('Banner site')).toBeInTheDocument();
    expect(screen.getByText('Footer site')).toBeInTheDocument();
  });
});
