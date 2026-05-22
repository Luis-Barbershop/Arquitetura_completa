package ifsp.edu.projeto.cortaai.barbershopservice.service;

import ifsp.edu.projeto.cortaai.barbershopservice.dto.ActivityDTO;
import ifsp.edu.projeto.cortaai.barbershopservice.dto.AppointmentSummaryDTO;
import ifsp.edu.projeto.cortaai.barbershopservice.dto.BarberPublicDTO;
import ifsp.edu.projeto.cortaai.barbershopservice.dto.BarbershopDTO;
import ifsp.edu.projeto.cortaai.barbershopservice.dto.BarbershopSummaryDTO;
import ifsp.edu.projeto.cortaai.barbershopservice.dto.CloseBarbershopRequestDTO;
import ifsp.edu.projeto.cortaai.barbershopservice.dto.CommissionRuleDTO;
import ifsp.edu.projeto.cortaai.barbershopservice.dto.CommissionRuleRequestDTO;
import ifsp.edu.projeto.cortaai.barbershopservice.dto.CreateActivityDTO;
import ifsp.edu.projeto.cortaai.barbershopservice.dto.CreateBarbershopDTO;
import ifsp.edu.projeto.cortaai.barbershopservice.dto.CreateBarbershopReviewDTO;
import ifsp.edu.projeto.cortaai.barbershopservice.dto.JoinRequestDTO;
import ifsp.edu.projeto.cortaai.barbershopservice.dto.RemoveTeamMemberRequestDTO;
import ifsp.edu.projeto.cortaai.barbershopservice.dto.TeamMemberResponseDTO;
import ifsp.edu.projeto.cortaai.barbershopservice.dto.UpdateActivityDTO;
import ifsp.edu.projeto.cortaai.barbershopservice.dto.UpdateBarbershopDTO;
import ifsp.edu.projeto.cortaai.barbershopservice.dto.UploadResultDTO;
import ifsp.edu.projeto.cortaai.barbershopservice.dto.UserInfoDTO;
import ifsp.edu.projeto.cortaai.barbershopservice.exception.DomainConflictException;
import ifsp.edu.projeto.cortaai.barbershopservice.exception.ForbiddenException;
import ifsp.edu.projeto.cortaai.barbershopservice.exception.NotFoundException;
import ifsp.edu.projeto.cortaai.barbershopservice.exception.UserServiceUnavailableException;
import ifsp.edu.projeto.cortaai.barbershopservice.feign.ScheduleServiceClient;
import ifsp.edu.projeto.cortaai.barbershopservice.feign.UserServiceClient;
import ifsp.edu.projeto.cortaai.barbershopservice.mapper.ActivityMapper;
import ifsp.edu.projeto.cortaai.barbershopservice.mapper.BarbershopMapper;
import ifsp.edu.projeto.cortaai.barbershopservice.model.Activity;
import ifsp.edu.projeto.cortaai.barbershopservice.model.BarberCommissionRule;
import ifsp.edu.projeto.cortaai.barbershopservice.model.Barbershop;
import ifsp.edu.projeto.cortaai.barbershopservice.model.BarbershopHighlight;
import ifsp.edu.projeto.cortaai.barbershopservice.model.BarbershopJoinRequest;
import ifsp.edu.projeto.cortaai.barbershopservice.model.BarbershopReview;
import ifsp.edu.projeto.cortaai.barbershopservice.model.enums.JoinRequestStatus;
import ifsp.edu.projeto.cortaai.barbershopservice.model.enums.JoinRequestType;
import ifsp.edu.projeto.cortaai.barbershopservice.repository.ActivityRepository;
import ifsp.edu.projeto.cortaai.barbershopservice.repository.BarberCommissionRuleRepository;
import ifsp.edu.projeto.cortaai.barbershopservice.repository.BarbershopHighlightRepository;
import ifsp.edu.projeto.cortaai.barbershopservice.repository.BarbershopJoinRequestRepository;
import ifsp.edu.projeto.cortaai.barbershopservice.repository.BarbershopRepository;
import ifsp.edu.projeto.cortaai.barbershopservice.repository.BarbershopReviewRepository;
import ifsp.edu.projeto.cortaai.barbershopservice.service.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BarbershopServiceTest {

    @Mock
    private BarbershopRepository barbershopRepository;
    @Mock
    private BarbershopReviewRepository barbershopReviewRepository;
    @Mock
    private ActivityRepository activityRepository;
    @Mock
    private BarbershopJoinRequestRepository joinRequestRepository;
    @Mock
    private BarberCommissionRuleRepository commissionRuleRepository;
    @Mock
    private BarbershopHighlightRepository highlightRepository;
    @Mock
    private StorageService storageService;
    @Mock
    private UserServiceClient userServiceClient;
    @Mock
    private ScheduleServiceClient scheduleServiceClient;
    @Mock
    private RabbitTemplate rabbitTemplate;

    private BarbershopService service;

    @BeforeEach
    void setUp() {
        BarbershopMapper barbershopMapper = Mappers.getMapper(BarbershopMapper.class);
        ActivityMapper activityMapper = Mappers.getMapper(ActivityMapper.class);
        service = new BarbershopService(
                barbershopRepository,
                barbershopReviewRepository,
                activityRepository,
                joinRequestRepository,
                commissionRuleRepository,
                highlightRepository,
                barbershopMapper,
                activityMapper,
                storageService,
                userServiceClient,
                scheduleServiceClient,
                rabbitTemplate
        );
    }

    @Test
    void shouldListBarbershopsWithHighlightUrls() {
        Barbershop shop = shop(UUID.randomUUID(), UUID.randomUUID());
        BarbershopHighlight highlight = highlight(shop, "https://cdn/highlight.jpg", "h1");
        shop.setHighlights(Set.of(highlight));
        when(barbershopRepository.findAll()).thenReturn(List.of(shop));

        List<BarbershopDTO> result = service.listBarbershops();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(shop.getId());
        assertThat(result.get(0).getHighlightUrls()).containsExactly("https://cdn/highlight.jpg");
    }

    @Test
    void shouldListBarbershopsByProximityWithRoundedDistance() {
        Barbershop shop = shop(UUID.randomUUID(), UUID.randomUUID());
        shop.setLatitude(-23.561684);
        shop.setLongitude(-46.655981);
        when(barbershopRepository.findByProximity(-23.55052, -46.633308, 5.0)).thenReturn(List.of(shop));

        List<BarbershopSummaryDTO> result = service.listBarbershopsByProximity(-23.55052, -46.633308, 5.0);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(shop.getId());
        assertThat(result.get(0).distanceKm()).isGreaterThan(0.0);
    }

    @Test
    void shouldListPublicBarbersWhenShopExists() {
        UUID shopId = UUID.randomUUID();
        UserInfoDTO barber = barber(UUID.randomUUID(), shopId);
        barber.setName("Ana");
        barber.setImageUrl("ana.png");
        when(barbershopRepository.existsById(shopId)).thenReturn(true);
        when(userServiceClient.getBarbersByBarbershop(shopId)).thenReturn(List.of(barber));

        List<BarberPublicDTO> result = service.listBarbers(shopId);

        assertThat(result).containsExactly(new BarberPublicDTO(barber.getId(), "Ana", "ana.png"));
    }

    @Test
    void shouldRejectListingBarbersForMissingShop() {
        UUID shopId = UUID.randomUUID();
        when(barbershopRepository.existsById(shopId)).thenReturn(false);

        assertThatThrownBy(() -> service.listBarbers(shopId))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Barbearia não encontrada.");

        verify(userServiceClient, never()).getBarbersByBarbershop(any());
    }

    @Test
    void shouldCreateReviewWithTrimmedBlankCommentAsNull() {
        UUID shopId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        CreateBarbershopReviewDTO dto = new CreateBarbershopReviewDTO();
        dto.setRating(5);
        dto.setComment("   ");
        when(userServiceClient.getUserByFirebaseUid("customer-uid")).thenReturn(customer(customerId));
        when(barbershopRepository.findById(shopId)).thenReturn(Optional.of(shop(shopId, UUID.randomUUID())));
        when(barbershopReviewRepository.existsByBarbershop_IdAndCustomerId(shopId, customerId)).thenReturn(false);

        service.createReview("customer-uid", shopId, dto);

        ArgumentCaptor<BarbershopReview> captor = ArgumentCaptor.forClass(BarbershopReview.class);
        verify(barbershopReviewRepository).save(captor.capture());
        assertThat(captor.getValue().getCustomerId()).isEqualTo(customerId);
        assertThat(captor.getValue().getRating()).isEqualTo(5);
        assertThat(captor.getValue().getComment()).isNull();
    }

    @Test
    void shouldRejectDuplicatedReview() {
        UUID shopId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        CreateBarbershopReviewDTO dto = new CreateBarbershopReviewDTO();
        dto.setRating(4);
        when(userServiceClient.getUserByFirebaseUid("customer-uid")).thenReturn(customer(customerId));
        when(barbershopRepository.findById(shopId)).thenReturn(Optional.of(shop(shopId, UUID.randomUUID())));
        when(barbershopReviewRepository.existsByBarbershop_IdAndCustomerId(shopId, customerId)).thenReturn(true);

        assertThatThrownBy(() -> service.createReview("customer-uid", shopId, dto))
                .isInstanceOf(DomainConflictException.class)
                .hasMessage("Você já avaliou esta barbearia.");

        verify(barbershopReviewRepository, never()).save(any());
    }

    @Test
    void shouldCreateBarbershopWithNormalizedCnpjLogoAndOwnerLink() throws IOException {
        UUID ownerId = UUID.randomUUID();
        CreateBarbershopDTO dto = createBarbershopDTO("Barbearia Centro", "11222333000181");
        MultipartFile logo = logoFile();
        when(userServiceClient.getUserByFirebaseUid("owner-uid")).thenReturn(barber(ownerId, null));
        when(barbershopRepository.findByOwnerId(ownerId)).thenReturn(Optional.empty());
        when(barbershopRepository.existsByCnpj("11222333000181")).thenReturn(false);
        when(barbershopRepository.save(any(Barbershop.class))).thenAnswer(invocation -> {
            Barbershop saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(UUID.randomUUID());
            }
            return saved;
        });
        when(storageService.uploadFile(logo, "barbershop-logos"))
                .thenReturn(new UploadResultDTO("logos/shop", "https://cdn/logo.png"));
        when(logo.isEmpty()).thenReturn(false);

        BarbershopDTO result = service.createBarbershop("owner-uid", dto, logo);

        assertThat(result.getName()).isEqualTo("Barbearia Centro");
        assertThat(result.getCnpj()).isEqualTo("11222333000181");
        assertThat(result.getLogoUrl()).isEqualTo("https://cdn/logo.png");
        verify(userServiceClient).updateUserBarbershopId(eq(ownerId), anyMap());
        verify(userServiceClient).makeBarberOwner("owner-uid");
    }

    @Test
    void shouldRejectCustomerTryingToCreateBarbershop() {
        when(userServiceClient.getUserByFirebaseUid("customer-uid")).thenReturn(customer(UUID.randomUUID()));

        assertThatThrownBy(() -> service.createBarbershop("customer-uid", createBarbershopDTO("Loja", "11222333000181"), null))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Apenas barbeiros podem gerenciar barbearias.");
    }

    @Test
    void shouldUpdateBarbershopFieldsForOwner() {
        UUID ownerId = UUID.randomUUID();
        Barbershop shop = shop(UUID.randomUUID(), ownerId);
        UpdateBarbershopDTO dto = new UpdateBarbershopDTO();
        dto.setName("Novo Nome");
        dto.setAddress("Rua 2");
        dto.setLatitude(-23.1);
        dto.setLongitude(-46.1);
        when(userServiceClient.getUserByFirebaseUid("owner-uid")).thenReturn(barber(ownerId, shop.getId()));
        when(barbershopRepository.findByOwnerId(ownerId)).thenReturn(Optional.of(shop));
        when(barbershopRepository.save(shop)).thenReturn(shop);

        BarbershopDTO result = service.updateBarbershop("owner-uid", dto);

        assertThat(result.getName()).isEqualTo("Novo Nome");
        assertThat(result.getAddress()).isEqualTo("Rua 2");
        assertThat(result.getLatitude()).isEqualTo(-23.1);
        assertThat(result.getLongitude()).isEqualTo(-46.1);
    }

    @Test
    void shouldCreateAndUpdateActivityForOwnerShop() {
        UUID ownerId = UUID.randomUUID();
        Barbershop shop = shop(UUID.randomUUID(), ownerId);
        CreateActivityDTO create = createActivityDTO("Corte", "70.00", 45);
        Activity saved = activity(shop, "Corte", "70.00", 45);
        when(userServiceClient.getUserByFirebaseUid("owner-uid")).thenReturn(barber(ownerId, shop.getId()));
        when(barbershopRepository.findByOwnerId(ownerId)).thenReturn(Optional.of(shop));
        when(activityRepository.save(any(Activity.class))).thenReturn(saved);

        ActivityDTO created = service.createActivity("owner-uid", create);

        assertThat(created.getActivityName()).isEqualTo("Corte");

        UpdateActivityDTO update = new UpdateActivityDTO();
        update.setActivityName("Corte premium");
        update.setPrice(new BigDecimal("90.00"));
        update.setDurationMinutes(60);
        when(activityRepository.findById(saved.getId())).thenReturn(Optional.of(saved));
        when(activityRepository.save(saved)).thenReturn(saved);

        ActivityDTO updated = service.updateActivity("owner-uid", saved.getId(), update);

        assertThat(updated.getActivityName()).isEqualTo("Corte premium");
        assertThat(updated.getPrice()).isEqualByComparingTo("90.00");
        assertThat(updated.getDurationMinutes()).isEqualTo(60);
    }

    @Test
    void shouldRejectActivityUpdateFromAnotherShop() {
        UUID ownerId = UUID.randomUUID();
        Barbershop ownerShop = shop(UUID.randomUUID(), ownerId);
        Activity foreignActivity = activity(shop(UUID.randomUUID(), UUID.randomUUID()), "Barba", "50.00", 30);
        when(userServiceClient.getUserByFirebaseUid("owner-uid")).thenReturn(barber(ownerId, ownerShop.getId()));
        when(barbershopRepository.findByOwnerId(ownerId)).thenReturn(Optional.of(ownerShop));
        when(activityRepository.findById(foreignActivity.getId())).thenReturn(Optional.of(foreignActivity));

        assertThatThrownBy(() -> service.updateActivity("owner-uid", foreignActivity.getId(), new UpdateActivityDTO()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Esta atividade não pertence à sua barbearia.");
    }

    @Test
    void shouldListTeamMembersWithOwnerAndCommissions() {
        UUID ownerId = UUID.randomUUID();
        UUID barberId = UUID.randomUUID();
        Barbershop shop = shop(UUID.randomUUID(), ownerId);
        UserInfoDTO owner = barber(ownerId, shop.getId());
        owner.setName("Dono");
        UserInfoDTO barber = barber(barberId, shop.getId());
        barber.setName("Equipe");
        Activity activity = activity(shop, "Corte", "80.00", 45);
        BarberCommissionRule rule = commissionRule(shop.getId(), barberId, activity, "35.00");
        when(userServiceClient.getUserByFirebaseUid("owner-uid")).thenReturn(owner);
        when(barbershopRepository.findByOwnerId(ownerId)).thenReturn(Optional.of(shop));
        when(userServiceClient.getBarbersByBarbershop(shop.getId())).thenReturn(List.of(owner, barber));
        when(commissionRuleRepository.findByBarbershopId(shop.getId())).thenReturn(List.of(rule));

        List<TeamMemberResponseDTO> result = service.listTeamMembers("owner-uid");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).isOwner()).isTrue();
        assertThat(result.get(1).barberId()).isEqualTo(barberId);
        assertThat(result.get(1).commissions()).extracting(CommissionRuleDTO::percentage)
                .containsExactly(new BigDecimal("35.00"));
    }

    @Test
    void shouldUpsertCommissionForTeamMember() {
        UUID ownerId = UUID.randomUUID();
        UUID barberId = UUID.randomUUID();
        Barbershop shop = shop(UUID.randomUUID(), ownerId);
        Activity activity = activity(shop, "Corte", "80.00", 45);
        CommissionRuleRequestDTO request = new CommissionRuleRequestDTO(activity.getId(), new BigDecimal("40.00"));
        when(userServiceClient.getUserByFirebaseUid("owner-uid")).thenReturn(barber(ownerId, shop.getId()));
        when(barbershopRepository.findByOwnerId(ownerId)).thenReturn(Optional.of(shop));
        when(userServiceClient.getUserById(barberId)).thenReturn(barber(barberId, shop.getId()));
        when(activityRepository.findById(activity.getId())).thenReturn(Optional.of(activity));
        when(commissionRuleRepository.findByBarbershopIdAndBarberIdAndActivityId(shop.getId(), barberId, activity.getId()))
                .thenReturn(Optional.empty());
        when(commissionRuleRepository.save(any(BarberCommissionRule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CommissionRuleDTO result = service.upsertCommission("owner-uid", barberId, request);

        assertThat(result.activityId()).isEqualTo(activity.getId());
        assertThat(result.activityName()).isEqualTo("Corte");
        assertThat(result.percentage()).isEqualByComparingTo("40.00");
    }

    @Test
    void shouldRemoveTeamMemberWithRedistributionAndPublishEvent() {
        UUID ownerId = UUID.randomUUID();
        UUID barberId = UUID.randomUUID();
        UUID destinationId = UUID.randomUUID();
        Barbershop shop = shop(UUID.randomUUID(), ownerId);
        when(userServiceClient.getUserByFirebaseUid("owner-uid")).thenReturn(barber(ownerId, shop.getId()));
        when(barbershopRepository.findByOwnerId(ownerId)).thenReturn(Optional.of(shop));
        when(userServiceClient.getUserById(barberId)).thenReturn(barber(barberId, shop.getId()));
        when(userServiceClient.getUserById(destinationId)).thenReturn(barber(destinationId, shop.getId()));

        service.removeTeamMember("owner-uid", barberId, new RemoveTeamMemberRequestDTO("REDISTRIBUTE", destinationId));

        ArgumentCaptor<Map<String, String>> bodyCaptor = ArgumentCaptor.forClass(Map.class);
        verify(userServiceClient).updateUserBarbershopId(eq(barberId), bodyCaptor.capture());
        assertThat(bodyCaptor.getValue()).containsEntry("barbershopId", null);
        verify(rabbitTemplate).convertAndSend(any(String.class), any(String.class), any(Map.class));
    }

    @Test
    void shouldRejectOwnerRemovingThemself() {
        UUID ownerId = UUID.randomUUID();
        Barbershop shop = shop(UUID.randomUUID(), ownerId);
        when(userServiceClient.getUserByFirebaseUid("owner-uid")).thenReturn(barber(ownerId, shop.getId()));
        when(barbershopRepository.findByOwnerId(ownerId)).thenReturn(Optional.of(shop));
        when(userServiceClient.getUserById(ownerId)).thenReturn(barber(ownerId, shop.getId()));

        assertThatThrownBy(() -> service.removeTeamMember("owner-uid", ownerId, new RemoveTeamMemberRequestDTO("CANCEL", null)))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("O dono nao pode remover a si mesmo. Utilize o encerramento da barbearia.");
    }

    @Test
    void shouldReturnOnlyConflictsFromOwnerShop() {
        UUID ownerId = UUID.randomUUID();
        UUID barberId = UUID.randomUUID();
        Barbershop shop = shop(UUID.randomUUID(), ownerId);
        AppointmentSummaryDTO ownAppointment = appointment(shop.getId(), barberId);
        AppointmentSummaryDTO anotherShopAppointment = appointment(UUID.randomUUID(), barberId);
        when(userServiceClient.getUserByFirebaseUid("owner-uid")).thenReturn(barber(ownerId, shop.getId()));
        when(barbershopRepository.findByOwnerId(ownerId)).thenReturn(Optional.of(shop));
        when(userServiceClient.getUserById(barberId)).thenReturn(barber(barberId, shop.getId()));
        when(scheduleServiceClient.getFutureAppointmentsByBarber(barberId))
                .thenReturn(List.of(ownAppointment, anotherShopAppointment));

        List<AppointmentSummaryDTO> result = service.getRemovalConflicts("owner-uid", barberId);

        assertThat(result).containsExactly(ownAppointment);
    }

    @Test
    void shouldCreateJoinRequestAndPublishNotificationEvent() {
        UUID barberId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Barbershop shop = shop(UUID.randomUUID(), ownerId);
        when(userServiceClient.getUserByFirebaseUid("barber-uid")).thenReturn(barber(barberId, null));
        when(barbershopRepository.findByCnpj("11222333000181")).thenReturn(Optional.of(shop));
        when(joinRequestRepository.findByBarberIdAndBarbershopId(barberId, shop.getId())).thenReturn(Optional.empty());
        when(joinRequestRepository.save(any(BarbershopJoinRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userServiceClient.getUserById(ownerId)).thenReturn(barber(ownerId, shop.getId()));

        service.requestToJoinBarbershop("barber-uid", "11.222.333/0001-81");

        ArgumentCaptor<BarbershopJoinRequest> captor = ArgumentCaptor.forClass(BarbershopJoinRequest.class);
        verify(joinRequestRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(JoinRequestStatus.PENDING);
        assertThat(captor.getValue().getRequestType()).isEqualTo(JoinRequestType.JOIN);
        verify(rabbitTemplate).convertAndSend(any(String.class), any(String.class), any(Object.class));
    }

    @Test
    void shouldApproveJoinRequestAndLinkBarber() {
        UUID ownerId = UUID.randomUUID();
        UUID barberId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        Barbershop shop = shop(UUID.randomUUID(), ownerId);
        BarbershopJoinRequest request = joinRequest(requestId, barberId, shop, JoinRequestStatus.PENDING, JoinRequestType.JOIN);
        when(userServiceClient.getUserByFirebaseUid("owner-uid")).thenReturn(barber(ownerId, shop.getId()));
        when(barbershopRepository.findByOwnerId(ownerId)).thenReturn(Optional.of(shop));
        when(joinRequestRepository.findById(requestId)).thenReturn(Optional.of(request));

        service.approveJoinRequest("owner-uid", requestId);

        assertThat(request.getStatus()).isEqualTo(JoinRequestStatus.APPROVED);
        verify(joinRequestRepository).save(request);
        verify(userServiceClient).updateUserBarbershopId(eq(barberId), anyMap());
    }

    @Test
    void shouldInviteBarberByCpfWhenNoPendingInviteExists() {
        UUID ownerId = UUID.randomUUID();
        UUID barberId = UUID.randomUUID();
        Barbershop shop = shop(UUID.randomUUID(), ownerId);
        when(userServiceClient.getUserByFirebaseUid("owner-uid")).thenReturn(barber(ownerId, shop.getId()));
        when(barbershopRepository.findByOwnerId(ownerId)).thenReturn(Optional.of(shop));
        when(userServiceClient.getBarberByCpf(Map.of("cpf", "12345678901"))).thenReturn(barber(barberId, null));
        when(joinRequestRepository.findByBarberIdAndBarbershopId(barberId, shop.getId())).thenReturn(Optional.empty());

        service.inviteBarberByCpf("owner-uid", "123.456.789-01");

        ArgumentCaptor<BarbershopJoinRequest> captor = ArgumentCaptor.forClass(BarbershopJoinRequest.class);
        verify(joinRequestRepository).save(captor.capture());
        assertThat(captor.getValue().getRequestType()).isEqualTo(JoinRequestType.INVITE);
        assertThat(captor.getValue().getStatus()).isEqualTo(JoinRequestStatus.PENDING);
    }

    @Test
    void shouldRejectDuplicatePendingInvite() {
        UUID ownerId = UUID.randomUUID();
        UUID barberId = UUID.randomUUID();
        Barbershop shop = shop(UUID.randomUUID(), ownerId);
        when(userServiceClient.getUserByFirebaseUid("owner-uid")).thenReturn(barber(ownerId, shop.getId()));
        when(barbershopRepository.findByOwnerId(ownerId)).thenReturn(Optional.of(shop));
        when(userServiceClient.getBarberByCpf(Map.of("cpf", "12345678901"))).thenReturn(barber(barberId, null));
        when(joinRequestRepository.findByBarberIdAndBarbershopId(barberId, shop.getId()))
                .thenReturn(Optional.of(joinRequest(UUID.randomUUID(), barberId, shop, JoinRequestStatus.PENDING, JoinRequestType.INVITE)));

        assertThatThrownBy(() -> service.inviteBarberByCpf("owner-uid", "12345678901"))
                .isInstanceOf(DomainConflictException.class)
                .hasMessage("Já existe um convite pendente para este barbeiro.");
    }

    @Test
    void shouldListMyPendingInvitesAndFallbackToEmptyWhenRepositoryFails() {
        UUID barberId = UUID.randomUUID();
        Barbershop shop = shop(UUID.randomUUID(), UUID.randomUUID());
        when(userServiceClient.getUserByFirebaseUid("barber-uid")).thenReturn(barber(barberId, null));
        when(joinRequestRepository.findByBarberIdAndStatusAndRequestType(barberId, JoinRequestStatus.PENDING, JoinRequestType.INVITE))
                .thenReturn(List.of(joinRequest(UUID.randomUUID(), barberId, shop, JoinRequestStatus.PENDING, JoinRequestType.INVITE)))
                .thenThrow(new RuntimeException("db off"));

        List<JoinRequestDTO> result = service.getMyPendingInvites("barber-uid");
        List<JoinRequestDTO> fallback = service.getMyPendingInvites("barber-uid");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBarbershopId()).isEqualTo(shop.getId());
        assertThat(result.get(0).getRequestType()).isEqualTo("INVITE");
        assertThat(fallback).isEmpty();
    }

    @Test
    void shouldAcceptInviteAndRejectForeignInvite() {
        UUID barberId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        Barbershop shop = shop(UUID.randomUUID(), UUID.randomUUID());
        BarbershopJoinRequest request = joinRequest(requestId, barberId, shop, JoinRequestStatus.PENDING, JoinRequestType.INVITE);
        when(userServiceClient.getUserByFirebaseUid("barber-uid")).thenReturn(barber(barberId, null));
        when(joinRequestRepository.findById(requestId)).thenReturn(Optional.of(request));

        service.acceptInvite("barber-uid", requestId);

        assertThat(request.getStatus()).isEqualTo(JoinRequestStatus.APPROVED);
        verify(userServiceClient).updateUserBarbershopId(eq(barberId), anyMap());

        UUID foreignRequestId = UUID.randomUUID();
        when(joinRequestRepository.findById(foreignRequestId))
                .thenReturn(Optional.of(joinRequest(foreignRequestId, UUID.randomUUID(), shop, JoinRequestStatus.PENDING, JoinRequestType.INVITE)));

        assertThatThrownBy(() -> service.rejectInvite("barber-uid", foreignRequestId))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Este convite não pertence a você.");
    }

    @Test
    void shouldFreeBarberWhenTheyAreNotOwner() {
        UUID barberId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        when(userServiceClient.getUserByFirebaseUid("barber-uid")).thenReturn(barber(barberId, shopId));
        when(barbershopRepository.findById(shopId)).thenReturn(Optional.of(shop(shopId, UUID.randomUUID())));

        service.freeBarber("barber-uid");

        verify(userServiceClient).updateUserBarbershopId(eq(barberId), anyMap());
    }

    @Test
    void shouldRejectOwnerTryingToLeaveTheirShop() {
        UUID ownerId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        when(userServiceClient.getUserByFirebaseUid("owner-uid")).thenReturn(barber(ownerId, shopId));
        when(barbershopRepository.findById(shopId)).thenReturn(Optional.of(shop(shopId, ownerId)));

        assertThatThrownBy(() -> service.freeBarber("owner-uid"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("O dono não pode sair da barbearia. Use o endpoint de fechar.");
    }

    @Test
    void shouldCloseBarbershopUnlinkingTeamMembersAndOwner() {
        UUID ownerId = UUID.randomUUID();
        UUID barberId = UUID.randomUUID();
        Barbershop shop = shop(UUID.randomUUID(), ownerId);
        CloseBarbershopRequestDTO dto = new CloseBarbershopRequestDTO();
        dto.setPassword("secret");
        when(userServiceClient.getUserByFirebaseUid("owner-uid")).thenReturn(barber(ownerId, shop.getId()));
        when(barbershopRepository.findByOwnerId(ownerId)).thenReturn(Optional.of(shop));
        when(userServiceClient.getBarbersByBarbershop(shop.getId()))
                .thenReturn(Arrays.asList(barber(ownerId, shop.getId()), null, barber(barberId, shop.getId())));

        service.closeBarbershop("owner-uid", dto);

        verify(userServiceClient).updateUserBarbershopId(eq(barberId), anyMap());
        verify(userServiceClient).updateUserBarbershopId(eq(ownerId), anyMap());
        verify(barbershopRepository).delete(shop);
    }

    @Test
    void shouldUpdateBarbershopLogoReplacingPreviousFile() throws IOException {
        UUID ownerId = UUID.randomUUID();
        Barbershop shop = shop(UUID.randomUUID(), ownerId);
        shop.setLogoUrlPublicId("old-logo");
        MultipartFile logo = logoFile();
        when(userServiceClient.getUserByFirebaseUid("owner-uid")).thenReturn(barber(ownerId, shop.getId()));
        when(barbershopRepository.findByOwnerId(ownerId)).thenReturn(Optional.of(shop));
        when(storageService.uploadFile(logo, "barbershop-logos"))
                .thenReturn(new UploadResultDTO("new-logo", "https://cdn/new-logo.png"));

        String result = service.updateBarbershopLogo("owner-uid", logo);

        assertThat(result).isEqualTo("https://cdn/new-logo.png");
        assertThat(shop.getLogoUrlPublicId()).isEqualTo("new-logo");
        verify(storageService).deleteFile("old-logo");
        verify(barbershopRepository).save(shop);
    }

    @Test
    void shouldUpdateActivityPhotoAndDeleteHighlightEvenWhenStorageDeleteFails() throws IOException {
        UUID ownerId = UUID.randomUUID();
        Barbershop shop = shop(UUID.randomUUID(), ownerId);
        Activity activity = activity(shop, "Corte", "80.00", 45);
        activity.setImageUrlPublicId("old-activity");
        MultipartFile photo = logoFile();
        when(userServiceClient.getUserByFirebaseUid("owner-uid")).thenReturn(barber(ownerId, shop.getId()));
        when(barbershopRepository.findByOwnerId(ownerId)).thenReturn(Optional.of(shop));
        when(activityRepository.findById(activity.getId())).thenReturn(Optional.of(activity));
        when(storageService.uploadFile(photo, "activity-photos"))
                .thenReturn(new UploadResultDTO("new-activity", "https://cdn/activity.png"));

        String result = service.updateActivityPhoto("owner-uid", activity.getId(), photo);

        assertThat(result).isEqualTo("https://cdn/activity.png");
        assertThat(activity.getImageUrlPublicId()).isEqualTo("new-activity");
        verify(storageService).deleteFile("old-activity");
        verify(activityRepository).save(activity);

        BarbershopHighlight highlight = highlight(shop, "https://cdn/high.jpg", "highlight-public-id");
        when(highlightRepository.findById(highlight.getId())).thenReturn(Optional.of(highlight));
        org.mockito.Mockito.doThrow(new IOException("cdn off")).when(storageService).deleteFile("highlight-public-id");

        service.deleteBarbershopHighlight("owner-uid", highlight.getId());

        verify(highlightRepository).delete(highlight);
    }

    @Test
    void shouldTranslateUserClientFailures() {
        when(userServiceClient.getUserByFirebaseUid("owner-uid")).thenThrow(new RuntimeException("timeout"));

        assertThatThrownBy(() -> service.updateBarbershop("owner-uid", new UpdateBarbershopDTO()))
                .isInstanceOf(UserServiceUnavailableException.class)
                .hasMessage("Não foi possível consultar dados do usuário no momento.");
    }

    private CreateBarbershopDTO createBarbershopDTO(String name, String cnpj) {
        CreateBarbershopDTO dto = new CreateBarbershopDTO();
        dto.setName(name);
        dto.setCnpj(cnpj);
        dto.setAddress("Rua Central");
        return dto;
    }

    private CreateActivityDTO createActivityDTO(String name, String price, int duration) {
        CreateActivityDTO dto = new CreateActivityDTO();
        dto.setActivityName(name);
        dto.setPrice(new BigDecimal(price));
        dto.setDurationMinutes(duration);
        return dto;
    }

    private UserInfoDTO barber(UUID id, UUID shopId) {
        UserInfoDTO user = new UserInfoDTO();
        user.setId(id);
        user.setName("Barbeiro");
        user.setEmail("barber@cortaai.com");
        user.setUserType("BARBER");
        user.setBarbershopId(shopId);
        user.setWorkStartTime(LocalTime.of(9, 0));
        user.setWorkEndTime(LocalTime.of(18, 0));
        return user;
    }

    private UserInfoDTO customer(UUID id) {
        UserInfoDTO user = new UserInfoDTO();
        user.setId(id);
        user.setName("Cliente");
        user.setEmail("customer@cortaai.com");
        user.setUserType("CUSTOMER");
        return user;
    }

    private Barbershop shop(UUID shopId, UUID ownerId) {
        Barbershop shop = new Barbershop();
        shop.setId(shopId);
        shop.setOwnerId(ownerId);
        shop.setName("Barbearia Teste");
        shop.setCnpj("11222333000181");
        shop.setAddress("Rua 1");
        shop.setAverageRating(4.5);
        shop.setReviewsCount(12L);
        shop.setLatitude(-23.5);
        shop.setLongitude(-46.6);
        return shop;
    }

    private Activity activity(Barbershop shop, String name, String price, int duration) {
        Activity activity = new Activity();
        activity.setId(UUID.randomUUID());
        activity.setBarbershop(shop);
        activity.setActivityName(name);
        activity.setPrice(new BigDecimal(price));
        activity.setDurationMinutes(duration);
        return activity;
    }

    private BarberCommissionRule commissionRule(UUID shopId, UUID barberId, Activity activity, String percentage) {
        BarberCommissionRule rule = new BarberCommissionRule();
        rule.setId(UUID.randomUUID());
        rule.setBarbershopId(shopId);
        rule.setBarberId(barberId);
        rule.setActivity(activity);
        rule.setPercentage(new BigDecimal(percentage));
        return rule;
    }

    private BarbershopJoinRequest joinRequest(UUID requestId,
                                              UUID barberId,
                                              Barbershop shop,
                                              JoinRequestStatus status,
                                              JoinRequestType type) {
        BarbershopJoinRequest request = new BarbershopJoinRequest();
        request.setId(requestId);
        request.setBarberId(barberId);
        request.setBarbershop(shop);
        request.setStatus(status);
        request.setRequestType(type);
        return request;
    }

    private BarbershopHighlight highlight(Barbershop shop, String imageUrl, String publicId) {
        BarbershopHighlight highlight = new BarbershopHighlight();
        highlight.setId(UUID.randomUUID());
        highlight.setBarbershop(shop);
        highlight.setImageUrl(imageUrl);
        highlight.setImageUrlPublicId(publicId);
        return highlight;
    }

    private AppointmentSummaryDTO appointment(UUID shopId, UUID barberId) {
        return new AppointmentSummaryDTO(
                UUID.randomUUID(),
                barberId,
                UUID.randomUUID(),
                shopId,
                "Cliente",
                "Corte",
                LocalDateTime.of(2026, 5, 22, 10, 0),
                LocalDateTime.of(2026, 5, 22, 11, 0),
                "PAYMENT_PENDING"
        );
    }

    private MultipartFile logoFile() {
        return org.mockito.Mockito.mock(MultipartFile.class);
    }
}
