import { beforeEach, describe, expect, it, vi } from 'vitest';

const getMock = vi.fn();
const postMock = vi.fn();

vi.mock('./api', () => ({
    default: {
        get: (...args) => getMock(...args),
        post: (...args) => postMock(...args),
    },
}));

import { createAppointmentPayment, getPendingPaymentCheckoutUrl, getMyPayments } from './paymentService';

describe('paymentService', () => {
    beforeEach(() => {
        getMock.mockReset();
        postMock.mockReset();
    });

    it('creates payment for appointment with default method', async () => {
        postMock.mockResolvedValueOnce({
            data: {
                id: 'tx-1',
                checkoutUrl: 'https://checkout.example/1',
            },
        });

        await expect(createAppointmentPayment('appt-1')).resolves.toEqual({
            id: 'tx-1',
            checkoutUrl: 'https://checkout.example/1',
        });

        expect(postMock).toHaveBeenCalledWith('/payments/create', {
            appointmentId: 'appt-1',
            paymentMethod: 'CREDIT_CARD',
        });
    });

    it('creates payment with explicit payment method', async () => {
        postMock.mockResolvedValueOnce({
            data: {
                id: 'tx-2',
                checkoutUrl: 'https://checkout.example/2',
            },
        });

        await createAppointmentPayment('appt-2', 'PIX');

        expect(postMock).toHaveBeenCalledWith('/payments/create', {
            appointmentId: 'appt-2',
            paymentMethod: 'PIX',
        });
    });

    it('lists my payments and resolves the pending checkout url for an appointment', async () => {
        getMock.mockResolvedValue({
            data: [
                {
                    appointmentId: 'appt-3',
                    status: 'PENDING',
                    checkoutUrl: 'https://checkout.example/pending',
                },
                {
                    appointmentId: 'appt-4',
                    status: 'APPROVED',
                    checkoutUrl: 'https://checkout.example/approved',
                },
            ],
        });

        await expect(getMyPayments()).resolves.toEqual([
            {
                appointmentId: 'appt-3',
                status: 'PENDING',
                checkoutUrl: 'https://checkout.example/pending',
            },
            {
                appointmentId: 'appt-4',
                status: 'APPROVED',
                checkoutUrl: 'https://checkout.example/approved',
            },
        ]);

        await expect(getPendingPaymentCheckoutUrl('appt-3')).resolves.toBe('https://checkout.example/pending');
        expect(getMock).toHaveBeenCalledWith('/payments/my-payments');
    });
});
