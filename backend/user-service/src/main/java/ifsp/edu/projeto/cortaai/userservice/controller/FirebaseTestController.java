package ifsp.edu.projeto.cortaai.userservice.controller;

import ifsp.edu.projeto.cortaai.userservice.dto.ChangePasswordRequestDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.EmailExistsResponseDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.FirebaseEmailRegisterRequestDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.FirebaseEmailRegisterResponseDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.FirebaseEmailSignInRequestDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.FirebaseEmailSignInResponseDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.FirebaseTokenDebugRequestDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.FirebaseTokenDebugResponseDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.ForgotPasswordRequestDTO;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/auth/email", "/api/auth/firebase-test"})
@RequiredArgsConstructor
@Tag(name = "Email Auth", description = "Autenticação com e-mail/senha via Firebase (rotas de produção + aliases legados)")
public class FirebaseTestController {

	private final FirebaseDebugService firebaseDebugService;

	@Operation(
			summary = "Login por e-mail/senha",
			description = "Usa o Identity Toolkit do Firebase para retornar idToken/refreshToken."
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Login realizado com sucesso"),
			@ApiResponse(responseCode = "400", description = "Corpo da requisição inválido"),
			@ApiResponse(responseCode = "401", description = "Credenciais inválidas")
	})
	@SecurityRequirements
	@PostMapping({"/login", "/sign-in-email"})
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
	@PostMapping({"/verify-token", "/verify-id-token"})
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

					**Campos obrigatórios para CUSTOMER**: email, password, userType="CUSTOMER", name, tell, documentCPF, birthDate.

					**Campos obrigatórios para BARBER**: todos acima.
					workStartTime/workEndTime são opcionais e podem ser preenchidos depois no perfil.
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
	@PostMapping({"/register", "/register-email"})
	public ResponseEntity<FirebaseEmailRegisterResponseDTO> registerWithEmail(
			@RequestBody @Valid FirebaseEmailRegisterRequestDTO request) {
		return ResponseEntity.ok(firebaseDebugService.registerWithEmailPassword(request));
	}

	@Operation(
			summary = "Recuperação de senha",
			description = """
					Envia um e-mail de redefinição de senha para o endereço informado.
					
					O Firebase Authentication dispara o e-mail automaticamente com um link seguro.
					O usuário clica no link, define a nova senha e pode fazer login normalmente.
					
					**Esta rota é pública** — não requer token.
					"""
	)
	@ApiResponses({
			@ApiResponse(responseCode = "204", description = "E-mail de recuperação enviado com sucesso"),
			@ApiResponse(responseCode = "400", description = "E-mail inválido ou ausente"),
			@ApiResponse(responseCode = "404", description = "E-mail não encontrado no Firebase")
	})
	@SecurityRequirements
	@PostMapping("/forgot-password")
	public ResponseEntity<Void> forgotPassword(
			@RequestBody @Valid ForgotPasswordRequestDTO request) {
		firebaseDebugService.forgotPassword(request);
		return ResponseEntity.noContent().build();
	}

	@Operation(
			summary = "Alterar senha (usuário autenticado)",
			description = """
					Altera a senha do usuário autenticado.
					
					Requer o `idToken` da sessão atual (obtido no login).
					Após a alteração, o Firebase invalida todas as sessões anteriores.
					O usuário precisará fazer login novamente com a nova senha.
					
					**Esta rota é pública** — o idToken vai no corpo, não no header.
					"""
	)
	@ApiResponses({
			@ApiResponse(responseCode = "204", description = "Senha alterada com sucesso"),
			@ApiResponse(responseCode = "400", description = "Dados inválidos ou senha muito curta"),
			@ApiResponse(responseCode = "401", description = "Token expirado ou inválido — faça login novamente")
	})
	@SecurityRequirements
	@PostMapping("/change-password")
	public ResponseEntity<Void> changePassword(
			@RequestBody @Valid ChangePasswordRequestDTO request) {
		firebaseDebugService.changePassword(request);
		return ResponseEntity.noContent().build();
	}

	@Operation(
			summary = "Verificar existência de e-mail",
			description = """
					Verifica se o e-mail já está cadastrado em qualquer perfil (CUSTOMER ou BARBER).
					
					**Usado pela lógica de redirecionamento inteligente do front-end:**
					- Antes de exibir erro de credenciais inválidas, o front chama este endpoint.
					- Se `exists=false`, o usuário é redirecionado para a tela de cadastro com o e-mail pré-preenchido.
					- Se `exists=true`, o erro de credenciais é exibido normalmente.
					
					**Esta rota é pública** — não requer token de autenticação.
					""")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Consulta realizada com sucesso"),
			@ApiResponse(responseCode = "400", description = "E-mail inválido ou ausente")
	})
	@SecurityRequirements
	@GetMapping("/exists")
	public ResponseEntity<EmailExistsResponseDTO> emailExists(
			@RequestParam("email") String email) {
		return ResponseEntity.ok(firebaseDebugService.checkEmailExists(email));
	}
}

