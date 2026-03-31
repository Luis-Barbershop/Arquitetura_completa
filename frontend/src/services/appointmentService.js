import api from './api';

// Criar Agendamento
// Envia o JSON para o AppointmentsController.createAppointments
export const createAppointment = async (appointmentData) => {
    // appointmentData já deve vir com a data em .toISOString()
    const response = await api.post('/appointments', appointmentData);
    return response.data;
};

// Buscar Meus Agendamentos
// Usa a lógica de Roles para chamar a rota certa do Controller
export const getMyAppointments = async () => {
    const role = localStorage.getItem('role');
    let url = '';

    // Verifica se é Cliente ou Barbeiro para usar a rota correta do Java
    if (role === 'ROLE_CUSTOMER') {
        url = '/appointments/customer/me'; // Rota definida no seu Controller
    } else if (role === 'ROLE_BARBER' || role === 'ROLE_OWNER') {
        url = '/appointments/barber/me';   // Rota definida no seu Controller
    } else {
        console.warn("Role não encontrada ou inválida");
        return [];
    }

    const response = await api.get(url);
    return response.data; // Retorna lista de AppointmentsDTO
};

// Cancelar Agendamento
// Conecta com AppointmentsController.cancelAppointments
export const cancelAppointment = async (id) => {
    const response = await api.patch(`/appointments/${id}/cancel`);
    return response.data;
};

export const getBarberAvailability = async (barberId, date, duration) => {
    // O Back-end espera: /api/barbers/{id}/availability?date=YYYY-MM-DD&duration=MINUTOS
    try {
        const response = await api.get(`/barbers/${barberId}/availability`, {
            params: {
                date: date,
                duration: duration
            }
        });
        return response.data; // Retorna lista de horários ["09:00:00", "09:30:00", ...]
    } catch (error) {
        console.error("Erro ao buscar disponibilidade:", error);
        return [];
    }
};

