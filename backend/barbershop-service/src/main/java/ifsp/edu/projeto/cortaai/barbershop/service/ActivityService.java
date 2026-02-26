package ifsp.edu.projeto.cortaai.barbershop.service;

import ifsp.edu.projeto.cortaai.barbershop.dto.ActivityDTO;
import ifsp.edu.projeto.cortaai.barbershop.dto.CreateActivityDTO;
import ifsp.edu.projeto.cortaai.barbershop.dto.UpdateActivityDTO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public interface ActivityService {

    List<ActivityDTO> findByBarbershopId(UUID barbershopId);
    
    ActivityDTO findById(UUID id);
    
    ActivityDTO create(UUID barbershopId, CreateActivityDTO dto, UUID requesterId);
    
    ActivityDTO update(UUID activityId, UpdateActivityDTO dto, UUID requesterId);
    
    void delete(UUID activityId, UUID requesterId);
    
    String updatePhoto(UUID activityId, MultipartFile file, UUID requesterId) throws IOException;
}
