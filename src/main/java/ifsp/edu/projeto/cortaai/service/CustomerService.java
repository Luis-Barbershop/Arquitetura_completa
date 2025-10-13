package ifsp.edu.projeto.cortaai.service;

import ifsp.edu.projeto.cortaai.dto.CustomerCreateDTO;
import ifsp.edu.projeto.cortaai.dto.CustomerDTO;
import ifsp.edu.projeto.cortaai.dto.LoginDTO; // Importe o LoginDTO
import java.util.List;
import java.util.UUID;

public interface CustomerService {

    List<CustomerDTO> findAll();

    CustomerDTO get(UUID id);

    UUID create(CustomerCreateDTO customerCreateDTO);

    void update(UUID id, CustomerDTO customerDTO);

    void delete(UUID id);

    boolean tellExists(String tell);

    boolean emailExists(String email);

    boolean documentCPFExists(String documentCPF);

    boolean previousAppointmentExists(String previousAppointment);

    CustomerDTO login(LoginDTO loginDTO); // Adicione esta linha
}