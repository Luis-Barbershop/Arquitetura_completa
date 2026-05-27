package ifsp.edu.projeto.cortaai.barbershopservice.service;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import ifsp.edu.projeto.cortaai.barbershopservice.dto.BarbershopDTO;
import ifsp.edu.projeto.cortaai.barbershopservice.dto.CreateBarbershopDTO;
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
import ifsp.edu.projeto.cortaai.barbershopservice.model.Barbershop;
import ifsp.edu.projeto.cortaai.barbershopservice.model.BarbershopJoinRequest;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BarbershopServiceEdgeCasesTest {

    @Mock private BarbershopRepository barbershopRepository;
    @Mock private BarbershopReviewRepository barbershopReviewRepository;
    @Mock private ActivityRepository activityRepository;
    @Mock private BarbershopJoinRequestRepository joinRequestRepository;
    @Mock private BarberCommissionRuleRepository commissionRuleRepository;
    @Mock private BarbershopHighlightRepository highlightRepository;
    @Mock private StorageService storageService;
    @Mock private UserServiceClient userServiceClient;
    @Mock private ScheduleServiceClient scheduleServiceClient;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private GeocodingService geocodingService;

    private BarbershopService service;

    @BeforeEach
    void setUp() {
        service = new BarbershopService(
                barbershopRepository,
                barbershopReviewRepository,
                activityRepository,
                joinRequestRepository,
                commissionRuleRepository,
                highlightRepository,
                Mappers.getMapper(BarbershopMapper.class),
                Mappers.getMapper(ActivityMapper.class),
                storageService,
                userServiceClient,
                scheduleServiceClient,
                rabbitTemplate,
                geocodingService
        );
    }

    // ─── createBarbershop: ramo de geocodificação ─────────────────────────────

    @Test
    void shouldGeocodeAddressWhenCoordsAbsentOnCreate() throws IOException {
        UUID ownerId = UUID.randomUUID();
        CreateBarbershopDTO dto = new CreateBarbershopDTO();
        dto.setName("Barbearia Geo");
        dto.setCnpj("11222333000181");
        dto.setAddress("Rua das Flores, 123, São Paulo");

        UserInfoDTO owner = barber(ownerId, null);
        when(userServiceClient.getUserByFirebaseUid("owner-uid")).thenReturn(owner);
        when(barbershopRepository.findByOwnerId(ownerId)).thenReturn(Optional.empty());
        when(barbershopRepository.existsByCnpj("11222333000181")).thenReturn(false);
        when(geocodingService.geocode("Rua das Flores, 123, São Paulo"))
                .thenReturn(new GeocodingService.Coords(-23.55, -46.63));
        when(barbershopRepository.save(any(Barbershop.class))).thenAnswer(inv -> {
            Barbershop b = inv.getArgument(0);
            if (b.getId() == null) b.setId(UUID.randomUUID());
            return b;
        });

        BarbershopDTO result = service.createBarbershop("owner-uid", dto, null);

        assertThat(result.getLatitude()).isEqualTo(-23.55);
        assertThat(result.getLongitude()).isEqualTo(-46.63);
        verify(geocodingService).geocode("Rua das Flores, 123, São Paulo");
    }

    @Test
    void shouldSkipGeocodeWhenAddressIsBlankOnCreate() throws IOException {
        UUID ownerId = UUID.randomUUID();
        CreateBarbershopDTO dto = new CreateBarbershopDTO();
        dto.setName("Barbearia Sem Endereço");
        dto.setCnpj("11222333000181");
        // address null → geocodingService nunca deve ser chamado

        UserInfoDTO owner = barber(ownerId, null);
        when(userServiceClient.getUserByFirebaseUid("owner-uid")).thenReturn(owner);
        when(barbershopRepository.findByOwnerId(ownerId)).thenReturn(Optional.empty());
        when(barbershopRepository.existsByCnpj("11222333000181")).thenReturn(false);
        when(barbershopRepository.save(any(Barbershop.class))).thenAnswer(inv -> {
            Barbershop b = inv.getArgument(0);
            if (b.getId() == null) b.setId(UUID.randomUUID());
            return b;
        });

        service.createBarbershop("owner-uid", dto, null);

        verify(geocodingService, never()).geocode(any());
    }

    @Test
    void shouldHandleGeocodingReturningNullOnCreate() throws IOException {
        UUID ownerId = UUID.randomUUID();
        CreateBarbershopDTO dto = new CreateBarbershopDTO();
        dto.setName("Barbearia Sem Coords");
        dto.setCnpj("11222333000181");
        dto.setAddress("Endereço desconhecido");

        UserInfoDTO owner = barber(ownerId, null);
        when(userServiceClient.getUserByFirebaseUid("owner-uid")).thenReturn(owner);
        when(barbershopRepository.findByOwnerId(ownerId)).thenReturn(Optional.empty());
        when(barbershopRepository.existsByCnpj("11222333000181")).thenReturn(false);
        when(geocodingService.geocode(any())).thenReturn(null);
        when(barbershopRepository.save(any(Barbershop.class))).thenAnswer(inv -> {
            Barbershop b = inv.getArgument(0);
            if (b.getId() == null) b.setId(UUID.randomUUID());
            return b;
        });

        BarbershopDTO result = service.createBarbershop("owner-uid", dto, null);

        // Sem exceção e coordenadas permanecem nulas
        assertThat(result.getLatitude()).isNull();
        assertThat(result.getLongitude()).isNull();
    }

    // ─── updateBarbershop: ramo de geocodificação ────────────────────────────

    @Test
    void shouldGeocodeOnUpdateWhenAddressChangesWithoutManualCoords() {
        UUID ownerId = UUID.randomUUID();
        Barbershop shop = shop(UUID.randomUUID(), ownerId);
        shop.setAddress("Rua Antiga");
        shop.setLatitude(null);
        shop.setLongitude(null);

        UpdateBarbershopDTO dto = new UpdateBarbershopDTO();
        dto.setAddress("Rua Nova, 500, Campinas");

        when(userServiceClient.getUserByFirebaseUid("owner-uid")).thenReturn(barber(ownerId, shop.getId()));
        when(barbershopRepository.findByOwnerId(ownerId)).thenReturn(Optional.of(shop));
        when(geocodingService.geocode("Rua Nova, 500, Campinas"))
                .thenReturn(new GeocodingService.Coords(-22.9, -47.0));
        when(barbershopRepository.save(shop)).thenReturn(shop);

        service.updateBarbershop("owner-uid", dto);

        assertThat(shop.getLatitude()).isEqualTo(-22.9);
        assertThat(shop.getLongitude()).isEqualTo(-47.0);
        verify(geocodingService).geocode("Rua Nova, 500, Campinas");
    }

    @Test
    void shouldSkipGeocodeOnUpdateWhenManualCoordsProvided() {
        UUID ownerId = UUID.randomUUID();
        Barbershop shop = shop(UUID.randomUUID(), ownerId);
        shop.setAddress("Rua Antiga");
        shop.setLatitude(-23.0);
        shop.setLongitude(-46.0);

        UpdateBarbershopDTO dto = new UpdateBarbershopDTO();
        dto.setAddress("Rua Mudou");
        dto.setLatitude(-22.5);
        dto.setLongitude(-47.5);

        when(userServiceClient.getUserByFirebaseUid("owner-uid")).thenReturn(barber(ownerId, shop.getId()));
        when(barbershopRepository.findByOwnerId(ownerId)).thenReturn(Optional.of(shop));
        when(barbershopRepository.save(shop)).thenReturn(shop);

        service.updateBarbershop("owner-uid", dto);

        verify(geocodingService, never()).geocode(any());
        assertThat(shop.getLatitude()).isEqualTo(-22.5);
    }

    // ─── listBarbers: ramo de exceção do Feign ────────────────────────────────

    @Test
    void shouldThrowUserServiceUnavailableWhenListBarbersClientFails() {
        UUID shopId = UUID.randomUUID();
        when(barbershopRepository.existsById(shopId)).thenReturn(true);
        when(userServiceClient.getBarbersByBarbershop(shopId)).thenThrow(new RuntimeException("timeout"));

        assertThatThrownBy(() -> service.listBarbers(shopId))
                .isInstanceOf(UserServiceUnavailableException.class)
                .hasMessage("Não foi possível listar os barbeiros desta barbearia no momento.");
    }

    // ─── freeBarber: ramos de guarda ─────────────────────────────────────────

    @Test
    void shouldThrowWhenBarberHasNoBarbershopLinked() {
        UUID barberId = UUID.randomUUID();
        when(userServiceClient.getUserByFirebaseUid("barber-uid")).thenReturn(barber(barberId, null));

        assertThatThrownBy(() -> service.freeBarber("barber-uid"))
                .isInstanceOf(DomainConflictException.class)
                .hasMessage("Você não está associado a nenhuma barbearia.");
    }

    @Test
    void shouldFreeBarberEvenWhenBarbershopRecordNotFound() {
        UUID barberId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        // Barbeiro vinculado mas a barbearia foi deletada no banco
        when(userServiceClient.getUserByFirebaseUid("barber-uid")).thenReturn(barber(barberId, shopId));
        when(barbershopRepository.findById(shopId)).thenReturn(Optional.empty());

        service.freeBarber("barber-uid");

        verify(userServiceClient).updateUserBarbershopId(eq(barberId), anyMap());
    }

    // ─── inviteBarberByCpf: ramos de Feign ───────────────────────────────────

    @Test
    void shouldThrowNotFoundWhenBarberCpfNotFoundViaCpfFeign() {
        UUID ownerId = UUID.randomUUID();
        Barbershop shop = shop(UUID.randomUUID(), ownerId);
        when(userServiceClient.getUserByFirebaseUid("owner-uid")).thenReturn(barber(ownerId, shop.getId()));
        when(barbershopRepository.findByOwnerId(ownerId)).thenReturn(Optional.of(shop));

        FeignException.NotFound notFound = new FeignException.NotFound(
                "404",
                dummyRequest(),
                null,
                Collections.emptyMap()
        );
        when(userServiceClient.getBarberByCpf(Map.of("cpf", "12345678901"))).thenThrow(notFound);

        assertThatThrownBy(() -> service.inviteBarberByCpf("owner-uid", "12345678901"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Nenhum barbeiro cadastrado com este CPF.");
    }

    @Test
    void shouldThrowUserServiceUnavailableWhenBarberCpfFeignFails() {
        UUID ownerId = UUID.randomUUID();
        Barbershop shop = shop(UUID.randomUUID(), ownerId);
        when(userServiceClient.getUserByFirebaseUid("owner-uid")).thenReturn(barber(ownerId, shop.getId()));
        when(barbershopRepository.findByOwnerId(ownerId)).thenReturn(Optional.of(shop));
        when(userServiceClient.getBarberByCpf(Map.of("cpf", "12345678901")))
                .thenThrow(new RuntimeException("user-service down"));

        assertThatThrownBy(() -> service.inviteBarberByCpf("owner-uid", "12345678901"))
                .isInstanceOf(UserServiceUnavailableException.class)
                .hasMessage("Não foi possível consultar o barbeiro no momento.");
    }

    @Test
    void shouldRejectNullCpfWhenInvitingBarber() {
        UUID ownerId = UUID.randomUUID();
        Barbershop shop = shop(UUID.randomUUID(), ownerId);
        when(userServiceClient.getUserByFirebaseUid("owner-uid")).thenReturn(barber(ownerId, shop.getId()));
        when(barbershopRepository.findByOwnerId(ownerId)).thenReturn(Optional.of(shop));

        assertThatThrownBy(() -> service.inviteBarberByCpf("owner-uid", null))
                .isInstanceOf(DomainConflictException.class)
                .hasMessage("CPF inválido. Informe 11 dígitos.");
    }

    // ─── requestToJoinBarbershop: ramos de CNPJ e request duplicada ──────────

    @Test
    void shouldThrowNotFoundWhenCnpjNotRegistered() {
        UUID barberId = UUID.randomUUID();
        when(userServiceClient.getUserByFirebaseUid("barber-uid")).thenReturn(barber(barberId, null));
        when(barbershopRepository.findByCnpj("11222333000181")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.requestToJoinBarbershop("barber-uid", "11.222.333/0001-81"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("11222333000181");
    }

    @Test
    void shouldThrowConflictWhenRequestToJoinAlreadyExists() {
        UUID barberId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Barbershop shop = shop(UUID.randomUUID(), ownerId);
        BarbershopJoinRequest existing = new BarbershopJoinRequest();
        existing.setId(UUID.randomUUID());
        existing.setBarberId(barberId);
        existing.setBarbershop(shop);
        existing.setStatus(JoinRequestStatus.PENDING);
        existing.setRequestType(JoinRequestType.JOIN);

        when(userServiceClient.getUserByFirebaseUid("barber-uid")).thenReturn(barber(barberId, null));
        when(barbershopRepository.findByCnpj("11222333000181")).thenReturn(Optional.of(shop));
        when(joinRequestRepository.findByBarberIdAndBarbershopId(barberId, shop.getId()))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.requestToJoinBarbershop("barber-uid", "11222333000181"))
                .isInstanceOf(DomainConflictException.class)
                .hasMessage("Você já tem uma solicitação para esta barbearia.");
    }

    // ─── updateUserBarbershop (helper): ramos de catch ───────────────────────

    @Test
    void shouldThrowNotFoundOnFeignNotFoundInUpdateUserBarbershop() throws IOException {
        // FeignException.NotFound → NotFoundException com mensagem de usuário não encontrado
        UUID ownerId = UUID.randomUUID();
        Barbershop shop = shop(UUID.randomUUID(), ownerId);

        FeignException.NotFound notFound = new FeignException.NotFound(
                "404",
                dummyRequest(),
                null,
                Collections.emptyMap()
        );

        UserInfoDTO owner = barber(ownerId, shop.getId());
        when(userServiceClient.getUserByFirebaseUid("owner-uid")).thenReturn(owner);
        when(barbershopRepository.findByOwnerId(ownerId)).thenReturn(Optional.of(shop));
        when(userServiceClient.getBarbersByBarbershop(shop.getId())).thenReturn(java.util.List.of());
        doThrow(notFound).when(userServiceClient).updateUserBarbershopId(eq(ownerId), anyMap());

        var dto = new ifsp.edu.projeto.cortaai.barbershopservice.dto.CloseBarbershopRequestDTO();
        dto.setPassword("secret");

        assertThatThrownBy(() -> service.closeBarbershop("owner-uid", dto))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Barbeiro não encontrado no serviço de usuários");
    }

    @Test
    void shouldThrowUserServiceUnavailableOnFeignExceptionInUpdateUserBarbershop() throws IOException {
        UUID ownerId = UUID.randomUUID();
        Barbershop shop = shop(UUID.randomUUID(), ownerId);

        // FeignException genérica (não NotFound) → UserServiceUnavailableException
        FeignException feignEx = FeignException.errorStatus(
                "PUT",
                feign.Response.builder()
                        .status(503)
                        .reason("Service Unavailable")
                        .request(dummyRequest())
                        .headers(Collections.emptyMap())
                        .build()
        );

        UserInfoDTO owner = barber(ownerId, shop.getId());
        when(userServiceClient.getUserByFirebaseUid("owner-uid")).thenReturn(owner);
        when(barbershopRepository.findByOwnerId(ownerId)).thenReturn(Optional.of(shop));
        when(userServiceClient.getBarbersByBarbershop(shop.getId())).thenReturn(java.util.List.of());
        doThrow(feignEx).when(userServiceClient).updateUserBarbershopId(eq(ownerId), anyMap());

        var dto = new ifsp.edu.projeto.cortaai.barbershopservice.dto.CloseBarbershopRequestDTO();
        dto.setPassword("secret");

        assertThatThrownBy(() -> service.closeBarbershop("owner-uid", dto))
                .isInstanceOf(UserServiceUnavailableException.class);
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private UserInfoDTO barber(UUID id, UUID shopId) {
        UserInfoDTO user = new UserInfoDTO();
        user.setId(id);
        user.setName("Barbeiro");
        user.setEmail("barber@cortaai.com");
        user.setUserType("BARBER");
        user.setBarbershopId(shopId);
        return user;
    }

    private Barbershop shop(UUID shopId, UUID ownerId) {
        Barbershop shop = new Barbershop();
        shop.setId(shopId);
        shop.setOwnerId(ownerId);
        shop.setName("Barbearia Teste");
        shop.setCnpj("11222333000181");
        shop.setAddress("Rua 1");
        return shop;
    }

    private Request dummyRequest() {
        return Request.create(
                Request.HttpMethod.GET,
                "http://user-service/test",
                Collections.emptyMap(),
                null,
                StandardCharsets.UTF_8,
                new RequestTemplate()
        );
    }
}
