package ifsp.edu.projeto.cortaai.schedule.mapper;

import ifsp.edu.projeto.cortaai.schedule.dto.BarberWorkHoursDTO;
import ifsp.edu.projeto.cortaai.schedule.dto.CreateBarberWorkHoursDTO;
import ifsp.edu.projeto.cortaai.schedule.model.BarberWorkHours;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BarberWorkHoursMapper {

    BarberWorkHoursDTO toDTO(BarberWorkHours workHours);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "barberId", ignore = true)
    @Mapping(target = "barbershopId", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "dateCreated", ignore = true)
    @Mapping(target = "lastUpdated", ignore = true)
    BarberWorkHours toEntity(CreateBarberWorkHoursDTO dto);

    List<BarberWorkHoursDTO> toDTOList(List<BarberWorkHours> workHours);
}
