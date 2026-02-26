package ifsp.edu.projeto.cortaai.barbershop.repository;

import ifsp.edu.projeto.cortaai.barbershop.model.Activity;
import ifsp.edu.projeto.cortaai.barbershop.model.Barbershop;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ActivityRepositoryTest {

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private BarbershopRepository barbershopRepository;

    private Barbershop barbershop;

    @BeforeEach
    void setUp() {
        barbershop = new Barbershop();
        barbershop.setName("Barbearia Teste");
        barbershop.setCnpj("12345678901234");
        barbershop.setOwnerId(UUID.randomUUID());
        barbershop = barbershopRepository.save(barbershop);
    }

    @Test
    @DisplayName("Deve salvar uma atividade com sucesso")
    void shouldSaveActivity() {
        // given
        Activity activity = new Activity();
        activity.setActivityName("Corte Masculino");
        activity.setPrice(BigDecimal.valueOf(35.00));
        activity.setDurationMinutes(30);
        activity.setBarbershop(barbershop);

        // when
        Activity saved = activityRepository.save(activity);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getActivityName()).isEqualTo("Corte Masculino");
        assertThat(saved.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(35.00));
    }

    @Test
    @DisplayName("Deve encontrar atividades por barbearia")
    void shouldFindByBarbershopId() {
        // given
        Activity activity1 = new Activity();
        activity1.setActivityName("Corte");
        activity1.setPrice(BigDecimal.valueOf(30.00));
        activity1.setDurationMinutes(30);
        activity1.setBarbershop(barbershop);

        Activity activity2 = new Activity();
        activity2.setActivityName("Barba");
        activity2.setPrice(BigDecimal.valueOf(20.00));
        activity2.setDurationMinutes(20);
        activity2.setBarbershop(barbershop);

        activityRepository.saveAll(List.of(activity1, activity2));

        // when
        List<Activity> activities = activityRepository.findByBarbershopId(barbershop.getId());

        // then
        assertThat(activities).hasSize(2);
    }
}
