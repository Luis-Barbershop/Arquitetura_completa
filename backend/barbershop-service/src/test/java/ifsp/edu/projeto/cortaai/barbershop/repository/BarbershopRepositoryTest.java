package ifsp.edu.projeto.cortaai.barbershop.repository;

import ifsp.edu.projeto.cortaai.barbershop.model.Barbershop;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class BarbershopRepositoryTest {

    @Autowired
    private BarbershopRepository barbershopRepository;

    @Test
    @DisplayName("Deve salvar uma barbearia com sucesso")
    void shouldSaveBarbershop() {
        // given
        Barbershop barbershop = new Barbershop();
        barbershop.setName("Barbearia Teste");
        barbershop.setCnpj("12345678901234");
        barbershop.setAddress("Rua Teste, 123");
        barbershop.setOwnerId(UUID.randomUUID());

        // when
        Barbershop saved = barbershopRepository.save(barbershop);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Barbearia Teste");
        assertThat(saved.getCnpj()).isEqualTo("12345678901234");
    }

    @Test
    @DisplayName("Deve encontrar barbearia por CNPJ")
    void shouldFindByCnpj() {
        // given
        Barbershop barbershop = new Barbershop();
        barbershop.setName("Barbearia CNPJ");
        barbershop.setCnpj("98765432109876");
        barbershop.setOwnerId(UUID.randomUUID());
        barbershopRepository.save(barbershop);

        // when
        Optional<Barbershop> found = barbershopRepository.findByCnpj("98765432109876");

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Barbearia CNPJ");
    }

    @Test
    @DisplayName("Deve encontrar barbearia por ownerId")
    void shouldFindByOwnerId() {
        // given
        UUID ownerId = UUID.randomUUID();
        Barbershop barbershop = new Barbershop();
        barbershop.setName("Barbearia Owner");
        barbershop.setCnpj("11111111111111");
        barbershop.setOwnerId(ownerId);
        barbershopRepository.save(barbershop);

        // when
        Optional<Barbershop> found = barbershopRepository.findByOwnerId(ownerId);

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getOwnerId()).isEqualTo(ownerId);
    }

    @Test
    @DisplayName("Deve verificar existência por CNPJ")
    void shouldCheckExistsByCnpj() {
        // given
        Barbershop barbershop = new Barbershop();
        barbershop.setName("Barbearia Exists");
        barbershop.setCnpj("22222222222222");
        barbershop.setOwnerId(UUID.randomUUID());
        barbershopRepository.save(barbershop);

        // when/then
        assertThat(barbershopRepository.existsByCnpj("22222222222222")).isTrue();
        assertThat(barbershopRepository.existsByCnpj("33333333333333")).isFalse();
    }
}
