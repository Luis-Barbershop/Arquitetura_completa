import React, { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
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
    const params = new URLSearchParams(location.search);
    const mpLinked = params.get('mpLinked');
    const mpReason = params.get('mpReason');
    if (mpLinked === 'true') {
      toast.success('Conta Mercado Pago vinculada com sucesso!');
    } else if (mpLinked === 'false') {
      if (mpReason === 'oauth_disabled_in_test') {
        toast.info('Vinculação simulada (ambiente de teste). Em produção o OAuth real será usado.');
      } else {
        toast.error('Não foi possível vincular a conta Mercado Pago.');
      }
    }
  }, [location.search]);

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
        if (response.data?.imageUrl) {
          localStorage.setItem('userProfileImage', response.data.imageUrl);
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
    <div className={`ca-page ${styles.pageContainer} ${styles.withNavbar}`}>
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
            <section
              className={`${styles.heroSection} ${styles.animateItem} ${styles.delay1}`}
              data-onboarding-id="barber-home-hero"
            >
              <p className={styles.heroKicker}>HOME DO PROFISSIONAL</p>
              <h1>Olá, {firstName}. Vamos fazer o dia render.</h1>
              <p>Confira os números da barbearia, priorize os agendamentos e mantenha os serviços mais procurados em destaque.</p>
            </section>

            <section className={`${styles.dashboardSection} ${styles.animateItem} ${styles.delay2}`}>
            <Invoicing barber={barber} />
            </section>

            <section className={`${styles.dashboardSection} ${styles.animateItem} ${styles.delay3}`}>
            <Buttonsbarber
              onReportsClick={() => {
                const owner = Boolean(barber?.isOwner) || isOwnerUser();
                navigate(owner ? '/barberHome/dashboard' : '/meus-agendamentos');
              }}
              onMyBookingsClick={() => navigate('/meus-agendamentos')}
            />
            </section>

            <section
              className={`${styles.dashboardSection} ${styles.animateItem} ${styles.delay4}`}
              data-onboarding-id="barber-home-actions"
            >
            <ActionsBarber
              onNavigateToStock={() => navigate('/barberHome/estoque')}
              barbershopId={barber?.barbershopId}
              showInsights={showInsights}
            />
            </section>

            <section
              className={`${styles.dashboardSection} ${styles.animateItem} ${styles.delay5}`}
              data-onboarding-id="barber-home-next"
            >
            <NextScheduling onViewAll={() => navigate('/meus-agendamentos')} />
            </section>

            <section className={`${styles.dashboardSection} ${styles.animateItem} ${styles.delay6}`}>
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
      <BarberNavbar
        activeTab={activeTab}
        onTabChange={handleTabChange}
        onLogout={handleOpenLogoutModal}
        isOwner={barber?.isOwner === true || isOwnerUser()}
        barbershopId={barber?.barbershopId}
      />

      {isLogoutModalOpen && (
        <div className={styles.modalBackdrop} onClick={handleCloseLogoutModal}>
          <div className={`${styles.modalCard} ${styles.logoutModalCard}`} onClick={(e) => e.stopPropagation()}>
            <p className={styles.modalKicker}>CONFIRMAR SAIDA</p>
            <h3 className={styles.modalTitle}>Deseja sair da sua conta?</h3>
            <p className={styles.modalSubtitle}>Ao sair, você será redirecionado para a tela de login do profissional.</p>

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