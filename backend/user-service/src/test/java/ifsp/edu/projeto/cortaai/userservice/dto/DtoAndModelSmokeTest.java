package ifsp.edu.projeto.cortaai.userservice.dto;

import ifsp.edu.projeto.cortaai.userservice.model.Barber;
import ifsp.edu.projeto.cortaai.userservice.model.Customer;
import ifsp.edu.projeto.cortaai.userservice.model.enums.AppointmentStatus;
import ifsp.edu.projeto.cortaai.userservice.model.enums.BarberSkills;
import ifsp.edu.projeto.cortaai.userservice.model.enums.JoinRequestStatus;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DtoAndModelSmokeTest {

    @Test
    void shouldExerciseRecordDtos() {
        UUID id = UUID.randomUUID();
        AuthResponseDTO auth = new AuthResponseDTO(id, "Ana", "ana@example.com", "11999999999",
                "12345678909", "img", "CUSTOMER", "EMAIL", true, "ROLE_CUSTOMER",
                true, false, null, null, null, null);
        LoginResponseDTO legacyLogin = new LoginResponseDTO("token", "Ana", "ROLE_CUSTOMER", id);
        FirebaseTokenDebugResponseDTO tokenDebug = new FirebaseTokenDebugResponseDTO(
                "uid", "ana@example.com", "Ana", "issuer", "aud", "iat", "exp", Map.of("claim", "value"));
        FirebaseEmailRegisterResponseDTO registerResponse = new FirebaseEmailRegisterResponseDTO(
                "id-token", "refresh", "3600", "uid", auth);

        assertThat(auth.id()).isEqualTo(id);
        assertThat(legacyLogin.userData()).isNull();
        assertThat(tokenDebug.claims()).containsEntry("claim", "value");
        assertThat(registerResponse.profile()).isEqualTo(auth);
        assertThat(new CreateBarberDTO("Bia", "11999999999", "bia@example.com", "12345678909",
                "secret", LocalTime.of(9, 0), LocalTime.of(18, 0)).workEndTime())
                .isEqualTo(LocalTime.of(18, 0));
        assertThat(new FirebaseEmailSignInResponseDTO("id", "refresh", "3600", "uid", "a@b.com", true).registered())
                .isTrue();
        assertThat(new ChangePasswordResponseDTO("id", "refresh").refreshToken()).isEqualTo("refresh");
        assertThat(new EmailExistsResponseDTO(false, null).exists()).isFalse();
        assertThat(new BarbershopInfoDTO(id, "Shop").name()).isEqualTo("Shop");
    }

    @Test
    void shouldExerciseMutableDtos() {
        UUID id = UUID.randomUUID();
        CustomerCreateDTO create = new CustomerCreateDTO();
        create.setName("Ana");
        create.setTell("11999999999");
        create.setEmail("ana@example.com");
        create.setDocumentCPF("12345678909");
        create.setPassword("secret");

        LoginDTO login = new LoginDTO();
        login.setEmail("ana@example.com");
        login.setPassword("secret");

        UpdateBarberDTO updateBarber = new UpdateBarberDTO();
        updateBarber.setName("Bia");
        updateBarber.setTell("11988888888");
        updateBarber.setEmail("bia@example.com");
        updateBarber.setBirthDate(LocalDate.of(1988, 2, 2));
        updateBarber.setWorkStartTime(LocalTime.of(9, 0));
        updateBarber.setWorkEndTime(LocalTime.of(18, 0));
        updateBarber.setActAsBarber(true);

        UploadResultDTO upload = new UploadResultDTO("public-id", "https://cdn/image.png");
        BarberInfoDTO barberInfo = new BarberInfoDTO();
        barberInfo.setId(id);
        barberInfo.setName("Bia");
        barberInfo.setEmail("bia@example.com");
        barberInfo.setTell("11988888888");

        WorkBlockDTO block = new WorkBlockDTO(LocalTime.of(9, 0), LocalTime.of(12, 0));
        DayScheduleDTO day = new DayScheduleDTO(DayOfWeek.MONDAY, List.of(block));
        SaveWeekScheduleDTO week = new SaveWeekScheduleDTO(List.of(day));

        assertThat(create.getEmail()).isEqualTo("ana@example.com");
        assertThat(login.getPassword()).isEqualTo("secret");
        assertThat(updateBarber.getActAsBarber()).isTrue();
        assertThat(upload.getPublicId()).isEqualTo("public-id");
        assertThat(barberInfo.getId()).isEqualTo(id);
        assertThat(week.getSchedule()).containsExactly(day);
    }

    @Test
    void shouldExerciseEntitiesAndEnums() {
        UUID activityId = UUID.randomUUID();
        Barber owner = Barber.builder()
                .id(UUID.randomUUID())
                .name("Bia")
                .email("bia@example.com")
                .isOwner(true)
                .assignedActivityIds(Set.of(activityId))
                .build();
        Customer customer = new Customer();
        customer.setFavoriteBarbershopIds(Set.of(UUID.randomUUID()));

        assertThat(owner.getUsername()).isEqualTo("bia@example.com");
        assertThat(owner.getAuthorities()).extracting(Object::toString)
                .containsExactly("ROLE_BARBER", "ROLE_OWNER");
        assertThat(owner.isAccountNonExpired()).isTrue();
        assertThat(owner.isAccountNonLocked()).isTrue();
        assertThat(owner.isCredentialsNonExpired()).isTrue();
        assertThat(owner.isEnabled()).isTrue();
        assertThat(customer.getFavoriteBarbershopIds()).hasSize(1);
        assertThat(BarberSkills.values()).contains(BarberSkills.BEARD);
        assertThat(AppointmentStatus.valueOf("SCHEDULED")).isEqualTo(AppointmentStatus.SCHEDULED);
        assertThat(JoinRequestStatus.valueOf("PENDING")).isEqualTo(JoinRequestStatus.PENDING);
    }
}
