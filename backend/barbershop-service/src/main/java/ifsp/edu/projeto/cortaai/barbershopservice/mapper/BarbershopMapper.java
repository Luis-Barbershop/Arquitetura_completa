package ifsp.edu.projeto.cortaai.barbershopservice.mapper;

import ifsp.edu.projeto.cortaai.barbershopservice.dto.BarbershopDTO;
import ifsp.edu.projeto.cortaai.barbershopservice.dto.CreateBarbershopDTO;
import ifsp.edu.projeto.cortaai.barbershopservice.model.Barbershop;
import ifsp.edu.projeto.cortaai.barbershopservice.model.BarbershopHighlight;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface BarbershopMapper {

    @Mapping(source = "highlights", target = "highlightUrls")
    BarbershopDTO toDTO(Barbershop barbershop);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ownerId", ignore = true)
    @Mapping(target = "logoUrl", ignore = true)
    @Mapping(target = "logoUrlPublicId", ignore = true)
    @Mapping(target = "bannerUrl", ignore = true)
    @Mapping(target = "bannerUrlPublicId", ignore = true)
    @Mapping(target = "highlights", ignore = true)
    @Mapping(target = "activities", ignore = true)
    @Mapping(target = "joinRequests", ignore = true)
    @Mapping(target = "dateCreated", ignore = true)
    @Mapping(target = "lastUpdated", ignore = true)
    Barbershop toEntity(CreateBarbershopDTO dto);

    default List<String> mapHighlights(Set<BarbershopHighlight> highlights) {
        if (highlights == null || highlights.isEmpty()) {
            return List.of();
        }
        return highlights.stream()
                .map(BarbershopHighlight::getImageUrl)
                .collect(Collectors.toList());
    }
}

