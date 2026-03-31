import React, { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { FiArrowLeft, FiCalendar, FiCheckCircle, FiChevronLeft, FiChevronRight, FiRefreshCw, FiScissors } from "react-icons/fi";
import DatePicker from "react-datepicker";
import { ptBR } from "date-fns/locale";
import Styles from "./CSS/AgendamentoPage.module.css";
import "react-datepicker/dist/react-datepicker.css";

import ServicesAgendamento from "../components/AgendamentoPage/ServicesAgendamento";

import api from "../services/api"; 

const AgendamentoPage = () => {
  const { barbershopId } = useParams();
  const navigate = useNavigate();

  const [servicesList, setServicesList] = useState([]);
  const [barbersList, setBarbersList] = useState([]);
  const [availableSlots, setAvailableSlots] = useState([]); 

  const [selectedServices, setSelectedServices] = useState([]);
  const [selectedBarber, setSelectedBarber] = useState(null);
  const [selectedDate, setSelectedDate] = useState(null);
  const [selectedTime, setSelectedTime] = useState("");
  const [isSummaryModalOpen, setIsSummaryModalOpen] = useState(false);
  const [isSubmittingAppointment, setIsSubmittingAppointment] = useState(false);

  const currentYear = new Date().getFullYear();
  const years = Array.from({ length: 4 }, (_, index) => currentYear + index);
  const monthNames = [
    "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
    "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
  ];

  const selectedBarberData = barbersList.find((barber) => String(barber.id) === String(selectedBarber));
  const totalDuration = selectedServices.reduce((acc, curr) => acc + curr.durationMinutes, 0);
  const totalPrice = selectedServices.reduce((acc, curr) => acc + curr.price, 0);
  const selectedDateLabel = selectedDate ? formatDateForSummary(selectedDate) : "Selecione um dia";

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
    setSelectedTime("");
    setAvailableSlots([]);
  };

  const getDayClassName = (date) => {
    const isWeekend = date.getDay() === 0 || date.getDay() === 6;
    const isToday = date.toDateString() === new Date().toDateString();

    if (isToday && isWeekend) {
      return `${Styles.calendarTodayDay} ${Styles.calendarWeekendDay}`;
    }

    if (isToday) {
      return Styles.calendarTodayDay;
    }

    if (isWeekend) {
      return Styles.calendarWeekendDay;
    }

    return "";
  };

 
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

  // Buscar Horários Disponíveis
  useEffect(() => {
    const fetchAvailability = async () => {
      if (!selectedBarber || !selectedDate || selectedServices.length === 0) {
        setAvailableSlots([]);
        return;
      }

      try {
        const response = await api.get(`/barbers/${selectedBarber}/availability`, {
          params: {
            date: formatDateToApi(selectedDate),
            duration: totalDuration 
          }
        });
        setAvailableSlots(response.data);
      } catch (error) {
        console.error("Erro ao buscar horários:", error);
        setAvailableSlots([]);
      }
    };

    fetchAvailability();
  }, [selectedBarber, selectedDate, selectedServices]);

  // Handler: Selecionar/Deselecionar Serviço
  const handleServiceToggle = (service) => {
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
          <div className={Styles.services_list}>
            {servicesList && servicesList.length > 0 ? (
              servicesList.map((service) => (
                <ServicesAgendamento
                  key={service.id}
                  data={service}
                  isSelected={selectedServices.some((selected) => selected.id === service.id)}
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
          <div className={Styles.calendarModule}>
            <aside className={Styles.calendarInfoPanel}>
              <p className={Styles.calendarInfoKicker}>DATA ESCOLHIDA</p>
              <h4 className={Styles.calendarInfoDate}>{selectedDateLabel}</h4>
              <p className={Styles.calendarInfoHint}>Datas anteriores estão bloqueadas automaticamente.</p>

              <div className={Styles.calendarLegend}>
                <div className={Styles.legendItem}>
                  <span className={`${Styles.legendDot} ${Styles.legendDotToday}`}></span>
                  <span>Hoje</span>
                </div>
                <div className={Styles.legendItem}>
                  <span className={`${Styles.legendDot} ${Styles.legendDotSelected}`}></span>
                  <span>Selecionado</span>
                </div>
              </div>
            </aside>

            <div className={Styles.calendarInlineContainer}>
              <DatePicker
                selected={selectedDate}
                onChange={(date) => {
                  setSelectedDate(date);
                  setSelectedTime("");
                }}
                inline
                calendarClassName={Styles.inlineCalendar}
                minDate={new Date()}
                locale={ptBR}
                fixedHeight
                dayClassName={getDayClassName}
                formatWeekDay={(nameOfDay) => nameOfDay.slice(0, 3).toUpperCase()}
                renderCustomHeader={({
                  date,
                  changeYear,
                  changeMonth,
                  decreaseMonth,
                  increaseMonth,
                  prevMonthButtonDisabled,
                  nextMonthButtonDisabled,
                }) => (
                  <div className={Styles.customCalendarHeader}>
                    <button
                      type="button"
                      onClick={decreaseMonth}
                      disabled={prevMonthButtonDisabled}
                      className={Styles.calendarNavButton}
                      aria-label="Mês anterior"
                    >
                      <FiChevronLeft />
                    </button>

                    <div className={Styles.calendarHeaderCenter}>
                      <select
                        value={date.getMonth()}
                        onChange={({ target: { value } }) => changeMonth(Number(value))}
                        className={Styles.calendarMonthSelect}
                      >
                        {monthNames.map((monthName, monthIndex) => (
                          <option key={monthName} value={monthIndex}>
                            {monthName}
                          </option>
                        ))}
                      </select>

                      <select
                        value={date.getFullYear()}
                        onChange={({ target: { value } }) => changeYear(Number(value))}
                        className={Styles.calendarYearSelect}
                      >
                        {years.map((year) => (
                          <option key={year} value={year}>
                            {year}
                          </option>
                        ))}
                      </select>
                    </div>

                    <button
                      type="button"
                      onClick={increaseMonth}
                      disabled={nextMonthButtonDisabled}
                      className={Styles.calendarNavButton}
                      aria-label="Próximo mês"
                    >
                      <FiChevronRight />
                    </button>
                  </div>
                )}
              />
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