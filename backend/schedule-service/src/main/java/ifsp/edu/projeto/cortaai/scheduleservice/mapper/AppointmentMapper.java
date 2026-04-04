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
        if (status == null) {
            return null;
        }
        if (status == ifsp.edu.projeto.cortaai.scheduleservice.model.enums.AppointmentStatus.CONCLUDED) {
            return ifsp.edu.projeto.cortaai.scheduleservice.model.enums.AppointmentStatus.COMPLETED.name();
        }
        return status.name();
    }
}

