package ifsp.edu.projeto.cortaai.service.impl;

import ifsp.edu.projeto.cortaai.dto.AppointmentRequestDTO;
import ifsp.edu.projeto.cortaai.dto.AppointmentsDTO;
import ifsp.edu.projeto.cortaai.exception.NotFoundException;
import ifsp.edu.projeto.cortaai.exception.ReferenceException;
import ifsp.edu.projeto.cortaai.mapper.AppointmentMapper;
import ifsp.edu.projeto.cortaai.model.*;
import ifsp.edu.projeto.cortaai.model.enums.AppointmentStatus;
import ifsp.edu.projeto.cortaai.repository.*;
import ifsp.edu.projeto.cortaai.service.AppointmentsService;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AppointmentsServiceImpl implements AppointmentsService {

    private final AppointmentsRepository appointmentsRepository;
    private final BarberRepository barberRepository;
    private final CustomerRepository customerRepository;
    private final BarbershopRepository barbershopRepository;
    private final ActivityRepository activityRepository;
    private final AppointmentMapper appointmentMapper;

    public AppointmentsServiceImpl(final AppointmentsRepository appointmentsRepository,
                                   final BarberRepository barberRepository,
                                   final CustomerRepository customerRepository,
                                   final BarbershopRepository barbershopRepository,
                                   final ActivityRepository activityRepository,
                                   final AppointmentMapper appointmentMapper) {
        this.appointmentsRepository = appointmentsRepository;
        this.barberRepository = barberRepository;
        this.customerRepository = customerRepository;
        this.barbershopRepository = barbershopRepository;
        this.activityRepository = activityRepository;
        this.appointmentMapper = appointmentMapper;
    }

    // MÉTODO AUXILIAR NOVO
    private Customer findCustomerByEmail(String email) {
        return customerRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Cliente (usuário autenticado) não encontrado"));
    }

    private Barber findBarberByEmail(String email) {
        return barberRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException("Barbeiro (usuário autenticado) não encontrado"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentsDTO> findForBarber(final String barberEmail) { // ALTERADO
        // Valida se o barbeiro existe (pelo email)
        final Barber barber = findBarberByEmail(barberEmail);

        return appointmentsRepository.findByBarberId(barber.getId()).stream()
                .map(appointmentMapper::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentsDTO> findForBarbershop(final String ownerEmail) { // ALTERADO
        // Valida se o requisitante é o dono (pelo email)
        final Barber owner = findBarberByEmail(ownerEmail);

        if (!owner.isOwner() || owner.getBarbershop() == null) {
            throw new ReferenceException("Apenas o dono de uma barbearia pode ver a agenda completa.");
        }

        final UUID barbershopId = owner.getBarbershop().getId();

        return appointmentsRepository.findByBarbershopId(barbershopId).stream()
                .map(appointmentMapper::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentsDTO> findForCustomer(final String customerEmail) {
        // 1. Busca o cliente pelo e-mail do token JWT (padrão do projeto)
        // Este método já lança NotFoundException se o cliente não existir.
        final Customer customer = findCustomerByEmail(customerEmail);

        // 2. Busca os agendamentos e mapeia para DTO
        return appointmentsRepository.findByCustomerId(customer.getId()).stream()
                .map(appointmentMapper::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentsDTO> findAll() {
        final List<Appointments> appointmentsList = appointmentsRepository.findAll(Sort.by("id"));
        return appointmentsList.stream()
                .map(appointmentMapper::toDTO)
                .toList();
    }

    @Override
    public AppointmentsDTO get(final Long id) {
        return appointmentsRepository.findById(id)
                .map(appointmentMapper::toDTO)
                .orElseThrow(NotFoundException::new);
    }

    @Override
    @Transactional
    public Long create(final AppointmentRequestDTO appointmentsDTO, final String customerEmail) { // ALTERADO
        // 1. Validar e buscar entidades
        final Barbershop barbershop = barbershopRepository.findById(appointmentsDTO.getBarbershopId())
                .orElseThrow(() -> new NotFoundException("Barbearia não encontrada"));

        // Busca o cliente PELO EMAIL DO TOKEN
        final Customer customer = findCustomerByEmail(customerEmail);

        final Barber barber = barberRepository.findById(appointmentsDTO.getBarberId())
                .orElseThrow(() -> new NotFoundException("Barbeiro não encontrado"));

        // 2. Validar se o barbeiro pertence à barbearia informada
        if (barber.getBarbershop() == null || !barber.getBarbershop().getId().equals(barbershop.getId())) {
            throw new ReferenceException("O barbeiro selecionado não pertence a esta barbearia.");
        }

        // 3. Validar os serviços (atividades) e calcular a duração total do agendamento
        final Set<Activity> activities = new HashSet<>(
                activityRepository.findAllById(appointmentsDTO.getActivityIds())
        );

        if(activities.size() != appointmentsDTO.getActivityIds().size()) {
            throw new NotFoundException("Um ou mais serviços não foram encontrados.");
        }

        int totalDuration = 0;
        for (Activity s : activities) {
            // 3a. Valida se o serviço é oferecido pela barbearia
            if (!s.getBarbershop().getId().equals(barbershop.getId())) {
                throw new ReferenceException("Serviço " + s.getActivityName() + " não pertence a esta barbearia.");
            }
            // 3b. Valida se o barbeiro está habilitado a realizar o serviço
            if (!barber.getActivities().contains(s)) {
                throw new ReferenceException("Barbeiro " + barber.getName() + " não executa o serviço " + s.getActivityName() + ".");
            }
            totalDuration += s.getDurationMinutes();
        }

        // 4. Calcular horário de término e verificar regras de negócio
        final OffsetDateTime startTime = appointmentsDTO.getStartTime();
        final OffsetDateTime endTime = startTime.plusMinutes(totalDuration);

        // 4a. NOVA VALIDAÇÃO: Verifica se o agendamento está dentro do horário de trabalho do barbeiro
        if (barber.getWorkStartTime() != null && barber.getWorkEndTime() != null) {
            LocalTime appointmentStartTime = startTime.toLocalTime();
            LocalTime appointmentEndTime = endTime.toLocalTime();

            if (appointmentStartTime.isBefore(barber.getWorkStartTime()) || appointmentEndTime.isAfter(barber.getWorkEndTime())) {
                throw new ReferenceException("O horário do agendamento está fora do expediente do barbeiro, que é das " +
                        barber.getWorkStartTime() + " às " + barber.getWorkEndTime() + ".");
            }
        }

        // 4b. Verifica se o horário solicitado conflita com outros agendamentos existentes
        List<Appointments> conflicts = appointmentsRepository.findConflictingAppointments(
                barber.getId(), startTime, endTime
        );

        if (!conflicts.isEmpty()) {
            throw new ReferenceException("Horário indisponível. Já existe um agendamento neste bloco.");
        }

        // 4c. NOVA VALIDAÇÃO: Verifica conflitos na própria agenda do cliente
        List<Appointments> customerConflicts = appointmentsRepository.findConflictingAppointmentsForCustomer(
                customer.getId(), startTime, endTime
        );

        if (!customerConflicts.isEmpty()) {
            throw new ReferenceException("Você já possui outro agendamento neste mesmo horário.");
        }

        // 5. Se todas as validações passaram, cria e salva o novo agendamento
        final Appointments appointments = new Appointments();
        appointments.setBarbershop(barbershop);
        appointments.setBarber(barber);
        appointments.setCustomer(customer); // Define o cliente autenticado
        appointments.setStartTime(appointmentsDTO.getStartTime());
        appointments.setEndTime(endTime); // 'endTime' calculado dentro da sua lógica existente
        appointments.setStatus(AppointmentStatus.SCHEDULED);
        appointments.setActivities(activities); // 'activities' validadas dentro da sua lógica

        return appointmentsRepository.save(appointments).getId();
    }

    @Override
    @Transactional
    public void update(final Long id, final AppointmentRequestDTO appointmentsDTO, final String customerEmail) { // ALTERADO
        final Appointments appointments = appointmentsRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        // VALIDAÇÃO DE PERMISSÃO
        final Customer customer = findCustomerByEmail(customerEmail);
        if (!appointments.getCustomer().getId().equals(customer.getId())) {
            throw new ReferenceException("Você só pode alterar seus próprios agendamentos.");
        }
        // Valida se o agendamento pode ser alterado (não pode estar concluído ou cancelado)
        if (appointments.getStatus() == AppointmentStatus.CONCLUDED || appointments.getStatus() == AppointmentStatus.CANCELLED) {
            throw new ReferenceException("Agendamentos concluídos ou cancelados não podem ser alterados.");
        }

        // A lógica de validação e cálculo é idêntica à do método de criação
        final Barbershop barbershop = barbershopRepository.findById(appointmentsDTO.getBarbershopId())
                .orElseThrow(() -> new NotFoundException("Barbearia não encontrada"));
        final Barber barber = barberRepository.findById(appointmentsDTO.getBarberId())
                .orElseThrow(() -> new NotFoundException("Barbeiro não encontrado"));

        if (barber.getBarbershop() == null || !barber.getBarbershop().getId().equals(barbershop.getId())) {
            throw new ReferenceException("O barbeiro selecionado não pertence a esta barbearia.");
        }

        final Set<Activity> activities = new HashSet<>(
                activityRepository.findAllById(appointmentsDTO.getActivityIds())
        );

        int totalDuration = 0;
        for (Activity s : activities) {
            if (!s.getBarbershop().getId().equals(barbershop.getId())) {
                throw new ReferenceException("Serviço " + s.getActivityName() + " não pertence a esta barbearia.");
            }
            if (!barber.getActivities().contains(s)) {
                throw new ReferenceException("Barbeiro " + barber.getName() + " não executa o serviço " + s.getActivityName() + ".");
            }
            totalDuration += s.getDurationMinutes();
        }

        final OffsetDateTime startTime = appointmentsDTO.getStartTime();
        final OffsetDateTime endTime = startTime.plusMinutes(totalDuration);

        // Valida o horário de trabalho do barbeiro
        if (barber.getWorkStartTime() != null && barber.getWorkEndTime() != null) {
            LocalTime appointmentStartTime = startTime.toLocalTime();
            LocalTime appointmentEndTime = endTime.toLocalTime();

            if (appointmentStartTime.isBefore(barber.getWorkStartTime()) || appointmentEndTime.isAfter(barber.getWorkEndTime())) {
                throw new ReferenceException("O horário do agendamento está fora do expediente do barbeiro, que é das " +
                        barber.getWorkStartTime() + " às " + barber.getWorkEndTime() + ".");
            }
        }

        // Na atualização, a verificação de conflito deve ignorar o próprio agendamento que está sendo alterado.
        List<Appointments> conflicts = appointmentsRepository.findConflictingAppointments(
                barber.getId(), startTime, endTime
        ).stream().filter(a -> !a.getId().equals(id)).collect(Collectors.toList());

        if (!conflicts.isEmpty()) {
            throw new ReferenceException("Horário indisponível. Já existe um agendamento neste bloco.");
        }

        // Atualiza a entidade existente com os novos dados
        appointments.setBarbershop(barbershop); // 'barbershop' validado
        appointments.setBarber(barber); // 'barber' validado
        appointments.setCustomer(customer); // Define o cliente autenticado
        appointments.setStartTime(startTime); // 'startTime' do DTO
        appointments.setEndTime(endTime); // 'endTime' calculado
        appointments.setStatus(AppointmentStatus.SCHEDULED);
        appointments.setActivities(activities); // 'activities' validadas

        appointmentsRepository.save(appointments);
    }

    @Override
    @Transactional
    public void conclude(final Long id, final String barberEmail) {
        final Appointments appointments = appointmentsRepository.findById(id)
                .orElseThrow(NotFoundException::new);

        final Barber barber = findBarberByEmail(barberEmail);

        // Apenas o barbeiro associado ao agendamento pode concluí-lo
        if (!appointments.getBarber().getId().equals(barber.getId())) {
            throw new ReferenceException("Você não tem permissão para concluir este agendamento.");
        }

        // O agendamento precisa estar com o status 'SCHEDULED'
        if (appointments.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new ReferenceException("Apenas agendamentos com status 'Agendado' podem ser concluídos.");
        }

        appointments.setStatus(AppointmentStatus.CONCLUDED);
        appointmentsRepository.save(appointments);
    }

    @Override
    public void delete(final Long id, final String userEmail) { // ALTERADO
        // Implementar lógica de permissão similar ao CANCEL se necessário
        final Appointments appointments = appointmentsRepository.findById(id)
                .orElseThrow(NotFoundException::new);

        final Barber barber = barberRepository.findByEmailIgnoreCase(userEmail).orElse(null);
        boolean isOwner = (barber != null && barber.isOwner() && barber.getBarbershop() != null);

        // Apenas o dono da barbearia pode deletar fisicamente
        if (isOwner && appointments.getBarbershop().getId().equals(barber.getBarbershop().getId())) {
            appointmentsRepository.deleteById(id);
        } else {
            throw new ReferenceException("Apenas o dono da barbearia pode excluir agendamentos fisicamente.");
        }
    }

    @Override
    @Transactional
    public void cancel(final Long id, final String userEmail) {
        final Appointments appointments = appointmentsRepository.findById(id)
                .orElseThrow(NotFoundException::new);

        final Customer customer = customerRepository.findByEmail(userEmail).orElse(null);
        final Barber barber = barberRepository.findByEmailIgnoreCase(userEmail).orElse(null);

        boolean isOwner = (barber != null && barber.isOwner() && barber.getBarbershop() != null);
        boolean isCustomer = (customer != null);
        // NOVA CONDIÇÃO: é o barbeiro do agendamento
        boolean isAssignedBarber = (barber != null && appointments.getBarber().getId().equals(barber.getId()));

        boolean canCancel = false;

        // REGRA 1: O cliente que agendou pode cancelar
        if (isCustomer && appointments.getCustomer().getId().equals(customer.getId())) {
            canCancel = true;
        }
        // REGRA 2: O dono da barbearia do agendamento pode cancelar
        else if (isOwner && appointments.getBarbershop().getId().equals(barber.getBarbershop().getId())) {
            canCancel = true;
        }
        // REGRA 3 (NOVA): O próprio barbeiro do agendamento pode cancelar
        else if (isAssignedBarber) {
            canCancel = true;
        }


        if (!canCancel) {
            throw new ReferenceException("Você não tem permissão para cancelar este agendamento.");
        }

        if (appointments.getStatus() == AppointmentStatus.CONCLUDED) {
            throw new ReferenceException("Agendamentos concluídos não podem ser cancelados.");
        }

        appointments.setStatus(AppointmentStatus.CANCELLED);
        appointmentsRepository.save(appointments);
    }
}