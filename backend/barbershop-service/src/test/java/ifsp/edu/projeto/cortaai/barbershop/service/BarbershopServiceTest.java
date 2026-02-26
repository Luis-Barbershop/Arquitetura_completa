package ifsp.edu.projeto.cortaai.barbershop.service;

import ifsp.edu.projeto.cortaai.barbershop.dto.BarbershopDTO;
import ifsp.edu.projeto.cortaai.barbershop.dto.CreateBarbershopDTO;
import ifsp.edu.projeto.cortaai.barbershop.dto.UpdateBarbershopDTO;
import ifsp.edu.projeto.cortaai.barbershop.exception.DuplicateResourceException;
import ifsp.edu.projeto.cortaai.barbershop.exception.ForbiddenException;
import ifsp.edu.projeto.cortaai.barbershop.exception.NotFoundException;
import ifsp.edu.projeto.cortaai.barbershop.mapper.BarbershopMapper;
import ifsp.edu.projeto.cortaai.barbershop.model.Barbershop;
import ifsp.edu.projeto.cortaai.barbershop.repository.BarbershopHighlightRepository;
import ifsp.edu.projeto.cortaai.barbershop.repository.BarbershopRepository;
import ifsp.edu.projeto.cortaai.barbershop.service.impl.BarbershopServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BarbershopServiceTest {

    @Mock
    private BarbershopRepository barbershopRepository;

    @Mock
    private BarbershopHighlightRepository highlightRepository;

    @Mock
    private BarbershopMapper barbershopMapper;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private BarbershopServiceImpl barbershopService;

    private Barbershop barbershop;
    private BarbershopDTO barbershopDTO;
    private UUID ownerId;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        
        barbershop = new Barbershop();
        barbershop.setId(UUID.randomUUID());
        barbershop.setName("Barbearia Teste");
        barbershop.setCnpj("12345678901234");
        barbershop.setOwnerId(ownerId);

        barbershopDTO = new BarbershopDTO();
        barbershopDTO.setId(barbershop.getId());
        barbershopDTO.setName("Barbearia Teste");
        barbershopDTO.setCnpj("12345678901234");
        barbershopDTO.setOwnerId(ownerId);
    }

    @Test
    @DisplayName("Deve listar todas as barbearias")
    void shouldFindAll() {
        // given
        when(barbershopRepository.findAll()).thenReturn(List.of(barbershop));
        when(barbershopMapper.toDTOList(anyList())).thenReturn(List.of(barbershopDTO));

        // when
        List<BarbershopDTO> result = barbershopService.findAll();

        // then
        assertThat(result).hasSize(1);
        verify(barbershopRepository).findAll();
    }

    @Test
    @DisplayName("Deve encontrar barbearia por ID")
    void shouldFindById() {
        // given
        when(barbershopRepository.findById(barbershop.getId())).thenReturn(Optional.of(barbershop));
        when(barbershopMapper.toDTO(barbershop)).thenReturn(barbershopDTO);

        // when
        BarbershopDTO result = barbershopService.findById(barbershop.getId());

        // then
        assertThat(result.getName()).isEqualTo("Barbearia Teste");
    }

    @Test
    @DisplayName("Deve lançar exceção quando barbearia não encontrada")
    void shouldThrowNotFoundExceptionWhenBarbershopNotFound() {
        // given
        UUID id = UUID.randomUUID();
        when(barbershopRepository.findById(id)).thenReturn(Optional.empty());

        // when/then
        assertThatThrownBy(() -> barbershopService.findById(id))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Deve criar barbearia com sucesso")
    void shouldCreateBarbershop() throws IOException {
        // given
        CreateBarbershopDTO createDTO = new CreateBarbershopDTO();
        createDTO.setName("Nova Barbearia");
        createDTO.setCnpj("98765432109876");

        when(barbershopRepository.existsByCnpj(createDTO.getCnpj())).thenReturn(false);
        when(barbershopMapper.toEntity(createDTO)).thenReturn(barbershop);
        when(barbershopRepository.save(any(Barbershop.class))).thenReturn(barbershop);
        when(barbershopMapper.toDTO(barbershop)).thenReturn(barbershopDTO);

        // when
        BarbershopDTO result = barbershopService.create(createDTO, ownerId);

        // then
        assertThat(result).isNotNull();
        verify(barbershopRepository).save(any(Barbershop.class));
    }

    @Test
    @DisplayName("Deve lançar exceção quando CNPJ já existe")
    void shouldThrowExceptionWhenCnpjExists() {
        // given
        CreateBarbershopDTO createDTO = new CreateBarbershopDTO();
        createDTO.setCnpj("12345678901234");

        when(barbershopRepository.existsByCnpj(createDTO.getCnpj())).thenReturn(true);

        // when/then
        assertThatThrownBy(() -> barbershopService.create(createDTO, ownerId))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("Deve atualizar barbearia com sucesso")
    void shouldUpdateBarbershop() {
        // given
        UpdateBarbershopDTO updateDTO = new UpdateBarbershopDTO();
        updateDTO.setName("Nome Atualizado");

        when(barbershopRepository.findById(barbershop.getId())).thenReturn(Optional.of(barbershop));
        when(barbershopRepository.save(any(Barbershop.class))).thenReturn(barbershop);
        when(barbershopMapper.toDTO(barbershop)).thenReturn(barbershopDTO);

        // when
        BarbershopDTO result = barbershopService.update(barbershop.getId(), updateDTO, ownerId);

        // then
        assertThat(result).isNotNull();
        verify(barbershopMapper).updateEntityFromDTO(eq(barbershop), eq(updateDTO));
    }

    @Test
    @DisplayName("Deve lançar exceção quando usuário não é proprietário")
    void shouldThrowForbiddenWhenNotOwner() {
        // given
        UUID differentOwnerId = UUID.randomUUID();
        UpdateBarbershopDTO updateDTO = new UpdateBarbershopDTO();

        when(barbershopRepository.findById(barbershop.getId())).thenReturn(Optional.of(barbershop));

        // when/then
        assertThatThrownBy(() -> barbershopService.update(barbershop.getId(), updateDTO, differentOwnerId))
                .isInstanceOf(ForbiddenException.class);
    }
}
