import React, { useState, useEffect, useMemo, useCallback } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { FiRefreshCw, FiChevronUp, FiChevronDown } from "react-icons/fi";
import { toast } from "react-toastify";
import Styles from "./CSS/AgendamentoPage.module.css";

import ServicesAgendamento from "../components/AgendamentoPage/ServicesAgendamento";
import CustomerHeader from "../components/HomePage/CustomerHeader";
import CustomerNavbar from "../components/HomePage/CustomerNavbar";
import { logoutUser } from "../services/authService";
import {
  isOfflineTransactionalError,
  getOfflineTransactionalMessage,
} from "../services/offlineTransactionalService";

import api from "../services/api";
import { getShopBarbers, getShopServices } from "../services/barbershopService";

const WEEK_DAY_SHORT = ["DOM", "SEG", "TER", "QUA", "QUI", "SEX", "SAB"];

const AgendamentoPage = () => {
  const { barbershopId } = useParams();
  const navigate = useNavigate();

  const [servicesList, setServicesList] = useState([]);
  const [barbersList, setBarbersList] = useState([]);
  const [availableSlots, setAvailableSlots] = useState([]); 
  const [barberActivitiesById, setBarberActivitiesById] = useState({});
  const [isLoadingBarberActivities, setIsLoadingBarberActivities] = useState(false);

  const [selectedServices, setSelectedServices] = useState([]);
  const [selectedBarber, setSelectedBarber] = useState(null);
  const [selectedDate, setSelectedDate] = useState(null);
  const [dateOptions, setDateOptions] = useState([]);
  const [isLoadingDateOptions, setIsLoadingDateOptions] = useState(false);
  const [selectedTime, setSelectedTime] = useState("");
  const [expandedPeriods, setExpandedPeriods] = useState({ morning: true, afternoon: true });
  const [offlineTransactionalNotice, setOfflineTransactionalNotice] = useState("");

  const formatDateToApi = useCallback((dateObj) => {
    if (!dateObj) return "";
    const year = dateObj.getFullYear();
    const month = String(dateObj.getMonth() + 1).padStart(2, "0");
    const day = String(dateObj.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
  }, []);

  const getDateKey = useCallback((dateObj) => formatDateToApi(dateObj), [formatDateToApi]);

  const buildDateWindow = useCallback((daysToShow = 14) => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    return Array.from({ length: daysToShow }, (_, index) => {
      const nextDate = new Date(today);
      nextDate.setDate(today.getDate() + index);
      return nextDate;
    });
  }, []);

  const formatCompactDate = useCallback((dateObj) => {
    const day = String(dateObj.getDate()).padStart(2, "0");
    const month = String(dateObj.getMonth() + 1).padStart(2, "0");
    return `${day}/${month}`;
  }, []);

  const getRelativeDateLabel = useCallback((dateObj, index) => {
    if (index === 0) return "Hoje";
    if (index === 1) return "Amanhã";
    return WEEK_DAY_SHORT[dateObj.getDay()];
  }, []);

  const fetchDateSlots = useCallback(async (barberId, dateObj, durationMinutes) => {
    // Endpoint correto: /appointments/availability?barberId=&date=&duration=
    const response = await api.get(`/appointments/availability`, {
      params: {
        barberId,
        date: formatDateToApi(dateObj),
        duration: durationMinutes || 30,
      },
    });

    const data = Array.isArray(response.data) ? response.data : [];
    // TimeSlotDTO: { startTime: "2026-04-08T09:00:00", endTime: ..., available: true }
    // Filtra apenas slots disponíveis e extrai o horário "HH:mm"
    return data
      .filter(slot => slot.available)
      .map(slot => {
        const raw = slot.startTime; // "2026-04-08T09:00:00" ou "09:00:00"
        if (!raw) return null;
        // Pega apenas a parte HH:mm — funciona tanto para ISO datetime quanto para time puro
        const timePart = raw.includes('T') ? raw.split('T')[1] : raw;
        return timePart.substring(0, 5); // "09:00"
      })
      .filter(Boolean);
  }, [formatDateToApi]);
  const [isSummaryModalOpen, setIsSummaryModalOpen] = useState(false);
  const [isSubmittingAppointment, setIsSubmittingAppointment] = useState(false);
  // null = ainda não escolheu, 'local' = pagar na barbearia, 'online' = pagar via MP
  const [paymentChoice, setPaymentChoice] = useState(null);

  const selectedBarberData = barbersList.find((barber) => String(barber.id) === String(selectedBarber));
  const selectedBarberActivityIds = useMemo(() => {
    if (!selectedBarber) return new Set();
    // Caso 1: barbeiro carregado via rota primária já traz assignedActivityIds (UUID[])
    if (selectedBarberData?.assignedActivityIds) {
      return new Set(selectedBarberData.assignedActivityIds.map(String));
    }
    // Caso 2: atividades carregadas via chamada extra (array de UUIDs ou objetos {id})
    const cached = barberActivitiesById[selectedBarber];
    if (!cached) return new Set();
    return new Set(
      cached.map((item) => String(typeof item === 'object' ? item.id : item))
    );
  }, [selectedBarber, selectedBarberData, barberActivitiesById]);
  const totalDuration = selectedServices.reduce((acc, curr) => acc + curr.durationMinutes, 0);
  const totalPrice = selectedServices.reduce((acc, curr) => acc + curr.price, 0);

  const formatCurrency = (value) => (
    new Intl.NumberFormat("pt-BR", {
      style: "currency",
      currency: "BRL",
    }).format(value)
  );

  useEffect(() => {
    const initializeDateOptions = async () => {
      setOfflineTransactionalNotice("");

      if (!selectedBarber || selectedServices.length === 0 || totalDuration <= 0) {
        setDateOptions([]);
        setSelectedDate(null);
        setAvailableSlots([]);
        setSelectedTime("");
        return;
      }

      setIsLoadingDateOptions(true);

      try {
        const windowDates = buildDateWindow(14);
        const baseOptions = windowDates.map((dateObj, index) => ({
          key: getDateKey(dateObj),
          date: dateObj,
          label: getRelativeDateLabel(dateObj, index),
          compact: formatCompactDate(dateObj),
          slots: [],
          isAvailable: false,
          status: "idle",
        }));

        // Carrega todos os 14 dias em paralelo — sem "Toque para consultar"
        const results = await Promise.allSettled(
          baseOptions.map((option) => fetchDateSlots(selectedBarber, option.date, totalDuration))
        );

        const hydratedOptions = baseOptions.map((option, index) => {
          const result = results[index];
          const slots = result.status === "fulfilled" ? result.value : [];
          return { ...option, slots, isAvailable: slots.length > 0, status: "loaded" };
        });

        setDateOptions(hydratedOptions);

        const selectedKey = selectedDate ? getDateKey(selectedDate) : null;
        const stillValid = hydratedOptions.find((o) => o.key === selectedKey && o.isAvailable);
        const firstAvailable = hydratedOptions.find((o) => o.isAvailable);

        if (stillValid) {
          setSelectedDate(stillValid.date);
        } else if (firstAvailable) {
          setSelectedDate(firstAvailable.date);
          setSelectedTime(firstAvailable.slots[0] || "");
        } else {
          setSelectedDate(hydratedOptions[0]?.date || null);
          setSelectedTime("");
        }
      } catch (error) {
        console.error("Erro ao preparar datas:", error);
        if (isOfflineTransactionalError(error)) {
          setOfflineTransactionalNotice(getOfflineTransactionalMessage(error));
        }
        setDateOptions([]);
        setSelectedDate(null);
        setAvailableSlots([]);
        setSelectedTime("");
      } finally {
        setIsLoadingDateOptions(false);
      }
    };

    initializeDateOptions();
  }, [
    buildDateWindow,
    fetchDateSlots,
    formatCompactDate,
    getDateKey,
    getRelativeDateLabel,
    selectedBarber,
    selectedDate,
    selectedServices.length,
    totalDuration,
  ]);

  const formatDateForSummary = (rawDate) => {
    if (!rawDate) return "--";
    const date = rawDate instanceof Date ? rawDate : new Date(rawDate);
    return date.toLocaleDateString("pt-BR", {
      weekday: "long",
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
    });
  };

  const selectedDateLabel = selectedDate ? formatDateForSummary(selectedDate) : "Selecione um dia";
  const nextAvailableDate = dateOptions.find((option) => option.isAvailable);

  const groupedSlots = useMemo(() => {
    const morning = [];
    const afternoon = [];

    availableSlots.forEach((slot) => {
      const hour = Number(String(slot).split(':')[0]);
      if (Number.isNaN(hour)) return;

      if (hour < 12) {
        morning.push(slot);
      } else {
        afternoon.push(slot);
      }
    });

    return { morning, afternoon };
  }, [availableSlots]);

  const togglePeriod = (period) => {
    setExpandedPeriods((prev) => ({ ...prev, [period]: !prev[period] }));
  };

  const getInitials = (name) => {
    if (!name) return "--";
    return name
      .split(" ")
      .slice(0, 2)
      .map((part) => part[0])
      .join("")
      .toUpperCase();
  };

  const handleLogout = () => {
    logoutUser();
    navigate('/');
  };

  useEffect(() => {
    const fetchSelectedBarberActivities = async () => {
      if (!selectedBarber) return;
      // Se o barbeiro já veio com assignedActivityIds na listagem, não precisa chamar extra
      if (selectedBarberData?.assignedActivityIds) {
        console.log(
          `[AgendamentoPage] Habilidades do barbeiro "${selectedBarberData?.name}" (via DTO):`,
          selectedBarberData.assignedActivityIds
        );
        return;
      }
      // Evita re-buscar se já foi carregado via endpoint extra
      if (barberActivitiesById[selectedBarber]) return;

      try {
        setIsLoadingBarberActivities(true);
        // Endpoint retorna Set<UUID> (array de strings)
        const response = await api.get(`/barbers/${selectedBarber}/activities`);
        const activities = Array.isArray(response.data) ? response.data : [];

        console.log(
          `[AgendamentoPage] Habilidades do barbeiro ID "${selectedBarber}" (via API):`,
          activities
        );

        setBarberActivitiesById((prev) => ({
          ...prev,
          [selectedBarber]: activities,
        }));
      } catch (error) {
        console.error("Erro ao carregar serviços do barbeiro selecionado:", error);
        setBarberActivitiesById((prev) => ({
          ...prev,
          [selectedBarber]: [],
        }));
      } finally {
        setIsLoadingBarberActivities(false);
      }
    };

    fetchSelectedBarberActivities();
  }, [selectedBarber, selectedBarberData, barberActivitiesById]);

  useEffect(() => {
    if (!selectedBarber) return;
    // Só filtra serviços quando temos dados de habilidade com conteúdo.
    // Array vazio ([]) é truthy em JS — checar .length > 0 explicitamente para não limpar tudo.
    const loadedActivities = barberActivitiesById[selectedBarber];
    const hasAssigned = (selectedBarberData?.assignedActivityIds?.length ?? 0) > 0;
    const hasCached = (loadedActivities?.length ?? 0) > 0;
    if (!hasAssigned && !hasCached) return;

    setSelectedServices((prev) => {
      const filtered = prev.filter((service) => selectedBarberActivityIds.has(String(service.id)));
      if (filtered.length === prev.length) {
        return prev;
      }

      return filtered;
    });
  }, [selectedBarber, selectedBarberData, barberActivitiesById, selectedBarberActivityIds]);

 
  useEffect(() => {
    const token = localStorage.getItem('token');
    if (!token) {
      toast.warn("Você precisa estar logado para fazer um agendamento.");
      navigate('/identificacao', { state: { mode: 'login', role: 'customer' } });
    }
  }, [navigate]);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const [servicesData, barbersData] = await Promise.all([
          getShopServices(barbershopId),
          getShopBarbers(barbershopId),
        ]);

        setServicesList(servicesData);
        setBarbersList(barbersData);

        if (Array.isArray(barbersData) && barbersData.length === 0) {
          toast.info("No momento não há profissionais disponíveis para esta barbearia.");
        }
      } catch (error) {
        console.error("Erro ao carregar dados:", error);
        toast.error("Erro ao carregar informações da barbearia.");
      }
    };

    if (barbershopId) fetchData();
  }, [barbershopId]);

  useEffect(() => {
    const syncSelectedDateSlots = async () => {
      setOfflineTransactionalNotice("");

      if (!selectedDate || !selectedBarber || totalDuration <= 0) {
        setAvailableSlots([]);
        setSelectedTime("");
        return;
      }

      const selectedKey = getDateKey(selectedDate);
      const selectedOption = dateOptions.find((option) => option.key === selectedKey);

      if (!selectedOption) {
        setAvailableSlots([]);
        setSelectedTime("");
        return;
      }

      // Lazy-load: datas não pré-carregadas só consultam ao serem escolhidas.
      if (selectedOption.status !== "loaded") {
        try {
          const slots = await fetchDateSlots(selectedBarber, selectedOption.date, totalDuration);

          setDateOptions((prev) => prev.map((option) => (
            option.key === selectedOption.key
              ? { ...option, slots, isAvailable: slots.length > 0, status: "loaded" }
              : option
          )));

          setAvailableSlots(slots);

          if (!slots.some((slot) => slot === selectedTime)) {
            setSelectedTime("");
          }
          return;
        } catch (error) {
          console.error("Erro ao buscar horários da data selecionada:", error);
          if (isOfflineTransactionalError(error)) {
            setOfflineTransactionalNotice(getOfflineTransactionalMessage(error));
          }
          setAvailableSlots([]);
          setSelectedTime("");
          return;
        }
      }

      const slots = selectedOption.slots || [];
      setAvailableSlots(slots);

      if (!slots.some((slot) => slot === selectedTime)) {
        setSelectedTime("");
      }
    };

    syncSelectedDateSlots();
  }, [selectedDate, dateOptions, selectedTime, selectedBarber, totalDuration, fetchDateSlots, getDateKey]);

  // Handler: Selecionar/Deselecionar Serviço
  const handleServiceToggle = (service) => {
    setSelectedServices(prev => {
      const exists = prev.some(s => s.id === service.id);
      const next = exists
        ? prev.filter(s => s.id !== service.id)   // Remove o serviço
        : [...prev, service];                       // Adiciona o serviço

      // Se há um barbeiro selecionado, verifica se ele consegue fazer todos os serviços resultantes
      if (selectedBarber) {
        const hasActivityData = selectedBarberActivityIds.size > 0;
        const canStillDoAll = !hasActivityData || next.every(s => selectedBarberActivityIds.has(String(s.id)));
        if (hasActivityData && !canStillDoAll) {
          // Desmarca o barbeiro para que o usuário escolha outro
          setSelectedBarber(null);
          toast.info("O profissional selecionado não realiza todos os serviços. Por favor, escolha outro profissional.");
        }
      }

      return next;
    });
    setSelectedTime("");
    setSelectedDate(null);
  };

  const handleOpenSummary = () => {
    if (!selectedBarber || !selectedDate || !selectedTime || selectedServices.length === 0) {
      toast.warn("Por favor, preencha todos os campos!");
      return;
    }

    setIsSummaryModalOpen(true);
  };

  const handleCloseSummary = () => {
    if (isSubmittingAppointment) return;
    setIsSummaryModalOpen(false);
    setPaymentChoice(null);
  };

  const handleAgendar = async () => {
    try {
      setIsSubmittingAppointment(true);
      setOfflineTransactionalNotice("");

      let timeString = selectedTime;
      if (timeString.length === 5) {
        timeString = `${timeString}:00`;
      }

      const apiDate = formatDateToApi(selectedDate);
      const startTime = `${apiDate}T${timeString}`;

      if (!apiDate || !timeString) {
        toast.error("Erro interno ao processar a data. Tente selecionar o horário novamente.");
        return;
      }

      const appointmentData = {
        barbershopId,
        barberId: selectedBarber,
        activityIds: selectedServices.map((service) => service.id),
        startTime,
      };

      await api.post("/appointments", appointmentData);
      setIsSummaryModalOpen(false);
      toast.success("Agendamento realizado com sucesso!");
      navigate("/meus-agendamentos");
    } catch (error) {
      if (isOfflineTransactionalError(error)) {
        setOfflineTransactionalNotice(getOfflineTransactionalMessage(error));
      }

      if (error.response && error.response.data) {
        toast.error(`Erro: ${error.response.data.message || "Falha ao agendar"}`);
      } else {
        toast.error("Erro ao realizar agendamento. Tente novamente.");
      }
    } finally {
      setIsSubmittingAppointment(false);
    }
  };

  // Fluxo online: cria o agendamento primeiro, depois redireciona para o checkout
  const handleAgendarOnline = async () => {
    try {
      setIsSubmittingAppointment(true);
      setOfflineTransactionalNotice("");

      let timeString = selectedTime;
      if (timeString.length === 5) timeString = `${timeString}:00`;

      const apiDate = formatDateToApi(selectedDate);
      const startTime = `${apiDate}T${timeString}`;

      if (!apiDate || !timeString) {
        toast.error("Erro interno ao processar a data. Tente selecionar o horário novamente.");
        return;
      }

      const appointmentData = {
        barbershopId,
        barberId: selectedBarber,
        activityIds: selectedServices.map((s) => s.id),
        startTime,
      };

      const appointmentResponse = await api.post("/appointments", appointmentData);
      const appointmentId = appointmentResponse.data?.id;

      if (!appointmentId) {
        toast.warn("Agendamento criado, mas não foi possível iniciar o pagamento. Tente pagar depois em 'Meus Agendamentos'.");
        navigate("/meus-agendamentos");
        return;
      }

      const paymentResponse = await api.post("/payments/create", {
        appointmentId,
        paymentMethod: "CREDIT_CARD",
      });

      const checkoutUrl = paymentResponse.data?.checkoutUrl;
      if (checkoutUrl) {
        window.location.href = checkoutUrl;
      } else {
        toast.warn("Pagamento iniciado, mas o link de checkout não foi retornado. Verifique seus agendamentos.");
        navigate("/meus-agendamentos");
      }
    } catch (error) {
      if (isOfflineTransactionalError(error)) {
        setOfflineTransactionalNotice(getOfflineTransactionalMessage(error));
      }

      if (error.response?.data) {
        toast.error(`Erro: ${error.response.data.message || "Falha ao iniciar pagamento"}`);
      } else {
        toast.error("Erro ao iniciar pagamento. Tente novamente.");
      }
    } finally {
      setIsSubmittingAppointment(false);
    }
  };

  return (
    <div className={`ca-page ${Styles.page_container}`}>
      <div className={`ca-container ${Styles.content_container}`}>
        <CustomerHeader activeTab="agendamentos" onLogout={handleLogout} />
        <CustomerNavbar activeTab="agendamentos" onLogout={handleLogout} />

        <section className={Styles.heroBlock}>
          <p className={Styles.kicker}>AGENDAMENTO ONLINE</p>
          <h1 className={Styles.title}>Monte seu horário em poucos passos</h1>
          <p className={Styles.subtitle}>Selecione serviços, profissional, data e horário. Antes de confirmar, você verá um resumo completo.</p>
        </section>

        {offlineTransactionalNotice && (
          <p className={`${Styles.warning} ca-state ca-state--error`}>
            {offlineTransactionalNotice}
          </p>
        )}

        <section className={Styles.section}>
          <h3 className={Styles.section_title}>1. Serviços</h3>
          {selectedBarber && isLoadingBarberActivities && (
            <p className={Styles.info_text}>Carregando serviços que este barbeiro executa...</p>
          )}
          <div className={Styles.services_list}>
            {servicesList && servicesList.length > 0 ? (
              servicesList.map((service) => (
                <ServicesAgendamento
                  key={service.id}
                  data={service}
                  isSelected={selectedServices.some((selected) => selected.id === service.id)}
                  disabled={
                    // Só desabilita se TEMOS dados de habilidade E o barbeiro não executa este serviço.
                    // Se não temos dados (Set vazio), não bloqueamos — evita travar a UI enquanto carrega.
                    selectedBarber !== null &&
                    selectedBarberActivityIds.size > 0 &&
                    !selectedBarberActivityIds.has(String(service.id))
                  }
                  onToggle={() => handleServiceToggle(service)}
                />
              ))
            ) : (
              <p className={Styles.info_text}>Nenhum serviço disponível.</p>
            )}
          </div>
        </section>

        <section className={Styles.section}>
          <h3 className={Styles.section_title}>2. Profissional</h3>
          {barbersList.length > 0 ? (
            <>
              {selectedServices.length > 0 && (
                <p className={Styles.info_text} style={{ marginBottom: '0.6rem' }}>
                  Mostrando apenas profissionais que realizam{' '}
                  <strong>todos os serviços selecionados</strong>.
                </p>
              )}
              <div className={Styles.barberGrid}>
                {barbersList.map((barber) => {
                  // Se há serviços selecionados, verifica se o barbeiro executa todos
                  const rawActivities = barber.assignedActivityIds || barberActivitiesById[barber.id] || null;
                  const barberActivityIds = new Set((rawActivities || []).map((item) => String(typeof item === 'object' ? item.id : item)));
                  const hasActivityData = barberActivityIds.size > 0;

                  // rawActivities === [] (array vazio explícito) = habilidades carregadas e não configuradas
                  const activitiesLoadedButEmpty = Array.isArray(rawActivities) && rawActivities.length === 0;

                  const canDoAllServices =
                    selectedServices.length === 0 ||
                    !hasActivityData ||
                    selectedServices.every(s => barberActivityIds.has(String(s.id)));

                  // Barbeiros que não atendem os serviços ficam desabilitados (não somem)
                  const isDisabled = selectedServices.length > 0 && hasActivityData && !canDoAllServices;
                  const isSelected = String(selectedBarber) === String(barber.id);

                  return (
                    <button
                      key={barber.id}
                      className={`${Styles.barberCard} ${isSelected ? Styles.barberCardSelected : ''} ${isDisabled ? Styles.barberCardDisabled : ''}`}
                      onClick={() => {
                        if (isDisabled) return;
                        if (isSelected) {
                          setSelectedBarber(null);
                          setSelectedDate(null);
                          setSelectedTime("");
                          setDateOptions([]);
                          setAvailableSlots([]);
                          return;
                        }
                        setSelectedBarber(barber.id);
                        setSelectedDate(null);
                        setSelectedTime("");
                      }}
                      title={isDisabled ? 'Este profissional não realiza todos os serviços selecionados' : barber.name}
                      disabled={isDisabled}
                    >
                      <span className={Styles.barberAvatar}>{getInitials(barber.name)}</span>
                      <span className={Styles.barberName}>{barber.name}</span>
                      {isDisabled && (
                        <span className={Styles.barberUnavailableTag}>Não realiza</span>
                      )}
                      {!isDisabled && activitiesLoadedButEmpty && (
                        <span className={Styles.barberNoActivitiesTag} title="Este profissional ainda não tem serviços configurados">
                          ⚠️ Sem serviços
                        </span>
                      )}
                    </button>
                  );
                })}
              </div>
            </>
          ) : (
            <p className={Styles.info_text}>No momento não há profissionais disponíveis para agendamento nesta barbearia.</p>
          )}
        </section>

        <section className={Styles.section}>
          <h3 className={Styles.section_title}>3. Data</h3>
          <div className={Styles.dateModule}>
            <div className={Styles.dateInfoPanel}>
              <p className={Styles.dateInfoKicker}>DATA ESCOLHIDA</p>
              <h4 className={Styles.dateInfoValue}>{selectedDateLabel}</h4>
              {nextAvailableDate ? (
                <p className={Styles.dateInfoHint}>
                  Próximo horário disponível: {formatDateForSummary(nextAvailableDate.date)}.
                </p>
              ) : (
                <p className={Styles.dateInfoHint}>
                  Selecione serviços e profissional para ver os próximos dias com horário.
                </p>
              )}
            </div>

            <div className={Styles.dateRail}>
              {selectedBarber && selectedServices.length > 0 ? (
                isLoadingDateOptions ? (
                  <p className={Styles.info_text}>Carregando datas inteligentes...</p>
                ) : dateOptions.length > 0 ? (
                  dateOptions.map((option) => (
                    <button
                      key={option.key}
                      type="button"
                      className={`${Styles.dateChip} ${selectedDate && option.key === getDateKey(selectedDate) ? Styles.dateChipSelected : ''}`}
                      onClick={() => {
                        if (option.status === "loaded" && !option.isAvailable) return;
                        if (selectedDate && option.key === getDateKey(selectedDate)) {
                          setSelectedDate(null);
                          setSelectedTime("");
                          setAvailableSlots([]);
                          return;
                        }
                        setSelectedDate(option.date);
                        setSelectedTime("");
                      }}
                      disabled={option.status === "loaded" && !option.isAvailable}
                    >
                      <span className={Styles.dateChipLabel}>{option.label}</span>
                      <strong className={Styles.dateChipValue}>{option.compact}</strong>
                      <small className={Styles.dateChipMeta}>
                        {option.status !== "loaded"
                          ? 'Toque para consultar'
                          : option.isAvailable
                            ? `${option.slots.length} horários`
                            : 'Indisponível'}
                      </small>
                    </button>
                  ))
                ) : (
                  <p className={Styles.info_text}>Sem datas disponíveis no momento.</p>
                )
              ) : (
                <p className={Styles.info_text}>Selecione serviços e profissional para liberar as datas.</p>
              )}
            </div>
          </div>
        </section>

        <section className={Styles.section}>
          <h3 className={Styles.section_title}>4. Horário</h3>
          {selectedBarber && selectedDate && selectedServices.length > 0 ? (
            availableSlots.length > 0 ? (
              <div className={Styles.slots_periods}>
                {groupedSlots.morning.length > 0 && (
                  <div className={Styles.periodCard}>
                    <button
                      type="button"
                      className={Styles.periodToggle}
                      onClick={() => togglePeriod('morning')}
                      aria-expanded={expandedPeriods.morning}
                    >
                      <span>Manha (ate 11:59)</span>
                      <span className={Styles.periodMeta}>
                        {groupedSlots.morning.length} horarios
                        {expandedPeriods.morning ? <FiChevronUp /> : <FiChevronDown />}
                      </span>
                    </button>

                    {expandedPeriods.morning && (
                      <div className={Styles.slots_grid}>
                        {groupedSlots.morning.map((time) => (
                          <button
                            key={time}
                            className={`${Styles.slot_button} ${selectedTime === time ? Styles.slot_selected : ''}`}
                            onClick={() => setSelectedTime((prev) => (prev === time ? "" : time))}
                          >
                            {time.substring(0, 5)}
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
                      onClick={() => togglePeriod('afternoon')}
                      aria-expanded={expandedPeriods.afternoon}
                    >
                      <span>Tarde (a partir de 12:00)</span>
                      <span className={Styles.periodMeta}>
                        {groupedSlots.afternoon.length} horarios
                        {expandedPeriods.afternoon ? <FiChevronUp /> : <FiChevronDown />}
                      </span>
                    </button>

                    {expandedPeriods.afternoon && (
                      <div className={Styles.slots_grid}>
                        {groupedSlots.afternoon.map((time) => (
                          <button
                            key={time}
                            className={`${Styles.slot_button} ${selectedTime === time ? Styles.slot_selected : ''}`}
                            onClick={() => setSelectedTime((prev) => (prev === time ? "" : time))}
                          >
                            {time.substring(0, 5)}
                          </button>
                        ))}
                      </div>
                    )}
                  </div>
                )}
              </div>
            ) : (
              <p className={Styles.warning}>Nenhum horário disponível para esta combinação.</p>
            )
          ) : (
            <p className={Styles.info_text}>Selecione serviços, profissional e data para visualizar horários.</p>
          )}
        </section>

        <div className={Styles.footer}>
          <div className={Styles.totalInfo}>
            <span>Total estimado</span>
            <strong>{formatCurrency(totalPrice)}</strong>
            <small>{totalDuration} min</small>
          </div>
          <button
            className={Styles.confirm_button}
            onClick={handleOpenSummary}
            disabled={!selectedTime || selectedServices.length === 0 || !selectedBarber || !selectedDate}
          >
            Confirmar Agendamento
          </button>
        </div>
      </div>

      {isSummaryModalOpen && (
        <div className={Styles.modalBackdrop} onClick={handleCloseSummary}>
          <div className={Styles.modalCard} onClick={(e) => e.stopPropagation()}>
            <p className={Styles.modalKicker}>RESUMO DO AGENDAMENTO</p>
            <h3 className={Styles.modalTitle}>Confira os dados antes de finalizar</h3>

            <div className={Styles.summaryList}>
              <div className={Styles.summaryRow}>
                <span>Profissional</span>
                <strong>{selectedBarberData?.name || '--'}</strong>
              </div>
              <div className={Styles.summaryRow}>
                <span>Data</span>
                <strong>{formatDateForSummary(selectedDate)}</strong>
              </div>
              <div className={Styles.summaryRow}>
                <span>Horário</span>
                <strong>{selectedTime.substring(0, 5)}</strong>
              </div>
              <div className={Styles.summaryRow}>
                <span>Duração total</span>
                <strong>{totalDuration} min</strong>
              </div>
              <div className={Styles.summaryServicesBlock}>
                <span>Serviços</span>
                <ul>
                  {selectedServices.map((service) => (
                    <li key={service.id}>
                      <span>{service.activityName}</span>
                      <strong>{formatCurrency(service.price)}</strong>
                    </li>
                  ))}
                </ul>
              </div>
            </div>

            <div className={Styles.summaryTotal}>
              <span>Total</span>
              <strong>{formatCurrency(totalPrice)}</strong>
            </div>

            <div className={Styles.modalActions}>
              {!paymentChoice ? (
                <>
                  <p className={Styles.paymentChoiceHeading}>Como deseja pagar?</p>
                  <p className={Styles.paymentChoiceSubtext}>Escolha uma opção para finalizar seu agendamento.</p>
                  <div className={Styles.paymentChoiceGrid}>
                    <button
                      type="button"
                      className={`${Styles.paymentChoiceBtn} ${Styles.paymentChoiceBtnLocal}`}
                      onClick={() => { setPaymentChoice('local'); handleAgendar(); }}
                      disabled={isSubmittingAppointment}
                    >
                      <span className={Styles.paymentChoiceBtnEmoji}>🏪</span>
                      Pagar no local
                    </button>
                    <button
                      type="button"
                      className={`${Styles.paymentChoiceBtn} ${Styles.paymentChoiceBtnOnline}`}
                      onClick={() => { setPaymentChoice('online'); handleAgendarOnline(); }}
                      disabled={isSubmittingAppointment}
                    >
                      <span className={Styles.paymentChoiceBtnEmoji}>💳</span>
                      Pagar online
                    </button>
                  </div>
                  <button
                    type="button"
                    className={Styles.modalSecondaryButton}
                    onClick={handleCloseSummary}
                    disabled={isSubmittingAppointment}
                    style={{ marginTop: '0.5rem', width: '100%' }}
                  >
                    Ajustar dados
                  </button>
                </>
              ) : (
                <>
                  <button
                    type="button"
                    className={Styles.modalSecondaryButton}
                    onClick={handleCloseSummary}
                    disabled={isSubmittingAppointment}
                  >
                    Cancelar
                  </button>
                  <button
                    type="button"
                    className={Styles.modalPrimaryButton}
                    disabled={isSubmittingAppointment}
                  >
                    {isSubmittingAppointment ? 'Processando...' : 'Aguarde...'}
                  </button>
                </>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default AgendamentoPage;

