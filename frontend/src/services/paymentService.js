import api from './api';

export const createAppointmentPayment = async (appointmentId, paymentMethod = 'CREDIT_CARD') => {
    const response = await api.post('/payments/create', {
        appointmentId,
        paymentMethod,
    });
    return response.data;
};
