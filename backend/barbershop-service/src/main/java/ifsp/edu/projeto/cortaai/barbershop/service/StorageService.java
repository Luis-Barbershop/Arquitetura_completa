package ifsp.edu.projeto.cortaai.barbershop.service;

import ifsp.edu.projeto.cortaai.barbershop.dto.UploadResultDTO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface StorageService {

    UploadResultDTO upload(MultipartFile file, String folder) throws IOException;
    
    void delete(String publicId);
}
