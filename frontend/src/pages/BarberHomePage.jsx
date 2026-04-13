import React, { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import api from '../services/api';
import { logoutUser } from '../services/authService';
import { isCustomer, isOwnerUser } from '../services/userContext';
import BarberHeader from '../components/BarberPage/BarberHeader';
import NoBarbershopPanel from '../components/BarberPage/NoBarbershopPanel';
import styles from './CSS/BarberHomePage.module.css';
import Invoicing from '../components/BarberPage/Invoicing';
import Buttonsbarber from '../components/BarberPage/Buttonsbarber';
import NextScheduling from '../components/BarberPage/NextScheduling';
import BarberNavbar from '../components/BarberPage/BarberNavbar';
import ServicesHomeBarber from '../components/BarberPage/ServicesHomeBarber';
import ActionsBarber from '../components/BarberPage/ActionsBarber';

function BarberHomePage() {
  const location = useLocation();
  const [barber, setBarber] = useState(null);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState(location.state?.activeTab || 'home');
  const [isLogoutModalOpen, setIsLogoutModalOpen] = useState(false);
  const navigate = useNavigate();
  const showInsights = import.meta.env.VITE_ENABLE_BARBER_INSIGHTS === 'true';

  useEffect(() => {
    // Guard: cliente não pode acessar painel de barbeiro
    if (isCustomer()) {
      navigate('/homepage', { replace: true });
      return;
    }

    const token = localStorage.getItem('token');
    if (!token) {
      navigate('/', { replace: true });
      return;
    }

    api.get('/auth/me')
      .then(response => {
        setBarber(response.data);
        // Sincroniza isOwner e barbershopId no localStorage com o valor mais recente do servidor
        if (response.data?.isOwner !== undefined && response.data?.isOwner !== null) {
          localStorage.setItem('isOwner', String(response.data.isOwner));
        }
        if (response.data?.barbershopId) {
          localStorage.setItem('barbershopId', String(response.data.barbershopId));
        }
        setLoading(false);
      })
      .catch(err => {
        console.error(err);
        setLoading(false);
        if (err.response?.status === 403) {
          navigate('/');
        }
      });
  }, [navigate]);

  useEffect(() => {
    if (location.state?.activeTab) {
      setActiveTab(location.state.activeTab);
    }
  }, [location.state]);

  const handleTabChange = (tab) => {
    if (tab === 'home') {
      // Já estamos na home — não faz nada (evita re-render desnecessário)
      return;
    }

    if (tab === 'agenda') {
      navigate('/meus-agendamentos');
      return;
    }

    if (tab === 'servicos') {
      navigate('/barberHome/servicos');
      return;
    }

    if (tab === 'estoque') {
      navigate('/barberHome/estoque');
      return;
    }

    if (tab === 'perfil') {
      navigate('/barberHome/perfil');
      return;
    }

    if (tab === 'time') {
      navigate('/barberHome/time');
      return;
    }

    if (tab === 'dashboards') {
      navigate('/barberHome/dashboard');
      return;
    }

    if (tab === 'novo-agendamento') {
      navigate('/barberHome/novo-agendamento');
      return;
    }
  };

  const handleCreateShop = () => navigate('/create-barbershop');
  const handleGoToProfile = () => navigate('/barberHome/perfil');

  const handleOpenLogoutModal = () => {
    setIsLogoutModalOpen(true);
  };

  const handleCloseLogoutModal = () => {
    setIsLogoutModalOpen(false);
  };

  const handleConfirmLogout = () => {
    logoutUser();
    navigate('/');
  };

  if (loading) return <div className={styles.loadingContainer}>Carregando...</div>;

  const hasLinkedBarbershop = Boolean(barber?.barbershopId);
  const firstName = barber?.name?.split(' ')[0] || 'Profissional';

  return (
    <div className={`${styles.pageContainer} ${hasLinkedBarbershop ? styles.withNavbar : styles.withoutNavbar}`}>
      <div className={styles.contentWrapper}>
        <BarberHeader
          barber={barber}
          onLogout={handleOpenLogoutModal}
          activeTab={activeTab}
          onTabChange={handleTabChange}
          isOwner={Boolean(barber?.isOwner) || isOwnerUser()}
          barbershopId={barber?.barbershopId}
        />

        {!hasLinkedBarbershop ? (
          <NoBarbershopPanel
            onCreateShop={handleCreateShop}
            onGoToProfile={handleGoToProfile}
          />
        ) : (
            <>
            <section className={styles.heroSection}>
              <p className={styles.heroKicker}>HOME DO PROFISSIONAL</p>
              <h1>Ola, {firstName}. Vamos fazer o dia render.</h1>
              <p>Confira os numeros da barbearia, priorize os agendamentos e mantenha os servicos mais procurados em destaque.</p>
            </section>

            <section className={styles.dashboardSection}>
            <Invoicing barber={barber} />
            </section>

            <section className={styles.dashboardSection}>
            <Buttonsbarber />
            </section>

            <section className={styles.dashboardSection}>
            <ActionsBarber
              onNavigateToStock={() => navigate('/barberHome/estoque')}
              barbershopId={barber?.barbershopId}
              showInsights={showInsights}
            />
            </section>

            <section className={styles.dashboardSection}>
            <NextScheduling onViewAll={() => navigate('/meus-agendamentos')} />
            </section>

            <section className={styles.dashboardSection}>
            <ServicesHomeBarber onNavigateToServices={() => navigate('/barberHome/servicos')} />
            </section>

            <section className={styles.dashboardSection}>
           
            </section>
            </>


        //   <div>
        //     <button
        //       onClick={() => navigate('/meus-agendamentos')}
        //       className={styles.agendaButton}
        //     >
        //       Ver Minha Agenda
        //     </button>

        //     <ManageMySkills shopId={barber.barbershopId} />

        //     {barber.owner && <ManageServices />}
        //   </div>
        )}
      </div>
      {hasLinkedBarbershop && (
        <BarberNavbar activeTab={activeTab} onTabChange={handleTabChange} isOwner={barber?.isOwner === true || isOwnerUser()} barbershopId={barber?.barbershopId} />
      )}

      {isLogoutModalOpen && (
        <div className={styles.modalBackdrop} onClick={handleCloseLogoutModal}>
          <div className={`${styles.modalCard} ${styles.logoutModalCard}`} onClick={(e) => e.stopPropagation()}>
            <p className={styles.modalKicker}>CONFIRMAR SAIDA</p>
            <h3 className={styles.modalTitle}>Deseja sair da sua conta?</h3>
            <p className={styles.modalSubtitle}>Ao sair, voce sera redirecionado para a tela de login do profissional.</p>

            <div className={styles.modalActions}>
              <button type="button" className={styles.modalSecondaryButton} onClick={handleCloseLogoutModal}>
                Permanecer
              </button>
              <button type="button" className={styles.modalDangerButton} onClick={handleConfirmLogout}>
                Sair da conta
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default BarberHomePage;