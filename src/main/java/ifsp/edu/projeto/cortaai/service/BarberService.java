package ifsp.edu.projeto.cortaai.service;

import ifsp.edu.projeto.cortaai.dto.BarberDTO;
import ifsp.edu.projeto.cortaai.dto.CustomerDTO;

import ifsp.edu.projeto.cortaai.dto.CreateBarberDTO;
import jakarta.validation.Valid;


import java.util.List;
import java.util.UUID;

public interface BarberService {

    List<BarberDTO> findAll();

    BarberDTO get(UUID id);

    void update(UUID id, BarberDTO barberDTO);

    void delete(UUID id);

    boolean tellExists(String tell);

    boolean emailExists(String email);

    boolean documentCPFExists(String documentCPF);

    UUID create(@Valid CreateBarberDTO createBarberDTO);

    List<CustomerDTO> findCustomerHistory(UUID id);

}