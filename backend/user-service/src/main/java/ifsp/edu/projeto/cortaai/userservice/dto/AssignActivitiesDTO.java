package ifsp.edu.projeto.cortaai.userservice.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Corpo da requisição para atribuir/substituir as atividades (habilidades) de um barbeiro.
 * A lista substitui completamente a seleção anterior.
 */
public record AssignActivitiesDTO(
        @NotNull List<UUID> activityIds
) {}
