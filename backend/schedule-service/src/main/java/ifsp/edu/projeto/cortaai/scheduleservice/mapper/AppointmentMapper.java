package ifsp.edu.projeto.cortaai.scheduleservice.mapper;

import ifsp.edu.projeto.cortaai.scheduleservice.dto.AppointmentActivityDTO;
import ifsp.edu.projeto.cortaai.scheduleservice.dto.AppointmentDTO;
import ifsp.edu.projeto.cortaai.scheduleservice.dto.BarberBlockDTO;
import ifsp.edu.projeto.cortaai.scheduleservice.model.Appointment;
import ifsp.edu.projeto.cortaai.scheduleservice.model.AppointmentActivity;
import ifsp.edu.projeto.cortaai.scheduleservice.model.BarberBlock;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface AppointmentMapper {

    @Mapping(source = "status", target = "status")
    AppointmentDTO toDTO(Appointment appointment);

    AppointmentActivityDTO toActivityDTO(AppointmentActivity activity);

    List<AppointmentActivityDTO> toActivityDTOList(Set<AppointmentActivity> activities);

    BarberBlockDTO toBlockDTO(BarberBlock block);

    List<BarberBlockDTO> toBlockDTOList(List<BarberBlock> blocks);

    default String mapStatus(ifsp.edu.projeto.cortaai.scheduleservice.model.enums.AppointmentStatus status) {
        return status != null ? status.name() : null;
    }
}

