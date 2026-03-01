package ifsp.edu.projeto.cortaai.userservice.service.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import ifsp.edu.projeto.cortaai.userservice.dto.UploadResultDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
public class CloudinaryStorageServiceImpl implements StorageService {

    private final Cloudinary cloudinary;

    public CloudinaryStorageServiceImpl(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    @Override
    public UploadResultDTO uploadFile(MultipartFile file, String folder) throws IOException {
        String publicId = folder + "/" + UUID.randomUUID().toString();

        Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(),
                ObjectUtils.asMap(
                        "public_id", publicId,
                        "folder", folder
                ));

        String secureUrl = (String) uploadResult.get("secure_url");
        String generatedPublicId = (String) uploadResult.get("public_id");

        return new UploadResultDTO(generatedPublicId, secureUrl);
    }

    @Override
    public void deleteFile(String publicId) throws IOException {
        if (publicId == null || publicId.isEmpty()) {
            return;
        }
        cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("invalidate", true));
    }
}

