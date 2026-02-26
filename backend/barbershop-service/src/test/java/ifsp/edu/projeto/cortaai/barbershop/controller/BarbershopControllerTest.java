package ifsp.edu.projeto.cortaai.barbershop.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import ifsp.edu.projeto.cortaai.barbershop.dto.BarbershopDTO;
import ifsp.edu.projeto.cortaai.barbershop.dto.CreateBarbershopDTO;
import ifsp.edu.projeto.cortaai.barbershop.service.ActivityService;
import ifsp.edu.projeto.cortaai.barbershop.service.BarbershopService;
import ifsp.edu.projeto.cortaai.barbershop.service.JoinRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BarbershopController.class)
class BarbershopControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BarbershopService barbershopService;

    @MockBean
    private ActivityService activityService;

    @MockBean
    private JoinRequestService joinRequestService;

    private BarbershopDTO barbershopDTO;
    private UUID barbershopId;
    private UUID ownerId;

    @BeforeEach
    void setUp() {
        barbershopId = UUID.randomUUID();
        ownerId = UUID.randomUUID();

        barbershopDTO = new BarbershopDTO();
        barbershopDTO.setId(barbershopId);
        barbershopDTO.setName("Barbearia Teste");
        barbershopDTO.setCnpj("12345678901234");
        barbershopDTO.setOwnerId(ownerId);
    }

    @Test
    @DisplayName("GET /api/barbershops - Deve listar todas as barbearias")
    void shouldListAllBarbershops() throws Exception {
        when(barbershopService.findAll()).thenReturn(List.of(barbershopDTO));

        mockMvc.perform(get("/api/barbershops"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Barbearia Teste"))
                .andExpect(jsonPath("$[0].cnpj").value("12345678901234"));
    }

    @Test
    @DisplayName("GET /api/barbershops/{id} - Deve buscar barbearia por ID")
    void shouldFindBarbershopById() throws Exception {
        when(barbershopService.findById(barbershopId)).thenReturn(barbershopDTO);

        mockMvc.perform(get("/api/barbershops/{id}", barbershopId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Barbearia Teste"));
    }

    @Test
    @DisplayName("POST /api/barbershops - Deve criar barbearia")
    void shouldCreateBarbershop() throws Exception {
        CreateBarbershopDTO createDTO = new CreateBarbershopDTO();
        createDTO.setName("Nova Barbearia");
        createDTO.setCnpj("12345678901234");
        createDTO.setAddress("Rua Teste, 123");

        MockMultipartFile shopPart = new MockMultipartFile(
                "shop",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(createDTO)
        );

        when(barbershopService.create(any(CreateBarbershopDTO.class), eq(ownerId), any()))
                .thenReturn(barbershopDTO);

        mockMvc.perform(multipart("/api/barbershops")
                        .file(shopPart)
                        .header("X-User-Id", ownerId.toString())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Barbearia Teste"));
    }

    @Test
    @DisplayName("DELETE /api/barbershops/{id} - Deve deletar barbearia")
    void shouldDeleteBarbershop() throws Exception {
        mockMvc.perform(delete("/api/barbershops/{id}", barbershopId)
                        .header("X-User-Id", ownerId.toString()))
                .andExpect(status().isNoContent());
    }
}
