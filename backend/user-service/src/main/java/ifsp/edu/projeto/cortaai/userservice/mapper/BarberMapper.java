package ifsp.edu.projeto.cortaai.userservice.mapper;

import ifsp.edu.projeto.cortaai.userservice.dto.BarberDTO;
import ifsp.edu.projeto.cortaai.userservice.model.Barber;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BarberMapper {

    // Ensina o MapStruct a mapear o ID aninhado
    @Mapping(source = "barbershop.id", target = "barbershopId")
    BarberDTO toDTO(Barber barber);

    // Ignora o mapeamento de volta do ID (é tratado pelo serviço)
    @Mapping(target = "barbershop", ignore = true)
    Barber toEntity(BarberDTO barberDTO);
}