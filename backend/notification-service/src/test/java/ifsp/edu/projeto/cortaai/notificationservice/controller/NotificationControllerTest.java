package ifsp.edu.projeto.cortaai.notificationservice.controller;

import ifsp.edu.projeto.cortaai.notificationservice.dto.NotificationDTO;
import ifsp.edu.projeto.cortaai.notificationservice.feign.UserInfoDTO;
import ifsp.edu.projeto.cortaai.notificationservice.feign.UserServiceClient;
import ifsp.edu.projeto.cortaai.notificationservice.model.NotificationChannel;
import ifsp.edu.projeto.cortaai.notificationservice.model.NotificationType;
import ifsp.edu.projeto.cortaai.notificationservice.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;
    @Mock
    private UserServiceClient userServiceClient;

    private NotificationController controller;

    @BeforeEach
    void setUp() {
        controller = new NotificationController(notificationService, userServiceClient);
    }

    @Test
    void shouldListNotificationsForResolvedUser() {
        UUID userId = UUID.randomUUID();
        NotificationDTO dto = notificationDto(UUID.randomUUID(), userId);
        when(userServiceClient.getUserByFirebaseUid("firebase-uid")).thenReturn(user(userId));
        when(notificationService.getMyNotifications(userId)).thenReturn(List.of(dto));

        ResponseEntity<List<NotificationDTO>> response = controller.getMyNotifications("firebase-uid");

        assertThat(response.getBody()).containsExactly(dto);
    }

    @Test
    void shouldMarkNotificationAsReadForResolvedUser() {
        UUID userId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        NotificationDTO dto = notificationDto(notificationId, userId);
        when(userServiceClient.getUserByFirebaseUid("firebase-uid")).thenReturn(user(userId));
        when(notificationService.markAsRead(notificationId, userId)).thenReturn(dto);

        ResponseEntity<NotificationDTO> response = controller.markAsRead(notificationId, "firebase-uid");

        assertThat(response.getBody()).isEqualTo(dto);
    }

    @Test
    void shouldReturnUnreadCountForResolvedUser() {
        UUID userId = UUID.randomUUID();
        when(userServiceClient.getUserByFirebaseUid("firebase-uid")).thenReturn(user(userId));
        when(notificationService.getUnreadCount(userId)).thenReturn(3L);

        ResponseEntity<Map<String, Long>> response = controller.getUnreadCount("firebase-uid");

        assertThat(response.getBody()).containsEntry("unreadCount", 3L);
    }

    @Test
    void shouldFailWhenFirebaseUidCannotBeResolved() {
        when(userServiceClient.getUserByFirebaseUid("missing-uid")).thenReturn(null);

        assertThatThrownBy(() -> controller.getUnreadCount("missing-uid"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Usuário não encontrado");
    }

    private static UserInfoDTO user(UUID userId) {
        UserInfoDTO user = new UserInfoDTO();
        user.setId(userId);
        user.setFirebaseUid("firebase-uid");
        return user;
    }

    private static NotificationDTO notificationDto(UUID id, UUID userId) {
        return new NotificationDTO(
                id,
                userId,
                NotificationType.APPOINTMENT_CREATED,
                "Titulo",
                "Mensagem",
                NotificationChannel.IN_APP,
                false,
                LocalDateTime.of(2026, 5, 22, 9, 0)
        );
    }
}
