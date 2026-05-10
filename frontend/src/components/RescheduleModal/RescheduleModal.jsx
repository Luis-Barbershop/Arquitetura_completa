import React, { useEffect, useMemo, useState } from 'react';
import { FiChevronDown, FiChevronUp } from 'react-icons/fi';
import {
  createDateOptionsBase,
  formatCompactDate,
  formatDateToApi,
  getRelativeDateLabel,
  hydrateDateOptionsWithAvailability,
} from '../../services/appointmentAvailabilityService';
import { getShopBarbers } from '../../services/barbershopService';
import Styles from './RescheduleModal.module.css';

const getInitials = (name = '') =>
  name
    .split(' ')
    .slice(0, 2)
    .map((p) => p[0])
    .join('')
    .toUpperCase();

const formatDateLabel = (dateObj) => {
  if (!dateObj) return '--';
  const d = dateObj instanceof Date ? dateObj : new Date(dateObj);
  return d.toLocaleDateString('pt-BR', {
    weekday: 'long',
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  });
};

// STEP 1 — seleção de barbeiro
// STEP 2 — seleção de data/hora
const STEP_BARBER = 1;
const STEP_DATETIME = 2;

const RescheduleModal = ({ appointment, onClose, onConfirm, isSubmitting }) => {
  const { barbershopId, barberId: currentBarberId } = appointment;
  const durationMinutes = appointment.totalDurationMinutes || appointment.durationMinutes || 30;

  /* ── estado ── */
  const [step, setStep] = useState(STEP_BARBER);
  const [barbers, setBarbers] = useState([]);
  const [loadingBarbers, setLoadingBarbers] = useState(true);
  const [selectedBarberId, setSelectedBarberId] = useState(String(currentBarberId || ''));

  const [dateOptions, setDateOptions] = useState([]);
  const [loadingDates, setLoadingDates] = useState(false);
  const [selectedDate, setSelectedDate] = useState(null);
  const [selectedTime, setSelectedTime] = useState('');
  const [expandedPeriods, setExpandedPeriods] = useState({ morning: true, afternoon: true });

  /* ── carrega barbeiros ── */
  useEffect(() => {
    if (!barbershopId) return;
    setLoadingBarbers(true);
    getShopBarbers(barbershopId)
      .then((data) => setBarbers(Array.isArray(data) ? data : []))
      .catch(() => setBarbers([]))
      .finally(() => setLoadingBarbers(false));
  }, [barbershopId]);

  /* ── carrega datas ao entrar no step 2 ── */
  useEffect(() => {
    if (step !== STEP_DATETIME || !selectedBarberId) return;

    setLoadingDates(true);
    setDateOptions([]);
    setSelectedDate(null);
    setSelectedTime('');

    const base = createDateOptionsBase(14).map((opt, i) => ({
      ...opt,
      label: getRelativeDateLabel(opt.date, i),
      compact: formatCompactDate(opt.date),
    }));

    hydrateDateOptionsWithAvailability({
      barberId: selectedBarberId,
      durationMinutes,
      dateOptions: base,
      minAdvanceHours: 3,
    })
      .then((hydrated) => {
        setDateOptions(hydrated);
        const first = hydrated.find((o) => o.isAvailable);
        if (first) {
          setSelectedDate(first.date);
          setSelectedTime(first.slots[0] || '');
        }
      })
      .catch(() => setDateOptions([]))
      .finally(() => setLoadingDates(false));
  }, [step, selectedBarberId, durationMinutes]);

  /* ── slots da data selecionada ── */
  const currentSlots = useMemo(() => {
    if (!selectedDate) return [];
    const key = formatDateToApi(selectedDate);
    return dateOptions.find((o) => o.key === key)?.slots || [];
  }, [selectedDate, dateOptions]);

  const groupedSlots = useMemo(() => {
    const morning = [];
    const afternoon = [];
    currentSlots.forEach((slot) => {
      const hour = Number(String(slot).split(':')[0]);
      if (!Number.isNaN(hour)) {
        (hour < 12 ? morning : afternoon).push(slot);
      }
    });
    return { morning, afternoon };
  }, [currentSlots]);

  /* ── helpers ── */
  const selectedBarberData = barbers.find((b) => String(b.id) === selectedBarberId);
  const nextAvailableDate = dateOptions.find((o) => o.isAvailable);

  const handleConfirm = () => {
    if (!selectedDate || !selectedTime) return;
    let time = selectedTime;
    if (time.length === 5) time = `${time}:00`;
    const startTime = `${formatDateToApi(selectedDate)}T${time}`;
    onConfirm(startTime, selectedBarberId !== String(currentBarberId) ? selectedBarberId : null);
  };

  const canConfirm = selectedDate && selectedTime && !isSubmitting;

  return (
    <div className={Styles.backdrop} onClick={onClose}>
      <div className={Styles.card} onClick={(e) => e.stopPropagation()}>

        {/* ── cabeçalho ── */}
        <div className={Styles.header}>
          <p className={Styles.kicker}>REAGENDAR ATENDIMENTO</p>
          <h3 className={Styles.title}>
            {step === STEP_BARBER ? 'Escolha o profissional' : 'Escolha data e horário'}
          </h3>
          {step === STEP_BARBER && (
            <p className={Styles.subtitle}>
              Pode manter o mesmo profissional ou trocar para outro disponível.
            </p>
          )}
          {step === STEP_DATETIME && selectedBarberData && (
            <p className={Styles.subtitle}>
              Profissional selecionado: <strong>{selectedBarberData.name}</strong>
            </p>
          )}
        </div>

        {/* ── conteúdo ── */}
        <div className={Styles.body}>

          {/* STEP 1 — barbeiros */}
          {step === STEP_BARBER && (
            loadingBarbers ? (
              <div className={Styles.spinner} aria-label="Carregando profissionais" />
            ) : barbers.length === 0 ? (
              <p className={Styles.empty}>Nenhum profissional disponível.</p>
            ) : (
              <div className={Styles.barberGrid}>
                {barbers.map((barber) => {
                  const isSelected = String(barber.id) === selectedBarberId;
                  const isCurrent = String(barber.id) === String(currentBarberId);
                  return (
                    <button
                      key={barber.id}
                      type="button"
                      className={`${Styles.barberCard} ${isSelected ? Styles.barberCardSelected : ''}`}
                      onClick={() => setSelectedBarberId(String(barber.id))}
                    >
                      {barber.imageUrl ? (
                        <>
                          <img
                            src={barber.imageUrl}
                            alt={barber.name}
                            className={Styles.barberAvatar}
                            onError={(e) => {
                              e.currentTarget.style.display = 'none';
                              const sibling = e.currentTarget.nextElementSibling;
                              if (sibling) sibling.style.display = 'inline-flex';
                            }}
                          />
                          <span className={Styles.barberInitials} style={{ display: 'none' }}>
                            {getInitials(barber.name)}
                          </span>
                        </>
                      ) : (
                        <span className={Styles.barberInitials}>
                          {getInitials(barber.name)}
                        </span>
                      )}
                      <span className={Styles.barberName}>{barber.name}</span>
                      {isCurrent && (
                        <span className={Styles.currentTag}>atual</span>
                      )}
                    </button>
                  );
                })}
              </div>
            )
          )}

          {/* STEP 2 — datas e horários */}
          {step === STEP_DATETIME && (
            <>
              {/* painel de info */}
              <div className={Styles.dateInfoPanel}>
                <p className={Styles.dateInfoKicker}>DATA ESCOLHIDA</p>
                <h4 className={Styles.dateInfoValue}>
                  {selectedDate ? formatDateLabel(selectedDate) : 'Selecione um dia'}
                </h4>
                {nextAvailableDate ? (
                  <p className={Styles.dateInfoHint}>
                    Próximo disponível: {formatDateLabel(nextAvailableDate.date)}.
                  </p>
                ) : (
                  !loadingDates && (
                    <p className={Styles.dateInfoHint}>Sem horários disponíveis nos próximos 14 dias.</p>
                  )
                )}
              </div>

              {/* trilho de datas */}
              <div className={Styles.dateRail}>
                {loadingDates ? (
                  <div className={Styles.spinner} aria-label="Carregando datas" />
                ) : dateOptions.map((opt) => (
                  <button
                    key={opt.key}
                    type="button"
                    className={`${Styles.dateChip} ${selectedDate && opt.key === formatDateToApi(selectedDate) ? Styles.dateChipSelected : ''}`}
                    disabled={!opt.isAvailable}
                    onClick={() => {
                      if (!opt.isAvailable) return;
                      setSelectedDate(opt.date);
                      setSelectedTime(opt.slots[0] || '');
                    }}
                  >
                    <span className={Styles.dateChipLabel}>{opt.label}</span>
                    <strong className={Styles.dateChipValue}>{opt.compact}</strong>
                    <small className={Styles.dateChipMeta}>
                      {opt.isAvailable ? `${opt.slots.length} horários` : 'Indisponível'}
                    </small>
                  </button>
                ))}
              </div>

              {/* horários */}
              {selectedDate && (
                currentSlots.length > 0 ? (
                  <div className={Styles.slotPeriods}>
                    {groupedSlots.morning.length > 0 && (
                      <div className={Styles.periodCard}>
                        <button
                          type="button"
                          className={Styles.periodToggle}
                          onClick={() => setExpandedPeriods((p) => ({ ...p, morning: !p.morning }))}
                        >
                          <span>Manhã (até 11:59)</span>
                          <span className={Styles.periodMeta}>
                            {groupedSlots.morning.length} horários
                            {expandedPeriods.morning ? <FiChevronUp /> : <FiChevronDown />}
                          </span>
                        </button>
                        {expandedPeriods.morning && (
                          <div className={Styles.slotGrid}>
                            {groupedSlots.morning.map((t) => (
                              <button
                                key={t}
                                type="button"
                                className={`${Styles.slotBtn} ${selectedTime === t ? Styles.slotBtnSelected : ''}`}
                                onClick={() => setSelectedTime((prev) => (prev === t ? '' : t))}
                              >
                                {t.substring(0, 5)}
                              </button>
                            ))}
                          </div>
                        )}
                      </div>
                    )}

                    {groupedSlots.afternoon.length > 0 && (
                      <div className={Styles.periodCard}>
                        <button
                          type="button"
                          className={Styles.periodToggle}
                          onClick={() => setExpandedPeriods((p) => ({ ...p, afternoon: !p.afternoon }))}
                        >
                          <span>Tarde (a partir de 12:00)</span>
                          <span className={Styles.periodMeta}>
                            {groupedSlots.afternoon.length} horários
                            {expandedPeriods.afternoon ? <FiChevronUp /> : <FiChevronDown />}
                          </span>
                        </button>
                        {expandedPeriods.afternoon && (
                          <div className={Styles.slotGrid}>
                            {groupedSlots.afternoon.map((t) => (
                              <button
                                key={t}
                                type="button"
                                className={`${Styles.slotBtn} ${selectedTime === t ? Styles.slotBtnSelected : ''}`}
                                onClick={() => setSelectedTime((prev) => (prev === t ? '' : t))}
                              >
                                {t.substring(0, 5)}
                              </button>
                            ))}
                          </div>
                        )}
                      </div>
                    )}
                  </div>
                ) : (
                  !loadingDates && (
                    <p className={Styles.empty}>Nenhum horário disponível nesta data.</p>
                  )
                )
              )}
            </>
          )}
        </div>

        {/* ── rodapé ── */}
        <div className={Styles.footer}>
          <button
            type="button"
            className={Styles.btnSecondary}
            onClick={step === STEP_BARBER ? onClose : () => setStep(STEP_BARBER)}
            disabled={isSubmitting}
          >
            {step === STEP_BARBER ? 'Cancelar' : 'Voltar'}
          </button>

          {step === STEP_BARBER ? (
            <button
              type="button"
              className={Styles.btnPrimary}
              disabled={!selectedBarberId}
              onClick={() => setStep(STEP_DATETIME)}
            >
              Próximo
            </button>
          ) : (
            <button
              type="button"
              className={Styles.btnPrimary}
              disabled={!canConfirm}
              onClick={handleConfirm}
            >
              {isSubmitting ? 'Salvando...' : 'Confirmar reagendamento'}
            </button>
          )}
        </div>
      </div>
    </div>
  );
};

export default RescheduleModal;
