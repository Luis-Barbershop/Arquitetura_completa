package ifsp.edu.projeto.cortaai.barbershopservice.controller;

import ifsp.edu.projeto.cortaai.barbershopservice.dto.ActivityDTO;
import ifsp.edu.projeto.cortaai.barbershopservice.dto.AppointmentSummaryDTO;
import ifsp.edu.projeto.cortaai.barbershopservice.dto.BarberJoinRequestDTO;
import ifsp.edu.projeto.cortaai.barbershopservice.dto.BarberPublicDTO;
import ifsp.edu.projeto.cortaai.barbershopservice.dto.BarbershopDTO;
import ifsp.edu.projeto.cortaai.barbershopservice.dto.BarbershopPublicDTO;
import ifsp.edu.projeto.cortaai.barbershopservice.dto.BarbershopSummaryDTO;
import ifsp.edu.projeto.cortaai.barbershopservice.dto.CloseBarbershopRequestDTO;
import ifsp.edu.projeto.cortaai.barbershopservice.dto.CommissionRuleDTO;
import ifsp.edu.projeto.cortaai.barbershopservice.dto.CommissionRuleRequestDTO;
import ifsp.edu.projeto.cortaai.barbershopservice.dto.CreateActivityDTO;
import ifsp.edu.projeto.cortaai.barbershopservice.dto.CreateBarbershopDTO;
import ifsp.edu.projeto.cortaai.barbershopservice.dto.CreateBarbershopReviewDTO;
import ifsp.edu.projeto.cortaai.barbershopservice.dto.InviteBarberDTO;
import ifsp.edu.projeto.cortaai.barbershopservice.dto.JoinRequestDTO;
import ifsp.edu.projeto.cortaai.barbershopservice.dto.RemoveTeamMemberRequestDTO;
import ifsp.edu.projeto.cortaai.barbershopservice.dto.TeamMemberResponseDTO;
import ifsp.edu.projeto.cortaai.barbershopservice.dto.UpdateActivityDTO;
import ifsp.edu.projeto.cortaai.barbershopservice.dto.UpdateBarbershopDTO;
import ifsp.edu.projeto.cortaai.barbershopservice.service.BarbershopService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BarbershopControllerTest {

    private BarbershopService service;
    private BarbershopController controller;
    private Principal principal;

    @BeforeEach
    void setUp() {
        service = mock(BarbershopService.class);
        controller = new BarbershopController(service);
        principal = () -> "firebase-uid";
    }

    @Test
    void shouldListAllBarbershopsWithAndWithoutProximity() {
        BarbershopPublicDTO shop = publicBarbershop();
        BarbershopSummaryDTO summary = new BarbershopSummaryDTO(
                shop.getId(), "Shop", "Rua 1", "logo.png", 4.8, 11L, -23.5, -46.6, 1.25);
        when(service.listBarbershops()).thenReturn(List.of(shop));
        when(service.listBarbershopsByProximity(-23.5, -46.6, 3.0)).thenReturn(List.of(summary));

        assertThat(controller.listAllBarbershops(null, null, 10.0).getBody()).isEqualTo(List.of(shop));
        assertThat(controller.listAllBarbershops(-23.5, -46.6, 3.0).getBody()).isEqualTo(List.of(summary));
    }

    @Test
    void shouldDelegatePublicReadAndReviewEndpoints() {
        UUID shopId = UUID.randomUUID();
        ActivityDTO activity = activity();
        BarberPublicDTO barber = new BarberPublicDTO(UUID.randomUUID(), "Ana", "ana.png");
        when(service.getBarbershop(shopId)).thenReturn(publicBarbershop());
        when(service.listActivities(shopId)).thenReturn(List.of(activity));
        when(service.listBarbers(shopId)).thenReturn(List.of(barber));
        when(service.hasCustomerReviewed("firebase-uid", shopId)).thenReturn(true);
        CreateBarbershopReviewDTO review = new CreateBarbershopReviewDTO();

        assertThat(controller.getBarbershop(shopId).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.listActivities(shopId).getBody()).containsExactly(activity);
        assertThat(controller.listBarbers(shopId).getBody()).containsExactly(barber);
        assertThat(controller.hasMyReview(principal, shopId).getBody()).isEqualTo(Map.of("reviewed", true));
        assertThat(controller.createReview(principal, shopId, review).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);

        verify(service).createReview("firebase-uid", shopId, review);
    }

    @Test
    void shouldDelegateOwnerShopAndActivityCommands() throws IOException {
        UUID activityId = UUID.randomUUID();
        UUID barberId = UUID.randomUUID();
        MultipartFile file = mock(MultipartFile.class);
        CreateBarbershopDTO createShop = new CreateBarbershopDTO();
        UpdateBarbershopDTO updateShop = new UpdateBarbershopDTO();
        CreateActivityDTO createActivity = new CreateActivityDTO();
        UpdateActivityDTO updateActivity = new UpdateActivityDTO();
        when(service.createBarbershop("firebase-uid", createShop, file)).thenReturn(barbershop());
        when(service.updateBarbershop("firebase-uid", updateShop)).thenReturn(barbershop());
        when(service.createActivity("firebase-uid", createActivity)).thenReturn(activity());
        when(service.updateActivity("firebase-uid", activityId, updateActivity)).thenReturn(activity());

        assertThat(controller.createBarbershop(principal, createShop, file).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(controller.updateBarbershop(principal, updateShop).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.createActivity(principal, createActivity).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(controller.updateActivity(principal, activityId, updateActivity).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.deleteActivity(principal, activityId).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(controller.removeBarber(principal, barberId).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        verify(service).deleteActivity("firebase-uid", activityId);
        verify(service).removeBarber("firebase-uid", barberId);
    }

    @Test
    void shouldDelegateTeamEndpoints() {
        UUID barberId = UUID.randomUUID();
        UUID ruleId = UUID.randomUUID();
        CommissionRuleRequestDTO request = new CommissionRuleRequestDTO(UUID.randomUUID(), BigDecimal.valueOf(40));
        RemoveTeamMemberRequestDTO removeRequest = new RemoveTeamMemberRequestDTO("CANCEL", null);
        CloseBarbershopRequestDTO closeRequest = new CloseBarbershopRequestDTO();
        CommissionRuleDTO rule = commissionRule();
        TeamMemberResponseDTO member = new TeamMemberResponseDTO(
                barberId, "Ana", "ana.png", "ana@example.com", false,
                LocalTime.of(9, 0), LocalTime.of(18, 0), List.of(rule));
        AppointmentSummaryDTO conflict = new AppointmentSummaryDTO(
                UUID.randomUUID(), barberId, UUID.randomUUID(), UUID.randomUUID(),
                "Cliente", "Ana", LocalDateTime.now(), LocalDateTime.now().plusHours(1), "SCHEDULED");
        when(service.listTeamMembers("firebase-uid")).thenReturn(List.of(member));
        when(service.getCommissions("firebase-uid", barberId)).thenReturn(List.of(rule));
        when(service.upsertCommission("firebase-uid", barberId, request)).thenReturn(rule);
        when(service.getRemovalConflicts("firebase-uid", barberId)).thenReturn(List.of(conflict));

        assertThat(controller.listTeamMembers(principal).getBody()).containsExactly(member);
        assertThat(controller.getCommissions(principal, barberId).getBody()).containsExactly(rule);
        assertThat(controller.upsertCommission(principal, barberId, request).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(controller.deleteCommission(principal, barberId, ruleId).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(controller.getRemovalConflicts(principal, barberId).getBody()).containsExactly(conflict);
        assertThat(controller.removeTeamMember(principal, barberId, removeRequest).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(controller.closeBarbershop(principal, closeRequest).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        verify(service).deleteCommission("firebase-uid", barberId, ruleId);
        verify(service).removeTeamMember("firebase-uid", barberId, removeRequest);
        verify(service).closeBarbershop("firebase-uid", closeRequest);
    }

    @Test
    void shouldDelegateJoinRequestAndInviteEndpoints() {
        UUID requestId = UUID.randomUUID();
        BarberJoinRequestDTO joinRequest = new BarberJoinRequestDTO();
        joinRequest.setCnpj("11.222.333/0001-81");
        InviteBarberDTO invite = new InviteBarberDTO();
        invite.setCpf("123.456.789-09");
        JoinRequestDTO pending = new JoinRequestDTO();
        when(service.getPendingJoinRequests("firebase-uid")).thenReturn(List.of(pending));
        when(service.getMyPendingInvites("firebase-uid")).thenReturn(List.of(pending));

        assertThat(controller.requestToJoin(principal, joinRequest).getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(controller.getPendingRequests(principal).getBody()).containsExactly(pending);
        assertThat(controller.approveJoinRequest(principal, requestId).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(controller.rejectJoinRequest(principal, requestId).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(controller.leaveShop(principal).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(controller.inviteBarber(principal, invite).getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(controller.getMyInvites(principal).getBody()).containsExactly(pending);
        assertThat(controller.acceptInvite(principal, requestId).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(controller.rejectInvite(principal, requestId).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        verify(service).requestToJoinBarbershop("firebase-uid", "11.222.333/0001-81");
        verify(service).inviteBarberByCpf("firebase-uid", "123.456.789-09");
        verify(service).freeBarber("firebase-uid");
    }

    @Test
    void shouldHandleUploadSuccessAndFailureResponses() throws IOException {
        UUID activityId = UUID.randomUUID();
        UUID highlightId = UUID.randomUUID();
        MultipartFile file = mock(MultipartFile.class);
        when(service.updateBarbershopLogo("firebase-uid", file)).thenReturn("logo.png");
        when(service.updateBarbershopBanner("firebase-uid", file)).thenThrow(new IOException("storage down"));
        when(service.updateActivityPhoto("firebase-uid", activityId, file)).thenReturn("activity.png");
        when(service.addBarbershopHighlight("firebase-uid", file)).thenReturn("highlight.png");

        assertThat(controller.uploadLogo(principal, file).getBody()).isEqualTo("logo.png");
        assertThat(controller.uploadBanner(principal, file).getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(controller.uploadBanner(principal, file).getBody()).isEqualTo("Falha no upload: storage down");
        assertThat(controller.uploadActivityPhoto(principal, activityId, file).getBody()).isEqualTo("activity.png");
        assertThat(controller.addHighlight(principal, file).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(controller.deleteHighlight(principal, highlightId).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        verify(service).deleteBarbershopHighlight("firebase-uid", highlightId);
    }

    private BarbershopDTO barbershop() {
        BarbershopDTO dto = new BarbershopDTO();
        dto.setId(UUID.randomUUID());
        dto.setName("Shop");
        return dto;
    }

    private BarbershopPublicDTO publicBarbershop() {
        BarbershopPublicDTO dto = new BarbershopPublicDTO();
        dto.setId(UUID.randomUUID());
        dto.setName("Shop");
        dto.setAddress("Rua 1");
        dto.setLogoUrl("logo.png");
        return dto;
    }

    private ActivityDTO activity() {
        ActivityDTO dto = new ActivityDTO();
        dto.setId(UUID.randomUUID());
        dto.setActivityName("Corte");
        dto.setPrice(BigDecimal.valueOf(50));
        return dto;
    }

    private CommissionRuleDTO commissionRule() {
        return new CommissionRuleDTO(UUID.randomUUID(), UUID.randomUUID(), "Corte", BigDecimal.valueOf(40));
    }
}
