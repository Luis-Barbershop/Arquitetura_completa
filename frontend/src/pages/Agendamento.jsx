
import { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import Header from '../components/AgendamentoPage/Header';
import ServicesAgendamento from '../components/AgendamentoPage/ServicesAgendamento';
// Vamos usar um CSS Module para os botões de horário ficarem bonitos
import Styles from './CSS/AgendamentoPage.module.css'; 
import { getShopServices, getShopBarbers } from '../services/barbershopService';
import { createAppointment, getBarberAvailability } from '../services/appointmentService'; // Importe a nova função

function AgendamentoPage() {
    const location = useLocation();
    const navigate = useNavigate();
    const { barbershopId, barbershopName } = location.state || {};

    const [services, setServices] = useState([]);
    const [barbers, setBarbers] = useState([]);
    
    // Estados do Formulário
    const [selectedServices, setSelectedServices] = useState([]); 
    const [selectedBarber, setSelectedBarber] = useState("");     
    const [date, setDate] = useState("");
    const [selectedTime, setSelectedTime] = useState(""); // Horário clicado
    
    // Estado para os slots livres vindos do banco
    const [availableSlots, setAvailableSlots] = useState([]);
    const [loadingSlots, setLoadingSlots] = useState(false);

    useEffect(() => {
        if (!barbershopId) {
            navigate('/homepage');
            return;
        }
        Promise.all([
            getShopServices(barbershopId),
            getShopBarbers(barbershopId)
        ]).then(([servicesData, barbersData]) => {
            setServices(servicesData);
            setBarbers(barbersData);
        });
    }, [barbershopId, navigate]);

    // Sempre que Barbeiro, Data ou Serviços mudarem, buscamos os horários
    useEffect(() => {
        const fetchAvailability = async () => {
            // Só busca se tiver tudo preenchido
            if (selectedBarber && date && selectedServices.length > 0) {
                setLoadingSlots(true);
                setAvailableSlots([]); // Limpa anteriores
                setSelectedTime("");   // Reseta seleção

                // 1. Calcula duração total dos serviços marcados
                const totalDuration = services
                    .filter(s => selectedServices.includes(s.id))
                    .reduce((total, s) => total + s.durationMinutes, 0);

                // 2. Chama a API
                const slots = await getBarberAvailability(selectedBarber, date, totalDuration);
                setAvailableSlots(slots);
                setLoadingSlots(false);
            }
        };

        fetchAvailability();
    }, [selectedBarber, date, selectedServices, services]);

    const toggleService = (serviceId) => {
        setSelectedServices(prev => 
            prev.includes(serviceId) ? prev.filter(id => id !== serviceId) : [...prev, serviceId]
        );
    };

    const handleConfirm = async () => {
        if (!selectedTime) {
            alert("Por favor, selecione um horário.");
            return;
        }

        try {
            // Formata data ISO combinando a data escolhida com o horário do slot
            // O slot vem como "09:00:00", precisamos apenas de "09:00"
            const timePart = selectedTime.substring(0, 5);
            const dateTime = `${date}T${timePart}:00`;

            await createAppointment({
                barbershopId,
                barberId: selectedBarber,
                activityIds: selectedServices,
                startTime: dateTime
            });

            alert("Agendamento Confirmado!");
            navigate('/meus-agendamentos');
        } catch (error) {
            alert("Erro ao agendar. Tente novamente.");
        }
    };

    return (
        <div className={Styles.page_container}>
            <Header title={barbershopName || "Barbearia"} />

            <div className={Styles.content_container}>
                
                {/* 1. Serviços */}
                <h3 className={Styles.section_title}>1. Serviços</h3>
                <div className={Styles.services_list}>
                    {services.map(service => (
                        <ServicesAgendamento 
                            key={service.id}
                            data={service}
                            isSelected={selectedServices.includes(service.id)}
                            onToggle={() => toggleService(service.id)}
                        />
                    ))}
                </div>

                {/* 2. Profissional */}
                <h3 className={Styles.section_title}>2. Profissional</h3>
                <select 
                    value={selectedBarber} 
                    onChange={(e) => setSelectedBarber(e.target.value)}
                    className={Styles.custom_select}
                >
                    <option value="">Selecione...</option>
                    {barbers.map(barber => (
                        <option key={barber.id} value={barber.id}>{barber.name}</option>
                    ))}
                </select>

                {/* 3. Data */}
                <h3 className={Styles.section_title}>3. Data</h3>
                <input 
                    type="date" 
                    value={date} 
                    onChange={e => setDate(e.target.value)}
                    className={Styles.date_input}
                    min={new Date().toISOString().split('T')[0]} // Impede datas passadas
                />

                {/* 4. Horários Disponíveis (Dinâmico) */}
                {selectedBarber && date && selectedServices.length > 0 && (
                    <div style={{ marginTop: '20px' }}>
                        <h3 className={Styles.section_title}>4. Horários Livres</h3>
                        
                        {loadingSlots ? (
                            <p style={{color:'white'}}>Buscando horários...</p>
                        ) : availableSlots.length > 0 ? (
                            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(80px, 1fr))', gap: '10px' }}>
                                {availableSlots.map((slot, index) => {
                                    // Formata "09:00:00" para "09:00" visualmente
                                    const timeLabel = slot.substring(0, 5);
                                    return (
                                        <button
                                            key={index}
                                            onClick={() => setSelectedTime(slot)}
                                            style={{
                                                padding: '10px',
                                                borderRadius: '5px',
                                                border: selectedTime === slot ? '2px solid #D4AF37' : '1px solid #444',
                                                backgroundColor: selectedTime === slot ? '#D4AF37' : '#0f0f0f',
                                                color: selectedTime === slot ? '#000' : '#fff',
                                                cursor: 'pointer',
                                                fontWeight: 'bold'
                                            }}
                                        >
                                            {timeLabel}
                                        </button>
                                    );
                                })}
                            </div>
                        ) : (
                            <p style={{color:'#F44336'}}>Nenhum horário disponível para esta data/duração.</p>
                        )}
                    </div>
                )}

                <button onClick={handleConfirm} className={Styles.confirm_button} disabled={!selectedTime}>
                    Confirmar Agendamento
                </button>
            </div>
        </div>
    );
}

export default AgendamentoPage;