package ifsp.edu.projeto.cortaai.notificationservice.service;

import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(mailSender);
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@cortaai.com");
        when(mailSender.createMimeMessage()).thenAnswer(invocation -> mimeMessage());
    }

    @Test
    void shouldSendAllTransactionalEmailsWithHtmlContent() throws Exception {
        LocalDateTime start = LocalDateTime.of(2026, 5, 22, 14, 30);
        LocalDateTime newStart = LocalDateTime.of(2026, 5, 23, 16, 0);

        emailService.sendAppointmentConfirmedToCustomer("cliente@example.com", "Cliente", "Barbearia", "Barbeiro", start, new BigDecimal("75.50"));
        emailService.sendNewAppointmentToBarber("barber@example.com", "Barbeiro", "Cliente", start, new BigDecimal("75.50"));
        emailService.sendCancelledByBarberToCustomer("cliente@example.com", "Cliente", "Barbearia", "Barbeiro", start);
        emailService.sendCancelledByCustomerToBarber("barber@example.com", "Barbeiro", "Cliente", start);
        emailService.sendConcludedToCustomer("cliente@example.com", "Cliente", "Barbeiro", "Barbearia");
        emailService.sendRescheduledToCustomer("cliente@example.com", "Cliente", "Barbearia", "Barbeiro", start, newStart);
        emailService.sendRescheduledToBarber("barber@example.com", "Barbeiro", "Cliente", start, newStart);
        emailService.sendPaymentApprovedToCustomer("cliente@example.com", "Cliente", new BigDecimal("99.90"));
        emailService.sendReminderToCustomer("cliente@example.com", "Cliente", "Barbearia", "Barbeiro", start);

        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, times(9)).send(messageCaptor.capture());

        MimeMessage firstMessage = messageCaptor.getAllValues().get(0);
        assertThat(firstMessage.getSubject()).contains("Agendamento confirmado").contains("CortaAI");
        assertThat(firstMessage.getAllRecipients()[0].toString()).isEqualTo("cliente@example.com");
        assertThat(messageText(firstMessage))
                .contains("Cliente")
                .contains("Barbearia")
                .contains("22/05/2026")
                .contains("https://cortaai.shop/meus-agendamentos");

        assertThat(messageCaptor.getAllValues())
                .extracting(message -> {
                    try {
                        return message.getSubject();
                    } catch (Exception ex) {
                        throw new AssertionError(ex);
                    }
                })
                .anyMatch(subject -> subject.toString().contains("Novo agendamento"))
                .anyMatch(subject -> subject.toString().contains("Pagamento aprovado"))
                .anyMatch(subject -> subject.toString().contains("Lembrete"));
    }

    @Test
    void shouldSendJoinInviteAndRemovalEmails() throws Exception {
        emailService.sendJoinRequestReceivedToOwner("owner@example.com", "Barbearia Top", "Barbeiro Silva");
        emailService.sendInviteReceivedToBarber("barber@example.com", "Barbearia Top");
        emailService.sendBarberRemovedToBarber("barber@example.com", "Barbeiro Silva", "Barbearia Top");

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, times(3)).send(captor.capture());

        MimeMessage joinMsg = captor.getAllValues().get(0);
        assertThat(joinMsg.getSubject()).contains("pedido de entrada").contains("CortaAI");
        assertThat(joinMsg.getAllRecipients()[0].toString()).isEqualTo("owner@example.com");
        assertThat(messageText(joinMsg)).contains("Barbeiro Silva").contains("Barbearia Top").contains("barber-team");

        MimeMessage inviteMsg = captor.getAllValues().get(1);
        assertThat(inviteMsg.getSubject()).contains("convite").contains("CortaAI");
        assertThat(messageText(inviteMsg)).contains("Barbearia Top").contains("barberProfile");

        MimeMessage removedMsg = captor.getAllValues().get(2);
        assertThat(removedMsg.getSubject()).contains("removido").contains("CortaAI");
        assertThat(messageText(removedMsg)).contains("Barbeiro Silva").contains("Barbearia Top").contains("marketplace");
    }

    @Test
    void shouldLogAndSkipSendWhenMessageCannotBeBuilt() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "invalid from address");

        emailService.sendPaymentApprovedToCustomer("cliente@example.com", "Cliente", new BigDecimal("10.00"));

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    private static MimeMessage mimeMessage() {
        return new MimeMessage(Session.getInstance(new Properties()));
    }

    private static String messageText(MimeMessage message) throws Exception {
        return contentText(message.getContent());
    }

    private static String contentText(Object content) throws Exception {
        if (content instanceof Multipart multipart) {
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < multipart.getCount(); i++) {
                text.append(contentText(multipart.getBodyPart(i).getContent()));
            }
            return text.toString();
        }
        return content.toString();
    }
}
