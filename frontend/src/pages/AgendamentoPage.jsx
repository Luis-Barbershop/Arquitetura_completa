// src/pages/AgendamentoPage.jsx
import React, { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import Styles from "./CSS/AgendamentoPage.module.css";

// Componentes
import Header from "../components/AgendamentoPage/Header";
import ServicesAgendamento from "../components/AgendamentoPage/ServicesAgendamento";

// Services (API)
import api from "../services/api"; 

const AgendamentoPage = () => {
  const { barbershopId } = useParams();
  const navigate = useNavigate();

  // Estados de Dados (Vindos da API)
  const [barbershop, setBarbershop] = useState(null);
  const [servicesList, setServicesList] = useState([]); // <--- O NOME CORRETO É servicesList
  const [barbersList, setBarbersList] = useState([]);
  const [availableSlots, setAvailableSlots] = useState([]); 

  // Estados de Seleção do Usuário
  const [selectedServices, setSelectedServices] = useState([]);
  const [selectedBarber, setSelectedBarber] = useState(null);
  const [selectedDate, setSelectedDate] = useState("");
  const [selectedTime, setSelectedTime] = useState("");

 
  useEffect(() => {
    const user = JSON.parse(localStorage.getItem('user') || 'null');
    if (!user) {
      alert("Você precisa estar logado para fazer um agendamento.");
      navigate("/login");
    }
  }, [navigate]);

  useEffect(() => {
    const fetchData = async () => {
      try {
        // 1. Serviços da Barbearia
        const servicesResponse = await api.get(`/barbershops/${barbershopId}/activities`);
        setServicesList(servicesResponse.data);

        // 2. Barbeiros da Barbearia (rota no user-service via gateway)
        const barbersResponse = await api.get(`/barbers/barbershop/${barbershopId}`);
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
      // Só busca se tivermos Barbeiro, Data e Serviços selecionados
      if (!selectedBarber || !selectedDate || selectedServices.length === 0) {
        setAvailableSlots([]);
        return;
      }

      // Calcula duração total em minutos
      const totalDuration = selectedServices.reduce((acc, curr) => acc + curr.durationMinutes, 0);

      try {
        const response = await api.get('/appointments/availability', {
          params: {
            barberId: selectedBarber,
            date: selectedDate,
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
    // Ao mudar serviço, limpamos o horário selecionado pois a duração mudou
    setSelectedTime("");
  };

  // Handler: Finalizar Agendamento
 const handleAgendar = async () => {
    // 1. Validação básica
    if (!selectedBarber || !selectedDate || !selectedTime || selectedServices.length === 0) {
      alert("Por favor, preencha todos os campos!");
      return;
    }

    try {
      // 2. Preparar o horário correto
      // O Java pode mandar "09:00" ou "09:00:00". 
      // Se vier com segundos (8 caracteres), não adicionamos nada.
      // Se vier sem segundos (5 caracteres), adicionamos ":00".
      let timeString = selectedTime;
      if (timeString.length === 5) {
        timeString = `${timeString}:00`;
      }

      // 3. Montar a string ISO completa (Ex: 2025-11-28T09:00:00)
      const dateTimeString = `${selectedDate}T${timeString}`;
      
      // 4. Criar o objeto Date
      const localDateObj = new Date(dateTimeString);

      // Verificação de Segurança para evitar o erro "Invalid time value"
      if (isNaN(localDateObj.getTime())) {
        console.error("Erro de Data: Formato inválido gerado ->", dateTimeString);
        alert("Erro interno ao processar a data. Tente selecionar o horário novamente.");
        return;
      }

      // 5. Converter para ISO UTC (formato que o Java aceita)
      const isoDateString = localDateObj.toISOString();

      const appointmentData = {
        barbershopId: barbershopId,
        barberId: selectedBarber,
        activityIds: selectedServices.map(s => s.id),
        startTime: isoDateString 
      };

      console.log("Enviando agendamento:", appointmentData);

      await api.post("/appointments", appointmentData);
      alert("Agendamento realizado com sucesso!");
      navigate("/homepage"); 

    } catch (error) {
      console.error("Erro ao agendar:", error);
      if (error.response && error.response.data) {
          // Mostra o erro específico do backend (ex: "Horário já ocupado")
          console.log("Detalhes do erro:", error.response.data);
          alert(`Erro: ${error.response.data.message || "Falha ao agendar"}`);
      } else {
          alert("Erro ao realizar agendamento. Tente novamente.");
      }
    }
  };

  return (
    <div className={Styles.page_container}>
      <Header title="Novo Agendamento" />
      
      <div className={Styles.content_container}>
        
        {/* 1. Seleção de Serviços */}
        <section className={Styles.section}>
          <h3 className={Styles.section_title}>1. Serviços</h3>
          <div className={Styles.services_list}>
            
            {/* CORREÇÃO: Usamos servicesList aqui */}
            {servicesList && servicesList.length > 0 ? (
              servicesList.map(service => (
                <ServicesAgendamento 
                  key={service.id}
                  data={service}
                  // Verifica se o ID está na lista de selecionados
                  isSelected={selectedServices.some(s => s.id === service.id)} 
                  // Chama a função correta
                  onToggle={() => handleServiceToggle(service)}
                />
              ))
            ) : (
              <p className={Styles.info_text}>Nenhum serviço disponível.</p>
            )}
          </div>
        </section>

        {/* 2. Seleção de Barbeiro */}
        <section className={Styles.section}>
          <h3 className={Styles.section_title}>2. Profissional</h3>
          <div className={Styles.barberGrid}> {/* Nota: verifique se este estilo existe no seu CSS ou use slots_grid */}
            {barbersList.map(barber => (
              <button 
                key={barber.id}
                // Reutilizando estilos de slot para ficar padrão, ou crie um especifico
                className={`${Styles.slot_button} ${selectedBarber === barber.id ? Styles.slot_selected : ''}`}
                style={{ minWidth: '100px', height: 'auto', padding: '10px' }}
                onClick={() => setSelectedBarber(barber.id)}
              >
                <div style={{ fontSize: '1.5rem', marginBottom: '5px' }}>
                    {/* Iniciais do Nome */}
                    {barber.name.charAt(0)}
                </div>
                <span>{barber.name}</span>
              </button>
            ))}
          </div>
        </section>

        {/* 3. Seleção de Data */}
        <section className={Styles.section}>
          <h3 className={Styles.section_title}>3. Data</h3>
          <input 
            type="date" 
            className={Styles.date_input}
            value={selectedDate}
            onChange={(e) => setSelectedDate(e.target.value)}
            min={new Date().toISOString().split("T")[0]} 
          />
        </section>

        {/* 4. Seleção de Horário (Dinâmico) */}
        {selectedBarber && selectedDate && selectedServices.length > 0 && (
          <section className={Styles.section}>
            <h3 className={Styles.section_title}>4. Horário</h3>
            {availableSlots.length > 0 ? (
              <div className={Styles.slots_grid}>
                {availableSlots.map(time => (
                  <button
                    key={time}
                    className={`${Styles.slot_button} ${selectedTime === time ? Styles.slot_selected : ''}`}
                    onClick={() => setSelectedTime(time)}
                  >
                    {time}
                  </button>
                ))}
              </div>
            ) : (
              <p className={Styles.warning}>
                {!selectedDate ? "Selecione uma data acima." : "Nenhum horário disponível."}
              </p>
            )}
          </section>
        )}

        {/* Botão Final */}
        <div className={Styles.footer}>
            <div className={Styles.totalInfo} style={{ color: '#c19006', fontWeight: 'bold', marginBottom: '10px', fontSize: '1.2rem', textAlign: 'center' }}>
                Total: R$ {selectedServices.reduce((acc, s) => acc + s.price, 0).toFixed(2)}
            </div>
            <button 
                className={Styles.confirm_button} 
                onClick={handleAgendar}
                disabled={!selectedTime}
            >
                Confirmar Agendamento
            </button>
        </div>

      </div>
    </div>
  );
};

export default AgendamentoPage;