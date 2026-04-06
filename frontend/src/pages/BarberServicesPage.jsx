import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';
import { logoutUser } from '../services/authService';
import { createService, deleteService, getMyServices } from '../services/barbershopService';
import BarberHeader from '../components/BarberPage/BarberHeader';
import BarberNavbar from '../components/BarberPage/BarberNavbar';
import ManageMySkills from '../components/BarberPage/ManageMySkills';
import styles from './CSS/BarberServicesPage.module.css';

function BarberServicesPage() {
  const navigate = useNavigate();

  const [barber, setBarber] = useState(null);
  const [loadingBarber, setLoadingBarber] = useState(true);
  const [loadingServices, setLoadingServices] = useState(true);
  const [services, setServices] = useState([]);

  const [name, setName] = useState('');
  const [price, setPrice] = useState('');
  const [duration, setDuration] = useState('30');
  const [isSaving, setIsSaving] = useState(false);
  const [toast, setToast] = useState(null);
  const [servicePendingDelete, setServicePendingDelete] = useState(null);
  const [skillsRefreshKey, setSkillsRefreshKey] = useState(0);

  const toastTimerRef = useRef(null);

  const isOwner = useMemo(() => {
    const ownerFlag = barber?.isOwner ?? barber?.owner;
    const ownerFromProfile = ownerFlag === true || ownerFlag === 'true';
    const role = barber?.role;
    const ownerFromRole = Array.isArray(role)
      ? role.some((item) => String(item).toUpperCase().includes('OWNER'))
      : String(role || '').toUpperCase().includes('OWNER');

    const ownerFromStorage =
      localStorage.getItem('isOwner') === 'true' ||
      String(localStorage.getItem('userRole') || '').toUpperCase().includes('OWNER');

    return ownerFromProfile || ownerFromRole || ownerFromStorage;
  }, [barber]);

  const showToast = useCallback((message, type = 'info') => {
    setToast({ message, type });

    if (toastTimerRef.current) {
      window.clearTimeout(toastTimerRef.current);
    }

    toastTimerRef.current = window.setTimeout(() => {
      setToast(null);
    }, 3200);
  }, []);

  const handleTabChange = (tab) => {
    if (tab === 'servicos') return;

    if (tab === 'home') {
      navigate('/barberHome');
      return;
    }

    if (tab === 'agenda') {
      navigate('/meus-agendamentos');
      return;
    }

    if (tab === 'estoque') {
      navigate('/barberHome/estoque');
      return;
    }

    navigate('/barberHome', { state: { activeTab: tab } });
  };

  const handleLogout = () => {
    logoutUser();
    navigate('/');
  };

  const loadServices = useCallback(async () => {
    try {
      setLoadingServices(true);
      const data = await getMyServices();
      setServices(data);
    } catch (error) {
      console.error('Erro ao carregar serviços:', error);
      setServices([]);
    } finally {
      setLoadingServices(false);
    }
  }, []);

  useEffect(() => {
    const token = localStorage.getItem('token');

    if (!token) {
      navigate('/identificacao', { state: { mode: 'login', role: 'barber' } });
      return;
    }

    api.get('/auth/me')
      .then((response) => {
        setBarber(response.data);
        setLoadingBarber(false);
      })
      .catch((err) => {
        console.error(err);
        setLoadingBarber(false);
        navigate('/identificacao', { state: { mode: 'login', role: 'barber' } });
      });
  }, [navigate]);

  useEffect(() => {
    loadServices();
  }, [loadServices]);

  useEffect(() => {
    // Mantém o painel atualizado automaticamente, sem ação manual.
    const intervalId = window.setInterval(() => {
      loadServices();
    }, 15000);

    return () => window.clearInterval(intervalId);
  }, [loadServices]);

  useEffect(() => () => {
    if (toastTimerRef.current) {
      window.clearTimeout(toastTimerRef.current);
    }
  }, []);

  const totalServices = services.length;

  const averagePrice = useMemo(() => {
    if (!services.length) return 'R$ 0,00';
    const total = services.reduce((sum, item) => sum + Number(item.price || 0), 0);
    return `R$ ${(total / services.length).toFixed(2).replace('.', ',')}`;
  }, [services]);

  const averageDuration = useMemo(() => {
    if (!services.length) return '0 min';
    const total = services.reduce((sum, item) => sum + Number(item.durationMinutes || 0), 0);
    return `${Math.round(total / services.length)} min`;
  }, [services]);

  const handleAddService = async (e) => {
    e.preventDefault();

    if (!name.trim() || !price || !duration) {
      return;
    }

    const parsedPrice = Number(String(price).replace(',', '.').trim());
    const parsedDuration = Number(duration);

    if (!Number.isFinite(parsedPrice) || parsedPrice <= 0) {
      showToast('Informe um preco valido maior que zero.', 'warning');
      return;
    }

    if (!Number.isFinite(parsedDuration) || parsedDuration < 5) {
      showToast('Informe uma duracao valida em minutos (minimo 5).', 'warning');
      return;
    }
    if (parsedDuration > 300) {
      showToast('A duracao maxima de um servico e 300 minutos.', 'warning');
      return;
    }

    try {
      setIsSaving(true);

      await createService({
        activityName: name.trim(),
        price: parsedPrice,
        durationMinutes: parsedDuration,
      });

      setName('');
      setPrice('');
      setDuration('30');
      await loadServices();
      setSkillsRefreshKey((prev) => prev + 1);
      showToast('Servico adicionado com sucesso.', 'success');
    } catch (error) {
      console.error('Erro ao criar serviço:', error);
      const status = error?.response?.status;
      const backendMessage =
        error?.response?.data?.message ||
        error?.response?.data?.error ||
        null;

      if (status === 403) {
        showToast('Apenas o dono da barbearia pode cadastrar servicos.', 'error');
      } else if (status === 400 && backendMessage) {
        showToast(`Nao foi possivel adicionar o servico: ${backendMessage}`, 'error');
      } else {
        showToast('Nao foi possivel adicionar o servico. Tente novamente.', 'error');
      }
    } finally {
      setIsSaving(false);
    }
  };

  const handleDeleteService = (service) => {
    setServicePendingDelete(service);
  };

  const confirmDeleteService = async () => {
    if (!servicePendingDelete) return;

    try {
      await deleteService(servicePendingDelete.id);
      await loadServices();
      setSkillsRefreshKey((prev) => prev + 1);
      showToast('Servico excluido com sucesso.', 'success');
    } catch (error) {
      console.error('Erro ao excluir serviço:', error);
      showToast('Nao foi possivel excluir o servico.', 'error');
    } finally {
      setServicePendingDelete(null);
    }
  };

  if (loadingBarber) {
    return <div className={styles.loadingContainer}>Carregando...</div>;
  }

  return (
    <div className={styles.pageContainer}>
      <div className={styles.contentWrapper}>
        <BarberHeader
          barber={barber}
          onLogout={handleLogout}
          activeTab="servicos"
          onTabChange={handleTabChange}
        />

        <section className={styles.heroSection}>
          <p className={styles.heroKicker}>GESTÃO DE SERVIÇOS</p>
          <h1>Cadastre e organize os serviços da sua barbearia</h1>
          <p>
            Mantenha seu catálogo atualizado para facilitar os agendamentos e melhorar a experiência dos clientes.
          </p>
        </section>

        <section className={styles.metricsGrid}>
          <article className={styles.metricCard}>
            <span className={styles.metricLabel}>Total de serviços</span>
            <strong className={styles.metricValue}>{totalServices}</strong>
          </article>

          <article className={styles.metricCard}>
            <span className={styles.metricLabel}>Preço médio</span>
            <strong className={styles.metricValue}>{averagePrice}</strong>
          </article>

          <article className={styles.metricCard}>
            <span className={styles.metricLabel}>Duração média</span>
            <strong className={styles.metricValue}>{averageDuration}</strong>
          </article>
        </section>

        <section className={styles.managementGrid}>
          <article className={`${styles.panelCard} ${styles.servicesPanel}`}>
            <div className={styles.panelHeader}>
              <h2>Serviços Cadastrados</h2>
              <span className={styles.autoUpdateBadge}>Atualização automática</span>
            </div>

            {loadingServices ? (
              <p className={styles.mutedText}>Carregando serviços...</p>
            ) : services.length ? (
              <ul className={styles.servicesList}>
                {services.map((service) => (
                  <li key={service.id} className={styles.serviceItem}>
                    <div className={styles.serviceMainInfo}>
                      <p className={styles.serviceName}>{service.activityName}</p>
                      <span className={styles.serviceMeta}>{service.durationMinutes} min</span>
                    </div>

                    <div className={styles.serviceActions}>
                      <strong className={styles.servicePrice}>R$ {Number(service.price).toFixed(2).replace('.', ',')}</strong>
                      <button
                        type="button"
                        className={styles.deleteButton}
                        onClick={() => handleDeleteService(service)}
                      >
                        Excluir
                      </button>
                    </div>
                  </li>
                ))}
              </ul>
            ) : (
              <p className={styles.mutedText}>Você ainda não cadastrou serviços. Use o formulário ao lado para adicionar.</p>
            )}
          </article>

          <article className={styles.panelCard}>
            <div className={styles.panelHeader}>
              <h2>Novo serviço</h2>
            </div>

            {!isOwner && (
              <p className={styles.mutedText}>
                Apenas o dono pode cadastrar servicos. Se voce acabou de criar a barbearia,
                faca logout e login para atualizar seu token.
              </p>
            )}

            <form className={styles.form} onSubmit={handleAddService}>
              <label className={styles.formLabel} htmlFor="service-name">Nome do serviço</label>
              <input
                id="service-name"
                className={styles.formInput}
                type="text"
                placeholder="Ex: Corte degradê"
                value={name}
                onChange={(e) => setName(e.target.value)}
                disabled={isSaving}
                required
              />

              <label className={styles.formLabel} htmlFor="service-price">Preço (R$)</label>
              <input
                id="service-price"
                className={styles.formInput}
                type="number"
                min="0"
                step="0.01"
                placeholder="0.00"
                value={price}
                onChange={(e) => setPrice(e.target.value)}
                disabled={isSaving}
                required
              />

              <label className={styles.formLabel} htmlFor="service-duration">Duração (minutos)</label>
              <input
                id="service-duration"
                className={styles.formInput}
                type="number"
                min="5"
                max="300"
                step="5"
                placeholder="30"
                value={duration}
                onChange={(e) => setDuration(e.target.value)}
                disabled={isSaving}
                required
              />

              <button type="submit" className={styles.primaryButton} disabled={isSaving}>
                {isSaving ? 'Salvando...' : 'Adicionar serviço'}
              </button>
            </form>
          </article>
        </section>

        {barber?.barbershopId && (
          <section className={styles.assignSection}>
            <article className={styles.panelCard}>
              <div className={styles.panelHeader}>
                <h2>Atribuir serviços ao meu perfil</h2>
              </div>
              <p className={styles.mutedText}>
                Selecione e salve aqui os serviços que voce realmente executa. Esse vinculo
                e obrigatorio para liberar o agendamento no fluxo do backend.
              </p>
              <ManageMySkills shopId={barber.barbershopId} refreshKey={skillsRefreshKey} />
            </article>
          </section>
        )}
      </div>

      {toast && (
        <div className={`${styles.toast} ${styles[`toast${toast.type.charAt(0).toUpperCase()}${toast.type.slice(1)}`]}`}>
          {toast.message}
        </div>
      )}

      {servicePendingDelete && (
        <div className={styles.confirmOverlay}>
          <div className={styles.confirmModal}>
            <h3>Excluir servico</h3>
            <p>
              Deseja realmente excluir <strong>{servicePendingDelete.activityName}</strong>?
            </p>
            <div className={styles.confirmActions}>
              <button
                type="button"
                className={styles.confirmCancelButton}
                onClick={() => setServicePendingDelete(null)}
              >
                Cancelar
              </button>
              <button
                type="button"
                className={styles.confirmDeleteButton}
                onClick={confirmDeleteService}
              >
                Excluir
              </button>
            </div>
          </div>
        </div>
      )}

      <BarberNavbar activeTab="servicos" onTabChange={handleTabChange} />
    </div>
  );
}

export default BarberServicesPage;

