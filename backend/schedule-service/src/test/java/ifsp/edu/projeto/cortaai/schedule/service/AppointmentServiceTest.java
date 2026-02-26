package ifsp.edu.projeto.cortaai.schedule.service;

import ifsp.edu.projeto.cortaai.schedule.dto.AppointmentDTO;
import ifsp.edu.projeto.cortaai.schedule.dto.CreateAppointmentDTO;
import ifsp.edu.projeto.cortaai.schedule.exception.ConflictException;
import ifsp.edu.projeto.cortaai.schedule.exception.NotFoundException;
import ifsp.edu.projeto.cortaai.schedule.mapper.AppointmentMapper;
import ifsp.edu.projeto.cortaai.schedule.model.Appointment;
import ifsp.edu.projeto.cortaai.schedule.model.enums.AppointmentStatus;
import ifsp.edu.projeto.cortaai.schedule.repository.AppointmentRepository;
import ifsp.edu.projeto.cortaai.schedule.service.impl.AppointmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private AppointmentMapper appointmentMapper;

    @Mock
    private AvailabilityService availabilityService;

    @InjectMocks
    private AppointmentServiceImpl appointmentService;

    private Appointment appointment;
    private AppointmentDTO appointmentDTO;
    private UUID customerId;
    private UUID barberId;
    private UUID barbershopId;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        barberId = UUID.randomUUID();
        barbershopId = UUID.randomUUID();

        appointment = new Appointment();
        appointment.setId(1L);
        appointment.setBarbershopId(barbershopId);
        appointment.setBarberId(barberId);
        appointment.setCustomerId(customerId);
        appointment.setStartTime(OffsetDateTime.now().plusDays(1));
        appointment.setEndTime(OffsetDateTime.now().plusDays(1).plusMinutes(30));
        appointment.setStatus(AppointmentStatus.SCHEDULED);

        appointmentDTO = new AppointmentDTO();
        appointmentDTO.setId(1L);
        appointmentDTO.setBarbershopId(barbershopId);
        appointmentDTO.setBarberId(barberId);
        appointmentDTO.setCustomerId(customerId);
        appointmentDTO.setStatus(AppointmentStatus.SCHEDULED);
    }

    @Test
    @DisplayName("Deve listar todos os agendamentos")
    void shouldFindAll() {
        // given
        when(appointmentRepository.findAll()).thenReturn(List.of(appointment));
        when(appointmentMapper.toDTOList(anyList())).thenReturn(List.of(appointmentDTO));

        // when
        List<AppointmentDTO> result = appointmentService.findAll();

        // then
        assertThat(result).hasSize(1);
        verify(appointmentRepository).findAll();
    }

    @Test
    @DisplayName("Deve encontrar agendamento por ID")
    void shouldFindById() {
        // given
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        when(appointmentMapper.toDTO(appointment)).thenReturn(appointmentDTO);

        // when
        AppointmentDTO result = appointmentService.findById(1L);

        // then
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Deve lançar exceção quando agendamento não encontrado")
    void shouldThrowNotFoundExceptionWhenAppointmentNotFound() {
        // given
        when(appointmentRepository.findById(1L)).thenReturn(Optional.empty());

        // when/then
        assertThatThrownBy(() -> appointmentService.findById(1L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Deve criar agendamento com sucesso")
    void shouldCreateAppointment() {
        // given
        CreateAppointmentDTO createDTO = new CreateAppointmentDTO();
        createDTO.setBarbershopId(barbershopId);
        createDTO.setBarberId(barberId);
        createDTO.setStartTime(OffsetDateTime.now().plusDays(1));
        createDTO.setActivityIds(List.of(UUID.randomUUID()));

        when(availabilityService.isTimeSlotAvailable(any(), any(), any())).thenReturn(true);
        when(appointmentMapper.toEntity(createDTO)).thenReturn(appointment);
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(appointment);
        when(appointmentMapper.toDTO(appointment)).thenReturn(appointmentDTO);

        // when
        AppointmentDTO result = appointmentService.create(createDTO, customerId);

        // then
        assertThat(result).isNotNull();
        verify(appointmentRepository).save(any(Appointment.class));
    }

    @Test
    @DisplayName("Deve lançar exceção quando horário não disponível")
    void shouldThrowConflictWhenTimeNotAvailable() {
        // given
        CreateAppointmentDTO createDTO = new CreateAppointmentDTO();
        createDTO.setBarbershopId(barbershopId);
        createDTO.setBarberId(barberId);
        createDTO.setStartTime(OffsetDateTime.now().plusDays(1));
        createDTO.setActivityIds(List.of(UUID.randomUUID()));

        when(availabilityService.isTimeSlotAvailable(any(), any(), any())).thenReturn(false);

        // when/then
        assertThatThrownBy(() -> appointmentService.create(createDTO, customerId))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Horário não disponível");
    }

    @Test
    @DisplayName("Deve cancelar agendamento com sucesso")
    void shouldCancelAppointment() {
        // given
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));

        // when
        appointmentService.cancel(1L, customerId);

        // then
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        verify(appointmentRepository).save(appointment);
    }
}
