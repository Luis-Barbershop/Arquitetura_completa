import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Styles from './CSS/MeusAgendamentos.module.css';
import { getMyAppointments, cancelAppointment } from '../services/appointmentService';

const MeusAgendamentosPage = () => {
    const navigate = useNavigate();
    const [appointments, setAppointments] = useState([]);
    const [loading, setLoading] = useState(true);
    
    // Identificar o papel para ajustar os textos
    const role = localStorage.getItem('role'); 
    const isCustomer = role === 'ROLE_CUSTOMER';

    useEffect(() => {
        carregarAgendamentos();
    }, []);

    const carregarAgendamentos = async () => {
        try {
            const data = await getMyAppointments();
            // Ordenar: Mais recentes primeiro
            const sorted = data.sort((a, b) => new Date(b.startTime) - new Date(a.startTime));
            setAppointments(sorted);
        } catch (error) {
            console.error("Erro ao buscar agendamentos:", error);
            // alert("Não foi possível carregar sua agenda.");
        } finally {
            setLoading(false);
        }
    };

    const handleCancel = async (id) => {
        if (window.confirm("Tem certeza que deseja cancelar este agendamento?")) {
            try {
                await cancelAppointment(id);
                alert("Agendamento cancelado com sucesso!");
                carregarAgendamentos(); // Recarrega a lista
            } catch (error) {
                alert("Erro ao cancelar. Tente novamente.");
            }
        }
    };

    // Função para formatar data bonita (Ex: 28/11 às 14:00)
    const formatData = (isoString) => {
        const date = new Date(isoString);
        return date.toLocaleString('pt-BR', { 
            day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' 
        });
    };

    // Função para traduzir status
    const translateStatus = (status) => {
        const map = {
            'SCHEDULED': 'Agendado',
            'CANCELLED': 'Cancelado',
            'COMPLETED': 'Concluído'
        };
        return map[status] || status;
    };

    return (
        <div className={Styles.container}>
            <div className={Styles.content}>
                
                {/* Botão de Voltar */}
                <button 
                    onClick={() => navigate(-1)} 
                    style={{background:'none', border:'none', color:'#c19006', cursor:'pointer', marginBottom:'20px'}}
                >
                    ← Voltar
                </button>

                <h1 className={Styles.title}>
                    {isCustomer ? "Meus Cortes" : "Minha Agenda"}
                </h1>

                {loading ? (
                    <p style={{textAlign:'center'}}>Carregando...</p>
                ) : appointments.length === 0 ? (
                    <div className={Styles.empty}>
                        <h3>Nenhum agendamento encontrado.</h3>
                        {isCustomer && <p>Que tal marcar um horário agora?</p>}
                    </div>
                ) : (
                    <div className={Styles.list}>
                        {appointments.map(app => (
                            <div key={app.id} className={Styles.card}>
                                
                                <div className={Styles.info}>
                                    <span className={Styles.date}>
                                        {formatData(app.startTime)}
                                    </span>
                                    
                                    {/* Se sou Cliente, mostro o Barbeiro. Se sou Barbeiro, mostro o Cliente */}
                                    <span className={Styles.details}>
                                        {isCustomer 
                                            ? `Com: ${app.barberName} (${app.barbershopName})`
                                            : `Cliente: ${app.customerName}`
                                        }
                                    </span>

                                    {/* Lista de serviços (caso seu DTO retorne activityNames como lista) */}
                                    <span className={Styles.details} style={{fontStyle:'italic'}}>
                                        {app.activityNames ? app.activityNames.join(", ") : "Serviço"}
                                    </span>

                                    <span className={`${Styles.details} ${Styles['status_' + app.status]}`}>
                                        {translateStatus(app.status)}
                                    </span>
                                </div>

                                {/* Botão Cancelar apenas se estiver Agendado */}
                                {app.status === 'SCHEDULED' && (
                                    <button 
                                        className={Styles.cancelButton}
                                        onClick={() => handleCancel(app.id)}
                                    >
                                        Cancelar
                                    </button>
                                )}
                            </div>
                        ))}
                    </div>
                )}
            </div>
        </div>
    );
};

export default MeusAgendamentosPage;