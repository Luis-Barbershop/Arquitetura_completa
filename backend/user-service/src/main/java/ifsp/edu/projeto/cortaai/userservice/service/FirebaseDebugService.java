package ifsp.edu.projeto.cortaai.userservice.service;

import ifsp.edu.projeto.cortaai.userservice.dto.FirebaseEmailRegisterRequestDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.FirebaseEmailRegisterResponseDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.FirebaseEmailSignInRequestDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.FirebaseEmailSignInResponseDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.FirebaseTokenDebugResponseDTO;

public interface FirebaseDebugService {

    FirebaseEmailSignInResponseDTO signInWithEmailPassword(FirebaseEmailSignInRequestDTO request);

    FirebaseTokenDebugResponseDTO verifyToken(String idToken);

    /**
     * Cria um novo usuário no Firebase Authentication e completa o perfil no banco.
     * Fluxo: signUp → verify → complete-profile.
     */
    FirebaseEmailRegisterResponseDTO registerWithEmailPassword(FirebaseEmailRegisterRequestDTO request);
}

