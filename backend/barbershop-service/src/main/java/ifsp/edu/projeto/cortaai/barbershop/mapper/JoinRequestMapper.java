package ifsp.edu.projeto.cortaai.barbershop.mapper;

import ifsp.edu.projeto.cortaai.barbershop.dto.JoinRequestDTO;
import ifsp.edu.projeto.cortaai.barbershop.model.BarbershopJoinRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface JoinRequestMapper {

    @Mapping(target = "barberName", ignore = true)
    @Mapping(target = "barberEmail", ignore = true)
    JoinRequestDTO toDTO(BarbershopJoinRequest joinRequest);

    List<JoinRequestDTO> toDTOList(List<BarbershopJoinRequest> joinRequests);
}
