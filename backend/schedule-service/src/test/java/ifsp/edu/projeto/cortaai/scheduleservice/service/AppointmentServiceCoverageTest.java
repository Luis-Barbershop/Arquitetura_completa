package ifsp.edu.projeto.cortaai.scheduleservice.service;

import ifsp.edu.projeto.cortaai.scheduleservice.config.RabbitConfig;
import ifsp.edu.projeto.cortaai.scheduleservice.dto.*;
import ifsp.edu.projeto.cortaai.scheduleservice.event.AppointmentCreatedEvent;
import ifsp.edu.projeto.cortaai.scheduleservice.exception.ConflictException;
import ifsp.edu.projeto.cortaai.scheduleservice.exception.NotFoundException;
import ifsp.edu.projeto.cortaai.scheduleservice.feign.BarbershopServiceClient;
import ifsp.edu.projeto.cortaai.scheduleservice.feign.UserServiceClient;
import ifsp.edu.projeto.cortaai.scheduleservice.mapper.AppointmentMapper;
import ifsp.edu.projeto.cortaai.scheduleservice.model.Appointment;
import ifsp.edu.projeto.cortaai.scheduleservice.model.BarberBlock;
import ifsp.edu.projeto.cortaai.scheduleservice.model.enums.AppointmentStatus;
import ifsp.edu.projeto.cortaai.scheduleservice.repository.AppointmentRepository;
import ifsp.edu.projeto.cortaai.scheduleservice.repository.BarberBlockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceCoverageTest {

    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private BarberBlockRepository barberBlockRepository;
    @Mock
    private AppointmentMapper appointmentMapper;
    @Mock
    private UserServiceClient userServiceClient;
    @Mock
    private BarbershopServiceClient barbershopServiceClient;
    @Mock
    private RabbitTemplate rabbitTemplate;
    @Mock
    private CacheManager cacheManager;
    @Mock
    private Cache cache;

    private AppointmentService service;

    @BeforeEach
    void setUp() {
        service = new AppointmentService(
                appointmentRepository,
                barberBlockRepository,
                appointmentMapper,
                userServiceClient,
                barbershopServiceClient,
                rabbitTemplate,
                cacheManager
        );
        lenient().when(cacheManager.getCache("appointmentAvailability")).thenReturn(cache);
        lenient().when(appointmentRepository.saveAndFlush(any(Appointment.class))).thenAnswer(invocation -> {
            Appointment appointment = invocation.getArgument(0);
            appointment.setId(UUID.randomUUID());
            return appointment;
        });
        lenient().when(appointmentMapper.toDTO(any(Appointment.class))).thenAnswer(invocation -> {
            Appointment appointment = invocation.getArgument(0);
            AppointmentDTO dto = new AppointmentDTO();
            dto.setId(appointment.getId());
            dto.setStatus(appointment.getStatus() != null ? appointment.getStatus().name() : null);
            dto.setStartTime(appointment.getStartTime());
            return dto;
        });
    }

    @Test
    void shouldCreateOnlineAppointmentWithPaymentPendingAndEvent() {
        UUID barberId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        CreateAppointmentDTO request = createRequest(barberId, shopId, "PIX");
        mockCreateDependencies(request, customer(UUID.randomUUID()), barber(barberId, shopId), shop(shopId));
        when(appointmentRepository.findConflictsForUpdate(barberId, request.getStartTime(), request.getStartTime().plusMinutes(45)))
                .thenReturn(List.of());
        when(barberBlockRepository.existsByBarberIdAndStartTimeLessThanAndEndTimeGreaterThan(barberId, request.getStartTime().plusMinutes(45), request.getStartTime()))
                .thenReturn(false);

        AppointmentDTO result = service.createAppointment("cliente@example.com", request);

        ArgumentCaptor<Appointment> appointmentCaptor = ArgumentCaptor.forClass(Appointment.class);
        verify(appointmentRepository).saveAndFlush(appointmentCaptor.capture());
        assertThat(result.getStatus()).isEqualTo("PAYMENT_PENDING");
        assertThat(appointmentCaptor.getValue().getStatus()).isEqualTo(AppointmentStatus.PAYMENT_PENDING);
        assertThat(appointmentCaptor.getValue().getActivities()).hasSize(2);
        verify(rabbitTemplate).convertAndSend(eq(RabbitConfig.EXCHANGE), eq("appointment.created"), any(AppointmentCreatedEvent.class));
    }

    @Test
    void shouldCreateLocalAppointmentAsScheduled() {
        UUID barberId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        CreateAppointmentDTO request = createRequest(barberId, shopId, "LOCAL");
        mockCreateDependencies(request, customer(UUID.randomUUID()), barber(barberId, shopId), shop(shopId));
        when(appointmentRepository.findConflictsForUpdate(any(), any(), any())).thenReturn(List.of());

        AppointmentDTO result = service.createAppointment("cliente@example.com", request);

        assertThat(result.getStatus()).isEqualTo("SCHEDULED");
    }

    @Test
    void shouldRejectInvalidCreateAppointmentDependencies() {
        UUID barberId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        CreateAppointmentDTO request = createRequest(barberId, shopId, "LOCAL");

        when(userServiceClient.getUserByEmail("cliente@example.com")).thenReturn(null);
        assertThatThrownBy(() -> service.createAppointment("cliente@example.com", request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Cliente");

        when(userServiceClient.getUserByEmail("cliente@example.com")).thenReturn(customer(UUID.randomUUID()));
        when(userServiceClient.getUserById(barberId)).thenReturn(null);
        assertThatThrownBy(() -> service.createAppointment("cliente@example.com", request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Barbeiro");

        when(userServiceClient.getUserById(barberId)).thenReturn(barber(barberId, shopId));
        when(barbershopServiceClient.getBarbershopById(shopId)).thenReturn(null);
        assertThatThrownBy(() -> service.createAppointment("cliente@example.com", request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Barbearia");

        when(barbershopServiceClient.getBarbershopById(shopId)).thenReturn(shop(shopId));
        when(barbershopServiceClient.getActivitiesByIds(shopId, request.getActivityIds())).thenReturn(List.of());
        assertThatThrownBy(() -> service.createAppointment("cliente@example.com", request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Nenhuma atividade");
    }

    @Test
    void shouldRejectConflictsLocksAndBlocksOnCreateAppointment() {
        UUID barberId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        CreateAppointmentDTO request = createRequest(barberId, shopId, "LOCAL");
        mockCreateDependencies(request, customer(UUID.randomUUID()), barber(barberId, shopId), shop(shopId));

        when(appointmentRepository.findConflictsForUpdate(any(), any(), any()))
                .thenReturn(List.of(Appointment.builder().id(UUID.randomUUID()).build()));
        assertThatThrownBy(() -> service.createAppointment("cliente@example.com", request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("já possui");

        doThrow(new PessimisticLockingFailureException("lock"))
                .when(appointmentRepository).findConflictsForUpdate(any(), any(), any());
        assertThatThrownBy(() -> service.createAppointment("cliente@example.com", request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("reservar");

        doReturn(List.of()).when(appointmentRepository).findConflictsForUpdate(any(), any(), any());
        when(barberBlockRepository.existsByBarberIdAndStartTimeLessThanAndEndTimeGreaterThan(any(), any(), any())).thenReturn(true);
        assertThatThrownBy(() -> service.createAppointment("cliente@example.com", request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("indisponível");
    }

    @Test
    void shouldCreateManualBookingAndRejectInvalidManualBookingRules() {
        UUID barberId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();
        BarberManualBookingDTO request = manualRequest(shopId, activityId);
        UserInfoDTO barber = barber(barberId, shopId);
        when(userServiceClient.getUserByFirebaseUid("firebase-uid")).thenReturn(barber);
        when(userServiceClient.getBarberAssignedActivities(barberId)).thenReturn(Set.of(activityId));
        when(barbershopServiceClient.getBarbershopById(shopId)).thenReturn(shop(shopId));
        when(barbershopServiceClient.getActivitiesByIds(shopId, List.of(activityId))).thenReturn(List.of(activity(activityId, "Corte", 30, "40.00")));
        when(appointmentRepository.findConflictsForUpdate(barberId, request.getStartTime(), request.getStartTime().plusMinutes(30))).thenReturn(List.of());

        AppointmentDTO result = service.createManualBooking("firebase-uid", request);

        assertThat(result.getStatus()).isEqualTo("WALK_IN");

        when(userServiceClient.getUserByFirebaseUid("missing")).thenReturn(null);
        assertThatThrownBy(() -> service.createManualBooking("missing", request))
                .isInstanceOf(NotFoundException.class);

        UserInfoDTO otherShopBarber = barber(UUID.randomUUID(), UUID.randomUUID());
        when(userServiceClient.getUserByFirebaseUid("other-shop")).thenReturn(otherShopBarber);
        assertThatThrownBy(() -> service.createManualBooking("other-shop", request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("barbearia");

        UserInfoDTO noServicesBarber = barber(UUID.randomUUID(), shopId);
        when(userServiceClient.getUserByFirebaseUid("no-services")).thenReturn(noServicesBarber);
        when(userServiceClient.getBarberAssignedActivities(noServicesBarber.getId())).thenReturn(Set.of());
        assertThatThrownBy(() -> service.createManualBooking("no-services", request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("não possui serviços");
    }

    @Test
    void shouldCalculateAvailabilityWithConflictsBlocksPastAndFallbacks() {
        UUID barberId = UUID.randomUUID();
        LocalDate date = LocalDate.now().plusDays(2);
        ReflectionTestUtils.setField(service, "appTimezone", "Invalid/Zone");
        LocalDateTime busyStart = date.atTime(9, 15);
        when(userServiceClient.getBarberWorkSchedule(barberId)).thenReturn(List.of(
                new DayScheduleDTO(date.getDayOfWeek(), List.of(new WorkBlockDTO(LocalTime.of(9, 0), LocalTime.of(10, 0))))
        ));
        when(appointmentRepository.findByBarberIdAndStartTimeBetween(eq(barberId), any(), any())).thenReturn(List.of(
                appointment(barberId, busyStart, busyStart.plusMinutes(15), AppointmentStatus.CONFIRMED),
                appointment(barberId, date.atTime(12, 0), date.atTime(12, 30), AppointmentStatus.CANCELLED)
        ));
        when(barberBlockRepository.findByBarberIdAndStartTimeBetween(eq(barberId), any(), any())).thenReturn(List.of(
                BarberBlock.builder().barberId(barberId).startTime(date.atTime(9, 45)).endTime(date.atTime(10, 0)).build()
        ));

        List<TimeSlotDTO> slots = service.getAvailability(barberId, date, 15);

        assertThat(slots).hasSize(4);
        assertThat(slots).extracting(TimeSlotDTO::isAvailable).containsExactly(true, false, true, false);

        doThrow(new RuntimeException("user off")).when(userServiceClient).getBarberWorkSchedule(barberId);
        assertThat(service.getAvailability(barberId, date, 15)).isEmpty();

        doReturn(List.of()).when(userServiceClient).getBarberWorkSchedule(barberId);
        assertThat(service.getAvailability(barberId, date, 15)).isEmpty();
    }

    @Test
    void shouldQueryAppointmentsForDifferentViews() {
        UUID userId = UUID.randomUUID();
        UUID barberId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        Appointment active = appointment(barberId, LocalDateTime.of(2026, 5, 22, 9, 0), LocalDateTime.of(2026, 5, 22, 9, 30), AppointmentStatus.CONFIRMED);
        Appointment cancelled = appointment(barberId, LocalDateTime.of(2026, 5, 22, 10, 0), LocalDateTime.of(2026, 5, 22, 10, 30), AppointmentStatus.CANCELLED);
        active.setCustomerId(userId);
        active.setBarbershopId(shopId);
        cancelled.setBarbershopId(shopId);
        AppointmentDTO mapped = new AppointmentDTO();
        when(appointmentMapper.toDTO(active)).thenReturn(mapped);

        UserInfoDTO barber = user("barber@example.com", userId, "BARBER");
        when(userServiceClient.getUserByEmail("barber@example.com")).thenReturn(barber);
        when(appointmentRepository.findByBarberIdOrderByStartTimeDesc(userId)).thenReturn(List.of(active));
        assertThat(service.getMyAppointments("barber@example.com")).containsExactly(mapped);

        UserInfoDTO customer = user("customer@example.com", userId, "CUSTOMER");
        when(userServiceClient.getUserByEmail("customer@example.com")).thenReturn(customer);
        when(appointmentRepository.findByCustomerIdOrderByStartTimeDesc(userId)).thenReturn(List.of(active));
        assertThat(service.getMyAppointments("customer@example.com")).containsExactly(mapped);

        LocalDate date = LocalDate.of(2026, 5, 22);
        when(appointmentRepository.findByBarberIdAndStartTimeBetween(eq(barberId), any(), any())).thenReturn(List.of(active, cancelled));
        assertThat(service.getBarberSchedule(barberId, date)).containsExactly(mapped);

        when(appointmentRepository.findByBarbershopIdAndStartTimeBetween(eq(shopId), any(), any())).thenReturn(List.of(cancelled, active));
        assertThat(service.getBarbershopAppointmentsByPeriod(shopId, date.atStartOfDay(), date.atTime(23, 0))).containsExactly(mapped);

        when(appointmentRepository.findFutureActiveByBarberId(eq(barberId), any())).thenReturn(List.of(active));
        assertThat(service.getFutureAppointmentsByBarber(barberId)).containsExactly(mapped);

        assertThatThrownBy(() -> service.getBarbershopAppointmentsByPeriod(shopId, date.atTime(23, 0), date.atStartOfDay()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldConfirmAndUpdatePaymentStatusVariants() {
        UUID appointmentId = UUID.randomUUID();
        UUID barberId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        Appointment appointment = appointment(barberId, LocalDateTime.now(), LocalDateTime.now().plusMinutes(30), AppointmentStatus.SCHEDULED);
        appointment.setId(appointmentId);
        appointment.setBarbershopId(shopId);
        UserInfoDTO caller = user("owner@example.com", UUID.randomUUID(), "OWNER");
        BarbershopInfoDTO shop = shop(shopId);
        shop.setOwnerId(caller.getId());
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(userServiceClient.getUserByEmail("owner@example.com")).thenReturn(caller);
        when(barbershopServiceClient.getBarbershopById(shopId)).thenReturn(shop);

        service.confirmAppointment("owner@example.com", appointmentId);

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);

        service.updatePaymentStatus(appointmentId, "PAID");
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);

        service.updatePaymentStatus(appointmentId, "CONCLUDED");
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);

        assertThatThrownBy(() -> service.updatePaymentStatus(appointmentId, "not-a-status"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Status inválido");
    }

    private void mockCreateDependencies(CreateAppointmentDTO request, UserInfoDTO customer, UserInfoDTO barber, BarbershopInfoDTO shop) {
        when(userServiceClient.getUserByEmail("cliente@example.com")).thenReturn(customer);
        when(userServiceClient.getUserById(request.getBarberId())).thenReturn(barber);
        when(barbershopServiceClient.getBarbershopById(request.getBarbershopId())).thenReturn(shop);
        when(barbershopServiceClient.getActivitiesByIds(request.getBarbershopId(), request.getActivityIds())).thenReturn(List.of(
                activity(request.getActivityIds().get(0), "Corte", 30, "40.00"),
                activity(request.getActivityIds().get(1), "Barba", 15, "20.00")
        ));
    }

    private static CreateAppointmentDTO createRequest(UUID barberId, UUID shopId, String paymentMethod) {
        CreateAppointmentDTO request = new CreateAppointmentDTO();
        request.setBarberId(barberId);
        request.setBarbershopId(shopId);
        request.setActivityIds(List.of(UUID.randomUUID(), UUID.randomUUID()));
        request.setStartTime(LocalDateTime.of(2026, 5, 22, 9, 0));
        request.setPaymentMethod(paymentMethod);
        return request;
    }

    private static BarberManualBookingDTO manualRequest(UUID shopId, UUID activityId) {
        BarberManualBookingDTO request = new BarberManualBookingDTO();
        request.setBarbershopId(shopId);
        request.setActivityIds(List.of(activityId));
        request.setClientName("Cliente presencial");
        request.setStartTime(LocalDateTime.of(2026, 5, 22, 11, 0));
        return request;
    }

    private static ActivityInfoDTO activity(UUID id, String name, int duration, String price) {
        ActivityInfoDTO activity = new ActivityInfoDTO();
        activity.setId(id);
        activity.setActivityName(name);
        activity.setDurationMinutes(duration);
        activity.setPrice(new BigDecimal(price));
        return activity;
    }

    private static UserInfoDTO customer(UUID id) {
        return user("cliente@example.com", id, "CUSTOMER");
    }

    private static UserInfoDTO barber(UUID id, UUID shopId) {
        UserInfoDTO user = user("barber@example.com", id, "BARBER");
        user.setBarbershopId(shopId);
        user.setName("Barbeiro");
        return user;
    }

    private static UserInfoDTO user(String email, UUID id, String type) {
        UserInfoDTO user = new UserInfoDTO();
        user.setId(id);
        user.setEmail(email);
        user.setName(type.equals("CUSTOMER") ? "Cliente" : "Barbeiro");
        user.setUserType(type);
        return user;
    }

    private static BarbershopInfoDTO shop(UUID id) {
        BarbershopInfoDTO shop = new BarbershopInfoDTO();
        shop.setId(id);
        shop.setName("Barbearia");
        return shop;
    }

    private static Appointment appointment(UUID barberId, LocalDateTime start, LocalDateTime end, AppointmentStatus status) {
        return Appointment.builder()
                .id(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .barberId(barberId)
                .barbershopId(UUID.randomUUID())
                .customerName("Cliente")
                .barberName("Barbeiro")
                .barbershopName("Barbearia")
                .startTime(start)
                .endTime(end)
                .totalPrice(BigDecimal.TEN)
                .status(status)
                .build();
    }
}
