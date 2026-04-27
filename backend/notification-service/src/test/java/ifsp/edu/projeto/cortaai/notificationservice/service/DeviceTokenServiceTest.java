package ifsp.edu.projeto.cortaai.notificationservice.service;

import ifsp.edu.projeto.cortaai.notificationservice.model.DeviceToken;
import ifsp.edu.projeto.cortaai.notificationservice.model.PushPlatform;
import ifsp.edu.projeto.cortaai.notificationservice.repository.DeviceTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceTokenServiceTest {

    @Mock
    private DeviceTokenRepository deviceTokenRepository;

    private DeviceTokenService deviceTokenService;

    @BeforeEach
    void setUp() {
        deviceTokenService = new DeviceTokenService(deviceTokenRepository);
    }

    @Test
    void shouldCreateNewTokenWhenNotExists() {
        UUID userId = UUID.randomUUID();
        when(deviceTokenRepository.findByToken("abc-token")).thenReturn(Optional.empty());

        deviceTokenService.registerToken(userId, "abc-token", PushPlatform.WEB);

        ArgumentCaptor<DeviceToken> captor = ArgumentCaptor.forClass(DeviceToken.class);
        verify(deviceTokenRepository).save(captor.capture());
        DeviceToken saved = captor.getValue();

        assertEquals(userId, saved.getUserId());
        assertEquals("abc-token", saved.getToken());
        assertEquals(PushPlatform.WEB, saved.getPlatform());
    }

    @Test
    void shouldUpdateExistingTokenAndReactivate() {
        UUID oldUser = UUID.randomUUID();
        UUID newUser = UUID.randomUUID();
        DeviceToken existing = DeviceToken.builder()
                .userId(oldUser)
                .token("existing-token")
                .platform(PushPlatform.WEB)
                .active(false)
                .build();

        when(deviceTokenRepository.findByToken("existing-token")).thenReturn(Optional.of(existing));

        deviceTokenService.registerToken(newUser, "existing-token", PushPlatform.WEB);

        verify(deviceTokenRepository).save(existing);
        assertEquals(newUser, existing.getUserId());
        assertEquals(PushPlatform.WEB, existing.getPlatform());
    }

    @Test
    void shouldDeactivateTokenOnlyForOwnerUser() {
        UUID ownerUser = UUID.randomUUID();
        DeviceToken existing = DeviceToken.builder()
                .userId(ownerUser)
                .token("tok-1")
                .platform(PushPlatform.WEB)
                .active(true)
                .build();

        when(deviceTokenRepository.findByToken("tok-1")).thenReturn(Optional.of(existing));

        deviceTokenService.deactivateToken(ownerUser, "tok-1");

        verify(deviceTokenRepository).save(existing);
    }

    @Test
    void shouldThrowWhenTokenIsBlank() {
        UUID userId = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class,
                () -> deviceTokenService.registerToken(userId, "   ", PushPlatform.WEB));

        verify(deviceTokenRepository, never()).save(any());
        verify(deviceTokenRepository, never()).findByToken(any());
    }

    @Test
    void shouldDeactivateByValueWhenInvalidTokenReported() {
        DeviceToken existing = DeviceToken.builder()
                .userId(UUID.randomUUID())
                .token("invalid-token")
                .platform(PushPlatform.WEB)
                .active(true)
                .build();

        when(deviceTokenRepository.findByToken(eq("invalid-token"))).thenReturn(Optional.of(existing));

        deviceTokenService.deactivateTokenByValue("invalid-token");

        verify(deviceTokenRepository).save(existing);
    }
}
