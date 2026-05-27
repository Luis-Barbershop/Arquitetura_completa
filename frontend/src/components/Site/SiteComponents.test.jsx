import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';

const navigate = vi.fn();

vi.mock('react-router-dom', () => ({
  useNavigate: () => navigate,
}));

import AboutUs from './AboutUs';
import BannerSite from './Banner';
import CTAStats from './CTAStats';
import Faq from './Faq';
import Footer from './Footer';
import HeaderSite from './Header';
import Mockup from './Mockup';
import Services from './Services';
import Tutorial from './Tutorial';

describe('site components', () => {
  it('renders institutional sections and CTA content', () => {
    render(
      <>
        <BannerSite />
        <AboutUs />
        <CTAStats />
      </>,
    );

    expect(screen.getByText(/seu negocio em alta/i)).toBeInTheDocument();
    expect(screen.getByText(/sobre nós/i)).toBeInTheDocument();
    expect(screen.getByText(/cortes finalizados/i)).toBeInTheDocument();
  });

  it('renders remaining institutional content blocks', () => {
    render(
      <>
        <Services />
        <Tutorial />
        <Mockup />
        <Footer />
      </>,
    );

    expect(screen.getByText(/nossos serviços/i)).toBeInTheDocument();
    expect(screen.getByText(/simplicidade em cada/i)).toBeInTheDocument();
    expect(screen.getByText(/um novo ritmo/i)).toBeInTheDocument();
    expect(screen.getByText(/todos os direitos reservados/i)).toBeInTheDocument();
  });

  it('toggles FAQ answers', () => {
    render(<Faq />);
    const firstQuestion = screen.getByRole('button', { name: /como faco para agendar/i });

    expect(firstQuestion).toHaveAttribute('aria-expanded', 'true');
    fireEvent.click(firstQuestion);
    expect(firstQuestion).toHaveAttribute('aria-expanded', 'false');
  });

  it('navigates from header auth buttons and scrolls nav links', () => {
    const target = document.createElement('div');
    target.id = 'inicio';
    target.scrollIntoView = vi.fn();
    document.body.appendChild(target);

    render(<HeaderSite />);

    fireEvent.click(screen.getByText('INICIO'));
    fireEvent.click(screen.getByRole('button', { name: /cadastro/i }));
    fireEvent.click(screen.getByRole('button', { name: /login/i }));

    expect(target.scrollIntoView).toHaveBeenCalledWith({ behavior: 'smooth', block: 'start' });
    expect(navigate).toHaveBeenCalledWith('/identificacao', { state: { mode: 'register' } });
    expect(navigate).toHaveBeenCalledWith('/identificacao', { state: { mode: 'login' } });
  });
});
