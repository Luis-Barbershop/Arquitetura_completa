package ifsp.edu.projeto.cortaai.userservice.service.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import ifsp.edu.projeto.cortaai.userservice.dto.UploadResultDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CloudinaryStorageServiceImplTest {

    @Mock
    private Cloudinary cloudinary;
    @Mock
    private Uploader uploader;
    @Mock
    private MultipartFile file;

    private CloudinaryStorageServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CloudinaryStorageServiceImpl(cloudinary);
    }

    @Test
    void shouldUploadFileReturningGeneratedPublicIdAndSecureUrl() throws Exception {
        byte[] bytes = "image".getBytes();
        when(file.getBytes()).thenReturn(bytes);
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(org.mockito.ArgumentMatchers.eq(bytes), org.mockito.ArgumentMatchers.anyMap()))
                .thenReturn(Map.of("secure_url", "https://cdn/image.png", "public_id", "folder/generated-id"));

        UploadResultDTO result = service.uploadFile(file, "folder");

        assertThat(result.getPublicId()).isEqualTo("folder/generated-id");
        assertThat(result.getSecureUrl()).isEqualTo("https://cdn/image.png");
    }

    @Test
    void shouldDeleteFileOnlyWhenPublicIdIsPresent() throws Exception {
        when(cloudinary.uploader()).thenReturn(uploader);
        ArgumentCaptor<Map> optionsCaptor = ArgumentCaptor.forClass(Map.class);

        service.deleteFile("public-id");
        service.deleteFile("");
        service.deleteFile(null);

        verify(uploader).destroy(org.mockito.ArgumentMatchers.eq("public-id"), optionsCaptor.capture());
        assertThat(optionsCaptor.getValue()).containsEntry("invalidate", true);
        verify(cloudinary, org.mockito.Mockito.times(1)).uploader();
        verify(uploader, never()).destroy(org.mockito.ArgumentMatchers.eq(""), org.mockito.ArgumentMatchers.anyMap());
    }
}
