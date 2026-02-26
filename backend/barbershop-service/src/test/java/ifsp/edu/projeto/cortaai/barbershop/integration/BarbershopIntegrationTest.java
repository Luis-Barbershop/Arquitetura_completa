package ifsp.edu.projeto.cortaai.barbershop.integration;

import ifsp.edu.projeto.cortaai.barbershop.model.Barbershop;
import ifsp.edu.projeto.cortaai.barbershop.repository.BarbershopRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BarbershopIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BarbershopRepository barbershopRepository;

    @Test
    @DisplayName("GET /api/barbershops - Integração: Lista barbearias")
    void shouldListBarbershopsIntegration() throws Exception {
        // given
        barbershopRepository.deleteAll();

        Barbershop barbershop = new Barbershop();
        barbershop.setName("Barbearia Integração");
        barbershop.setCnpj("11111111111111");
        barbershop.setOwnerId(UUID.randomUUID());
        barbershopRepository.save(barbershop);

        // when/then
        mockMvc.perform(get("/api/barbershops")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Barbearia Integração"));
    }

    @Test
    @DisplayName("GET /api/barbershops/{id} - Integração: Busca por ID")
    void shouldFindByIdIntegration() throws Exception {
        // given
        Barbershop barbershop = new Barbershop();
        barbershop.setName("Barbearia Por ID");
        barbershop.setCnpj("22222222222222");
        barbershop.setOwnerId(UUID.randomUUID());
        barbershop = barbershopRepository.save(barbershop);

        // when/then
        mockMvc.perform(get("/api/barbershops/{id}", barbershop.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Barbearia Por ID"));
    }

    @Test
    @DisplayName("GET /api/barbershops/{id} - Integração: 404 quando não encontrado")
    void shouldReturn404WhenNotFound() throws Exception {
        mockMvc.perform(get("/api/barbershops/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
