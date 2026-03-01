package ifsp.edu.projeto.cortaai.barbershopservice.mapper;

import ifsp.edu.projeto.cortaai.barbershopservice.dto.ActivityDTO;
import ifsp.edu.projeto.cortaai.barbershopservice.dto.CreateActivityDTO;
import ifsp.edu.projeto.cortaai.barbershopservice.model.Activity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ActivityMapper {

    @Mapping(source = "barbershop.id", target = "barbershopId")
    ActivityDTO toDTO(Activity activity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "barbershop", ignore = true)
    @Mapping(target = "imageUrl", ignore = true)
    @Mapping(target = "imageUrlPublicId", ignore = true)
    @Mapping(target = "dateCreated", ignore = true)
    @Mapping(target = "lastUpdated", ignore = true)
    Activity toEntity(CreateActivityDTO dto);
}

