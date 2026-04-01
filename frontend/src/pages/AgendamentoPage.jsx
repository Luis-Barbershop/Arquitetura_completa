import React, { useState, useEffect, useMemo } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { FiArrowLeft, FiCalendar, FiCheckCircle, FiRefreshCw, FiScissors } from "react-icons/fi";
import Styles from "./CSS/AgendamentoPage.module.css";

import ServicesAgendamento from "../components/AgendamentoPage/ServicesAgendamento";

import api from "../services/api"; 

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

  const fetchDateSlots = async (barberId, dateObj, durationMinutes) => {
    const response = await api.get(`/barbers/${barberId}/availability`, {
      params: {
        date: formatDateToApi(dateObj),
        duration: durationMinutes,
      },
    });

    return Array.isArray(response.data) ? response.data : [];
  };
  const [isSummaryModalOpen, setIsSummaryModalOpen] = useState(false);
  const [isSubmittingAppointment, setIsSubmittingAppointment] = useState(false);

  const selectedBarberData = barbersList.find((barber) => String(barber.id) === String(selectedBarber));
  const selectedBarberActivities = useMemo(
    () => (selectedBarber && barberActivitiesById[selectedBarber] ? barberActivitiesById[selectedBarber] : []),
    [selectedBarber, barberActivitiesById]
  );
  const selectedBarberActivityIds = useMemo(
    () => new Set(selectedBarberActivities.map((activity) => String(activity.id))),
    [selectedBarberActivities]
  );
  const totalDuration = selectedServices.reduce((acc, curr) => acc + curr.durationMinutes, 0);
  const totalPrice = selectedServices.reduce((acc, curr) => acc + curr.price, 0);

  const weekDayShort = ["DOM", "SEG", "TER", "QUA", "QUI", "SEX", "SAB"];

  const formatCurrency = (value) => (
    new Intl.NumberFormat("pt-BR", {
      style: "currency",
      currency: "BRL",
    }).format(value)
  );

  const formatDateToApi = (dateObj) => {
    if (!dateObj) return "";
    const year = dateObj.getFullYear();
    const month = String(dateObj.getMonth() + 1).padStart(2, "0");
    const day = String(dateObj.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
  };

  const getDateKey = (dateObj) => formatDateToApi(dateObj);

  const buildDateWindow = (daysToShow = 14) => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    return Array.from({ length: daysToShow }, (_, index) => {
      const nextDate = new Date(today);
      nextDate.setDate(today.getDate() + index);
      return nextDate;
    });
  };

  const formatCompactDate = (dateObj) => {
    const day = String(dateObj.getDate()).padStart(2, "0");
    const month = String(dateObj.getMonth() + 1).padStart(2, "0");
    return `${day}/${month}`;
  };

  const getRelativeDateLabel = (dateObj, index) => {
    if (index === 0) return "Hoje";
    if (index === 1) return "Amanhã";
    return weekDayShort[dateObj.getDay()];
  };

  useEffect(() => {
    const initializeDateOptions = async () => {
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
        const initialOptions = windowDates.map((dateObj, index) => ({
          key: getDateKey(dateObj),
          date: dateObj,
          label: getRelativeDateLabel(dateObj, index),
          compact: formatCompactDate(dateObj),
          slots: [],
          isAvailable: false,
          status: "idle",
        }));

        setDateOptions(initialOptions);

        // Carrega apenas os primeiros dias para evitar tempestade de requests.
        let hydratedOptions = [...initialOptions];
        const preloadCount = 4;

        for (let index = 0; index < preloadCount; index += 1) {
          const current = hydratedOptions[index];
          if (!current) break;

          try {
            const slots = await fetchDateSlots(selectedBarber, current.date, totalDuration);
            hydratedOptions[index] = {
              ...current,
              slots,
              isAvailable: slots.length > 0,
              status: "loaded",
            };
          } catch (error) {
            hydratedOptions[index] = {
              ...current,
              slots: [],
              isAvailable: false,
              status: "loaded",
            };
          }
        }

        setDateOptions(hydratedOptions);

        const selectedKey = selectedDate ? getDateKey(selectedDate) : null;
        const stillValidSelectedDate = hydratedOptions.find((option) => option.key === selectedKey && option.isAvailable);
        const firstAvailable = hydratedOptions.find((option) => option.isAvailable);

        if (stillValidSelectedDate) {
          setSelectedDate(stillValidSelectedDate.date);
        } else if (firstAvailable) {
          setSelectedDate(firstAvailable.date);
          setSelectedTime(firstAvailable.slots[0] || "");
        } else {
          setSelectedDate(hydratedOptions[0]?.date || null);
          setSelectedTime("");
        }
      } catch (error) {
        console.error("Erro ao preparar datas:", error);
        setDateOptions([]);
        setSelectedDate(null);
        setAvailableSlots([]);
        setSelectedTime("");
      } finally {
        setIsLoadingDateOptions(false);
      }
    };

    initializeDateOptions();
  }, [selectedBarber, selectedServices, totalDuration]);

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

  const getInitials = (name) => {
    if (!name) return "--";
    return name
      .split(" ")
      .slice(0, 2)
      .map((part) => part[0])
      .join("")
      .toUpperCase();
  };

  const clearSelections = () => {
    setSelectedServices([]);
    setSelectedBarber(null);
    setSelectedDate(null);
    setDateOptions([]);
    setSelectedTime("");
    setAvailableSlots([]);
  };

  useEffect(() => {
    const fetchSelectedBarberActivities = async () => {
      if (!selectedBarber) return;
      if (barberActivitiesById[selectedBarber]) return;

      try {
        setIsLoadingBarberActivities(true);
        const response = await api.get(`/barbers/${selectedBarber}/activities`);
        const activities = Array.isArray(response.data) ? response.data : [];

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
  }, [selectedBarber, barberActivitiesById]);

  useEffect(() => {
    if (!selectedBarber) return;

    if (!barberActivitiesById[selectedBarber]) return;

    setSelectedServices((prev) => {
      const filtered = prev.filter((service) => selectedBarberActivityIds.has(String(service.id)));
      if (filtered.length === prev.length) {
        return prev;
      }

      return filtered;
    });
  }, [selectedBarber, barberActivitiesById, selectedBarberActivityIds]);

 
  useEffect(() => {
    const token = localStorage.getItem('token');
    if (!token) {
      alert("Você precisa estar logado para fazer um agendamento.");
      navigate('/identificacao', { state: { mode: 'login', role: 'customer' } });
    }
  }, [navigate]);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const servicesResponse = await api.get(`/barbershops/${barbershopId}/activities`);
        setServicesList(servicesResponse.data);

        const barbersResponse = await api.get(`/barbershops/${barbershopId}/barbers`);
        setBarbersList(barbersResponse.data);

      } catch (error) {
        console.error("Erro ao carregar dados:", error);
        alert("Erro ao carregar informações da barbearia.");
      }
    };

    if (barbershopId) fetchData();
  }, [barbershopId]);

  useEffect(() => {
    const syncSelectedDateSlots = async () => {
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
  }, [selectedDate, dateOptions, selectedTime, selectedBarber, totalDuration]);

  // Handler: Selecionar/Deselecionar Serviço
  const handleServiceToggle = (service) => {
    if (selectedBarber && !selectedBarberActivityIds.has(String(service.id))) {
      alert("Este barbeiro nao executa esse servico. Escolha outro profissional ou outro servico.");
      return;
    }

    setSelectedServices(prev => {
      const exists = prev.some(s => s.id === service.id);
      if (exists) {
        return prev.filter(s => s.id !== service.id); // Remove
      } else {
        return [...prev, service]; // Adiciona
      }
    });
    setSelectedTime("");
  };

  const handleOpenSummary = () => {
    if (!selectedBarber || !selectedDate || !selectedTime || selectedServices.length === 0) {
      alert("Por favor, preencha todos os campos!");
      return;
    }

    setIsSummaryModalOpen(true);
  };

  const handleCloseSummary = () => {
    if (isSubmittingAppointment) return;
    setIsSummaryModalOpen(false);
  };

  const handleAgendar = async () => {
    try {
      setIsSubmittingAppointment(true);
      let timeString = selectedTime;
      if (timeString.length === 5) {
        timeString = `${timeString}:00`;
      }

      const apiDate = formatDateToApi(selectedDate);
      const dateTimeString = `${apiDate}T${timeString}`;
      const localDateObj = new Date(dateTimeString);

      if (isNaN(localDateObj.getTime())) {
        alert("Erro interno ao processar a data. Tente selecionar o horário novamente.");
        return;
      }

      const isoDateString = localDateObj.toISOString();

      const appointmentData = {
        barbershopId,
        barberId: selectedBarber,
        activityIds: selectedServices.map((service) => service.id),
        startTime: isoDateString,
      };

      await api.post("/appointments", appointmentData);
      setIsSummaryModalOpen(false);
      alert("Agendamento realizado com sucesso!");
      navigate("/meus-agendamentos");
    } catch (error) {
      if (error.response && error.response.data) {
        alert(`Erro: ${error.response.data.message || "Falha ao agendar"}`);
      } else {
        alert("Erro ao realizar agendamento. Tente novamente.");
      }
    } finally {
      setIsSubmittingAppointment(false);
    }
  };

  return (
    <div className={Styles.page_container}>
      <div className={Styles.content_container}>
        <header className={Styles.topMenu}>
          <div className={Styles.brandBlock}>
            <div className={Styles.brandBadge}>CA</div>
            <div>
              <h3>CortaAI</h3>
              <p>Novo agendamento</p>
            </div>
          </div>

          <nav className={Styles.menuCenter} aria-label="Navegação de agendamento">
            <button onClick={() => navigate('/homepage')}>
              <FiScissors />
              <span>Home</span>
            </button>
            <button onClick={() => navigate('/meus-agendamentos')}>
              <FiCalendar />
              <span>Meus agendamentos</span>
            </button>
            <button className={Styles.menuItemActive}>
              <FiCheckCircle />
              <span>Novo agendamento</span>
            </button>
          </nav>

          <div className={Styles.menuActions}>
            <button className={Styles.secondaryAction} onClick={clearSelections}>
              <FiRefreshCw />
              <span>Limpar</span>
            </button>
            <button className={Styles.ghostAction} onClick={() => navigate(-1)}>
              <FiArrowLeft />
              <span>Voltar</span>
            </button>
          </div>
        </header>

        <section className={Styles.heroBlock}>
          <p className={Styles.kicker}>AGENDAMENTO ONLINE</p>
          <h1 className={Styles.title}>Monte seu horário em poucos passos</h1>
          <p className={Styles.subtitle}>Selecione serviços, profissional, data e horário. Antes de confirmar, você verá um resumo completo.</p>
        </section>

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
                  disabled={selectedBarber ? !selectedBarberActivityIds.has(String(service.id)) : false}
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
            <div className={Styles.barberGrid}>
              {barbersList.map((barber) => (
                <button
                  key={barber.id}
                  className={`${Styles.barberCard} ${String(selectedBarber) === String(barber.id) ? Styles.barberCardSelected : ''}`}
                  onClick={() => {
                    setSelectedBarber(barber.id);
                    setSelectedTime("");
                  }}
                >
                  <span className={Styles.barberAvatar}>{getInitials(barber.name)}</span>
                  <span className={Styles.barberName}>{barber.name}</span>
                </button>
              ))}
            </div>
          ) : (
            <p className={Styles.info_text}>Nenhum profissional encontrado.</p>
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
              <div className={Styles.slots_grid}>
                {availableSlots.map((time) => (
                  <button
                    key={time}
                    className={`${Styles.slot_button} ${selectedTime === time ? Styles.slot_selected : ''}`}
                    onClick={() => setSelectedTime(time)}
                  >
                    {time.substring(0, 5)}
                  </button>
                ))}
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
              <button
                type="button"
                className={Styles.modalSecondaryButton}
                onClick={handleCloseSummary}
                disabled={isSubmittingAppointment}
              >
                Ajustar dados
              </button>
              <button
                type="button"
                className={Styles.modalPrimaryButton}
                onClick={handleAgendar}
                disabled={isSubmittingAppointment}
              >
                {isSubmittingAppointment ? 'Confirmando...' : 'Finalizar agendamento'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default AgendamentoPage;