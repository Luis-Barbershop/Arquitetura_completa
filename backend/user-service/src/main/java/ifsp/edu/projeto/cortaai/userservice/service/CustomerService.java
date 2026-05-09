package ifsp.edu.projeto.cortaai.userservice.service;

import ifsp.edu.projeto.cortaai.userservice.dto.CustomerDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.UpdateCustomerDTO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public interface CustomerService {

    List<CustomerDTO> findAll();

    CustomerDTO get(UUID id);

    /** Atualiza perfil identificando o customer pelo firebaseUid. */
    void updateByFirebaseUid(String firebaseUid, UpdateCustomerDTO dto);

    /** Exclui a conta do customer identificado pelo firebaseUid. */
    void deleteByFirebaseUid(String firebaseUid);

    /** Atualiza a foto de perfil do customer identificado pelo firebaseUid. */
    String updateProfilePhotoByFirebaseUid(String firebaseUid, MultipartFile file) throws IOException;

    List<UUID> listFavoriteBarbershopIdsByFirebaseUid(String firebaseUid);

    void addFavoriteBarbershopByFirebaseUid(String firebaseUid, UUID barbershopId);

    void removeFavoriteBarbershopByFirebaseUid(String firebaseUid, UUID barbershopId);

    // Métodos de validação
    boolean tellExists(String tell);
    boolean emailExists(String email);
    boolean documentCPFExists(String documentCPF);
}
