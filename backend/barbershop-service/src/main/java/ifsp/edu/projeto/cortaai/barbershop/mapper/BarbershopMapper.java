package ifsp.edu.projeto.cortaai.barbershop.mapper;

import ifsp.edu.projeto.cortaai.barbershop.dto.BarbershopDTO;
import ifsp.edu.projeto.cortaai.barbershop.dto.CreateBarbershopDTO;
import ifsp.edu.projeto.cortaai.barbershop.model.Barbershop;
import ifsp.edu.projeto.cortaai.barbershop.model.BarbershopHighlight;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface BarbershopMapper {

    @Mapping(target = "highlightUrls", expression = "java(mapHighlights(barbershop.getHighlights()))")
    BarbershopDTO toDTO(Barbershop barbershop);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ownerId", ignore = true)
    @Mapping(target = "logoUrl", ignore = true)
    @Mapping(target = "logoUrlPublicId", ignore = true)
    @Mapping(target = "bannerUrl", ignore = true)
    @Mapping(target = "bannerUrlPublicId", ignore = true)
    @Mapping(target = "dateCreated", ignore = true)
    @Mapping(target = "lastUpdated", ignore = true)
    @Mapping(target = "activities", ignore = true)
    @Mapping(target = "highlights", ignore = true)
    @Mapping(target = "joinRequests", ignore = true)
    Barbershop toEntity(CreateBarbershopDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "cnpj", ignore = true)
    @Mapping(target = "ownerId", ignore = true)
    @Mapping(target = "logoUrl", ignore = true)
    @Mapping(target = "logoUrlPublicId", ignore = true)
    @Mapping(target = "bannerUrl", ignore = true)
    @Mapping(target = "bannerUrlPublicId", ignore = true)
    @Mapping(target = "dateCreated", ignore = true)
    @Mapping(target = "lastUpdated", ignore = true)
    @Mapping(target = "activities", ignore = true)
    @Mapping(target = "highlights", ignore = true)
    @Mapping(target = "joinRequests", ignore = true)
    void updateEntityFromDTO(@MappingTarget Barbershop barbershop, ifsp.edu.projeto.cortaai.barbershop.dto.UpdateBarbershopDTO dto);

    List<BarbershopDTO> toDTOList(List<Barbershop> barbershops);

    default List<String> mapHighlights(Set<BarbershopHighlight> highlights) {
        if (highlights == null) {
            return List.of();
        }
        return highlights.stream()
                .map(BarbershopHighlight::getImageUrl)
                .collect(Collectors.toList());
    }
}
