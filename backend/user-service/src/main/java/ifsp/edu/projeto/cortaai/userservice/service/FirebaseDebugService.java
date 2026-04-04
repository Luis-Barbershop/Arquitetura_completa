package ifsp.edu.projeto.cortaai.userservice.service;

import ifsp.edu.projeto.cortaai.userservice.dto.ChangePasswordRequestDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.EmailExistsResponseDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.FirebaseEmailRegisterRequestDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.FirebaseEmailRegisterResponseDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.FirebaseEmailSignInRequestDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.FirebaseEmailSignInResponseDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.FirebaseTokenDebugResponseDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.ForgotPasswordRequestDTO;

public interface FirebaseDebugService {

    FirebaseEmailSignInResponseDTO signInWithEmailPassword(FirebaseEmailSignInRequestDTO request);

    FirebaseTokenDebugResponseDTO verifyToken(String idToken);

    /**
     * Cria um novo usuário no Firebase Authentication e completa o perfil no banco.
     * Fluxo: signUp → verify → complete-profile.
     */
    FirebaseEmailRegisterResponseDTO registerWithEmailPassword(FirebaseEmailRegisterRequestDTO request);

    /**
     * Envia e-mail de recuperação de senha via Firebase.
     * Chama accounts:sendOobCode com requestType=PASSWORD_RESET.
     */
    void forgotPassword(ForgotPasswordRequestDTO request);

    /**
     * Altera a senha do usuário autenticado via Firebase.
     * Requer o idToken válido da sessão atual.
     * Chama accounts:update com o novo password.
     */
    void changePassword(ChangePasswordRequestDTO request);

    /**
     * Verifica se um e-mail já está cadastrado em qualquer perfil (CUSTOMER ou BARBER).
     * Usado pelo front-end para o redirecionamento inteligente de cadastro.
     */
    EmailExistsResponseDTO checkEmailExists(String email);
}

