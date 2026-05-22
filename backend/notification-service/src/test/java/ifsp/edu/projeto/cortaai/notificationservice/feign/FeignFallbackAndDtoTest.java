package ifsp.edu.projeto.cortaai.notificationservice.feign;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FeignFallbackAndDtoTest {

    @Test
    void shouldReturnNullWhenScheduleFallbackIsUsed() {
        assertThat(new ScheduleServiceClientFallback().getAppointmentById(UUID.randomUUID())).isNull();
    }

    @Test
    void shouldExposeUserInfoProperties() {
        UUID userId = UUID.randomUUID();
        UserInfoDTO user = new UserInfoDTO();

        user.setId(userId);
        user.setFirebaseUid("firebase-uid");

        assertThat(user.getId()).isEqualTo(userId);
        assertThat(user.getFirebaseUid()).isEqualTo("firebase-uid");
    }
}
