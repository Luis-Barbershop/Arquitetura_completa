package ifsp.edu.projeto.cortaai.service.impl;

import ifsp.edu.projeto.cortaai.dto.*;
import ifsp.edu.projeto.cortaai.events.BeforeDeleteBarber;
import ifsp.edu.projeto.cortaai.exception.NotFoundException;
import ifsp.edu.projeto.cortaai.exception.ReferenceException;
import ifsp.edu.projeto.cortaai.mapper.ActivityMapper;
import ifsp.edu.projeto.cortaai.mapper.BarberMapper;
import ifsp.edu.projeto.cortaai.mapper.BarbershopMapper;
import ifsp.edu.projeto.cortaai.model.*;
import org.springframework.transaction.annotation.Transactional;
import ifsp.edu.projeto.cortaai.model.enums.AppointmentStatus;
import ifsp.edu.projeto.cortaai.model.enums.JoinRequestStatus;
import ifsp.edu.projeto.cortaai.repository.*;
import ifsp.edu.projeto.cortaai.service.BarberService;
import ifsp.edu.projeto.cortaai.service.JwtTokenService;
import ifsp.edu.projeto.cortaai.service.StorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Service
public class BarberServiceImpl implements BarberService {

    private final BarberRepository barberRepository;
    private final BarbershopRepository barbershopRepository;
    private final BarbershopJoinRequestRepository joinRequestRepository;
    private final ActivityRepository activityRepository;
    private final ApplicationEventPublisher publisher;
    private final BarberMapper barberMapper;
    private final PasswordEncoder passwordEncoder;
    private final BarbershopMapper barbershopMapper;
    private final ActivityMapper activityMapper;

    // DEPENDÊNCIAS ADICIONADAS
    private final StorageService storageService;
    private final BarbershopHighlightRepository barbershopHighlightRepository;
    private final AppointmentsRepository appointmentsRepository;
    private final JwtTokenService jwtTokenService;

    @Value("${app.availability.slot-interval-minutes}")
    private int slotIntervalMinutes;

    public BarberServiceImpl(final BarberRepository barberRepository,
                             final BarbershopRepository barbershopRepository,
                             final BarbershopJoinRequestRepository joinRequestRepository,
                             final ActivityRepository activityRepository,
                             final ApplicationEventPublisher publisher,
                             final BarberMapper barberMapper,
                             final PasswordEncoder passwordEncoder,
                             final BarbershopMapper barbershopMapper,
                             final ActivityMapper activityMapper,
                             final StorageService storageService,
                             final BarbershopHighlightRepository barbershopHighlightRepository,
                             final AppointmentsRepository appointmentsRepository,
                             final JwtTokenService jwtTokenService) {
        this.barberRepository = barberRepository;
        this.barbershopRepository = barbershopRepository;
        this.joinRequestRepository = joinRequestRepository;
        this.activityRepository = activityRepository;
        this.publisher = publisher;
        this.barberMapper = barberMapper;
        this.passwordEncoder = passwordEncoder;
        this.barbershopMapper = barbershopMapper;
        this.activityMapper = activityMapper;
        this.storageService = storageService;
        this.barbershopHighlightRepository = barbershopHighlightRepository; // INJETADO
        this.appointmentsRepository = appointmentsRepository;
        this.jwtTokenService = jwtTokenService;
    }

    // --- Gestão de Barbeiros (Global) ---

    // MÉTODO AUXILIAR NOVO
    private Barber findBarberByEmail(String email) {
        return barberRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException("Barbeiro (usuário autenticado) não encontrado"));
    }

    @Override
    public List<BarberDTO> findAll() {
        return barberRepository.findAll(Sort.by("id")).stream()
                .map(barberMapper::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponseDTO login(final LoginDTO loginDTO) { // TIPO DE RETORNO ALTERADO
        final Barber barber = barberRepository.findByEmailIgnoreCase(loginDTO.getEmail())
                .orElseThrow(() -> new NotFoundException("Usuário ou senha inválidos"));

        if (!passwordEncoder.matches(loginDTO.getPassword(), barber.getPassword())) {
            throw new NotFoundException("Usuário ou senha inválidos");
        }

        // 1. Gera o token JWT para o barbeiro
        final String token = jwtTokenService.generateToken(barber);

        // 2. Mapeia o barbeiro para DTO
        final BarberDTO barberDTO = barberMapper.toDTO(barber);

        // 3. Retorna o LoginResponseDTO
        return LoginResponseDTO.builder()
                .token(token)
                .userData(barberDTO)
                .build();
    }

    @Override
    public BarberDTO get(final UUID id) {
        return barberRepository.findById(id)
                .map(barberMapper::toDTO)
                .orElseThrow(NotFoundException::new);
    }

    @Override
    @Transactional
    public UUID create(final CreateBarberDTO createBarberDTO, final MultipartFile file) throws IOException { // Assinatura alterada
        final Barber barber = new Barber();
        barber.setName(createBarberDTO.getName());
        barber.setTell(createBarberDTO.getTell());
        barber.setEmail(createBarberDTO.getEmail());
        barber.setDocumentCPF(createBarberDTO.getDocumentCPF());
        barber.setPassword(passwordEncoder.encode(createBarberDTO.getPassword()));
        barber.setOwner(false);
        barber.setBarbershop(null);
        // Define o horário de trabalho se for fornecido
        if (createBarberDTO.getWorkStartTime() != null && createBarberDTO.getWorkEndTime() != null) {
            barber.setWorkStartTime(createBarberDTO.getWorkStartTime());
            barber.setWorkEndTime(createBarberDTO.getWorkEndTime());
        }

        // Salva o barbeiro primeiro para ter um ID
        final Barber savedBarber = barberRepository.save(barber);

        // Se um arquivo foi enviado, faz o upload e atualiza o barbeiro
        if (file != null && !file.isEmpty()) {
            final UploadResultDTO uploadResult = storageService.uploadFile(file, "barber-profiles");
            savedBarber.setImageUrl(uploadResult.getSecureUrl());
            savedBarber.setImageUrlPublicId(uploadResult.getPublicId());
            barberRepository.save(savedBarber); // Salva novamente com os dados da imagem
        }

        return savedBarber.getId();
    }

    @Override
    @Transactional
    public void update(final String email, final BarberDTO barberDTO) {
        // Busca o barbeiro pelo e-mail do token
        final Barber barber = findBarberByEmail(email);

        barber.setName(barberDTO.getName());
        barber.setTell(barberDTO.getTell());
        barber.setEmail(barberDTO.getEmail());
        barber.setDocumentCPF(barberDTO.getDocumentCPF());

        barberRepository.save(barber);
    }

    @Override
    @Transactional
    public void delete(final String email) {
        // Busca o barbeiro pelo e-mail do token
        final Barber barber = findBarberByEmail(email);

        publisher.publishEvent(new BeforeDeleteBarber(barber.getId()));
        barberRepository.delete(barber);
    }

    // --- Gestão de Barbearias (Fluxo 1) ---
    @Override
    @Transactional
    public BarbershopDTO createBarbershop(final String ownerEmail, final CreateBarbershopDTO createBarbershopDTO, final MultipartFile file) throws IOException { // Assinatura alterada
        final Barber owner = findBarberByEmail(ownerEmail);

        if (owner.getBarbershop() != null) {
            throw new ReferenceException("Barbeiro já está vinculado a uma barbearia.");
        }

        final Barbershop barbershop = barbershopMapper.toEntity(createBarbershopDTO);

        // Se um arquivo de logo foi enviado, faz o upload ANTES de salvar
        if (file != null && !file.isEmpty()) {
            final UploadResultDTO uploadResult = storageService.uploadFile(file, "barbershop-logos");
            barbershop.setLogoUrl(uploadResult.getSecureUrl());
            barbershop.setLogoUrlPublicId(uploadResult.getPublicId());
        }

        final Barbershop savedBarbershop = barbershopRepository.save(barbershop);

        owner.setBarbershop(savedBarbershop);
        owner.setOwner(true);
        barberRepository.save(owner);

        return barbershopMapper.toDTO(savedBarbershop);
    }

    @Override
    @Transactional
    public BarbershopDTO updateBarbershop(final String ownerEmail, final UpdateBarbershopDTO updateBarbershopDTO) { // ALTERADO
        final Barber owner = findBarberByEmail(ownerEmail); // ALTERADO

        if (!owner.isOwner() || owner.getBarbershop() == null) {
            throw new ReferenceException("Apenas o dono de uma barbearia pode editar suas informações.");
        }

        final Barbershop barbershop = owner.getBarbershop();

        if (updateBarbershopDTO.getName() != null) {
            barbershop.setName(updateBarbershopDTO.getName());
        }
        if (updateBarbershopDTO.getAddress() != null) {
            barbershop.setAddress(updateBarbershopDTO.getAddress());
        }

        final Barbershop updatedBarbershop = barbershopRepository.save(barbershop);
        return barbershopMapper.toDTO(updatedBarbershop);
    }

    @Override
    public BarbershopDTO getBarbershop(final UUID barbershopId) {
        final Barbershop barbershop = barbershopRepository.findById(barbershopId)
                .orElseThrow(NotFoundException::new);
        return barbershopMapper.toDTO(barbershop);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BarbershopDTO> listBarbershops() {
        return barbershopRepository.findAll().stream()
                .map(barbershopMapper::toDTO)
                .toList();
    }

    @Override
    @Transactional
    public ActivityDTO updateActivity(final String ownerEmail, final UUID activityId, final UpdateActivityDTO updateActivityDTO) {
        // 1. Valida se o usuário é o dono e obtém a barbearia
        final Barbershop barbershop = getBarbershopFromOwner(ownerEmail);

        // 2. Busca a atividade e valida se ela pertence à barbearia do dono
        final Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new NotFoundException("Serviço (Activity) não encontrado."));
        if (!activity.getBarbershop().getId().equals(barbershop.getId())) {
            throw new ReferenceException("Este serviço não pertence à sua barbearia.");
        }

        // 3. Atualiza os campos se eles foram fornecidos no DTO
        if (updateActivityDTO.getActivityName() != null) {
            activity.setActivityName(updateActivityDTO.getActivityName());
        }
        if (updateActivityDTO.getPrice() != null) {
            activity.setPrice(updateActivityDTO.getPrice());
        }
        if (updateActivityDTO.getDurationMinutes() != null) {
            activity.setDurationMinutes(updateActivityDTO.getDurationMinutes());
        }

        // 4. Salva e retorna o DTO atualizado
        final Activity savedActivity = activityRepository.save(activity);
        return activityMapper.toDTO(savedActivity);
    }

    @Override
    @Transactional
    public void deleteActivity(final String ownerEmail, final UUID activityId) {
        // 1. Valida se o usuário é o dono e obtém a barbearia
        final Barbershop barbershop = getBarbershopFromOwner(ownerEmail);

        // 2. Busca a atividade e valida se ela pertence à barbearia do dono
        final Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new NotFoundException("Serviço (Activity) não encontrado."));
        if (!activity.getBarbershop().getId().equals(barbershop.getId())) {
            throw new ReferenceException("Este serviço não pertence à sua barbearia.");
        }

        // 3. REGRA DE NEGÓCIO CRÍTICA: Verifica se o serviço está em algum agendamento futuro
        if (appointmentsRepository.existsByActivitiesIdAndStatus(activityId, AppointmentStatus.SCHEDULED)) {
            throw new ReferenceException("Este serviço não pode ser excluído pois está vinculado a agendamentos futuros.");
        }

        // 4. Deleta a atividade. O JPA cuidará de remover a associação das tabelas ManyToMany (barber_activities).
        activityRepository.delete(activity);
    }

    @Override
    @Transactional
    public void closeBarbershop(final String ownerEmail, final CloseBarbershopRequestDTO closeBarbershopRequestDTO) {
        // 1. Valida se o usuário é o dono da barbearia
        final Barber owner = findBarberByEmail(ownerEmail);
        if (!owner.isOwner() || owner.getBarbershop() == null) {
            throw new ReferenceException("Apenas o dono de uma barbearia pode realizar esta ação.");
        }

        // 2. Valida a senha do dono para confirmar a ação
        if (!passwordEncoder.matches(closeBarbershopRequestDTO.getPassword(), owner.getPassword())) {
            throw new ReferenceException("Senha incorreta. Ação não autorizada.");
        }

        final Barbershop barbershop = owner.getBarbershop();
        final UUID barbershopId = barbershop.getId();

        // 3. Verifica se existem agendamentos em aberto (status = SCHEDULED)
        if (appointmentsRepository.existsByBarbershopIdAndStatus(barbershopId, AppointmentStatus.SCHEDULED)) {
            throw new ReferenceException("Não é possível fechar a barbearia. Existem agendamentos pendentes.");
        }

        // 4. Desvincula todos os barbeiros (staff) da barbearia
        final List<Barber> staff = barberRepository.findByBarbershopId(barbershopId);
        for (Barber barber : staff) {
            // O dono será desvinculado por último, ao deletar a barbearia
            if (!barber.getId().equals(owner.getId())) {
                barber.setBarbershop(null);
                barber.getActivities().clear(); // Limpa as habilidades do barbeiro
                barberRepository.save(barber);
            }
        }

        // 5. Deleta todas as atividades (serviços) associadas à barbearia
        List<Activity> activities = activityRepository.findByBarbershopId(barbershopId);
        activityRepository.deleteAll(activities);

        // 6. Finalmente, deleta a barbearia.
        owner.setBarbershop(null);
        owner.setOwner(false);
        barberRepository.save(owner);

        barbershopRepository.delete(barbershop);
    }

    // --- Gestão de Serviços (Fluxo 1) ---


    // Permite que um barbeiro (dono) crie um novo serviço para sua barbearia.

    @Override
    @Transactional
    public ActivityDTO createActivities(final String ownerEmail, final CreateActivityDTO createActivityDTO) { // ALTERADO
        final Barber owner = findBarberByEmail(ownerEmail); // ALTERADO

        if (!owner.isOwner() || owner.getBarbershop() == null) {
            throw new ReferenceException("Apenas o dono da barbearia pode criar serviços.");
        }

        // Usa o mapper para converter o DTO para a entidade
        final Activity activity = activityMapper.toEntity(createActivityDTO);
        activity.setBarbershop(owner.getBarbershop());
        final Activity savedActivity = activityRepository.save(activity);
        return activityMapper.toDTO(savedActivity);
    }


    // Lista todos os serviços (atividades) disponíveis em uma barbearia específica.

    @Override
    public List<ActivityDTO> listActivities(final UUID barbershopId) {
        // Valida se a barbearia existe antes de buscar os serviços
        if (!barbershopRepository.existsById(barbershopId)) {
            throw new NotFoundException("Barbearia não encontrada.");
        }
        // Busca as atividades e as mapeia para DTOs
        return activityRepository.findByBarbershopId(barbershopId).stream()
                .map(activityMapper::toDTO)
                .toList();
    }

    /**
     * Lista todos os barbeiros que trabalham em uma barbearia específica.
     */
    @Override
    @Transactional(readOnly = true)
    public List<BarberDTO> listBarbersByBarbershop(final UUID barbershopId) {
        if (!barbershopRepository.existsById(barbershopId)) {
            throw new NotFoundException("Barbearia não encontrada.");
        }
        return barberRepository.findByBarbershopId(barbershopId).stream()
                .map(barberMapper::toDTO)
                .toList();
    }

    // --- Gestão de Vínculos (Fluxos 2 e 3) ---

    @Override
    @Transactional
    public void requestToJoinBarbershop(final String barberEmail, final String cnpj) { // ALTERADO
        final Barber barber = findBarberByEmail(barberEmail); // ALTERADO

        if (barber.getBarbershop() != null) {
            throw new ReferenceException("Barbeiro já está em uma barbearia.");
        }

        final Barbershop barbershop = barbershopRepository.findByCnpj(cnpj)
                .orElseThrow(() -> new NotFoundException("Barbearia não encontrada pelo CNPJ"));

        joinRequestRepository.findByBarberIdAndBarbershopId(barber.getId(), barbershop.getId()) // ALTERADO (usa barber.getId())
                .ifPresent(req -> {
                    throw new ReferenceException("Pedido para entrar nesta barbearia já está pendente.");
                });

        final BarbershopJoinRequest request = new BarbershopJoinRequest();
        request.setBarber(barber);
        request.setBarbershop(barbershop);
        request.setStatus(JoinRequestStatus.PENDING);
        joinRequestRepository.save(request);
    }

    @Override
    @Transactional
    public void approveJoinRequest(final String ownerEmail, final Long requestId) { // ALTERADO
        final Barber owner = findBarberByEmail(ownerEmail); // ALTERADO

        final BarbershopJoinRequest request = joinRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Pedido não encontrado"));

        if (!owner.isOwner() || !owner.getBarbershop().getId().equals(request.getBarbershop().getId())) {
            throw new ReferenceException("Apenas o dono desta barbearia pode aprovar pedidos.");
        }

        if (request.getStatus() != JoinRequestStatus.PENDING) {
            throw new ReferenceException("Este pedido não está mais pendente.");
        }

        final Barber barberToJoin = request.getBarber();
        if (barberToJoin.getBarbershop() != null) {
            throw new ReferenceException("O barbeiro já entrou em outra barbearia.");
        }

        barberToJoin.setBarbershop(request.getBarbershop());
        barberRepository.save(barberToJoin);

        joinRequestRepository.delete(request);
    }

    @Override
    @Transactional
    public void freeBarber(final String barberEmail) { // ALTERADO
        final Barber barber = findBarberByEmail(barberEmail); // ALTERADO

        if (barber.getBarbershop() == null) {
            return;
        }
        if (barber.isOwner()) {
            throw new ReferenceException("O dono não pode sair da própria barbearia (deve excluí-la ou passar a posse).");
        }

        barber.setBarbershop(null);
        barber.getActivities().clear();
        barberRepository.save(barber);
    }

    @Override
    @Transactional
    public void removeBarber(final String ownerEmail, final UUID barberIdToRemove) { // ALTERADO
        final Barber owner = findBarberByEmail(ownerEmail); // ALTERADO

        final Barber barberToRemove = barberRepository.findById(barberIdToRemove)
                .orElseThrow(() -> new NotFoundException("Barbeiro a ser removido não encontrado"));

        if (!owner.isOwner() || owner.getBarbershop() == null) {
            throw new ReferenceException("Apenas o dono da barbearia pode remover barbeiros.");
        }

        if (barberToRemove.getBarbershop() == null || !barberToRemove.getBarbershop().getId().equals(owner.getBarbershop().getId())) {
            throw new ReferenceException("O barbeiro informado não pertence a esta barbearia.");
        }

        if (owner.getId().equals(barberToRemove.getId())) {
            throw new ReferenceException("O dono não pode remover a si mesmo.");
        }

        barberToRemove.setBarbershop(null);
        barberToRemove.getActivities().clear();
        barberRepository.save(barberToRemove);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JoinRequestDTO> getPendingJoinRequests(final String ownerEmail) { // ALTERADO
        final Barber owner = findBarberByEmail(ownerEmail); // ALTERADO

        if (!owner.isOwner() || owner.getBarbershop() == null) {
            throw new ReferenceException("Apenas o dono de uma barbearia pode ver os pedidos pendentes.");
        }

        final UUID barbershopId = owner.getBarbershop().getId();

        final List<BarbershopJoinRequest> requests = joinRequestRepository
                .findByBarbershopIdAndStatus(barbershopId, JoinRequestStatus.PENDING);

        return requests.stream()
                .map(request -> {
                    JoinRequestDTO dto = new JoinRequestDTO();
                    dto.setRequestId(request.getId());

                    BarberInfoDTO barberInfo = new BarberInfoDTO();
                    barberInfo.setId(request.getBarber().getId());
                    barberInfo.setName(request.getBarber().getName());
                    barberInfo.setEmail(request.getBarber().getEmail());
                    barberInfo.setTell(request.getBarber().getTell());

                    dto.setBarber(barberInfo);
                    return dto;
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<JoinRequestHistoryDTO> getJoinRequestHistory(final String barberEmail) { // ALTERADO para usar email do Principal
        final Barber barber = findBarberByEmail(barberEmail); // Usa o helper existente para obter o barbeiro autenticado

        final List<BarbershopJoinRequest> requests = joinRequestRepository.findByBarberId(barber.getId());

        // Mapeia a lista de entidades para a lista de DTOs
        return requests.stream()
                .map(request -> {
                    JoinRequestHistoryDTO dto = new JoinRequestHistoryDTO();
                    dto.setRequestId(request.getId());
                    dto.setStatus(request.getStatus());
                    if (request.getBarbershop() != null) {
                        dto.setBarbershopId(request.getBarbershop().getId());
                        dto.setBarbershopName(request.getBarbershop().getName());
                    }
                    return dto;
                })
                .toList(); // Alterado de collect(Collectors.toList()) para toList()
    }

    @Override
    @Transactional
    public void rejectJoinRequest(final String ownerEmail, final Long requestId) { // ALTERADO para usar email do Principal
        final Barber owner = findBarberByEmail(ownerEmail); // Usa o helper existente para obter o dono autenticado

        final BarbershopJoinRequest request = joinRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Pedido não encontrado"));

        // Valida se quem está recusando é o dono da barbearia para a qual o pedido foi feito
        // Adicionado owner.getBarbershop() == null para evitar NullPointerException antes de chamar .getId()
        if (!owner.isOwner() || owner.getBarbershop() == null || !owner.getBarbershop().getId().equals(request.getBarbershop().getId())) {
            throw new ReferenceException("Apenas o dono desta barbearia pode recusar pedidos.");
        }

        // Valida se o pedido ainda está pendente
        if (request.getStatus() != JoinRequestStatus.PENDING) {
            throw new ReferenceException("Este pedido não está mais pendente.");
        }

        request.setStatus(JoinRequestStatus.REJECTED);
        joinRequestRepository.save(request);
    }


    // --- Gestão de Habilidades (Fluxo 2) ---

    @Override
    @Transactional
    public void assignActivities(final String barberEmail, final BarberActivityAssignDTO barberActivityAssignDTO) {
        final Barber barber = findBarberByEmail(barberEmail);

        if (barber.getBarbershop() == null) {
            throw new ReferenceException("Barbeiro não está em uma barbearia para vincular serviços.");
        }

        final UUID barbershopId = barber.getBarbershop().getId();

        final Set<Activity> servicesToAssign = new HashSet<>(
                activityRepository.findAllById(barberActivityAssignDTO.getActivityIds())
        );

        for (Activity s : servicesToAssign) {
            if (!s.getBarbershop().getId().equals(barbershopId)) {
                throw new ReferenceException("Serviço ID " + s.getId() + " não pertence à barbearia deste barbeiro.");
            }
        }

        barber.setActivities(servicesToAssign);
        barberRepository.save(barber);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityDTO> getMyAssignedActivities(String barberEmail) {
        // Reutiliza o método findBarberByEmail que já trata a exceção Not Found
        final Barber barber = findBarberByEmail(barberEmail);

        // Mapeia as atividades do barbeiro para DTOs
        return barber.getActivities().stream()
                .map(activityMapper::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityDTO> listActivitiesByBarber(final UUID barberId) {
        // Busca o barbeiro pelo ID. O findById já lança uma exceção se não encontrar.
        final Barber barber = barberRepository.findById(barberId)
                .orElseThrow(() -> new NotFoundException("Barbeiro não encontrado."));

        // Retorna a lista de atividades do barbeiro, convertida para DTOs.
        return barber.getActivities().stream()
                .map(activityMapper::toDTO)
                .toList();
    }

    @Override
    @Transactional
    public void setWorkHours(final String email, final BarberWorkHoursDTO workHoursDTO) {
        // Busca o barbeiro pelo e-mail do token
        final Barber barber = findBarberByEmail(email);

        if (workHoursDTO.getWorkStartTime().isAfter(workHoursDTO.getWorkEndTime())) {
            throw new ReferenceException("O horário de início do expediente deve ser anterior ao de término.");
        }

        barber.setWorkStartTime(workHoursDTO.getWorkStartTime());
        barber.setWorkEndTime(workHoursDTO.getWorkEndTime());

        barberRepository.save(barber);
    }

    // --- Métodos de validação ---

    @Override
    public boolean tellExists(final String tell) {
        return barberRepository.existsByTellIgnoreCase(tell);
    }

    @Override
    public boolean emailExists(final String email) {
        return barberRepository.existsByEmailIgnoreCase(email);
    }

    @Override
    public boolean documentCPFExists(final String documentCPF) {
        return barberRepository.existsByDocumentCPFIgnoreCase(documentCPF);
    }

    @Override
    @Transactional
    public String updateBarberProfilePhoto(String email, MultipartFile file) throws IOException {
        final Barber barber = findBarberByEmail(email);

        // 1. Deletar foto antiga
        String oldPublicId = barber.getImageUrlPublicId();
        if (oldPublicId != null) {
            storageService.deleteFile(oldPublicId);
        }

        // 2. Faz o upload
        final UploadResultDTO uploadResult = storageService.uploadFile(file, "barber-profiles");

        // 3. Salva
        barber.setImageUrl(uploadResult.getSecureUrl());
        barber.setImageUrlPublicId(uploadResult.getPublicId());
        barberRepository.save(barber);
        return uploadResult.getSecureUrl();
    }

    @Override
    @Transactional
    public String updateActivityPhoto(String ownerEmail, UUID activityId, MultipartFile file) throws IOException {
        final Barbershop barbershop = getBarbershopFromOwner(ownerEmail);

        final Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new NotFoundException("Serviço (Activity) não encontrado"));
        if (!activity.getBarbershop().getId().equals(barbershop.getId())) {
            throw new ReferenceException("Este serviço não pertence à sua barbearia.");
        }

        // 1. Deletar foto antiga
        String oldPublicId = activity.getImageUrlPublicId();
        if (oldPublicId != null) {
            storageService.deleteFile(oldPublicId);
        }

        // 2. Upload
        final UploadResultDTO uploadResult = storageService.uploadFile(file, "activity-images");

        // 3. Salva
        activity.setImageUrl(uploadResult.getSecureUrl());
        activity.setImageUrlPublicId(uploadResult.getPublicId());
        activityRepository.save(activity);
        return uploadResult.getSecureUrl();
    }

    @Override
    @Transactional
    public String updateBarbershopLogo(String ownerEmail, MultipartFile file) throws IOException {
        final Barbershop barbershop = getBarbershopFromOwner(ownerEmail);

        // 1. Deletar logo antigo
        String oldPublicId = barbershop.getLogoUrlPublicId();
        if (oldPublicId != null) {
            storageService.deleteFile(oldPublicId);
        }

        // 2. Upload
        final UploadResultDTO uploadResult = storageService.uploadFile(file, "barbershop-logos");

        // 3. Salva
        barbershop.setLogoUrl(uploadResult.getSecureUrl());
        barbershop.setLogoUrlPublicId(uploadResult.getPublicId());
        barbershopRepository.save(barbershop);
        return uploadResult.getSecureUrl();
    }

    @Override
    @Transactional
    public String updateBarbershopBanner(String ownerEmail, MultipartFile file) throws IOException {
        final Barbershop barbershop = getBarbershopFromOwner(ownerEmail);

        // 1. Deletar banner antigo
        String oldPublicId = barbershop.getBannerUrlPublicId();
        if (oldPublicId != null) {
            storageService.deleteFile(oldPublicId);
        }

        // 2. Upload
        final UploadResultDTO uploadResult = storageService.uploadFile(file, "barbershop-banners");

        // 3. Salva
        barbershop.setBannerUrl(uploadResult.getSecureUrl());
        barbershop.setBannerUrlPublicId(uploadResult.getPublicId());
        barbershopRepository.save(barbershop);
        return uploadResult.getSecureUrl();
    }

    @Override
    @Transactional
    public String addBarbershopHighlight(String ownerEmail, MultipartFile file) throws IOException {
        final Barbershop barbershop = getBarbershopFromOwner(ownerEmail);

        // 1. Upload (não há foto antiga para deletar aqui)
        final UploadResultDTO uploadResult = storageService.uploadFile(file, "barbershop-highlights");

        BarbershopHighlight highlight = new BarbershopHighlight();
        highlight.setBarbershop(barbershop);
        highlight.setImageUrl(uploadResult.getSecureUrl());
        highlight.setImageUrlPublicId(uploadResult.getPublicId()); // Salva o Public ID

        barbershopHighlightRepository.save(highlight);
        return uploadResult.getSecureUrl();
    }

    @Override
    @Transactional
    public void deleteBarbershopHighlight(String ownerEmail, UUID highlightId) {
        final Barbershop barbershop = getBarbershopFromOwner(ownerEmail);

        final BarbershopHighlight highlight = barbershopHighlightRepository.findById(highlightId)
                .orElseThrow(() -> new NotFoundException("Imagem de destaque não encontrada"));

        if (!highlight.getBarbershop().getId().equals(barbershop.getId())) {
            throw new ReferenceException("Esta imagem não pertence à sua barbearia.");
        }

        // 1. Deletar a imagem do Cloudinary
        String publicId = highlight.getImageUrlPublicId();
        if (publicId != null) {
            try {
                storageService.deleteFile(publicId);
            } catch (IOException e) {
                // Em um app real, logaríamos o erro, mas não impediríamos a exclusão do banco
                // logger.error("Falha ao deletar arquivo do Cloudinary: " + publicId, e);
            }
        }

        // 2. Deletar a entidade do banco
        barbershopHighlightRepository.delete(highlight);
    }


    // --- NOVO MÉTODO DE APOIO ---

    /**
     * Valida se o ID pertence a um dono e retorna a barbearia dele.
     */
    private Barbershop getBarbershopFromOwner(String ownerEmail) { // NOVA ASSINATURA
        final Barber owner = findBarberByEmail(ownerEmail); // Usa o helper existente

        if (!owner.isOwner() || owner.getBarbershop() == null) {
            throw new ReferenceException("Apenas o dono de uma barbearia pode realizar esta ação.");
        }

        return owner.getBarbershop();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocalTime> getAvailableSlots(UUID barberId, LocalDate date, int durationInMinutes) {
        final Barber barber = barberRepository.findById(barberId)
                .orElseThrow(() -> new NotFoundException("Barbeiro não encontrado"));

        if (barber.getWorkStartTime() == null || barber.getWorkEndTime() == null) {
            return new ArrayList<>();
        }

        // --- MUDANÇA 1: Definir o Fuso Horário do Brasil ---
        final ZoneId zoneId = ZoneId.of("America/Sao_Paulo");

        final LocalTime workStart = barber.getWorkStartTime();
        final LocalTime workEnd = barber.getWorkEndTime();

        // --- MUDANÇA 2: Ajustar o intervalo de busca para cobrir o dia COMPLETO no fuso correto ---
        // Se buscarmos apenas "00:00 UTC", perdemos os agendamentos da noite anterior no Brasil
        // Aqui pegamos o inicio e fim do dia no Brasil e convertemos para o Offset do banco para a busca.
        OffsetDateTime startOfDay = date.atStartOfDay(zoneId).toOffsetDateTime();
        OffsetDateTime endOfDay = date.atTime(LocalTime.MAX).atZone(zoneId).toOffsetDateTime();

        List<Appointments> existingAppointments = appointmentsRepository
                .findByBarberIdAndStartTimeBetween(barberId, startOfDay, endOfDay)
                .stream()
                .filter(a -> a.getStatus() != ifsp.edu.projeto.cortaai.model.enums.AppointmentStatus.CANCELLED)
                .sorted(Comparator.comparing(Appointments::getStartTime))
                .toList();

        List<LocalTime[]> freeBlocks = new ArrayList<>();
        LocalTime currentPointer = workStart;

        for (Appointments appointment : existingAppointments) {
            // --- MUDANÇA 3: Converter o horário do agendamento (UTC) para o horário do Brasil ---
            // Isso transforma o "12:00 UTC" de volta para "09:00 Brasil" antes de calcular
            LocalTime appointmentStart = appointment.getStartTime().atZoneSameInstant(zoneId).toLocalTime();
            LocalTime appointmentEnd = appointment.getEndTime().atZoneSameInstant(zoneId).toLocalTime();

            // Lógica para evitar erros caso um agendamento comece antes do expediente (casos raros)
            if (appointmentEnd.isBefore(workStart)) continue;
            if (appointmentStart.isAfter(workEnd)) break;

            // Ajuste visual: Se o agendamento começa antes do ponteiro atual, ignoramos para evitar overlaps negativos
            if (appointmentStart.isBefore(currentPointer)) {
                currentPointer = appointmentEnd.isAfter(currentPointer) ? appointmentEnd : currentPointer;
                continue;
            }

            if (currentPointer.isBefore(appointmentStart)) {
                freeBlocks.add(new LocalTime[]{currentPointer, appointmentStart});
            }
            currentPointer = appointmentEnd;
        }

        if (currentPointer.isBefore(workEnd)) {
            freeBlocks.add(new LocalTime[]{currentPointer, workEnd});
        }

        List<LocalTime> availableSlots = new ArrayList<>();
        long durationLong = (long) durationInMinutes;

        for (LocalTime[] block : freeBlocks) {
            LocalTime blockStart = block[0];
            LocalTime blockEnd = block[1];

            LocalTime potentialSlotStart = blockStart;

            while (potentialSlotStart.isBefore(blockEnd)) {
                LocalTime potentialSlotEnd = potentialSlotStart.plusMinutes(durationLong);

                if (!potentialSlotEnd.isAfter(blockEnd) && !potentialSlotEnd.isAfter(workEnd)) {
                    availableSlots.add(potentialSlotStart);
                }
                potentialSlotStart = potentialSlotStart.plusMinutes(slotIntervalMinutes);
            }
        }

        return availableSlots;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DailyAvailabilityDTO> getMonthlyAvailability(UUID barberId, int year, int month) {
        final Barber barber = barberRepository.findById(barberId)
                .orElseThrow(() -> new NotFoundException("Barbeiro não encontrado."));

        if (barber.getWorkStartTime() == null || barber.getWorkEndTime() == null) {
            return new ArrayList<>(); // Não há expediente, logo, não há disponibilidade.
        }

        List<DailyAvailabilityDTO> monthlyAvailability = new ArrayList<>();
        LocalDate date = LocalDate.of(year, month, 1);
        int daysInMonth = date.lengthOfMonth();

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate currentDate = LocalDate.of(year, month, day);
            // Verificamos a disponibilidade para a menor duração de serviço possível (ex: 15 min)
            // para saber se o dia está "aberto" para agendamentos.
            List<LocalTime> slots = getAvailableSlots(barberId, currentDate, 15);
            monthlyAvailability.add(new DailyAvailabilityDTO(currentDate, !slots.isEmpty()));
        }

        return monthlyAvailability;
    }


}