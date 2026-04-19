package ifsp.edu.projeto.cortaai.paymentservice.controller;

import ifsp.edu.projeto.cortaai.paymentservice.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WebhookControllerTest {

    private MockMvc mockMvc;

    private PaymentService paymentService;

        @BeforeEach
        void setUp() {
                paymentService = mock(PaymentService.class);
                WebhookController webhookController = new WebhookController(paymentService);
                mockMvc = MockMvcBuilders.standaloneSetup(webhookController).build();
        }

    @Test
    void shouldReturnOkAndNotProcessWhenSignatureIsInvalid() throws Exception {
        when(paymentService.isWebhookTrusted("12345", "ts=1,v1=invalid", "req-001"))
                .thenReturn(false);

        mockMvc.perform(post("/api/payments/webhook")
                        .header("Content-Type", "application/json")
                        .header("x-signature", "ts=1,v1=invalid")
                        .header("x-request-id", "req-001")
                        .content("""
                                {
                                  "type": "payment",
                                  "action": "payment.updated",
                                  "data": { "id": "12345" }
                                }
                                """))
                .andExpect(status().isOk());

        verify(paymentService).isWebhookTrusted("12345", "ts=1,v1=invalid", "req-001");
        verify(paymentService, never()).processWebhook(anyString(), anyString(), anyString());
    }

    @Test
    void shouldReturnOkAndProcessWhenSignatureIsValid() throws Exception {
        when(paymentService.isWebhookTrusted("12345", "ts=999,v1=valid", "req-002"))
                .thenReturn(true);

        mockMvc.perform(post("/api/payments/webhook")
                        .header("Content-Type", "application/json")
                        .header("x-signature", "ts=999,v1=valid")
                        .header("x-request-id", "req-002")
                        .content("""
                                {
                                  "type": "payment",
                                  "action": "payment.updated",
                                  "data": { "id": "12345" }
                                }
                                """))
                .andExpect(status().isOk());

        verify(paymentService).isWebhookTrusted("12345", "ts=999,v1=valid", "req-002");
        verify(paymentService).processWebhook(
                eq("12345"),
                eq("payment"),
                eq("{type=payment, action=payment.updated, data.id=12345}")
        );
    }
}
