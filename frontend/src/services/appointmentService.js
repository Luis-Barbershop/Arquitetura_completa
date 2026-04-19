import api from './api';

const WALK_IN_CUSTOMER_ID = '00000000-0000-0000-0000-000000000000';

const extractActivityNames = (appointment) => {
    if (Array.isArray(appointment?.activityNames) && appointment.activityNames.length > 0) {
        return appointment.activityNames.filter(Boolean);
    }

    if (Array.isArray(appointment?.activities) && appointment.activities.length > 0) {
        return appointment.activities
            .map((item) => item?.activityName)
            .filter(Boolean);
    }

    return [];
};

const fetchBarberName = async (barberId) => {
    if (!barberId) return '';

    try {
        const response = await api.get(`/barbers/${barberId}`);
        return response.data?.name || '';
    } catch (error) {
        console.error(`Erro ao buscar barbeiro ${barberId}:`, error);
        return '';
    }
};

const fetchCustomerName = async (customerId) => {
    if (!customerId) return '';

    try {
        const response = await api.get(`/customers/${customerId}`);
        return response.data?.name || '';
    } catch (error) {
        console.error(`Erro ao buscar cliente ${customerId}:`, error);
        return '';
    }
};

const fetchBarbershopNameMap = async () => {
    try {
        const response = await api.get('/barbershops');
        const shops = Array.isArray(response.data) ? response.data : [];
        return shops.reduce((acc, shop) => {
            if (shop?.id) {
                acc[shop.id] = shop.name || '';
            }
            return acc;
        }, {});
    } catch (error) {
        console.error('Erro ao buscar barbearias:', error);
        return {};
    }
};

// Criar Agendamento
// Envia o JSON para o AppointmentsController.createAppointments
export const createAppointment = async (appointmentData) => {
    // appointmentData deve ter startTime no formato "yyyy-MM-ddTHH:mm:ss" (sem timezone)
    const response = await api.post('/appointments', appointmentData);
    return response.data;
};

// Buscar Meus Agendamentos
// Usa a lógica de Roles para chamar a rota certa do Controller
export const getMyAppointments = async () => {
    const response = await api.get('/appointments/my-appointments');
    const appointments = Array.isArray(response.data) ? response.data : [];

    if (!appointments.length) {
        return [];
    }

    const uniqueBarberIds = [...new Set(appointments.map((item) => item.barberId).filter(Boolean))];
    const uniqueCustomerIds = [...new Set(
        appointments
            .map((item) => item.customerId)
            .filter((id) => id && id !== WALK_IN_CUSTOMER_ID)
    )];

    const [barbershopNameMap, barberNameEntries, customerNameEntries] = await Promise.all([
        fetchBarbershopNameMap(),
        Promise.all(uniqueBarberIds.map(async (id) => [id, await fetchBarberName(id)])),
        Promise.all(uniqueCustomerIds.map(async (id) => [id, await fetchCustomerName(id)])),
    ]);

    const barberNameMap = Object.fromEntries(barberNameEntries);
    const customerNameMap = Object.fromEntries(customerNameEntries);

    return appointments.map((appointment) => ({
        ...appointment,
        barberName: barberNameMap[appointment.barberId] || appointment.barberName || 'Barbeiro',
        customerName: customerNameMap[appointment.customerId] || appointment.customerName || 'Cliente',
        barbershopName: barbershopNameMap[appointment.barbershopId] || appointment.barbershopName || 'Barbearia',
        activityNames: extractActivityNames(appointment),
    }));
};

// Cancelar Agendamento
// Conecta com AppointmentsController.cancelAppointments
export const cancelAppointment = async (id) => {
    const response = await api.put(`/appointments/${id}/cancel`);
    return response.data;
};

export const getBarbershopSchedule = async (shopId, date) => {
    try {
        const response = await api.get(`/appointments/barbershop/${shopId}`, {
            params: { date }
        });
        const appointments = Array.isArray(response.data) ? response.data : [];
        return appointments.map((appointment) => ({
            ...appointment,
            activityNames: extractActivityNames(appointment),
        }));
    } catch (error) {
        console.error('Erro ao buscar agenda da barbearia:', error);
        return [];
    }
};

