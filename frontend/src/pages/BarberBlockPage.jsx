import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { CalendarBlank, CalendarX, Clock, Trash } from '@phosphor-icons/react';
import { toast } from 'react-toastify';
import api from '../services/api';
import { logoutUser } from '../services/authService';
import { createBarberBlock, deleteBarberBlock, getBarberBlocks } from '../services/barberBlockService';
import { isCustomer, isOwnerUser } from '../services/userContext';
import { navigateToBarberTab } from '../services/navigationService';
import BarberHeader from '../components/BarberPage/BarberHeader';
import BarberNavbar from '../components/BarberPage/BarberNavbar';
import styles from './CSS/BarberBlockPage.module.css';

const toDateInputValue = (date = new Date()) => {
  const localDate = new Date(date.getTime() - date.getTimezoneOffset() * 60000);
  return localDate.toISOString().slice(0, 10);
};

const parseLocalDate = (value) => {
  const [year, month, day] = value.split('-').map(Number);
  return new Date(year, month - 1, day);
};

const formatDateLabel = (value) => {
  if (!value) return '';
  const [year, month, day] = value.slice(0, 10).split('-');
  return `${day}/${month}/${year}`;
};

const formatTimeLabel = (value) => value?.split('T')?.[1]?.slice(0, 5) || '--:--';

const buildDateTime = (date, time) => `${date}T${time}:00`;

const buildDateRange = (startDate, endDate) => {
  const start = parseLocalDate(startDate);
  const end = parseLocalDate(endDate);
  const dates = [];

  for (const cursor = new Date(start); cursor <= end; cursor.setDate(cursor.getDate() + 1)) {
    dates.push(toDateInputValue(cursor));
  }

  return dates;
};

const getCancelledAppointmentsCount = (block) => Number(block?.cancelledAppointmentsCount) || 0;

const formatCancelledAppointmentsMessage = (count) => (
  count === 1
    ? '1 atendimento foi cancelado automaticamente.'
    : `${count} atendimentos foram cancelados automaticamente.`
);

const buildSuccessMessage = (baseMessage, cancelledCount) => (
  cancelledCount > 0
    ? `${baseMessage} ${formatCancelledAppointmentsMessage(cancelledCount)}`
    : baseMessage
);

function BarberBlockPage() {
  const navigate = useNavigate();
  const today = useMemo(() => toDateInputValue(), []);

  const [barber, setBarber] = useState(null);
  const [loading, setLoading] = useState(true);
  const [loadingBlocks, setLoadingBlocks] = useState(false);
  const [saving, setSaving] = useState(false);
  const [blocks, setBlocks] = useState([]);

  const [listDate, setListDate] = useState(today);
  const [hourDate, setHourDate] = useState(today);
  const [hourStart, setHourStart] = useState('12:00');
  const [hourEnd, setHourEnd] = useState('13:00');
  const [dayMode, setDayMode] = useState('single');
  const [singleDay, setSingleDay] = useState(today);
  const [rangeStart, setRangeStart] = useState(today);
  const [rangeEnd, setRangeEnd] = useState(today);
  const [reason, setReason] = useState('');

  const isOwner = isOwnerUser();
  const barberId = barber?.id || localStorage.getItem('internalUserId');

  const handleLogout = () => {
    logoutUser();
    navigate('/');
  };

  const handleTabChange = (tab) => {
    navigateToBarberTab(tab, navigate, {
      isOwner,
      currentPath: '/barber/indisponibilidade',
    });
  };

  const loadBlocks = useCallback(async (dateOverride = listDate) => {
    if (!barberId || !dateOverride) return;

    try {
      setLoadingBlocks(true);
      const data = await getBarberBlocks({ barberId, date: dateOverride });
      setBlocks(Array.isArray(data) ? data : []);
    } catch (error) {
      console.error('Erro ao carregar bloqueios:', error);
      setBlocks([]);
    } finally {
      setLoadingBlocks(false);
    }
  }, [barberId, listDate]);

  useEffect(() => {
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
      .then((response) => {
        const profile = response.data;
        setBarber(profile);
        if (profile?.barbershopName) localStorage.setItem('barbershopName', profile.barbershopName);
        if (profile?.barbershopId) localStorage.setItem('barbershopId', String(profile.barbershopId));
      })
      .catch((error) => {
        console.error('Erro ao carregar barbeiro:', error);
        navigate('/');
      })
      .finally(() => setLoading(false));
  }, [navigate]);

  useEffect(() => {
    loadBlocks();
  }, [loadBlocks]);

  const createBlock = async (payload) => {
    return createBarberBlock({
      barberId,
      reason,
      ...payload,
    });
  };

  const handleCreateHourBlock = async (event) => {
    event.preventDefault();

    if (!barberId) {
      toast.error('Não foi possível identificar seu perfil de barbeiro.');
      return;
    }

    if (hourEnd <= hourStart) {
      toast.warn('O horário final precisa ser maior que o inicial.');
      return;
    }

    try {
      setSaving(true);
      const block = await createBlock({
        startTime: buildDateTime(hourDate, hourStart),
        endTime: buildDateTime(hourDate, hourEnd),
      });
      const cancelledCount = getCancelledAppointmentsCount(block);
      setListDate(hourDate);
      toast.success(buildSuccessMessage('Bloqueio criado.', cancelledCount));
      await loadBlocks(hourDate);
    } catch (error) {
      console.error('Erro ao criar bloqueio por hora:', error);
    } finally {
      setSaving(false);
    }
  };

  const handleCreateDayBlocks = async (event) => {
    event.preventDefault();

    if (!barberId) {
      toast.error('Não foi possível identificar seu perfil de barbeiro.');
      return;
    }

    const startDate = dayMode === 'single' ? singleDay : rangeStart;
    const endDate = dayMode === 'single' ? singleDay : rangeEnd;

    if (endDate < startDate) {
      toast.warn('A data final precisa ser igual ou posterior à data inicial.');
      return;
    }

    const dates = buildDateRange(startDate, endDate);

    try {
      setSaving(true);
      let cancelledCount = 0;
      for (const date of dates) {
        const block = await createBlock({
          startTime: `${date}T00:00:00`,
          endTime: `${date}T23:59:59`,
        });
        cancelledCount += getCancelledAppointmentsCount(block);
      }
      setListDate(startDate);
      toast.success(buildSuccessMessage(
        dates.length === 1 ? 'Dia bloqueado.' : 'Período bloqueado.',
        cancelledCount,
      ));
      await loadBlocks(startDate);
    } catch (error) {
      console.error('Erro ao criar bloqueio por dia:', error);
    } finally {
      setSaving(false);
    }
  };

  const handleDeleteBlock = async (blockId) => {
    try {
      await deleteBarberBlock(blockId);
      toast.success('Bloqueio removido.');
      await loadBlocks();
    } catch (error) {
      console.error('Erro ao remover bloqueio:', error);
    }
  };

  if (loading) {
    return <div className={styles.loadingContainer}>Carregando indisponibilidade...</div>;
  }

  return (
    <div className={`${styles.pageContainer} ${styles.withNavbar}`}>
      <div className={styles.contentWrapper}>
        <BarberHeader
          barber={barber}
          onLogout={handleLogout}
          activeTab="indisponibilidade"
          onTabChange={handleTabChange}
        />

        <section className={styles.heroSection}>
          <p className={styles.heroKicker}>INDISPONIBILIDADE</p>
          <h1>Bloqueie horários, folgas e períodos fora da agenda</h1>
          <p>Os bloqueios impedem novos agendamentos no intervalo escolhido, cancelam automaticamente atendimentos existentes na janela e aparecem por data para conferência rápida.</p>
        </section>

        <section className={styles.grid}>
          <article className={styles.panel}>
            <div className={styles.panelHeader}>
              <div>
                <p className={styles.panelKicker}>Por horário</p>
                <h2>Bloqueio avulso</h2>
              </div>
              <Clock size={22} weight="duotone" />
            </div>

            <form className={styles.form} onSubmit={handleCreateHourBlock}>
              <label className={styles.label} htmlFor="hour-date">Data</label>
              <input
                id="hour-date"
                className={styles.input}
                type="date"
                value={hourDate}
                onChange={(event) => setHourDate(event.target.value)}
                required
              />

              <div className={styles.twoColumns}>
                <div>
                  <label className={styles.label} htmlFor="hour-start">Início</label>
                  <input
                    id="hour-start"
                    className={styles.input}
                    type="time"
                    value={hourStart}
                    onChange={(event) => setHourStart(event.target.value)}
                    required
                  />
                </div>
                <div>
                  <label className={styles.label} htmlFor="hour-end">Fim</label>
                  <input
                    id="hour-end"
                    className={styles.input}
                    type="time"
                    value={hourEnd}
                    onChange={(event) => setHourEnd(event.target.value)}
                    required
                  />
                </div>
              </div>

              <label className={styles.label} htmlFor="hour-reason">Motivo</label>
              <input
                id="hour-reason"
                className={styles.input}
                value={reason}
                onChange={(event) => setReason(event.target.value)}
                maxLength={255}
                placeholder="Ex: Almoço, consulta, compromisso"
              />

              <button className={styles.primaryButton} type="submit" disabled={saving}>
                {saving ? 'Salvando...' : 'Bloquear horário'}
              </button>
            </form>
          </article>

          <article className={styles.panel}>
            <div className={styles.panelHeader}>
              <div>
                <p className={styles.panelKicker}>Dia inteiro</p>
                <h2>Folga ou período</h2>
              </div>
              <CalendarX size={22} weight="duotone" />
            </div>

            <form className={styles.form} onSubmit={handleCreateDayBlocks}>
              <div className={styles.segmented}>
                <button
                  type="button"
                  className={dayMode === 'single' ? styles.segmentActive : styles.segment}
                  onClick={() => setDayMode('single')}
                >
                  Um dia
                </button>
                <button
                  type="button"
                  className={dayMode === 'range' ? styles.segmentActive : styles.segment}
                  onClick={() => setDayMode('range')}
                >
                  Período
                </button>
              </div>

              {dayMode === 'single' ? (
                <>
                  <label className={styles.label} htmlFor="single-day">Data</label>
                  <input
                    id="single-day"
                    className={styles.input}
                    type="date"
                    value={singleDay}
                    onChange={(event) => setSingleDay(event.target.value)}
                    required
                  />
                </>
              ) : (
                <div className={styles.twoColumns}>
                  <div>
                    <label className={styles.label} htmlFor="range-start">Início</label>
                    <input
                      id="range-start"
                      className={styles.input}
                      type="date"
                      value={rangeStart}
                      onChange={(event) => setRangeStart(event.target.value)}
                      required
                    />
                  </div>
                  <div>
                    <label className={styles.label} htmlFor="range-end">Fim</label>
                    <input
                      id="range-end"
                      className={styles.input}
                      type="date"
                      value={rangeEnd}
                      onChange={(event) => setRangeEnd(event.target.value)}
                      required
                    />
                  </div>
                </div>
              )}

              <button className={styles.secondaryButton} type="submit" disabled={saving}>
                {saving ? 'Salvando...' : 'Bloquear dia'}
              </button>
            </form>
          </article>
        </section>

        <section className={styles.listPanel}>
          <div className={styles.listHeader}>
            <div>
              <p className={styles.panelKicker}>Bloqueios ativos</p>
              <h2>{formatDateLabel(listDate)}</h2>
            </div>
            <label className={styles.dateFilter} htmlFor="list-date">
              <CalendarBlank size={16} weight="duotone" />
              <input
                id="list-date"
                type="date"
                value={listDate}
                onChange={(event) => setListDate(event.target.value)}
              />
            </label>
          </div>

          {loadingBlocks ? (
            <p className={styles.emptyText}>Carregando bloqueios...</p>
          ) : blocks.length ? (
            <ul className={styles.blockList}>
              {blocks.map((block) => (
                <li key={block.id} className={styles.blockItem}>
                  <div>
                    <strong>{formatTimeLabel(block.startTime)} - {formatTimeLabel(block.endTime)}</strong>
                    <span>{block.reason || 'Sem motivo informado'}</span>
                  </div>
                  <button
                    type="button"
                    className={styles.deleteButton}
                    onClick={() => handleDeleteBlock(block.id)}
                    aria-label="Remover bloqueio"
                  >
                    <Trash size={18} weight="duotone" />
                  </button>
                </li>
              ))}
            </ul>
          ) : (
            <p className={styles.emptyText}>Nenhum bloqueio cadastrado para esta data.</p>
          )}
        </section>
      </div>

      <BarberNavbar
        activeTab="indisponibilidade"
        onTabChange={handleTabChange}
        onLogout={handleLogout}
      />
    </div>
  );
}

export default BarberBlockPage;
