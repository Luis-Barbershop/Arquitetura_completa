package ifsp.edu.projeto.cortaai.notificationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Serviço de envio de e-mails transacionais HTML.
 * Todos os métodos são assíncronos — falhas são logadas sem propagar exceção.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${notification.from-email}")
    private String fromEmail;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

    // ─── Agendamento confirmado → cliente ───────────────────────────────────────

    @Async
    public void sendAppointmentConfirmedToCustomer(
            String toEmail, String customerName,
            String barbershopName, String barberName,
            LocalDateTime startTime, BigDecimal totalPrice) {

        String subject = "✅ Agendamento confirmado — CortaAI";
        String body = baseTemplate(
                "Agendamento confirmado!",
                String.format("Olá, <strong>%s</strong>!", customerName),
                String.format("""
                        Seu agendamento está confirmado. Confira os detalhes abaixo:
                        <br><br>
                        <table style="width:100%%;border-collapse:collapse;">
                          <tr><td style="padding:6px 0;color:#888;">Barbearia</td>
                              <td style="padding:6px 0;font-weight:600;">%s</td></tr>
                          <tr><td style="padding:6px 0;color:#888;">Barbeiro</td>
                              <td style="padding:6px 0;font-weight:600;">%s</td></tr>
                          <tr><td style="padding:6px 0;color:#888;">Data e hora</td>
                              <td style="padding:6px 0;font-weight:600;">%s</td></tr>
                          <tr><td style="padding:6px 0;color:#888;">Valor total</td>
                              <td style="padding:6px 0;font-weight:600;color:#c19006;">R$&nbsp;%.2f</td></tr>
                        </table>
                        """,
                        barbershopName, barberName,
                        startTime.format(FORMATTER), totalPrice),
                "Ver meus agendamentos", "https://cortaai.shop/meus-agendamentos"
        );
        send(toEmail, subject, body);
    }

    // ─── Novo agendamento → barbeiro ────────────────────────────────────────────

    @Async
    public void sendNewAppointmentToBarber(
            String toEmail, String barberName,
            String customerName, LocalDateTime startTime,
            BigDecimal totalPrice) {

        String subject = "📅 Novo agendamento — CortaAI";
        String body = baseTemplate(
                "Novo agendamento!",
                String.format("Olá, <strong>%s</strong>!", barberName),
                String.format("""
                        Você tem um novo agendamento confirmado:
                        <br><br>
                        <table style="width:100%%;border-collapse:collapse;">
                          <tr><td style="padding:6px 0;color:#888;">Cliente</td>
                              <td style="padding:6px 0;font-weight:600;">%s</td></tr>
                          <tr><td style="padding:6px 0;color:#888;">Data e hora</td>
                              <td style="padding:6px 0;font-weight:600;">%s</td></tr>
                          <tr><td style="padding:6px 0;color:#888;">Valor</td>
                              <td style="padding:6px 0;font-weight:600;color:#c19006;">R$&nbsp;%.2f</td></tr>
                        </table>
                        """,
                        customerName, startTime.format(FORMATTER), totalPrice),
                "Ver minha agenda", "https://cortaai.shop/barberHome"
        );
        send(toEmail, subject, body);
    }

    // ─── Cancelamento pelo barbeiro → cliente ───────────────────────────────────

    @Async
    public void sendCancelledByBarberToCustomer(
            String toEmail, String customerName,
            String barbershopName, String barberName,
            LocalDateTime startTime) {

        String subject = "❌ Agendamento cancelado — CortaAI";
        String body = baseTemplate(
                "Agendamento cancelado",
                String.format("Olá, <strong>%s</strong>!", customerName),
                String.format("""
                        Infelizmente seu agendamento foi cancelado pelo barbeiro.
                        <br><br>
                        <table style="width:100%%;border-collapse:collapse;">
                          <tr><td style="padding:6px 0;color:#888;">Barbearia</td>
                              <td style="padding:6px 0;font-weight:600;">%s</td></tr>
                          <tr><td style="padding:6px 0;color:#888;">Barbeiro</td>
                              <td style="padding:6px 0;font-weight:600;">%s</td></tr>
                          <tr><td style="padding:6px 0;color:#888;">Horário cancelado</td>
                              <td style="padding:6px 0;font-weight:600;">%s</td></tr>
                        </table>
                        <br>Você pode agendar um novo horário quando quiser!
                        """,
                        barbershopName, barberName, startTime.format(FORMATTER)),
                "Agendar novamente", "https://cortaai.shop/homepage"
        );
        send(toEmail, subject, body);
    }

    // ─── Cancelamento pelo cliente → barbeiro ───────────────────────────────────

    @Async
    public void sendCancelledByCustomerToBarber(
            String toEmail, String barberName,
            String customerName, LocalDateTime startTime) {

        String subject = "❌ Agendamento cancelado pelo cliente — CortaAI";
        String body = baseTemplate(
                "Agendamento cancelado",
                String.format("Olá, <strong>%s</strong>!", barberName),
                String.format("""
                        O cliente <strong>%s</strong> cancelou o agendamento
                        marcado para <strong>%s</strong>.
                        <br><br>
                        O horário está disponível novamente na sua agenda.
                        """,
                        customerName, startTime.format(FORMATTER)),
                "Ver minha agenda", "https://cortaai.shop/barberHome"
        );
        send(toEmail, subject, body);
    }

    // ─── Atendimento concluído → cliente (pedir avaliação) ──────────────────────

    @Async
    public void sendConcludedToCustomer(
            String toEmail, String customerName,
            String barberName, String barbershopName) {

        String subject = "⭐ Como foi seu atendimento? — CortaAI";
        String body = baseTemplate(
                "Atendimento concluído!",
                String.format("Olá, <strong>%s</strong>!", customerName),
                String.format("""
                        Seu atendimento com <strong>%s</strong> na <strong>%s</strong>
                        foi concluído. Esperamos que tenha gostado!
                        <br><br>
                        Sua avaliação ajuda outros clientes a encontrar os melhores barbeiros.
                        """,
                        barberName, barbershopName),
                "Avaliar atendimento", "https://cortaai.shop/meus-agendamentos"
        );
        send(toEmail, subject, body);
    }

      // ─── Atendimento reagendado ────────────────────────────────────────────────

      @Async
      public void sendRescheduledToCustomer(
          String toEmail, String customerName,
          String barbershopName, String barberName,
          LocalDateTime oldStartTime, LocalDateTime newStartTime) {

        String subject = "🔁 Agendamento reagendado — CortaAI";
        String body = baseTemplate(
            "Horario atualizado!",
            String.format("Olá, <strong>%s</strong>!", customerName),
            String.format("""
                Seu agendamento na <strong>%s</strong> com <strong>%s</strong>
                foi reagendado.
                <br><br>
                <table style="width:100%%;border-collapse:collapse;">
                  <tr><td style="padding:6px 0;color:#888;">Horario anterior</td>
                    <td style="padding:6px 0;font-weight:600;">%s</td></tr>
                  <tr><td style="padding:6px 0;color:#888;">Novo horario</td>
                    <td style="padding:6px 0;font-weight:600;color:#c19006;">%s</td></tr>
                </table>
                """,
                barbershopName, barberName,
                oldStartTime.format(FORMATTER),
                newStartTime.format(FORMATTER)),
            "Ver meus agendamentos", "https://cortaai.shop/meus-agendamentos"
        );
        send(toEmail, subject, body);
      }

      @Async
      public void sendRescheduledToBarber(
          String toEmail, String barberName,
          String customerName,
          LocalDateTime oldStartTime, LocalDateTime newStartTime) {

        String subject = "🔁 Atendimento reagendado — CortaAI";
        String body = baseTemplate(
            "Agenda atualizada!",
            String.format("Olá, <strong>%s</strong>!", barberName),
            String.format("""
                O atendimento com <strong>%s</strong> foi reagendado.
                <br><br>
                <table style="width:100%%;border-collapse:collapse;">
                  <tr><td style="padding:6px 0;color:#888;">Horario anterior</td>
                    <td style="padding:6px 0;font-weight:600;">%s</td></tr>
                  <tr><td style="padding:6px 0;color:#888;">Novo horario</td>
                    <td style="padding:6px 0;font-weight:600;color:#c19006;">%s</td></tr>
                </table>
                """,
                customerName,
                oldStartTime.format(FORMATTER),
                newStartTime.format(FORMATTER)),
            "Ver minha agenda", "https://cortaai.shop/barberHome"
        );
        send(toEmail, subject, body);
      }

    // ─── Pagamento aprovado → cliente ───────────────────────────────────────────

    @Async
    public void sendPaymentApprovedToCustomer(
            String toEmail, String customerName, BigDecimal amount) {

        String subject = "💳 Pagamento aprovado — CortaAI";
        String body = baseTemplate(
                "Pagamento aprovado!",
                String.format("Olá, <strong>%s</strong>!", customerName),
                String.format("""
                        Seu pagamento de <strong style="color:#c19006;">R$&nbsp;%.2f</strong>
                        foi aprovado com sucesso pelo Mercado Pago.
                        <br><br>
                        Seu agendamento está garantido. Nos vemos em breve!
                        """, amount),
                "Ver meus agendamentos", "https://cortaai.shop/meus-agendamentos"
        );
        send(toEmail, subject, body);
    }

    // ─── Lembrete de agendamento próximo → cliente ──────────────────────────────

    @Async
    public void sendReminderToCustomer(
            String toEmail, String customerName,
            String barbershopName, String barberName,
            LocalDateTime startTime) {

        String subject = "⏰ Lembrete: seu agendamento está chegando — CortaAI";
        String body = baseTemplate(
                "Lembrete de agendamento",
                String.format("Olá, <strong>%s</strong>!", customerName),
                String.format("""
                        Este é um lembrete de que seu agendamento está chegando:
                        <br><br>
                        <table style="width:100%%;border-collapse:collapse;">
                          <tr><td style="padding:6px 0;color:#888;">Barbearia</td>
                              <td style="padding:6px 0;font-weight:600;">%s</td></tr>
                          <tr><td style="padding:6px 0;color:#888;">Barbeiro</td>
                              <td style="padding:6px 0;font-weight:600;">%s</td></tr>
                          <tr><td style="padding:6px 0;color:#888;">Horário</td>
                              <td style="padding:6px 0;font-weight:600;">%s</td></tr>
                        </table>
                        """,
                        barbershopName, barberName, startTime.format(FORMATTER)),
                "Ver meus agendamentos", "https://cortaai.shop/meus-agendamentos"
        );
        send(toEmail, subject, body);
    }

    // ─── Envio interno ───────────────────────────────────────────────────────────

    private void send(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("E-mail enviado para {}: {}", to, subject);
        } catch (MessagingException e) {
            log.error("Falha ao enviar e-mail para {}: {}", to, e.getMessage());
        }
    }

    // ─── Join / Invite / Remoção ─────────────────────────────────────────────────

    @Async
    public void sendJoinRequestReceivedToOwner(
            String toEmail, String barbershopName, String barberName) {

        String subject = "✂ Novo pedido de entrada na sua barbearia — CortaAI";
        String body = baseTemplate(
                "Novo pedido de entrada!",
                "Um barbeiro quer entrar na sua equipe.",
                String.format("""
                        O barbeiro <strong>%s</strong> solicitou entrada na barbearia <strong>%s</strong>.
                        <br><br>
                        Acesse o painel para aprovar ou recusar o pedido.
                        """, barberName, barbershopName),
                "Ver pedidos pendentes", "https://cortaai.shop/barber-team"
        );
        send(toEmail, subject, body);
    }

    @Async
    public void sendInviteReceivedToBarber(String toEmail, String barbershopName) {

        String subject = "✂ Você recebeu um convite de barbearia — CortaAI";
        String body = baseTemplate(
                "Convite recebido!",
                "Uma barbearia quer você na equipe.",
                String.format("""
                        A barbearia <strong>%s</strong> convidou você para fazer parte da equipe.
                        <br><br>
                        Acesse seu perfil para aceitar ou recusar o convite.
                        """, barbershopName),
                "Ver meu perfil", "https://cortaai.shop/barberProfile"
        );
        send(toEmail, subject, body);
    }

    @Async
    public void sendBarberRemovedToBarber(String toEmail, String barberName, String barbershopName) {

        String subject = "✂ Você foi removido da barbearia — CortaAI";
        String body = baseTemplate(
                "Você foi removido da equipe",
                String.format("Olá, <strong>%s</strong>!", barberName),
                String.format("""
                        Informamos que você foi removido da barbearia <strong>%s</strong>.
                        <br><br>
                        Você pode procurar outra barbearia pelo marketplace do CortaAI.
                        """, barbershopName),
                "Explorar barbearias", "https://cortaai.shop/marketplace"
        );
        send(toEmail, subject, body);
    }

    // ─── Template base HTML ──────────────────────────────────────────────────────
    private String baseTemplate(String title, String greeting, String content, String ctaLabel, String ctaUrl) {
        return """
                <!DOCTYPE html>
                <html lang="pt-BR">
                <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"></head>
                <body style="margin:0;padding:0;background:#0d0d0d;font-family:'Helvetica Neue',Helvetica,Arial,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background:#0d0d0d;padding:40px 16px;">
                    <tr><td align="center">
                      <table width="520" cellpadding="0" cellspacing="0"
                             style="background:#141414;border-radius:20px;border:1px solid #2a2a2a;overflow:hidden;">

                        <!-- Header -->
                        <tr>
                          <td style="background:linear-gradient(135deg,#1a1400,#0d0d0d);
                                     padding:28px 36px;border-bottom:1px solid #2a2a2a;">
                            <span style="color:#c19006;font-size:22px;font-weight:800;letter-spacing:-0.5px;">
                              ✂ CortaAI
                            </span>
                          </td>
                        </tr>

                        <!-- Body -->
                        <tr>
                          <td style="padding:36px 36px 28px;">
                            <h1 style="margin:0 0 12px;color:#f0f0f0;font-size:22px;font-weight:700;">%s</h1>
                            <p style="margin:0 0 20px;color:#bbb;font-size:15px;">%s</p>
                            <p style="margin:0 0 28px;color:#aaa;font-size:14px;line-height:1.7;">%s</p>
                            <a href="%s"
                               style="display:inline-block;background:#c19006;color:#111;
                                      padding:12px 28px;border-radius:10px;font-weight:700;
                                      font-size:14px;text-decoration:none;letter-spacing:0.3px;">
                              %s
                            </a>
                          </td>
                        </tr>

                        <!-- Footer -->
                        <tr>
                          <td style="padding:20px 36px;border-top:1px solid #222;
                                     color:#555;font-size:12px;line-height:1.6;">
                            Você está recebendo este e-mail porque tem uma conta no CortaAI.<br>
                            © 2026 CortaAI — Todos os direitos reservados.
                          </td>
                        </tr>

                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(title, greeting, content, ctaUrl, ctaLabel);
    }
}
