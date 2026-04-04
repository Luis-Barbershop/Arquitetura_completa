package ifsp.edu.projeto.cortaai.scheduleservice.service;

import ifsp.edu.projeto.cortaai.scheduleservice.config.RabbitConfig;
import ifsp.edu.projeto.cortaai.scheduleservice.dto.*;
import ifsp.edu.projeto.cortaai.scheduleservice.event.*;
import ifsp.edu.projeto.cortaai.scheduleservice.exception.ConflictException;
import ifsp.edu.projeto.cortaai.scheduleservice.exception.NotFoundException;
import ifsp.edu.projeto.cortaai.scheduleservice.feign.BarbershopServiceClient;
import ifsp.edu.projeto.cortaai.scheduleservice.feign.UserServiceClient;
import ifsp.edu.projeto.cortaai.scheduleservice.mapper.AppointmentMapper;
import ifsp.edu.projeto.cortaai.scheduleservice.model.Appointment;
import ifsp.edu.projeto.cortaai.scheduleservice.model.AppointmentActivity;
import ifsp.edu.projeto.cortaai.scheduleservice.model.BarberBlock;
import ifsp.edu.projeto.cortaai.scheduleservice.model.enums.AppointmentStatus;
import ifsp.edu.projeto.cortaai.scheduleservice.repository.AppointmentRepository;
import ifsp.edu.projeto.cortaai.scheduleservice.repository.BarberBlockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final BarberBlockRepository barberBlockRepository;
    private final AppointmentMapper appointmentMapper;
    private final UserServiceClient userServiceClient;
    private final BarbershopServiceClient barbershopServiceClient;
    private final RabbitTemplate rabbitTemplate;

    // ========== CRIAÇÃO ==========

    public AppointmentDTO createAppointment(String callerEmail, CreateAppointmentDTO dto) {

        // 1. Validar customer via Feign
        UserInfoDTO customer = userServiceClient.getUserById(dto.getCustomerId());
        if (customer == null || !"CUSTOMER".equalsIgnoreCase(customer.getUserType())) {
            throw new NotFoundException("Cliente não encontrado ou tipo inválido.");
        }

        // 2. Validar barber via Feign
        UserInfoDTO barber = userServiceClient.getUserById(dto.getBarberId());
        if (barber == null || !"BARBER".equalsIgnoreCase(barber.getUserType())) {
            throw new NotFoundException("Barbeiro não encontrado ou tipo inválido.");
        }

        // 3. Validar barbershop + buscar activities via Feign
        BarbershopInfoDTO shop = barbershopServiceClient.getBarbershopById(dto.getBarbershopId());
        if (shop == null) {
            throw new NotFoundException("Barbearia não encontrada.");
        }

        List<ActivityInfoDTO> activities = barbershopServiceClient
                .getActivitiesByIds(dto.getBarbershopId(), dto.getActivityIds());

        if (activities == null || activities.isEmpty()) {
            throw new NotFoundException("Nenhuma atividade encontrada para os IDs informados.");
        }

        // 4. Calcular duração total e preço
        int totalDuration = activities.stream()
                .mapToInt(ActivityInfoDTO::getDurationMinutes)
                .sum();

        BigDecimal totalPrice = activities.stream()
                .map(ActivityInfoDTO::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 5. Calcular endTime
        LocalDateTime endTime = dto.getStartTime().plusMinutes(totalDuration);

        // 6. Verificar conflito de horário
        boolean hasConflict = appointmentRepository.hasConflict(
                dto.getBarberId(), dto.getStartTime(), endTime);
        if (hasConflict) {
            throw new ConflictException("O barbeiro já possui um agendamento neste horário.");
        }

        // 7. Verificar BarberBlock
        boolean isBlocked = barberBlockRepository
                .existsByBarberIdAndStartTimeLessThanAndEndTimeGreaterThan(
                        dto.getBarberId(), endTime, dto.getStartTime());
        if (isBlocked) {
            throw new ConflictException("O barbeiro está indisponível neste período (bloqueio de agenda).");
        }

        // 8. Criar Appointment com snapshots desnormalizados
        Appointment appointment = Appointment.builder()
                .customerId(dto.getCustomerId())
                .barberId(dto.getBarberId())
                .barbershopId(dto.getBarbershopId())
                .customerName(customer.getName())
                .barberName(barber.getName())
                .barbershopName(shop.getName())
                .startTime(dto.getStartTime())
                .endTime(endTime)
                .totalPrice(totalPrice)
                .status(AppointmentStatus.SCHEDULED)
                .build();

        // 9. Criar AppointmentActivities (snapshots)
        Set<AppointmentActivity> appointmentActivities = activities.stream()
                .map(act -> AppointmentActivity.builder()
                        .activityId(act.getId())
                        .activityName(act.getActivityName())
                        .price(act.getPrice())
                        .durationMinutes(act.getDurationMinutes())
                        .appointment(appointment)
                        .build())
                .collect(Collectors.toSet());

        appointment.setActivities(appointmentActivities);

        Appointment saved = appointmentRepository.save(appointment);

        // 10. Publicar evento no RabbitMQ
        AppointmentCreatedEvent event = new AppointmentCreatedEvent(
                saved.getId(), saved.getCustomerId(), saved.getBarberId(),
                saved.getBarbershopId(), saved.getCustomerName(), customer.getEmail(),
                saved.getBarberName(), barber.getEmail(),
                saved.getBarbershopName(), saved.getStartTime(), saved.getTotalPrice()
        );
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, "appointment.created", event);
        log.info("Evento AppointmentCreatedEvent publicado para appointment {}", saved.getId());

        // 11. Retornar
        return appointmentMapper.toDTO(saved);
    }

    // ========== CANCELAMENTO ==========

    public void cancelAppointment(String callerEmail, UUID appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("Agendamento não encontrado."));

        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new ConflictException("Agendamento já está cancelado.");
        }

        // Verificar que caller é customer, barber ou owner da barbearia
        UserInfoDTO caller = userServiceClient.getUserByEmail(callerEmail);
        boolean isCustomer = caller.getId().equals(appointment.getCustomerId());
        boolean isBarber = caller.getId().equals(appointment.getBarberId());

        if (!isCustomer && !isBarber) {
            // Verificar se é owner da barbearia
            BarbershopInfoDTO shop = barbershopServiceClient.getBarbershopById(appointment.getBarbershopId());
            if (shop == null || !caller.getId().equals(shop.getOwnerId())) {
                throw new NotFoundException("Você não tem permissão para cancelar este agendamento.");
            }
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(appointment);

        // Buscar emails para o evento
        String customerEmail = null;
        String barberEmail = null;
        try {
            UserInfoDTO customerInfo = userServiceClient.getUserById(appointment.getCustomerId());
            if (customerInfo != null) customerEmail = customerInfo.getEmail();
        } catch (Exception e) {
            log.warn("Não foi possível buscar email do customer: {}", e.getMessage());
        }
        try {
            UserInfoDTO barberInfo = userServiceClient.getUserById(appointment.getBarberId());
            if (barberInfo != null) barberEmail = barberInfo.getEmail();
        } catch (Exception e) {
            log.warn("Não foi possível buscar email do barber: {}", e.getMessage());
        }

        // Publicar evento
        String cancelledBy = callerEmail;
        AppointmentCancelledEvent event = new AppointmentCancelledEvent(
                appointment.getId(), appointment.getCustomerId(),
                appointment.getBarberId(), cancelledBy,
                appointment.getCustomerName(), customerEmail,
                appointment.getBarberName(), barberEmail,
                appointment.getBarbershopName(), appointment.getStartTime()
        );
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, "appointment.cancelled", event);
        log.info("Evento AppointmentCancelledEvent publicado para appointment {}", appointment.getId());
    }

    // ========== CONCLUSÃO ==========

    public void concludeAppointment(String callerEmail, UUID appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("Agendamento não encontrado."));

        // Verificar que caller é o barber
        UserInfoDTO caller = userServiceClient.getUserByEmail(callerEmail);
        if (!caller.getId().equals(appointment.getBarberId())) {
            throw new NotFoundException("Apenas o barbeiro do agendamento pode concluí-lo.");
        }

        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointmentRepository.save(appointment);

        // Buscar email do customer para o evento
        String customerEmail = null;
        try {
            UserInfoDTO customerInfo = userServiceClient.getUserById(appointment.getCustomerId());
            if (customerInfo != null) customerEmail = customerInfo.getEmail();
        } catch (Exception e) {
            log.warn("Não foi possível buscar email do customer: {}", e.getMessage());
        }

        // Publicar evento
        AppointmentConcludedEvent event = new AppointmentConcludedEvent(
                appointment.getId(), appointment.getCustomerId(),
                appointment.getBarberId(), appointment.getBarbershopId(),
                appointment.getCustomerName(), customerEmail,
                appointment.getBarberName(), appointment.getBarbershopName(),
                appointment.getStartTime()
        );
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, "appointment.concluded", event);
        log.info("Evento AppointmentConcludedEvent publicado para appointment {}", appointment.getId());
    }

    // ========== CONFIRMAÇÃO ==========

    public void confirmAppointment(String callerEmail, UUID appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("Agendamento não encontrado."));

        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new ConflictException("Apenas agendamentos com status SCHEDULED podem ser confirmados.");
        }

        // Verificar que caller é barber ou owner
        UserInfoDTO caller = userServiceClient.getUserByEmail(callerEmail);
        boolean isBarber = caller.getId().equals(appointment.getBarberId());

        if (!isBarber) {
            BarbershopInfoDTO shop = barbershopServiceClient.getBarbershopById(appointment.getBarbershopId());
            if (shop == null || !caller.getId().equals(shop.getOwnerId())) {
                throw new NotFoundException("Você não tem permissão para confirmar este agendamento.");
            }
        }

        appointment.setStatus(AppointmentStatus.CONFIRMED);
        appointmentRepository.save(appointment);
    }

    // ========== DISPONIBILIDADE ==========

    @Transactional(readOnly = true)
    public List<TimeSlotDTO> getAvailability(UUID barberId, LocalDate date) {

        // 1. Buscar horários de trabalho do barbeiro (cache Redis 5min)
        UserInfoDTO barber = getBarberWorkHoursCached(barberId);

        LocalTime workStart = barber.getWorkStartTime();
        LocalTime workEnd = barber.getWorkEndTime();

        if (workStart == null || workEnd == null) {
            return List.of(); // Barbeiro sem horário de trabalho configurado
        }

        // 2. Buscar agendamentos do dia
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.atTime(23, 59, 59);

        List<Appointment> dayAppointments = appointmentRepository
                .findByBarberIdAndStartTimeBetween(barberId, dayStart, dayEnd)
                .stream()
                .filter(a -> a.getStatus() != AppointmentStatus.CANCELLED
                          && a.getStatus() != AppointmentStatus.NO_SHOW)
                .collect(Collectors.toList());

        // 3. Buscar bloqueios do dia
        List<BarberBlock> dayBlocks = barberBlockRepository
                .findByBarberIdAndStartTimeBetween(barberId, dayStart, dayEnd);

        // 4. Gerar slots de 30 min
        List<TimeSlotDTO> slots = new ArrayList<>();
        LocalDateTime slotStart = date.atTime(workStart);
        LocalDateTime workEndDateTime = date.atTime(workEnd);

        while (slotStart.plusMinutes(30).compareTo(workEndDateTime) <= 0) {
            LocalDateTime slotEnd = slotStart.plusMinutes(30);

            boolean occupied = isSlotOccupied(slotStart, slotEnd, dayAppointments, dayBlocks);

            slots.add(new TimeSlotDTO(slotStart, slotEnd, !occupied));

            slotStart = slotEnd;
        }

        return slots;
    }

    @Cacheable(value = "barberWorkHours", key = "#barberId")
    public UserInfoDTO getBarberWorkHoursCached(UUID barberId) {
        return userServiceClient.getUserById(barberId);
    }

    private boolean isSlotOccupied(LocalDateTime slotStart, LocalDateTime slotEnd,
                                    List<Appointment> appointments, List<BarberBlock> blocks) {
        // Verifica sobreposição com agendamentos
        for (Appointment a : appointments) {
            if (a.getStartTime().isBefore(slotEnd) && a.getEndTime().isAfter(slotStart)) {
                return true;
            }
        }
        // Verifica sobreposição com bloqueios
        for (BarberBlock b : blocks) {
            if (b.getStartTime().isBefore(slotEnd) && b.getEndTime().isAfter(slotStart)) {
                return true;
            }
        }
        return false;
    }

    // ========== CONSULTAS ==========

    @Transactional(readOnly = true)
    public AppointmentDTO getAppointmentById(UUID id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Agendamento não encontrado."));
        return appointmentMapper.toDTO(appointment);
    }

    @Transactional(readOnly = true)
    public List<AppointmentDTO> getMyAppointments(String email) {
        UserInfoDTO caller = userServiceClient.getUserByEmail(email);
        String userType = caller.getUserType() != null ? caller.getUserType().toUpperCase() : "";

        if ("BARBER".equals(userType)) {
            return appointmentRepository.findByBarberIdOrderByStartTimeDesc(caller.getId())
                    .stream()
                    .map(appointmentMapper::toDTO)
                    .collect(Collectors.toList());
        }

        return appointmentRepository.findByCustomerIdOrderByStartTimeDesc(caller.getId())
                .stream()
                .map(appointmentMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AppointmentDTO> getBarberSchedule(UUID barberId, LocalDate date) {
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.atTime(23, 59, 59);
        return appointmentRepository.findByBarberIdAndStartTimeBetween(barberId, dayStart, dayEnd)
                .stream()
                .map(appointmentMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AppointmentDTO> getBarbershopSchedule(UUID shopId, LocalDate date) {
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.atTime(23, 59, 59);
        return appointmentRepository.findByBarbershopIdAndStartTimeBetween(shopId, dayStart, dayEnd)
                .stream()
                .map(appointmentMapper::toDTO)
                .collect(Collectors.toList());
    }

    // ========== ATUALIZAÇÃO INTERNA (para payment-service) ==========

    public void updatePaymentStatus(UUID appointmentId, String status) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("Agendamento não encontrado."));

        try {
            String normalized = status == null ? "" : status.toUpperCase();
            if ("PAID".equals(normalized)) {
                normalized = "CONFIRMED";
            } else if ("CONCLUDED".equals(normalized)) {
                normalized = "COMPLETED";
            }

            AppointmentStatus newStatus = AppointmentStatus.valueOf(normalized);
            appointment.setStatus(newStatus);
            appointmentRepository.save(appointment);
            log.info("Status do appointment {} atualizado para {}", appointmentId, newStatus);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Status inválido: " + status);
        }
    }
}

