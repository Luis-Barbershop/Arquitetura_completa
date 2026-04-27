package ifsp.edu.projeto.cortaai.notificationservice.dto;

import ifsp.edu.projeto.cortaai.notificationservice.model.PushPlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO para registro de token de push notification do dispositivo.
 */
public record RegisterDeviceTokenRequestDTO(
        @NotBlank(message = "token é obrigatório")
        String token,

        @NotNull(message = "platform é obrigatória")
        PushPlatform platform
) {
}
