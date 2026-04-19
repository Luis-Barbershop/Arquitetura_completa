package ifsp.edu.projeto.cortaai.barbershopservice.service;

import ifsp.edu.projeto.cortaai.barbershopservice.config.RabbitConfig;
import ifsp.edu.projeto.cortaai.barbershopservice.event.JoinRequestCreatedEvent;
import ifsp.edu.projeto.cortaai.barbershopservice.dto.*;
import ifsp.edu.projeto.cortaai.barbershopservice.exception.DomainConflictException;
import ifsp.edu.projeto.cortaai.barbershopservice.exception.ForbiddenException;
import ifsp.edu.projeto.cortaai.barbershopservice.exception.NotFoundException;
import ifsp.edu.projeto.cortaai.barbershopservice.exception.UserServiceUnavailableException;
import ifsp.edu.projeto.cortaai.barbershopservice.feign.UserServiceClient;
import ifsp.edu.projeto.cortaai.barbershopservice.mapper.ActivityMapper;
import ifsp.edu.projeto.cortaai.barbershopservice.mapper.BarbershopMapper;
import ifsp.edu.projeto.cortaai.barbershopservice.model.*;
import ifsp.edu.projeto.cortaai.barbershopservice.model.enums.JoinRequestStatus;
import ifsp.edu.projeto.cortaai.barbershopservice.model.enums.JoinRequestType;
import ifsp.edu.projeto.cortaai.barbershopservice.repository.*;
import ifsp.edu.projeto.cortaai.barbershopservice.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BarbershopService {

    private static final Logger log = LoggerFactory.getLogger(BarbershopService.class);

    private final BarbershopRepository barbershopRepository;
    private final BarbershopReviewRepository barbershopReviewRepository;
    private final ActivityRepository activityRepository;
    private final BarbershopJoinRequestRepository joinRequestRepository;
    private final BarbershopHighlightRepository highlightRepository;
    private final BarbershopMapper barbershopMapper;
    private final ActivityMapper activityMapper;
    private final StorageService storageService;
    private final UserServiceClient userServiceClient;
    private final RabbitTemplate rabbitTemplate;

    // ========== HELPERS ==========

    private UserInfoDTO resolveUser(String email) {
        UserInfoDTO user;
        try {
            user = userServiceClient.getUserByEmail(email);
        } catch (FeignException.NotFound ex) {
            throw new NotFoundException("Usuário não encontrado: " + email);
        } catch (Exception ex) {
            throw new UserServiceUnavailableException("Não foi possível consultar dados do usuário no momento.");
        }
        if (user == null) throw new NotFoundException("Usuário não encontrado: " + email);
        return user;
    }

    private UserInfoDTO resolveUserByUid(String firebaseUid) {
        UserInfoDTO user;
        try {
            user = userServiceClient.getUserByFirebaseUid(firebaseUid);
        } catch (FeignException.NotFound ex) {
            throw new NotFoundException("Usuário não encontrado para o UID: " + firebaseUid);
        } catch (Exception ex) {
            throw new UserServiceUnavailableException("Não foi possível consultar dados do usuário no momento.");
        }
        if (user == null) throw new NotFoundException("Usuário não encontrado para o UID: " + firebaseUid);
        return user;
    }

    private UserInfoDTO resolveUser(UUID userId) {
        UserInfoDTO user;
        try {
            user = userServiceClient.getUserById(userId);
        } catch (FeignException.NotFound ex) {
            throw new NotFoundException("Usuário não encontrado: " + userId);
        } catch (Exception ex) {
            throw new UserServiceUnavailableException("Não foi possível consultar dados do usuário no momento.");
        }
        if (user == null) throw new NotFoundException("Usuário não encontrado: " + userId);
        return user;
    }

    private void updateUserBarbershop(UUID userId, UUID barbershopId) {
        try {
            Map<String, String> body = new HashMap<>();
            body.put("barbershopId", barbershopId != null ? barbershopId.toString() : null);
        log.info("event=user-service-link-update-request userId={} barbershopId={}",
            maskIdentifier(userId),
            maskIdentifier(barbershopId));
            userServiceClient.updateUserBarbershopId(userId, body);
        } catch (FeignException.NotFound ex) {
            log.warn("event=user-service-link-update-not-found userId={} barbershopId={} httpStatus={} error={} message={}",
            maskIdentifier(userId),
            maskIdentifier(barbershopId),
            ex.status(),
            ex.getClass().getSimpleName(),
            sanitizeMessage(ex.getMessage()));
            throw new NotFoundException("Barbeiro não encontrado no serviço de usuários: " + userId);
        } catch (FeignException ex) {
            log.error("event=user-service-link-update-feign-failure userId={} barbershopId={} httpStatus={} error={} message={}",
            maskIdentifier(userId),
            maskIdentifier(barbershopId),
            ex.status(),
            ex.getClass().getSimpleName(),
            sanitizeMessage(ex.getMessage()),
            ex);
            String detail = ex.status() > 0 ? " (HTTP " + ex.status() + ")" : "";
            throw new UserServiceUnavailableException(
                    "Não foi possível atualizar o vínculo da barbearia no serviço de usuários." + detail
            );
        } catch (UserServiceUnavailableException ex) {
            log.error("event=user-service-link-update-fallback-failure userId={} barbershopId={} error={} message={}",
            maskIdentifier(userId),
            maskIdentifier(barbershopId),
            ex.getClass().getSimpleName(),
            sanitizeMessage(ex.getMessage()),
            ex);
            throw ex;
        } catch (Exception ex) {
            log.error("event=user-service-link-update-unexpected-failure userId={} barbershopId={} error={} message={}",
            maskIdentifier(userId),
            maskIdentifier(barbershopId),
            ex.getClass().getSimpleName(),
            sanitizeMessage(ex.getMessage()),
            ex);
            String detail = (ex.getMessage() != null && !ex.getMessage().isBlank())
                    ? " Causa: " + ex.getMessage()
                    : "";
            throw new UserServiceUnavailableException(
                    "Não foi possível atualizar o vínculo da barbearia no serviço de usuários." + detail
            );
        }
    }

    private Barbershop findOwnerShop(UUID ownerId) {
        return barbershopRepository.findByOwnerId(ownerId)
                .orElseThrow(() -> new NotFoundException("Você não possui uma barbearia cadastrada."));
    }

    private void assertOwner(UserInfoDTO user) {
        if (!"BARBER".equals(user.getUserType())) {
            throw new ForbiddenException("Apenas barbeiros podem gerenciar barbearias.");
        }
    }

    // ========== LEITURA PÚBLICA ==========

    @Transactional(readOnly = true)
    public List<BarbershopDTO> listBarbershops() {
        return barbershopRepository.findAll().stream()
                .map(barbershopMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ActivityDTO> listActivities(UUID shopId) {
        return activityRepository.findByBarbershopId(shopId).stream()
                .map(activityMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BarberPublicDTO> listBarbers(UUID shopId) {
        if (!barbershopRepository.existsById(shopId)) {
            throw new NotFoundException("Barbearia não encontrada.");
        }

        try {
            return userServiceClient.getBarbersByBarbershop(shopId).stream()
                    .map(barber -> new BarberPublicDTO(
                            barber.getId(),
                            barber.getName(),
                            barber.getImageUrl()
                    ))
                    .collect(Collectors.toList());
        } catch (Exception ex) {
            throw new UserServiceUnavailableException("Não foi possível listar os barbeiros desta barbearia no momento.");
        }
    }

    @Transactional(readOnly = true)
    public BarbershopDTO getBarbershop(UUID shopId) {
        Barbershop shop = barbershopRepository.findById(shopId)
                .orElseThrow(() -> new NotFoundException("Barbearia não encontrada."));
        return barbershopMapper.toDTO(shop);
    }

    public void createReview(String customerUid, UUID shopId, CreateBarbershopReviewDTO dto) {
        UserInfoDTO customer = resolveUserByUid(customerUid);

        if (!"CUSTOMER".equals(customer.getUserType())) {
            throw new ForbiddenException("Apenas clientes podem avaliar barbearias.");
        }

        Barbershop shop = barbershopRepository.findById(shopId)
                .orElseThrow(() -> new NotFoundException("Barbearia não encontrada."));

        if (barbershopReviewRepository.existsByBarbershop_IdAndCustomerId(shopId, customer.getId())) {
            throw new DomainConflictException("Você já avaliou esta barbearia.");
        }

        BarbershopReview review = new BarbershopReview();
        review.setBarbershop(shop);
        review.setCustomerId(customer.getId());
        review.setRating(dto.getRating());
        String normalizedComment = dto.getComment() != null ? dto.getComment().trim() : null;
        review.setComment((normalizedComment == null || normalizedComment.isEmpty()) ? null : normalizedComment);

        barbershopReviewRepository.save(review);
    }

    // ========== FLUXO 1: GESTÃO DO DONO (OWNER) ==========

    public BarbershopDTO createBarbershop(String ownerUid, CreateBarbershopDTO dto, MultipartFile logoFile) throws IOException {
        UserInfoDTO owner = resolveUserByUid(ownerUid);
        assertOwner(owner);

        if (barbershopRepository.findByOwnerId(owner.getId()).isPresent()) {
            throw new DomainConflictException("Você já possui uma barbearia.");
        }
        if (barbershopRepository.existsByCnpj(dto.getCnpj())) {
            throw new DomainConflictException("CNPJ já cadastrado.");
        }

        Barbershop shop = barbershopMapper.toEntity(dto);
        shop.setOwnerId(owner.getId());

        Barbershop saved = barbershopRepository.save(shop);

        // Upload de logo se enviado
        if (logoFile != null && !logoFile.isEmpty()) {
            UploadResultDTO result = storageService.uploadFile(logoFile, "barbershop-logos");
            saved.setLogoUrl(result.getSecureUrl());
            saved.setLogoUrlPublicId(result.getPublicId());
            // Atualiza a barbearia salva com a url da imagem
            saved = barbershopRepository.save(saved); 
        }

        // 1. Atualiza barbershopId no user-service
        updateUserBarbershop(owner.getId(), saved.getId());

        // 2. AVISA O USER-SERVICE PARA ELEVAR O PRIVILÉGIO NO FIREBASE (isOwner = true)
        // ESSA É A LINHA QUE FALTAVA!
        userServiceClient.makeBarberOwner(ownerUid);

        return barbershopMapper.toDTO(saved);
    }

    public BarbershopDTO updateBarbershop(String ownerUid, UpdateBarbershopDTO dto) {
        UserInfoDTO owner = resolveUserByUid(ownerUid);
        Barbershop shop = findOwnerShop(owner.getId());

        if (dto.getName() != null) shop.setName(dto.getName());
        if (dto.getAddress() != null) shop.setAddress(dto.getAddress());

        return barbershopMapper.toDTO(barbershopRepository.save(shop));
    }

    public ActivityDTO createActivity(String ownerUid, CreateActivityDTO dto) {
        UserInfoDTO owner = resolveUserByUid(ownerUid);
        Barbershop shop = findOwnerShop(owner.getId());

        Activity activity = activityMapper.toEntity(dto);
        activity.setBarbershop(shop);

        return activityMapper.toDTO(activityRepository.save(activity));
    }

    public ActivityDTO updateActivity(String ownerUid, UUID activityId, UpdateActivityDTO dto) {
        UserInfoDTO owner = resolveUserByUid(ownerUid);
        Barbershop shop = findOwnerShop(owner.getId());

        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new NotFoundException("Atividade não encontrada."));

        if (!activity.getBarbershop().getId().equals(shop.getId())) {
            throw new ForbiddenException("Esta atividade não pertence à sua barbearia.");
        }

        if (dto.getActivityName() != null) activity.setActivityName(dto.getActivityName());
        if (dto.getPrice() != null) activity.setPrice(dto.getPrice());
        if (dto.getDurationMinutes() != null) activity.setDurationMinutes(dto.getDurationMinutes());

        return activityMapper.toDTO(activityRepository.save(activity));
    }

    public void deleteActivity(String ownerUid, UUID activityId) {
        UserInfoDTO owner = resolveUserByUid(ownerUid);
        Barbershop shop = findOwnerShop(owner.getId());

        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new NotFoundException("Atividade não encontrada."));

        if (!activity.getBarbershop().getId().equals(shop.getId())) {
            throw new ForbiddenException("Esta atividade não pertence à sua barbearia.");
        }

        activityRepository.delete(activity);
    }

    public void removeBarber(String ownerUid, UUID barberId) {
        UserInfoDTO owner = resolveUserByUid(ownerUid);
        Barbershop shop = findOwnerShop(owner.getId());

        UserInfoDTO targetBarber = resolveUser(barberId);
        if (!"BARBER".equalsIgnoreCase(targetBarber.getUserType())) {
            throw new DomainConflictException("Somente barbeiros podem ser removidos da equipe.");
        }
        if (targetBarber.getId().equals(owner.getId())) {
            throw new ForbiddenException("O dono nao pode remover a si mesmo. Utilize o encerramento da barbearia.");
        }
        if (targetBarber.getBarbershopId() == null || !targetBarber.getBarbershopId().equals(shop.getId())) {
            throw new ForbiddenException("Este barbeiro nao pertence a sua barbearia.");
        }

        // Remove a associação do barbeiro com a barbearia no user-service
        updateUserBarbershop(barberId, null);
    }

    public void closeBarbershop(String ownerUid, CloseBarbershopRequestDTO dto) {
        UserInfoDTO owner = resolveUserByUid(ownerUid);
        Barbershop shop = findOwnerShop(owner.getId());

        if (dto == null || dto.getPassword() == null || dto.getPassword().isBlank()) {
            throw new DomainConflictException("A confirmacao de senha e obrigatoria para encerrar a barbearia.");
        }

        // Desvincula barbeiros associados antes de apagar a barbearia para evitar inconsistencias.
        List<UserInfoDTO> linkedBarbers;
        try {
            linkedBarbers = userServiceClient.getBarbersByBarbershop(shop.getId());
        } catch (Exception ex) {
            throw new UserServiceUnavailableException("Nao foi possivel consultar a equipe vinculada para encerrar a barbearia.");
        }

        for (UserInfoDTO linked : linkedBarbers) {
            if (linked == null || linked.getId() == null || linked.getId().equals(owner.getId())) {
                continue;
            }
            updateUserBarbershop(linked.getId(), null);
        }

        // Remove a associação do dono
        updateUserBarbershop(owner.getId(), null);

        // Deleta a barbearia (cascade remove activities, highlights, join requests)
        barbershopRepository.delete(shop);
    }

    // ========== FLUXO 2: JOIN REQUESTS ==========

    public void requestToJoinBarbershop(String barberUid, String cnpj) {
        UserInfoDTO barber = resolveUserByUid(barberUid);
        assertOwner(barber); // É barbeiro

        if (barber.getBarbershopId() != null) {
            throw new DomainConflictException("Você já faz parte de uma barbearia. Saia antes de solicitar entrada em outra.");
        }

        Barbershop shop = barbershopRepository.findByCnpj(cnpj)
                .orElseThrow(() -> new NotFoundException("Barbearia com CNPJ " + cnpj + " não encontrada."));

        // Verifica se já existe um pedido pendente
        joinRequestRepository.findByBarberIdAndBarbershopId(barber.getId(), shop.getId())
                .ifPresent(req -> {
                    throw new DomainConflictException("Você já tem uma solicitação para esta barbearia.");
                });

        BarbershopJoinRequest request = new BarbershopJoinRequest();
        request.setBarberId(barber.getId());
        request.setBarbershop(shop);
        request.setStatus(JoinRequestStatus.PENDING);
        request.setRequestType(JoinRequestType.JOIN);
        BarbershopJoinRequest saved = joinRequestRepository.save(request);

        // Publica evento para o notification-service notificar o dono da barbearia
        try {
            UserInfoDTO owner = resolveUser(shop.getOwnerId());
            JoinRequestCreatedEvent event = new JoinRequestCreatedEvent(
                    saved.getId(),
                    barber.getId(),
                    barber.getName(),
                    barber.getEmail(),
                    shop.getId(),
                    shop.getName(),
                    owner.getId(),
                    "JOIN"
            );
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.RK_JOIN_REQUEST_CREATED, event);
            log.info("event=join-request-created-published requestId={} barberId={} shopId={} ownerId={}",
            maskIdentifier(saved.getId()),
            maskIdentifier(barber.getId()),
            maskIdentifier(shop.getId()),
            maskIdentifier(owner.getId()));
        } catch (Exception ex) {
            // Falha na publicação do evento não deve reverter a solicitação
        log.warn("event=join-request-rabbit-publish-failed requestId={} error={}",
            maskIdentifier(saved.getId()),
            sanitizeMessage(ex.getMessage()));
        }
    }

    @Transactional(readOnly = true)
    public List<JoinRequestDTO> getPendingJoinRequests(String ownerUid) {
        UserInfoDTO owner = resolveUserByUid(ownerUid);
        Barbershop shop = findOwnerShop(owner.getId());

        List<BarbershopJoinRequest> requests = joinRequestRepository
                .findByBarbershopIdAndStatusAndRequestType(shop.getId(), JoinRequestStatus.PENDING, JoinRequestType.JOIN);

        return requests.stream().map(req -> {
            JoinRequestDTO dto = new JoinRequestDTO();
            dto.setRequestId(req.getId());
            dto.setBarberId(req.getBarberId());
            dto.setStatus(req.getStatus().name());
            // Enriquecer com dados do user-service (best-effort)
            try {
                UserInfoDTO barberInfo = resolveUser(req.getBarberId());
                dto.setBarberName(barberInfo.getName());
                dto.setBarberEmail(barberInfo.getEmail());
            } catch (Exception e) {
                dto.setBarberName("(indisponível)");
                dto.setBarberEmail("(indisponível)");
            }
            return dto;
        }).collect(Collectors.toList());
    }

    public void approveJoinRequest(String ownerUid, UUID requestId) {
        UserInfoDTO owner = resolveUserByUid(ownerUid);
        Barbershop shop = findOwnerShop(owner.getId());

        BarbershopJoinRequest request = joinRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Solicitação não encontrada."));

        if (!request.getBarbershop().getId().equals(shop.getId())) {
            throw new ForbiddenException("Esta solicitação não pertence à sua barbearia.");
        }

        request.setStatus(JoinRequestStatus.APPROVED);
        joinRequestRepository.save(request);

        // Atualiza barbershopId no user-service
        updateUserBarbershop(request.getBarberId(), shop.getId());
    }

    public void rejectJoinRequest(String ownerUid, UUID requestId) {
        UserInfoDTO owner = resolveUserByUid(ownerUid);
        Barbershop shop = findOwnerShop(owner.getId());

        BarbershopJoinRequest request = joinRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Solicitação não encontrada."));

        if (!request.getBarbershop().getId().equals(shop.getId())) {
            throw new ForbiddenException("Esta solicitação não pertence à sua barbearia.");
        }

        request.setStatus(JoinRequestStatus.REJECTED);
        joinRequestRepository.save(request);
    }

    // ========== FLUXO 2B: CONVITE DO OWNER (INVITE) ==========

    /**
     * Owner convida um barbeiro pelo CPF.
     * Cria um BarbershopJoinRequest do tipo INVITE com status PENDING.
     * O barbeiro verá o convite no seu perfil e poderá aceitar ou recusar.
     */
    public void inviteBarberByCpf(String ownerUid, String cpf) {
        UserInfoDTO owner = resolveUserByUid(ownerUid);
        Barbershop shop = findOwnerShop(owner.getId());

        String cleanCpf = cpf.replaceAll("\\D", "");
        if (cleanCpf.length() != 11) {
            throw new DomainConflictException("CPF inválido. Informe 11 dígitos.");
        }

        // Busca o barbeiro pelo CPF no user-service
        UserInfoDTO barber;
        try {
            barber = userServiceClient.getBarberByCpf(cleanCpf);
        } catch (FeignException.NotFound ex) {
            throw new NotFoundException("Nenhum barbeiro cadastrado com este CPF.");
        } catch (Exception ex) {
            throw new UserServiceUnavailableException("Não foi possível consultar o barbeiro no momento.");
        }
        if (barber == null) {
            throw new NotFoundException("Nenhum barbeiro cadastrado com este CPF.");
        }

        // Validações
        if (barber.getId().equals(owner.getId())) {
            throw new DomainConflictException("Você não pode convidar a si mesmo.");
        }
        if (barber.getBarbershopId() != null) {
            throw new DomainConflictException("Este barbeiro já faz parte de uma barbearia.");
        }

        BarbershopJoinRequest request = joinRequestRepository
                .findByBarberIdAndBarbershopId(barber.getId(), shop.getId())
                .orElseGet(() -> {
                    BarbershopJoinRequest fresh = new BarbershopJoinRequest();
                    fresh.setBarberId(barber.getId());
                    fresh.setBarbershop(shop);
                    return fresh;
                });

        if (request.getStatus() == JoinRequestStatus.PENDING && request.getRequestType() == JoinRequestType.INVITE) {
            throw new DomainConflictException("Já existe um convite pendente para este barbeiro.");
        }

        request.setStatus(JoinRequestStatus.PENDING);
        request.setRequestType(JoinRequestType.INVITE);
        joinRequestRepository.save(request);

        // Publica evento para o notification-service notificar o barbeiro convidado
        try {
            JoinRequestCreatedEvent event = new JoinRequestCreatedEvent(
                    request.getId(),
                    barber.getId(),
                    barber.getName(),
                    barber.getEmail(),
                    shop.getId(),
                    shop.getName(),
                    owner.getId(),
                    "INVITE"
            );
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.RK_JOIN_REQUEST_CREATED, event);
            log.info("event=invite-created-published requestId={} barberId={} shopId={}",
            maskIdentifier(request.getId()),
            maskIdentifier(barber.getId()),
            maskIdentifier(shop.getId()));
        } catch (Exception ex) {
            log.warn("event=invite-rabbit-publish-failed barberId={} shopId={} error={}",
            maskIdentifier(barber.getId()),
            maskIdentifier(shop.getId()),
            sanitizeMessage(ex.getMessage()));
        }

    log.info("event=barber-invited ownerId={} barberId={} shopId={} cpf=***",
        maskIdentifier(owner.getId()),
        maskIdentifier(barber.getId()),
        maskIdentifier(shop.getId()));
    }

    /**
     * Lista os convites (INVITE) pendentes para o barbeiro autenticado.
     */
    @Transactional(readOnly = true)
    public List<JoinRequestDTO> getMyPendingInvites(String barberUid) {
        UserInfoDTO barber = resolveUserByUid(barberUid);
    log.info("event=my-invites-request barberId={} uid={}", maskIdentifier(barber.getId()), maskIdentifier(barberUid));

        List<BarbershopJoinRequest> invites;
        try {
            invites = joinRequestRepository
                    .findByBarberIdAndStatusAndRequestType(barber.getId(), JoinRequestStatus.PENDING, JoinRequestType.INVITE);
        } catch (Exception ex) {
            log.error("event=my-invites-query-failed barberId={} error={}",
                    maskIdentifier(barber.getId()),
                    sanitizeMessage(ex.getMessage()),
                    ex);
            return java.util.Collections.emptyList();
        }

        return invites.stream().map(req -> {
            JoinRequestDTO dto = new JoinRequestDTO();
            dto.setRequestId(req.getId());
            dto.setBarberId(req.getBarberId());
            dto.setStatus(req.getStatus() != null ? req.getStatus().name() : null);
            dto.setRequestType(req.getRequestType() != null ? req.getRequestType().name() : null);
            if (req.getBarbershop() != null) {
                dto.setBarbershopId(req.getBarbershop().getId());
                dto.setBarbershopName(req.getBarbershop().getName());
            }
            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * Barbeiro aceita um convite (INVITE) de uma barbearia.
     */
    public void acceptInvite(String barberUid, UUID requestId) {
        UserInfoDTO barber = resolveUserByUid(barberUid);

        BarbershopJoinRequest request = joinRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Convite não encontrado."));

        if (!request.getBarberId().equals(barber.getId())) {
            throw new ForbiddenException("Este convite não pertence a você.");
        }
        if (request.getRequestType() != JoinRequestType.INVITE) {
            throw new ForbiddenException("Esta solicitação não é um convite.");
        }
        if (request.getStatus() != JoinRequestStatus.PENDING) {
            throw new DomainConflictException("Este convite já foi processado.");
        }
        if (barber.getBarbershopId() != null) {
            throw new DomainConflictException("Você já faz parte de uma barbearia. Saia antes de aceitar outro convite.");
        }

        request.setStatus(JoinRequestStatus.APPROVED);
        joinRequestRepository.save(request);

        // Atualiza barbershopId no user-service
        updateUserBarbershop(barber.getId(), request.getBarbershop().getId());

    log.info("event=invite-accepted barberId={} shopId={}",
        maskIdentifier(barber.getId()),
        maskIdentifier(request.getBarbershop().getId()));
    }

    /**
     * Barbeiro recusa um convite (INVITE) de uma barbearia.
     */
    public void rejectInvite(String barberUid, UUID requestId) {
        UserInfoDTO barber = resolveUserByUid(barberUid);

        BarbershopJoinRequest request = joinRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Convite não encontrado."));

        if (!request.getBarberId().equals(barber.getId())) {
            throw new ForbiddenException("Este convite não pertence a você.");
        }
        if (request.getRequestType() != JoinRequestType.INVITE) {
            throw new ForbiddenException("Esta solicitação não é um convite.");
        }

        request.setStatus(JoinRequestStatus.REJECTED);
        joinRequestRepository.save(request);

    log.info("event=invite-rejected barberId={} shopId={}",
        maskIdentifier(barber.getId()),
        maskIdentifier(request.getBarbershop().getId()));
    }

    // ========== FLUXO 3: SAIR DA LOJA ==========

    public void freeBarber(String barberUid) {
        UserInfoDTO barber = resolveUserByUid(barberUid);

        if (barber.getBarbershopId() == null) {
            throw new DomainConflictException("Você não está associado a nenhuma barbearia.");
        }

        // Verifica se é o dono — dono não pode sair, tem que fechar
        Barbershop shop = barbershopRepository.findById(barber.getBarbershopId()).orElse(null);
        if (shop != null && shop.getOwnerId().equals(barber.getId())) {
            throw new ForbiddenException("O dono não pode sair da barbearia. Use o endpoint de fechar.");
        }

        // Remove associação no user-service
        updateUserBarbershop(barber.getId(), null);
    }

    // ========== FLUXO 4: GESTÃO DE IMAGENS ==========

    public String updateBarbershopLogo(String ownerUid, MultipartFile file) throws IOException {
        UserInfoDTO owner = resolveUserByUid(ownerUid);
        Barbershop shop = findOwnerShop(owner.getId());

        if (shop.getLogoUrlPublicId() != null) {
            storageService.deleteFile(shop.getLogoUrlPublicId());
        }

        UploadResultDTO result = storageService.uploadFile(file, "barbershop-logos");
        shop.setLogoUrl(result.getSecureUrl());
        shop.setLogoUrlPublicId(result.getPublicId());
        barbershopRepository.save(shop);

        return result.getSecureUrl();
    }

    public String updateBarbershopBanner(String ownerUid, MultipartFile file) throws IOException {
        UserInfoDTO owner = resolveUserByUid(ownerUid);
        Barbershop shop = findOwnerShop(owner.getId());

        if (shop.getBannerUrlPublicId() != null) {
            storageService.deleteFile(shop.getBannerUrlPublicId());
        }

        UploadResultDTO result = storageService.uploadFile(file, "barbershop-banners");
        shop.setBannerUrl(result.getSecureUrl());
        shop.setBannerUrlPublicId(result.getPublicId());
        barbershopRepository.save(shop);

        return result.getSecureUrl();
    }

    public String updateActivityPhoto(String ownerUid, UUID activityId, MultipartFile file) throws IOException {
        UserInfoDTO owner = resolveUserByUid(ownerUid);
        Barbershop shop = findOwnerShop(owner.getId());

        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new NotFoundException("Atividade não encontrada."));

        if (!activity.getBarbershop().getId().equals(shop.getId())) {
            throw new ForbiddenException("Esta atividade não pertence à sua barbearia.");
        }

        if (activity.getImageUrlPublicId() != null) {
            storageService.deleteFile(activity.getImageUrlPublicId());
        }

        UploadResultDTO result = storageService.uploadFile(file, "activity-photos");
        activity.setImageUrl(result.getSecureUrl());
        activity.setImageUrlPublicId(result.getPublicId());
        activityRepository.save(activity);

        return result.getSecureUrl();
    }

    public String addBarbershopHighlight(String ownerUid, MultipartFile file) throws IOException {
        UserInfoDTO owner = resolveUserByUid(ownerUid);
        Barbershop shop = findOwnerShop(owner.getId());

        UploadResultDTO result = storageService.uploadFile(file, "barbershop-highlights");

        BarbershopHighlight highlight = new BarbershopHighlight();
        highlight.setImageUrl(result.getSecureUrl());
        highlight.setImageUrlPublicId(result.getPublicId());
        highlight.setBarbershop(shop);
        highlightRepository.save(highlight);

        return result.getSecureUrl();
    }

    public void deleteBarbershopHighlight(String ownerUid, UUID highlightId) {
        UserInfoDTO owner = resolveUserByUid(ownerUid);
        Barbershop shop = findOwnerShop(owner.getId());

        BarbershopHighlight highlight = highlightRepository.findById(highlightId)
                .orElseThrow(() -> new NotFoundException("Destaque não encontrado."));

        if (!highlight.getBarbershop().getId().equals(shop.getId())) {
            throw new ForbiddenException("Este destaque não pertence à sua barbearia.");
        }

        try {
            storageService.deleteFile(highlight.getImageUrlPublicId());
        } catch (IOException e) {
            // Log, mas não impede a exclusão do registro
        }

        highlightRepository.delete(highlight);
    }

    private String maskIdentifier(Object value) {
        if (value == null) {
            return "***";
        }
        String normalized = value.toString().trim();
        if (normalized.length() <= 6) {
            return "***";
        }
        return normalized.substring(0, 4) + "..." + normalized.substring(normalized.length() - 2);
    }

    private String sanitizeMessage(String value) {
        if (value == null || value.isBlank()) {
            return "n/a";
        }
        return value
                .replaceAll("(?i)bearer\\s+[a-z0-9._-]+", "bearer ***")
                .replaceAll("(?i)token[=:\\s]+[^\\s,;]+", "token=***")
                .replaceAll("(?i)authorization[^\\s]*", "authorization***");
    }
}

