package ifsp.edu.projeto.cortaai.barbershopservice.service.storage;

import ifsp.edu.projeto.cortaai.barbershopservice.dto.UploadResultDTO;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface StorageService {
    UploadResultDTO uploadFile(MultipartFile file, String folder) throws IOException;
    void deleteFile(String publicId) throws IOException;
}

