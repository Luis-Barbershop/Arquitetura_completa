package ifsp.edu.projeto.cortaai.userservice.service;

import ifsp.edu.projeto.cortaai.userservice.dto.FirebaseEmailSignInRequestDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.FirebaseEmailSignInResponseDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.FirebaseTokenDebugResponseDTO;

public interface FirebaseDebugService {

    FirebaseEmailSignInResponseDTO signInWithEmailPassword(FirebaseEmailSignInRequestDTO request);

    FirebaseTokenDebugResponseDTO verifyToken(String idToken);
}

