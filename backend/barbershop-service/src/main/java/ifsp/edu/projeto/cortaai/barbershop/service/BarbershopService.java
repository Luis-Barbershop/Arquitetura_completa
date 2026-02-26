package ifsp.edu.projeto.cortaai.barbershop.service;

import ifsp.edu.projeto.cortaai.barbershop.dto.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public interface BarbershopService {

    // Barbershop CRUD
    List<BarbershopDTO> findAll();
    
    BarbershopDTO findById(UUID id);
    
    BarbershopDTO findByCnpj(String cnpj);
    
    BarbershopDTO findByOwnerId(UUID ownerId);
    
    BarbershopDTO create(CreateBarbershopDTO dto, UUID ownerId) throws IOException;
    
    BarbershopDTO create(CreateBarbershopDTO dto, UUID ownerId, MultipartFile logo) throws IOException;
    
    BarbershopDTO update(UUID id, UpdateBarbershopDTO dto, UUID requesterId);
    
    void delete(UUID id, UUID requesterId);

    // Image management
    String updateLogo(UUID barbershopId, MultipartFile file, UUID requesterId) throws IOException;
    
    String updateBanner(UUID barbershopId, MultipartFile file, UUID requesterId) throws IOException;
    
    String addHighlight(UUID barbershopId, MultipartFile file, UUID requesterId) throws IOException;
    
    void deleteHighlight(UUID barbershopId, UUID highlightId, UUID requesterId);
}
