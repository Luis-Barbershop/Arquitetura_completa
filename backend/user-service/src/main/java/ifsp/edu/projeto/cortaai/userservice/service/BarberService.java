package ifsp.edu.projeto.cortaai.userservice.service;

import ifsp.edu.projeto.cortaai.userservice.dto.*;
import java.util.List;
import java.util.UUID;

public interface BarberService {
    BarberDTO createBarber(CreateBarberDTO createBarberDTO);
    LoginResponseDTO login(LoginDTO loginDTO);
    BarberDTO update(UUID id, UpdateBarberDTO dto);
    BarberDTO findById(UUID id);
    List<BarberDTO> findAll();
    List<BarberDTO> findByBarbershopId(UUID barbershopId);
}