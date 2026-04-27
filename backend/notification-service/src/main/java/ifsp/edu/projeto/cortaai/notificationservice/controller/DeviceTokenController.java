package ifsp.edu.projeto.cortaai.notificationservice.controller;

import ifsp.edu.projeto.cortaai.notificationservice.dto.RegisterDeviceTokenRequestDTO;
import ifsp.edu.projeto.cortaai.notificationservice.feign.UserInfoDTO;
import ifsp.edu.projeto.cortaai.notificationservice.feign.UserServiceClient;
import ifsp.edu.projeto.cortaai.notificationservice.service.DeviceTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/notifications/device-tokens")
@RequiredArgsConstructor
@Tag(name = "Device Tokens", description = "Endpoints para registro de token push do dispositivo")
public class DeviceTokenController {

    private final DeviceTokenService deviceTokenService;
    private final UserServiceClient userServiceClient;

    @Operation(summary = "Registrar token de dispositivo")
    @PostMapping
    public ResponseEntity<Void> registerToken(
            @Parameter(hidden = true) @RequestHeader("X-User-UID") String firebaseUid,
            @Valid @RequestBody RegisterDeviceTokenRequestDTO dto
    ) {
        UUID userId = resolveUserId(firebaseUid);
        deviceTokenService.registerToken(userId, dto.token(), dto.platform());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Desativar token de dispositivo")
    @DeleteMapping
    public ResponseEntity<Void> unregisterToken(
            @Parameter(hidden = true) @RequestHeader("X-User-UID") String firebaseUid,
            @RequestParam("token") String token
    ) {
        UUID userId = resolveUserId(firebaseUid);
        deviceTokenService.deactivateToken(userId, token);
        return ResponseEntity.noContent().build();
    }

    private UUID resolveUserId(String firebaseUid) {
        UserInfoDTO user = userServiceClient.getUserByFirebaseUid(firebaseUid);
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("Usuário não encontrado para o UID informado.");
        }
        return user.getId();
    }
}
