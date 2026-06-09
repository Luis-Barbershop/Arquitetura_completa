import { describe, expect, it, vi } from 'vitest';

const postMock = vi.fn();

vi.mock('./api', () => ({
    default: {
        post: (...args) => postMock(...args),
    },
}));

import { createAppointmentPayment } from './paymentService';

describe('paymentService', () => {
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
});
