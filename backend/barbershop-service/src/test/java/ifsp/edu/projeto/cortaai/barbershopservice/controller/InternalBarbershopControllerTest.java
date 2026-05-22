package ifsp.edu.projeto.cortaai.barbershopservice.controller;

import ifsp.edu.projeto.cortaai.barbershopservice.model.Activity;
import ifsp.edu.projeto.cortaai.barbershopservice.model.BarberCommissionRule;
import ifsp.edu.projeto.cortaai.barbershopservice.model.Barbershop;
import ifsp.edu.projeto.cortaai.barbershopservice.repository.ActivityRepository;
import ifsp.edu.projeto.cortaai.barbershopservice.repository.BarberCommissionRuleRepository;
import ifsp.edu.projeto.cortaai.barbershopservice.repository.BarbershopRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InternalBarbershopControllerTest {

    private BarbershopRepository barbershopRepository;
    private ActivityRepository activityRepository;
    private BarberCommissionRuleRepository commissionRuleRepository;
    private InternalBarbershopController controller;

    @BeforeEach
    void setUp() {
        barbershopRepository = mock(BarbershopRepository.class);
        activityRepository = mock(ActivityRepository.class);
        commissionRuleRepository = mock(BarberCommissionRuleRepository.class);
        controller = new InternalBarbershopController(
                barbershopRepository,
                activityRepository,
                commissionRuleRepository
        );
    }

    @Test
    void shouldReturnInternalBarbershopInfoWhenFound() {
        UUID shopId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Barbershop shop = new Barbershop();
        shop.setId(shopId);
        shop.setOwnerId(ownerId);
        shop.setName("Shop");
        shop.setCnpj("11222333000181");
        shop.setAddress("Rua 1");
        when(barbershopRepository.findById(shopId)).thenReturn(Optional.of(shop));

        var response = controller.getBarbershopById(shopId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().id()).isEqualTo(shopId);
        assertThat(response.getBody().ownerId()).isEqualTo(ownerId);
        assertThat(response.getBody().name()).isEqualTo("Shop");
    }

    @Test
    void shouldReturnNotFoundWhenInternalBarbershopDoesNotExist() {
        UUID shopId = UUID.randomUUID();
        when(barbershopRepository.findById(shopId)).thenReturn(Optional.empty());

        assertThat(controller.getBarbershopById(shopId).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldFilterActivitiesByRequestedIds() {
        UUID shopId = UUID.randomUUID();
        Activity included = activity("Corte", shopId);
        Activity excluded = activity("Barba", shopId);
        when(activityRepository.findByBarbershopId(shopId)).thenReturn(List.of(included, excluded));

        var response = controller.getActivitiesByIds(shopId, List.of(included.getId()));

        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).id()).isEqualTo(included.getId());
        assertThat(response.getBody().get(0).barbershopId()).isEqualTo(shopId);
    }

    @Test
    void shouldReturnAllActivities() {
        UUID shopId = UUID.randomUUID();
        Activity corte = activity("Corte", shopId);
        Activity barba = activity("Barba", shopId);
        when(activityRepository.findByBarbershopId(shopId)).thenReturn(List.of(corte, barba));

        assertThat(controller.getAllActivities(shopId).getBody())
                .extracting("activityName")
                .containsExactly("Corte", "Barba");
    }

    @Test
    void shouldReturnBarberCommissionRules() {
        UUID shopId = UUID.randomUUID();
        UUID barberId = UUID.randomUUID();
        Activity activity = activity("Corte", shopId);
        BarberCommissionRule rule = new BarberCommissionRule();
        rule.setId(UUID.randomUUID());
        rule.setActivity(activity);
        rule.setPercentage(BigDecimal.valueOf(45));
        when(commissionRuleRepository.findByBarbershopIdAndBarberId(shopId, barberId)).thenReturn(List.of(rule));

        var response = controller.getBarberCommissions(shopId, barberId);

        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).activityId()).isEqualTo(activity.getId());
        assertThat(response.getBody().get(0).activityName()).isEqualTo("Corte");
        assertThat(response.getBody().get(0).percentage()).isEqualByComparingTo("45");
    }

    private Activity activity(String name, UUID shopId) {
        Barbershop shop = new Barbershop();
        shop.setId(shopId);
        Activity activity = new Activity();
        activity.setId(UUID.randomUUID());
        activity.setActivityName(name);
        activity.setPrice(BigDecimal.valueOf(50));
        activity.setDurationMinutes(45);
        activity.setBarbershop(shop);
        return activity;
    }
}
