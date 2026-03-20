package ifsp.edu.projeto.cortaai.userservice.controller;

import ifsp.edu.projeto.cortaai.userservice.dto.FirebaseEmailSignInRequestDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.FirebaseEmailSignInResponseDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.FirebaseTokenDebugRequestDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.FirebaseTokenDebugResponseDTO;
import ifsp.edu.projeto.cortaai.userservice.service.FirebaseDebugService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/firebase-test")
@RequiredArgsConstructor
@Tag(name = "Firebase Test", description = "Endpoints para testar Firebase sem frontend/Postman")
public class FirebaseTestController {

	private final FirebaseDebugService firebaseDebugService;

	@Operation(
			summary = "Login Firebase por email/senha",
			description = "Usa o Identity Toolkit do Firebase para retornar idToken/refreshToken."
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Login realizado com sucesso"),
			@ApiResponse(responseCode = "400", description = "Corpo da requisição inválido"),
			@ApiResponse(responseCode = "401", description = "Credenciais inválidas")
	})
	@SecurityRequirements
	@PostMapping("/sign-in-email")
	public ResponseEntity<FirebaseEmailSignInResponseDTO> signInWithEmail(
			@RequestBody @Valid FirebaseEmailSignInRequestDTO request) {
		return ResponseEntity.ok(firebaseDebugService.signInWithEmailPassword(request));
	}

	@Operation(
			summary = "Verifica e decodifica um Firebase ID Token",
			description = "Valida o idToken no Firebase Admin SDK e retorna claims para depuracao."
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Token valido"),
			@ApiResponse(responseCode = "400", description = "Corpo da requisição inválido"),
			@ApiResponse(responseCode = "401", description = "Token inválido ou expirado")
	})
	@SecurityRequirements
	@PostMapping("/verify-id-token")
	public ResponseEntity<FirebaseTokenDebugResponseDTO> verifyIdToken(
			@RequestBody @Valid FirebaseTokenDebugRequestDTO request) {
		return ResponseEntity.ok(firebaseDebugService.verifyToken(request.idToken()));
	}
}

