package ifsp.edu.projeto.cortaai.userservice.service;

import ifsp.edu.projeto.cortaai.dto.CustomerCreateDTO;
import ifsp.edu.projeto.cortaai.dto.CustomerDTO;
import ifsp.edu.projeto.cortaai.dto.LoginDTO;
import ifsp.edu.projeto.cortaai.dto.LoginResponseDTO; // NOVO IMPORT
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public interface CustomerService {

    List<CustomerDTO> findAll();

    CustomerDTO get(UUID id);

    UUID create(CustomerCreateDTO customerCreateDTO, MultipartFile file) throws IOException;

    void update(String email, CustomerDTO customerDTO);

    void delete(String email);

    LoginResponseDTO login(LoginDTO loginDTO); // TIPO DE RETORNO ALTERADO

    // --- Métodos de validação ---
    boolean tellExists(String tell);

    boolean emailExists(String email);

    boolean documentCPFExists(String documentCPF);

    // NOVO MÉTODO
    String updateProfilePhoto(String email, MultipartFile file) throws IOException;

}