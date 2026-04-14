package ifsp.edu.projeto.cortaai.scheduleservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO para agendamento manual feito pelo barbeiro (walk-in / presencial).
 * Não requer customerId — o cliente é identificado apenas pelo nome.
 * Nenhum evento de pagamento é gerado para este tipo de agendamento.
 */
@Getter
@Setter
public class BarberManualBookingDTO {

    /** UUID da barbearia onde o atendimento ocorrerá. Obrigatório. */
    @NotNull(message = "O ID da barbearia é obrigatório.")
    private UUID barbershopId;

    /** Lista de IDs de atividades/serviços que serão realizados. Obrigatório. */
    @NotNull(message = "Informe ao menos uma atividade.")
    private List<UUID> activityIds;

    /** Data e hora de início do atendimento. Obrigatório. */
    @NotNull(message = "O horário de início é obrigatório.")
    private LocalDateTime startTime;

    /** Nome do cliente walk-in. Obrigatório. */
    @NotBlank(message = "O nome do cliente é obrigatório.")
    @Size(max = 70, message = "O nome do cliente deve ter no máximo 70 caracteres.")
    private String clientName;

    /** Telefone do cliente para contato. Opcional. */
    @Size(min = 11, max = 11, message = "O telefone deve conter exatamente 11 dígitos.")
    @Pattern(regexp = "^\\d{11}$", message = "O telefone deve conter apenas números (11 dígitos).")
    private String clientPhone;
}
