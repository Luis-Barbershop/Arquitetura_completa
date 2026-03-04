package ifsp.edu.projeto.cortaai.userservice.service;

import ifsp.edu.projeto.cortaai.userservice.dto.BarberDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.UpdateBarberDTO;

import java.util.List;
import java.util.UUID;

public interface BarberService {
    BarberDTO update(UUID id, UpdateBarberDTO dto);
    BarberDTO findById(UUID id);
    BarberDTO get(UUID id);
    List<BarberDTO> findAll();
    List<BarberDTO> findByBarbershopId(UUID barbershopId);

    // Validações
    boolean emailExists(String email);
    boolean documentCPFExists(String documentCPF);
    boolean tellExists(String tell);
}
