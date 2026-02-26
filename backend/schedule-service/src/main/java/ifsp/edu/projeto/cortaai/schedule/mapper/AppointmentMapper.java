package ifsp.edu.projeto.cortaai.schedule.mapper;

import ifsp.edu.projeto.cortaai.schedule.dto.AppointmentDTO;
import ifsp.edu.projeto.cortaai.schedule.dto.CreateAppointmentDTO;
import ifsp.edu.projeto.cortaai.schedule.dto.UpdateAppointmentDTO;
import ifsp.edu.projeto.cortaai.schedule.model.Appointment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface AppointmentMapper {

    @Mapping(target = "activityIds", source = "activityIds")
    @Mapping(target = "barbershopName", ignore = true)
    @Mapping(target = "barberName", ignore = true)
    @Mapping(target = "customerName", ignore = true)
    AppointmentDTO toDTO(Appointment appointment);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customerId", ignore = true)
    @Mapping(target = "endTime", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "dateCreated", ignore = true)
    @Mapping(target = "lastUpdated", ignore = true)
    @Mapping(target = "activityIds", source = "activityIds")
    Appointment toEntity(CreateAppointmentDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "barbershopId", ignore = true)
    @Mapping(target = "customerId", ignore = true)
    @Mapping(target = "endTime", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "dateCreated", ignore = true)
    @Mapping(target = "lastUpdated", ignore = true)
    void updateEntityFromDTO(@MappingTarget Appointment appointment, UpdateAppointmentDTO dto);

    List<AppointmentDTO> toDTOList(List<Appointment> appointments);
}
