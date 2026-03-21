package ifsp.edu.projeto.cortaai.userservice.controller;

import ifsp.edu.projeto.cortaai.userservice.dto.FirebaseEmailRegisterRequestDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.FirebaseEmailRegisterResponseDTO;
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

	@Operation(
			summary = "Cadastro Firebase por email/senha + complete-profile",
			description = """
					Fluxo completo de cadastro para testes via Swagger:
					1. Cria o usuário no Firebase Authentication (signUp).
					2. Provisiona no backend via `/api/auth/verify`.
					3. Completa o perfil via `/api/auth/{customers|barbers}/complete-profile`.

					**Campos obrigatórios para CUSTOMER**: email, password, userType="CUSTOMER", name, tell, documentCPF.

					**Campos obrigatórios para BARBER**: todos acima + workStartTime (HH:mm), workEndTime (HH:mm).
					isOwner é opcional (default false).

					O `idToken` retornado pode ser usado no botão **Authorize** do Swagger para testar rotas protegidas.
					"""
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Cadastro e perfil criados com sucesso"),
			@ApiResponse(responseCode = "400", description = "Corpo inválido ou e-mail já cadastrado"),
			@ApiResponse(responseCode = "500", description = "Erro ao comunicar com Firebase")
	})
	@SecurityRequirements
	@PostMapping("/register-email")
	public ResponseEntity<FirebaseEmailRegisterResponseDTO> registerWithEmail(
			@RequestBody @Valid FirebaseEmailRegisterRequestDTO request) {
		return ResponseEntity.ok(firebaseDebugService.registerWithEmailPassword(request));
	}
}

