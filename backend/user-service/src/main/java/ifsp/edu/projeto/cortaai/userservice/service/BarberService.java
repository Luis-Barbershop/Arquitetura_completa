package ifsp.edu.projeto.cortaai.userservice.service;

import ifsp.edu.projeto.cortaai.userservice.dto.*;
import jakarta.validation.Valid;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public interface BarberService {
    BarberDTO createBarber(CreateBarberDTO createBarberDTO);
    LoginResponseDTO login(LoginDTO loginDTO);
    BarberDTO update(Long id, UpdateBarberDTO updateBarberDTO);
    BarberDTO findById(Long id);
    List<BarberDTO> findAll();
}