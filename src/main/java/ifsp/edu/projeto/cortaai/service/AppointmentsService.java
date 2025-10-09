package ifsp.edu.projeto.cortaai.service;

import ifsp.edu.projeto.cortaai.dto.AppointmentsDTO;
import java.util.List;
import java.util.UUID;

public interface AppointmentsService {

    List<AppointmentsDTO> findAll();

    AppointmentsDTO get(Long id);

    Long create(AppointmentsDTO appointmentsDTO);

    void update(Long id, AppointmentsDTO appointmentsDTO);

    void delete(Long id);
}