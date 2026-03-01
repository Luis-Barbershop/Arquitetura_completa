package ifsp.edu.projeto.cortaai.userservice.mapper;

import ifsp.edu.projeto.cortaai.userservice.dto.BarberDTO;
import ifsp.edu.projeto.cortaai.userservice.model.Barber;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BarberMapper {

    BarberDTO toDTO(Barber barber);
}