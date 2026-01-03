package ifsp.edu.projeto.cortaai.userservice.mapper;

import ifsp.edu.projeto.cortaai.userservice.dto.CustomerDTO;
import ifsp.edu.projeto.cortaai.userservice.model.Customer;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring") // Define que é um mapper e o Spring deve gerenciá-lo
public interface CustomerMapper {

    CustomerDTO toDTO(Customer customer);

    Customer toEntity(CustomerDTO customerDTO);
}