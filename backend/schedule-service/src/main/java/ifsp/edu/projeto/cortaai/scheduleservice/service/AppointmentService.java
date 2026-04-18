package ifsp.edu.projeto.cortaai.scheduleservice.service;

import ifsp.edu.projeto.cortaai.scheduleservice.config.RabbitConfig;
import ifsp.edu.projeto.cortaai.scheduleservice.dto.*;
import ifsp.edu.projeto.cortaai.scheduleservice.event.*;
import ifsp.edu.projeto.cortaai.scheduleservice.exception.ConflictException;
import ifsp.edu.projeto.cortaai.scheduleservice.exception.ForbiddenException;
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
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
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

    private static final UUID WALK_IN_CUSTOMER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private final AppointmentRepository appointmentRepository;
    private final BarberBlockRepository barberBlockRepository;
    private final AppointmentMapper appointmentMapper;
    private final UserServiceClient userServiceClient;
    private final BarbershopServiceClient barbershopServiceClient;
    private final RabbitTemplate rabbitTemplate;

    // ========== CRIAÇÃO ==========

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public AppointmentDTO createAppointment(String callerEmail, CreateAppointmentDTO dto) {

        // 1. Resolver customer pelo e-mail do caller (header confiável do gateway).
        //    Ignoramos dto.customerId para evitar que o frontend passe Firebase UID ou UUID errado.
        UserInfoDTO customer = userServiceClient.getUserByEmail(callerEmail);
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
        ensureNoConflictForSlot(dto.getBarberId(), dto.getStartTime(), endTime);

        // 7. Verificar BarberBlock
        boolean isBlocked = barberBlockRepository
                .existsByBarberIdAndStartTimeLessThanAndEndTimeGreaterThan(
                        dto.getBarberId(), endTime, dto.getStartTime());
        if (isBlocked) {
            throw new ConflictException("O barbeiro está indisponível neste período (bloqueio de agenda).");
        }

        // 8. Criar Appointment com snapshots desnormalizados
        Appointment appointment = Appointment.builder()
                .customerId(customer.getId())   // UUID interno, resolvido pelo callerEmail
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

        Appointment saved = appointmentRepository.saveAndFlush(appointment);

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

    // ========== AGENDAMENTO MANUAL (WALK-IN) ==========

    /**
     * Cria um agendamento manual pelo barbeiro, sem exigir customer cadastrado
     * e sem gerar evento de pagamento. O status é WALK_IN.
     *
     * @param barberFirebaseUid UID do Firebase do barbeiro (extraído do token pelo gateway)
     * @param dto               dados do agendamento manual
     * @return AppointmentDTO   agendamento criado
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public AppointmentDTO createManualBooking(String barberFirebaseUid, BarberManualBookingDTO dto) {

        // 1. Resolver barbeiro pelo Firebase UID
        UserInfoDTO barber = userServiceClient.getUserByFirebaseUid(barberFirebaseUid);
        if (barber == null || !"BARBER".equalsIgnoreCase(barber.getUserType())) {
            throw new NotFoundException("Barbeiro não encontrado ou tipo inválido.");
        }

        if (barber.getBarbershopId() == null || !barber.getBarbershopId().equals(dto.getBarbershopId())) {
            throw new ConflictException("O barbeiro só pode registrar walk-in na barbearia em que está vinculado.");
        }

        Set<UUID> assignedActivityIds = userServiceClient.getBarberAssignedActivities(barber.getId());
        if (assignedActivityIds == null || assignedActivityIds.isEmpty()) {
            throw new ConflictException("O barbeiro não possui serviços atribuídos para registrar walk-in.");
        }

        List<UUID> invalidActivities = dto.getActivityIds().stream()
                .filter(activityId -> !assignedActivityIds.contains(activityId))
                .toList();
        if (!invalidActivities.isEmpty()) {
            throw new ConflictException("Foram informados serviços que não estão atribuídos ao barbeiro.");
        }

        // 2. Validar barbershop + buscar activities via Feign
        BarbershopInfoDTO shop = barbershopServiceClient.getBarbershopById(dto.getBarbershopId());
        if (shop == null) {
            throw new NotFoundException("Barbearia não encontrada.");
        }

        List<ActivityInfoDTO> activities = barbershopServiceClient
                .getActivitiesByIds(dto.getBarbershopId(), dto.getActivityIds());

        if (activities == null || activities.isEmpty()) {
            throw new NotFoundException("Nenhuma atividade encontrada para os IDs informados.");
        }

        // 3. Calcular duração total e preço
        int totalDuration = activities.stream()
                .mapToInt(ActivityInfoDTO::getDurationMinutes)
                .sum();

        BigDecimal totalPrice = activities.stream()
                .map(ActivityInfoDTO::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 4. Calcular endTime
        LocalDateTime endTime = dto.getStartTime().plusMinutes(totalDuration);

        // 5. Verificar conflito de horário
        ensureNoConflictForSlot(barber.getId(), dto.getStartTime(), endTime);

        // 6. Verificar BarberBlock
        boolean isBlocked = barberBlockRepository
                .existsByBarberIdAndStartTimeLessThanAndEndTimeGreaterThan(
                        barber.getId(), endTime, dto.getStartTime());
        if (isBlocked) {
            throw new ConflictException("O barbeiro está indisponível neste período (bloqueio de agenda).");
        }

        // 7. Snapshot do cliente walk-in usa UUID sintético fixo.
        //    Não representa um cliente cadastrado e evita distorcer métricas por vincular ao barbeiro.
        Appointment appointment = Appointment.builder()
                .customerId(WALK_IN_CUSTOMER_ID)
                .barberId(barber.getId())
                .barbershopId(dto.getBarbershopId())
                .customerName(dto.getClientName())  // nome real do cliente walk-in
                .barberName(barber.getName())
                .barbershopName(shop.getName())
                .startTime(dto.getStartTime())
                .endTime(endTime)
                .totalPrice(totalPrice)
                .status(AppointmentStatus.WALK_IN)
                .build();

        // 8. Criar AppointmentActivities (snapshots)
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

        Appointment saved = appointmentRepository.saveAndFlush(appointment);
        log.info("Agendamento manual (WALK_IN) criado: id={}, barbeiro={}, cliente='{}', telefone='{}'",
                saved.getId(), barber.getId(), dto.getClientName(), dto.getClientPhone());

        // 9. Sem evento de pagamento — WALK_IN não passa por gateway de pagamento
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

    /**
     * Retorna os slots disponíveis para um barbeiro em um dia.
     *
     * <p>Lógica:
     * <ol>
     *   <li>Busca workStart / workEnd do barbeiro via Feign (com cache Redis).</li>
     *   <li>Gera slots de {@code SLOT_STEP_MINUTES} (15 min) dentro do expediente.</li>
     *   <li>Para cada slot de início, verifica se os próximos {@code durationMinutes} estão livres
     *       (sem agendamento nem bloqueio ativos).</li>
     *   <li>Retorna apenas os slots em que o bloco completo cabe.</li>
     * </ol>
     *
     * @param barberId        UUID do barbeiro
     * @param date            data solicitada
     * @param durationMinutes duração total dos serviços selecionados (mín. 15)
     */
    @Transactional(readOnly = true)
    public List<TimeSlotDTO> getAvailability(UUID barberId, LocalDate date, int durationMinutes) {
        final int SLOT_STEP_MINUTES = 15;
        final int effectiveDuration = Math.max(durationMinutes, SLOT_STEP_MINUTES);

        // 1. Buscar blocos de horário do barbeiro para o dia da semana solicitado
        java.time.DayOfWeek dayOfWeek = date.getDayOfWeek();
        List<DayScheduleDTO> weekSchedule;
        try {
            weekSchedule = userServiceClient.getBarberWorkSchedule(barberId);
        } catch (Exception ex) {
            log.warn("Falha ao buscar agenda semanal do barbeiro {}, usando horário legado", barberId, ex);
            weekSchedule = null;
        }

        // Determina os blocos de trabalho para o dia solicitado
        List<WorkBlockDTO> dayBlocks;
        if (weekSchedule != null && !weekSchedule.isEmpty()) {
            dayBlocks = weekSchedule.stream()
                    .filter(d -> d.getDayOfWeek() == dayOfWeek)
                    .flatMap(d -> d.getBlocks().stream())
                    .sorted(Comparator.comparing(WorkBlockDTO::getStartTime))
                    .collect(Collectors.toList());
        } else {
            // Fallback para horário legado (workStartTime/workEndTime)
            UserInfoDTO barber = getBarberWorkHoursCached(barberId);
            LocalTime workStart = barber.getWorkStartTime();
            LocalTime workEnd   = barber.getWorkEndTime();
            if (workStart == null || workEnd == null) {
                return List.of();
            }
            dayBlocks = List.of(new WorkBlockDTO(workStart, workEnd));
        }

        if (dayBlocks.isEmpty()) {
            return List.of(); // Barbeiro não trabalha neste dia
        }

        // 2. Buscar agendamentos ativos do dia
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd   = date.atTime(23, 59, 59);

        List<Appointment> dayAppointments = appointmentRepository
                .findByBarberIdAndStartTimeBetween(barberId, dayStart, dayEnd)
                .stream()
                .filter(a -> a.getStatus() != AppointmentStatus.CANCELLED
                          && a.getStatus() != AppointmentStatus.NO_SHOW)
                .collect(Collectors.toList());

        // 3. Buscar bloqueios do dia
        List<BarberBlock> dayBlocksSchedule = barberBlockRepository
                .findByBarberIdAndStartTimeBetween(barberId, dayStart, dayEnd);

        // 4. Gerar slots dentro de cada bloco de trabalho
        List<TimeSlotDTO> slots = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (WorkBlockDTO workBlock : dayBlocks) {
            LocalDateTime slotStart = date.atTime(workBlock.getStartTime());
            LocalDateTime blockEnd  = date.atTime(workBlock.getEndTime());

            while (true) {
                LocalDateTime slotEnd = slotStart.plusMinutes(effectiveDuration);
                if (slotEnd.compareTo(blockEnd) > 0) break;

                boolean occupied = isRangeOccupied(slotStart, slotEnd, dayAppointments, dayBlocksSchedule);
                boolean inPast   = slotStart.isBefore(now);

                slots.add(new TimeSlotDTO(slotStart, slotEnd, !occupied && !inPast));

                slotStart = slotStart.plusMinutes(SLOT_STEP_MINUTES);
            }
        }

        return slots;
    }

    @Cacheable(value = "barberWorkHours", key = "#barberId")
    public UserInfoDTO getBarberWorkHoursCached(UUID barberId) {
        return userServiceClient.getUserById(barberId);
    }

    private boolean isRangeOccupied(LocalDateTime slotStart, LocalDateTime slotEnd,
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
    public List<AppointmentDTO> getBarbershopSchedule(UUID shopId, LocalDate date, String callerEmail, String correlationId) {
        UserInfoDTO caller = userServiceClient.getUserByEmail(callerEmail);
        if (caller == null || caller.getId() == null) {
            throw new NotFoundException("Usuário autenticado não encontrado.");
        }

        String safeCorrelationId = (correlationId == null || correlationId.isBlank()) ? "N/A" : correlationId;

        // Verificação de ownership via barbershopId do próprio perfil do barbeiro
        // (evita dependência síncrona com barbershop-service, que pode estar lento no startup)
        boolean isOwner = shopId.equals(caller.getBarbershopId())
                && "BARBER".equalsIgnoreCase(caller.getUserType());

        if (!isOwner) {
            log.warn(
                    "SECURITY_EVENT=MASTER_SCHEDULE_ACCESS_DENIED userId={} userType={} targetShopId={} date={} correlationId={}",
                    caller.getId(), caller.getUserType(), shopId, date, safeCorrelationId
            );
            throw new ForbiddenException("Apenas barbeiros vinculados a esta barbearia podem visualizar a agenda da equipe.");
        }

        log.info(
                "SECURITY_EVENT=MASTER_SCHEDULE_ACCESS_GRANTED userId={} userType={} targetShopId={} date={} correlationId={}",
                caller.getId(), caller.getUserType(), shopId, date, safeCorrelationId
        );

        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.atTime(23, 59, 59);
        return appointmentRepository.findByBarbershopIdAndStartTimeBetween(shopId, dayStart, dayEnd)
                .stream()
                .map(appointmentMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AppointmentDTO> getBarbershopAppointmentsByPeriod(UUID shopId, LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null || from.isAfter(to)) {
            throw new IllegalArgumentException("Período inválido para consulta da agenda da barbearia.");
        }

        return appointmentRepository.findByBarbershopIdAndStartTimeBetween(shopId, from, to)
                .stream()
                .filter(this::includeInOperationalReports)
                .sorted(Comparator.comparing(Appointment::getStartTime))
                .map(appointmentMapper::toDTO)
                .collect(Collectors.toList());
    }

    private void ensureNoConflictForSlot(UUID barberId, LocalDateTime startTime, LocalDateTime endTime) {
        try {
            List<Appointment> conflicts = appointmentRepository.findConflictsForUpdate(barberId, startTime, endTime);
            if (!conflicts.isEmpty()) {
                throw new ConflictException("O barbeiro já possui um agendamento neste horário.");
            }
        } catch (PessimisticLockingFailureException ex) {
            throw new ConflictException("Não foi possível reservar o horário neste momento. Tente novamente.");
        }
    }

    private boolean includeInOperationalReports(Appointment appointment) {
        return appointment.getStatus() != AppointmentStatus.CANCELLED
                && appointment.getStatus() != AppointmentStatus.NO_SHOW;
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

