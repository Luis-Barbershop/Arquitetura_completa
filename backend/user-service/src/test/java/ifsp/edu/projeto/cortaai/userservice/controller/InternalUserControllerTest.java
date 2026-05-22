package ifsp.edu.projeto.cortaai.userservice.controller;

import ifsp.edu.projeto.cortaai.userservice.dto.DayScheduleDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.MpConnectionStatusDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.SaveMpCredentialsDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.UserInfoDTO;
import ifsp.edu.projeto.cortaai.userservice.model.Barber;
import ifsp.edu.projeto.cortaai.userservice.model.Customer;
import ifsp.edu.projeto.cortaai.userservice.repository.BarberRepository;
import ifsp.edu.projeto.cortaai.userservice.repository.CustomerRepository;
import ifsp.edu.projeto.cortaai.userservice.service.BarberWorkScheduleService;
import ifsp.edu.projeto.cortaai.userservice.service.FirebaseAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InternalUserControllerTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private BarberRepository barberRepository;

    @Mock
    private FirebaseAuthService firebaseAuthService;

    @Mock
    private BarberWorkScheduleService workScheduleService;

    private InternalUserController controller;

    @BeforeEach
    void setUp() {
        controller = new InternalUserController(
                customerRepository,
                barberRepository,
                firebaseAuthService,
                workScheduleService
        );
    }

    @Test
    void shouldFindUserByIdPreferringCustomerBeforeBarberLookup() {
        UUID id = UUID.randomUUID();
        Customer customer = customer(id);

        when(customerRepository.findById(id)).thenReturn(Optional.of(customer));

        ResponseEntity<UserInfoDTO> response = controller.getUserById(id);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().userType()).isEqualTo("CUSTOMER");
        assertThat(response.getBody().role()).isEqualTo("ROLE_CUSTOMER");
        verify(barberRepository, never()).findById(id);
    }

    @Test
    void shouldFindBarberByIdAndReturnNotFoundWhenMissing() {
        UUID id = UUID.randomUUID();
        Barber barber = barber(id);

        when(customerRepository.findById(id)).thenReturn(Optional.empty());
        when(barberRepository.findById(id)).thenReturn(Optional.of(barber));

        ResponseEntity<UserInfoDTO> found = controller.getUserById(id);

        assertThat(found.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(found.getBody()).isNotNull();
        assertThat(found.getBody().userType()).isEqualTo("BARBER");
        assertThat(found.getBody().barbershopId()).isEqualTo(barber.getBarbershopId());
        assertThat(found.getBody().workStartTime()).isEqualTo(LocalTime.of(9, 0));

        UUID missingId = UUID.randomUUID();
        when(customerRepository.findById(missingId)).thenReturn(Optional.empty());
        when(barberRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThat(controller.getUserById(missingId).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldResolveByEmailAndFirebaseUidWithBarberPrecedence() {
        Barber barber = barber(UUID.randomUUID());
        Customer customer = customer(UUID.randomUUID());

        when(barberRepository.findByEmail("same@mail.com")).thenReturn(Optional.of(barber));
        ResponseEntity<UserInfoDTO> byEmail = controller.getUserByEmail("same@mail.com");
        assertThat(byEmail.getBody().userType()).isEqualTo("BARBER");
        verify(customerRepository, never()).findByEmail("same@mail.com");

        when(barberRepository.findByFirebaseUid("firebase-uid")).thenReturn(Optional.empty());
        when(customerRepository.findByFirebaseUid("firebase-uid")).thenReturn(Optional.of(customer));
        ResponseEntity<UserInfoDTO> byUid = controller.getUserByFirebaseUid("firebase-uid");
        assertThat(byUid.getBody().userType()).isEqualTo("CUSTOMER");
    }

    @Test
    void shouldReturnBarbersByBarbershopAndAssignedActivities() {
        UUID barbershopId = UUID.randomUUID();
        UUID barberId = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();
        Barber barber = barber(barberId);
        barber.setAssignedActivityIds(Set.of(activityId));

        when(barberRepository.findActiveByBarbershopId(barbershopId)).thenReturn(List.of(barber));
        when(barberRepository.findById(barberId)).thenReturn(Optional.of(barber));

        ResponseEntity<List<UserInfoDTO>> barbers = controller.getBarbersByBarbershop(barbershopId);
        ResponseEntity<Set<UUID>> activities = controller.getBarberAssignedActivities(barberId);

        assertThat(barbers.getBody()).singleElement().extracting(UserInfoDTO::id).isEqualTo(barberId);
        assertThat(activities.getBody()).containsExactly(activityId);

        UUID missingId = UUID.randomUUID();
        when(barberRepository.findById(missingId)).thenReturn(Optional.empty());
        assertThat(controller.getBarberAssignedActivities(missingId).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldUpdateBarbershopIdAndUnlinkWhenBodyIsNullOrBlank() {
        UUID barberId = UUID.randomUUID();
        UUID barbershopId = UUID.randomUUID();
        Barber barber = barber(barberId);

        when(barberRepository.findById(barberId)).thenReturn(Optional.of(barber));

        assertThat(controller.updateUserBarbershopId(barberId, Map.of("barbershopId", barbershopId.toString())).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(barber.getBarbershopId()).isEqualTo(barbershopId);

        assertThat(controller.updateUserBarbershopId(barberId, null).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(barber.getBarbershopId()).isNull();
        verify(barberRepository, times(2)).save(barber);
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingMissingBarberBarbershop() {
        UUID barberId = UUID.randomUUID();
        when(barberRepository.findById(barberId)).thenReturn(Optional.empty());

        assertThat(controller.updateUserBarbershopId(barberId, Map.of("barbershopId", UUID.randomUUID().toString())).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        verify(barberRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldPromoteBarberOwnerAndPersistLocalFlags() {
        Barber barber = barber(UUID.randomUUID());
        barber.setOwner(false);
        barber.setActAsBarber(false);
        barber.setRole("ROLE_BARBER");

        when(barberRepository.findByFirebaseUid("uid-owner")).thenReturn(Optional.of(barber));

        assertThat(controller.makeBarberOwner("uid-owner").getStatusCode()).isEqualTo(HttpStatus.OK);

        verify(firebaseAuthService).setCustomUserClaims("uid-owner", "BARBER", true);
        assertThat(barber.isOwner()).isTrue();
        assertThat(barber.isActAsBarber()).isTrue();
        assertThat(barber.getRole()).isEqualTo("ROLE_OWNER");
        verify(barberRepository).save(barber);
    }

    @Test
    void shouldSaveReadStatusAndDisconnectMercadoPagoCredentials() {
        UUID barberId = UUID.randomUUID();
        Barber barber = barber(barberId);
        SaveMpCredentialsDTO dto = new SaveMpCredentialsDTO("access", "refresh", "123456789", "public");

        when(barberRepository.findById(barberId)).thenReturn(Optional.of(barber));

        assertThat(controller.saveMpCredentials(barberId, dto).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.getMpCredentials(barberId).getBody()).isEqualTo(dto);

        MpConnectionStatusDTO status = controller.getMpConnectionStatus(barberId).getBody();
        assertThat(status.linked()).isTrue();
        assertThat(status.mpUserIdMasked()).isEqualTo("****6789");
        assertThat(status.hasPublicKey()).isTrue();

        assertThat(controller.disconnectMpCredentials(barberId).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(barber.getMpAccessToken()).isNull();
        assertThat(barber.getMpRefreshToken()).isNull();
        assertThat(barber.getMpUserId()).isNull();
        assertThat(barber.getMpPublicKey()).isNull();
    }

    @Test
    void shouldThrowWhenMercadoPagoBarberIsMissing() {
        UUID barberId = UUID.randomUUID();
        when(barberRepository.findById(barberId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.getMpConnectionStatus(barberId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Barbeiro não encontrado");
    }

    @Test
    void shouldFindBarberByCleanCpfAndRejectBlankCpf() {
        Barber barber = barber(UUID.randomUUID());
        when(barberRepository.findByDocumentCPF("12345678900")).thenReturn(Optional.of(barber));

        ResponseEntity<UserInfoDTO> found = controller.getBarberByCpf(Map.of("cpf", "123.456.789-00"));

        assertThat(found.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(found.getBody().id()).isEqualTo(barber.getId());
        assertThat(controller.getBarberByCpf(Map.of("cpf", " ")).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(controller.getBarberByCpf(null).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldDelegateWorkScheduleLookup() {
        UUID barberId = UUID.randomUUID();
        DayScheduleDTO day = new DayScheduleDTO(DayOfWeek.MONDAY, List.of());
        when(workScheduleService.getScheduleByBarberId(barberId)).thenReturn(List.of(day));

        ResponseEntity<List<DayScheduleDTO>> response = controller.getBarberWorkSchedule(barberId);

        assertThat(response.getBody()).containsExactly(day);
    }

    @Test
    void shouldPersistMercadoPagoFieldsReceivedFromDto() {
        UUID barberId = UUID.randomUUID();
        Barber barber = barber(barberId);
        when(barberRepository.findById(barberId)).thenReturn(Optional.of(barber));

        controller.saveMpCredentials(barberId, new SaveMpCredentialsDTO("access-2", "refresh-2", "mp-42", "pk-42"));

        ArgumentCaptor<Barber> captor = ArgumentCaptor.forClass(Barber.class);
        verify(barberRepository).save(captor.capture());
        assertThat(captor.getValue().getMpAccessToken()).isEqualTo("access-2");
        assertThat(captor.getValue().getMpRefreshToken()).isEqualTo("refresh-2");
        assertThat(captor.getValue().getMpUserId()).isEqualTo("mp-42");
        assertThat(captor.getValue().getMpPublicKey()).isEqualTo("pk-42");
    }

    private Customer customer(UUID id) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setName("Cliente");
        customer.setEmail("customer@mail.com");
        customer.setFirebaseUid("customer-uid");
        customer.setRole("ROLE_CUSTOMER");
        customer.setImageUrl("https://img/customer.png");
        return customer;
    }

    private Barber barber(UUID id) {
        Barber barber = new Barber();
        barber.setId(id);
        barber.setName("Barbeiro");
        barber.setEmail("barber@mail.com");
        barber.setFirebaseUid("barber-uid");
        barber.setRole("ROLE_BARBER");
        barber.setBarbershopId(UUID.randomUUID());
        barber.setWorkStartTime(LocalTime.of(9, 0));
        barber.setWorkEndTime(LocalTime.of(18, 0));
        barber.setImageUrl("https://img/barber.png");
        return barber;
    }
}
