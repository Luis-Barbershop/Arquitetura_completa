package ifsp.edu.projeto.cortaai.userservice.mapper;

import ifsp.edu.projeto.cortaai.userservice.dto.CustomerDTO;
import ifsp.edu.projeto.cortaai.userservice.model.Customer;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MapperTest {

    private final CustomerMapper customerMapper = Mappers.getMapper(CustomerMapper.class);

    @Test
    void shouldMapCustomerDtoToEntityAndBack() {
        UUID id = UUID.randomUUID();
        CustomerDTO dto = new CustomerDTO();
        dto.setId(id);
        dto.setName("Ana");
        dto.setEmail("ana@example.com");
        dto.setTell("11999999999");
        dto.setDocumentCPF("12345678909");
        dto.setBirthDate(LocalDate.of(1990, 1, 1));
        dto.setImageUrl("https://cdn/customer.png");

        Customer entity = customerMapper.toEntity(dto);
        CustomerDTO mappedBack = customerMapper.toDTO(entity);

        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getName()).isEqualTo("Ana");
        assertThat(entity.getEmail()).isEqualTo("ana@example.com");
        assertThat(mappedBack.getImageUrl()).isEqualTo("https://cdn/customer.png");
    }
}
