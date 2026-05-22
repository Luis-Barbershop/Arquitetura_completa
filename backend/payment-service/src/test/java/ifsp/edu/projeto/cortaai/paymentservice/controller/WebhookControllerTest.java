package ifsp.edu.projeto.cortaai.paymentservice.controller;

import ifsp.edu.projeto.cortaai.paymentservice.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WebhookControllerTest {

    private MockMvc mockMvc;

    private PaymentService paymentService;
    private WebhookController webhookController;

    @BeforeEach
    void setUp() {
        paymentService = mock(PaymentService.class);
        webhookController = new WebhookController(paymentService);
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

    @Test
    void shouldReturnOkAndIgnorePayloadWithoutDataId() throws Exception {
        mockMvc.perform(post("/api/payments/webhook")
                        .header("Content-Type", "application/json")
                        .content("""
                                {
                                  "type": "payment",
                                  "action": "payment.updated",
                                  "data": {}
                                }
                                """))
                .andExpect(status().isOk());

        verify(paymentService, never()).isWebhookTrusted(anyString(), anyString(), anyString());
        verify(paymentService, never()).processWebhook(anyString(), anyString(), anyString());
    }

    @Test
    void shouldReturnOkWhenWebhookProcessingThrows() throws Exception {
        when(paymentService.isWebhookTrusted("12345", "ts=999,v1=valid", "req-003"))
                .thenReturn(true);
        doThrow(new RuntimeException("mp indisponível"))
                .when(paymentService).processWebhook(eq("12345"), eq("payment"), anyString());

        mockMvc.perform(post("/api/payments/webhook")
                        .header("Content-Type", "application/json")
                        .header("x-signature", "ts=999,v1=valid")
                        .header("x-request-id", "req-003")
                        .content("""
                                {
                                  "type": "payment",
                                  "action": "payment.updated",
                                  "data": { "id": "12345" }
                                }
                                """))
                .andExpect(status().isOk());

        verify(paymentService).processWebhook(
                eq("12345"),
                eq("payment"),
                eq("{type=payment, action=payment.updated, data.id=12345}")
        );
    }

    @Test
    void shouldMaskWebhookIdentifiers() {
        assertThatMaskedIdentifier(null, "***");
        assertThatMaskedIdentifier("   ", "***");
        assertThatMaskedIdentifier("123456", "***");
        assertThatMaskedIdentifier("  123456789  ", "1234...89");
    }

    private void assertThatMaskedIdentifier(String value, String expected) {
        org.assertj.core.api.Assertions.assertThat(
                        (String) ReflectionTestUtils.invokeMethod(webhookController, "maskIdentifier", value))
                .isEqualTo(expected);
    }
}
