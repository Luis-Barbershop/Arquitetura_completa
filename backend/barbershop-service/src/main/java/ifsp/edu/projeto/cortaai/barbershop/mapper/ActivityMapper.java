package ifsp.edu.projeto.cortaai.barbershop.mapper;

import ifsp.edu.projeto.cortaai.barbershop.dto.ActivityDTO;
import ifsp.edu.projeto.cortaai.barbershop.dto.CreateActivityDTO;
import ifsp.edu.projeto.cortaai.barbershop.dto.UpdateActivityDTO;
import ifsp.edu.projeto.cortaai.barbershop.model.Activity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ActivityMapper {

    @Mapping(target = "barbershopId", source = "barbershop.id")
    ActivityDTO toDTO(Activity activity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "barbershop", ignore = true)
    @Mapping(target = "imageUrl", ignore = true)
    @Mapping(target = "imageUrlPublicId", ignore = true)
    @Mapping(target = "dateCreated", ignore = true)
    @Mapping(target = "lastUpdated", ignore = true)
    Activity toEntity(CreateActivityDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "barbershop", ignore = true)
    @Mapping(target = "imageUrl", ignore = true)
    @Mapping(target = "imageUrlPublicId", ignore = true)
    @Mapping(target = "dateCreated", ignore = true)
    @Mapping(target = "lastUpdated", ignore = true)
    void updateEntityFromDTO(@MappingTarget Activity activity, UpdateActivityDTO dto);

    List<ActivityDTO> toDTOList(List<Activity> activities);
}
