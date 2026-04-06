import React, { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
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
  const [isJoinModalOpen, setIsJoinModalOpen] = useState(false);
  const [joinCnpj, setJoinCnpj] = useState('');
  const [joinError, setJoinError] = useState('');
  const [isSendingJoinRequest, setIsSendingJoinRequest] = useState(false);
  const [isLogoutModalOpen, setIsLogoutModalOpen] = useState(false);
  const navigate = useNavigate();
  const showInsights = import.meta.env.VITE_ENABLE_BARBER_INSIGHTS === 'true';

  const formatCnpj = (value) => {
    const digits = value.replace(/\D/g, '').slice(0, 14);
    return digits
      .replace(/(\d{2})(\d)/, '$1.$2')
      .replace(/(\d{3})(\d)/, '$1.$2')
      .replace(/(\d{3})(\d)/, '$1/$2')
      .replace(/(\d{4})(\d{1,2})$/, '$1-$2');
  };

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
      setActiveTab('home');
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

    setActiveTab(tab);
  };

  const handleCreateShop = () => navigate('/create-barbershop');

  const handleOpenJoinModal = () => {
    setJoinError('');
    setJoinCnpj('');
    setIsJoinModalOpen(true);
  };

  const handleCloseJoinModal = () => {
    if (isSendingJoinRequest) return;
    setIsJoinModalOpen(false);
    setJoinError('');
  };

  const handleJoinCnpjChange = (e) => {
    setJoinCnpj(formatCnpj(e.target.value));
    if (joinError) setJoinError('');
  };

  const handleSubmitJoinRequest = async (e) => {
    e.preventDefault();
    const normalizedCnpj = joinCnpj.replace(/\D/g, '');

    if (normalizedCnpj.length !== 14) {
      setJoinError('Informe um CNPJ valido com 14 numeros.');
      return;
    }

    try {
      setIsSendingJoinRequest(true);
      await api.post('/barbershops/join-request', { cnpj: normalizedCnpj });
      toast.success('Pedido enviado! Aguarde o dono aceitar.');
      setIsJoinModalOpen(false);
      setJoinCnpj('');
      setJoinError('');
    } catch (error) {
      setJoinError('Erro ao enviar pedido. Verifique o CNPJ e tente novamente.');
    } finally {
      setIsSendingJoinRequest(false);
    }
  };

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
          isOwner={isOwnerUser()}
        />

        {!hasLinkedBarbershop ? (
          <NoBarbershopPanel
            onCreateShop={handleCreateShop}
            onJoinShop={handleOpenJoinModal}
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
        <BarberNavbar activeTab={activeTab} onTabChange={handleTabChange} isOwner={isOwnerUser()} />
      )}

      {isJoinModalOpen && (
        <div className={styles.modalBackdrop} onClick={handleCloseJoinModal}>
          <div className={styles.modalCard} onClick={(e) => e.stopPropagation()}>
            <p className={styles.modalKicker}>SOLICITAR ENTRADA</p>
            <h3 className={styles.modalTitle}>Entrar em uma barbearia</h3>
            <p className={styles.modalSubtitle}>Digite o CNPJ da barbearia para enviar seu pedido de vinculacao.</p>

            <form onSubmit={handleSubmitJoinRequest} className={styles.modalForm}>
              <label htmlFor="join-cnpj" className={styles.modalLabel}>CNPJ</label>
              <input
                id="join-cnpj"
                type="text"
                inputMode="numeric"
                className={styles.modalInput}
                placeholder="00.000.000/0000-00"
                value={joinCnpj}
                onChange={handleJoinCnpjChange}
                maxLength={18}
                autoFocus
              />

              {joinError && <p className={styles.modalError}>{joinError}</p>}

              <div className={styles.modalActions}>
                <button type="button" className={styles.modalSecondaryButton} onClick={handleCloseJoinModal}>
                  Cancelar
                </button>
                <button type="submit" className={styles.modalPrimaryButton} disabled={isSendingJoinRequest}>
                  {isSendingJoinRequest ? 'Enviando...' : 'Enviar solicitacao'}
                </button>
              </div>
            </form>
          </div>
        </div>
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