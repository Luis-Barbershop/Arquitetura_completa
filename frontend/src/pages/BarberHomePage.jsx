import React, { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import api from '../services/api';
import { logoutUser } from '../services/authService';
import { isOwnerUser } from '../services/userContext';
import { useAuthGuard } from '../hooks/useAuthGuard';
import { navigateToBarberTab } from '../services/navigationService';
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
  const { isAuthorized } = useAuthGuard({
    allowCustomer: false,
    allowBarber: true,
  });
  const showInsights = import.meta.env.VITE_ENABLE_BARBER_INSIGHTS === 'true';

  useEffect(() => {
    if (!isAuthorized) {
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
  }, [isAuthorized, navigate]);

  useEffect(() => {
    if (location.state?.activeTab) {
      setActiveTab(location.state.activeTab);
    }
  }, [location.state]);

  const handleTabChange = (tab) => {
    const owner = Boolean(barber?.isOwner) || isOwnerUser();
    navigateToBarberTab(tab, navigate, {
      isOwner: owner,
      currentPath: '/barberHome',
    });
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
    <div className={`ca-page ${styles.pageContainer} ${hasLinkedBarbershop ? styles.withNavbar : styles.withoutNavbar}`}>
      <div className={`ca-container ${styles.contentWrapper}`}>
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
            <Buttonsbarber
              onReportsClick={() => {
                const owner = Boolean(barber?.isOwner) || isOwnerUser();
                navigate(owner ? '/barberHome/dashboard' : '/meus-agendamentos');
              }}
              onMyBookingsClick={() => navigate('/meus-agendamentos')}
            />
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