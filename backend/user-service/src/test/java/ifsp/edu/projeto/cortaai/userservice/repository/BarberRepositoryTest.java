package ifsp.edu.projeto.cortaai.userservice.repository;

import ifsp.edu.projeto.cortaai.userservice.model.Barber;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class BarberRepositoryTest {

    @Autowired
    private BarberRepository barberRepository;

    @Test
    @DisplayName("Deve salvar um barbeiro com sucesso")
    void shouldSaveBarber() {
        // given
        Barber barber = new Barber();
        barber.setName("João Barbeiro");
        barber.setTell("11999998888");
        barber.setEmail("joao@email.com");
        barber.setDocumentCPF("12345678901");
        barber.setPassword("senha123");

        // when
        Barber saved = barberRepository.save(barber);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("João Barbeiro");
    }

    @Test
    @DisplayName("Deve encontrar barbeiro por email")
    void shouldFindByEmail() {
        // given
        Barber barber = new Barber();
        barber.setName("Maria Barbeira");
        barber.setTell("11999997777");
        barber.setEmail("maria@email.com");
        barber.setDocumentCPF("98765432109");
        barber.setPassword("senha123");
        barberRepository.save(barber);

        // when
        Optional<Barber> found = barberRepository.findByEmail("maria@email.com");

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Maria Barbeira");
    }

    @Test
    @DisplayName("Deve verificar existência por email")
    void shouldCheckExistsByEmail() {
        // given
        Barber barber = new Barber();
        barber.setName("Pedro Barbeiro");
        barber.setTell("11999996666");
        barber.setEmail("pedro@email.com");
        barber.setDocumentCPF("11122233344");
        barber.setPassword("senha123");
        barberRepository.save(barber);

        // when/then
        assertThat(barberRepository.existsByEmail("pedro@email.com")).isTrue();
        assertThat(barberRepository.existsByEmail("outro@email.com")).isFalse();
    }
}
