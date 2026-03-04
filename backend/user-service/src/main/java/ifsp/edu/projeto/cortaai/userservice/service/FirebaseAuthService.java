package ifsp.edu.projeto.cortaai.userservice.service;

import ifsp.edu.projeto.cortaai.userservice.dto.AuthResponseDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.CompleteProfileBarberDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.CompleteProfileCustomerDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.FirebaseAuthRequestDTO;

/**
 * Serviço de autenticação Firebase para o user-service.
 *
 * <p>Responsável por:
 * <ol>
 *   <li>Verificar o Firebase ID Token recebido do cliente;</li>
 *   <li>Auto-provisionar o usuário na base local na primeira autenticação;</li>
 *   <li>Retornar um {@link AuthResponseDTO} com os dados do usuário e o status do perfil.</li>
 * </ol>
 */
public interface FirebaseAuthService {

    /**
     * Valida o Firebase ID Token e auto-provisiona o usuário se ainda não existir.
     *
     * @param request contém o {@code idToken} e o {@code userType} (CUSTOMER | BARBER)
     * @return dados do usuário + indicação se o perfil está completo
     */
    AuthResponseDTO verifyAndProvision(FirebaseAuthRequestDTO request);

    /**
     * Completa o perfil de um customer após login social.
     *
     * @param firebaseUid UID do Firebase (extraído do header X-User-UID)
     * @param dto         dados complementares
     * @return perfil atualizado
     */
    AuthResponseDTO completeCustomerProfile(String firebaseUid, CompleteProfileCustomerDTO dto);

    /**
     * Completa o perfil de um barbeiro após login social.
     *
     * @param firebaseUid UID do Firebase (extraído do header X-User-UID)
     * @param dto         dados complementares
     * @return perfil atualizado
     */
    AuthResponseDTO completeBarberProfile(String firebaseUid, CompleteProfileBarberDTO dto);

    /**
     * Retorna os dados do usuário a partir do UID do Firebase.
     *
     * @param firebaseUid UID do Firebase (extraído do header X-User-UID injetado pelo Gateway)
     * @return perfil do usuário
     */
    AuthResponseDTO getMe(String firebaseUid);
}
