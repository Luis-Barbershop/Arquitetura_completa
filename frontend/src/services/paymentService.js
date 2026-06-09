import api from './api';

export const createAppointmentPayment = async (appointmentId, paymentMethod = 'CREDIT_CARD') => {
    const response = await api.post('/payments/create', {
        appointmentId,
        paymentMethod,
    });
    return response.data;
};

export const getMyPayments = async () => {
    const response = await api.get('/payments/my-payments');
    return Array.isArray(response.data) ? response.data : [];
};

export const getPendingPaymentCheckoutUrl = async (appointmentId) => {
    const payments = await getMyPayments();
    const pendingPayment = payments.find((payment) =>
        payment?.appointmentId === appointmentId
        && ['PENDING', 'IN_PROCESS', 'APPROVED'].includes(payment?.status)
        && payment?.checkoutUrl
    );

    return pendingPayment?.checkoutUrl || null;
};
