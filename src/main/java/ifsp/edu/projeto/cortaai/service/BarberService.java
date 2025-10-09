package ifsp.edu.projeto.cortaai.service;

import ifsp.edu.projeto.cortaai.dto.BarberDTO;
import java.util.List;
import java.util.UUID;

public interface BarberService {

    List<BarberDTO> findAll();

    BarberDTO get(UUID id);

    UUID create(BarberDTO barberDTO);

    void update(UUID id, BarberDTO barberDTO);

    void delete(UUID id);

    boolean tellExists(String tell);

    boolean emailExists(String email);

    boolean documentCPFExists(String documentCPF);
}