import api from './api';

// Criar Agendamento
// POST /api/appointments
export const createAppointment = async (appointmentData) => {
    const response = await api.post('/appointments', appointmentData);
    return response.data;
};

// Buscar Meus Agendamentos (funciona para Customer e Barber)
// O backend identifica o tipo pelo X-User-UID do Firebase
// GET /api/appointments/my-appointments
export const getMyAppointments = async () => {
    const response = await api.get('/appointments/my-appointments');
    return response.data;
};

// Cancelar Agendamento
// PUT /api/appointments/{id}/cancel
export const cancelAppointment = async (id) => {
    const response = await api.put(`/appointments/${id}/cancel`);
    return response.data;
};

// Consultar horários disponíveis
// GET /api/appointments/availability?barberId=UUID&date=YYYY-MM-DD
export const getBarberAvailability = async (barberId, date, duration) => {
    try {
        const response = await api.get('/appointments/availability', {
            params: {
                barberId: barberId,
                date: date,
            }
        });
        return response.data; // Lista de TimeSlotDTO (horários livres)
    } catch (error) {
        console.error("Erro ao buscar disponibilidade:", error);
        return [];
    }
};

