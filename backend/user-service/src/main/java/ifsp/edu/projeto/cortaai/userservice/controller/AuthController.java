package ifsp.edu.projeto.cortaai.userservice.controller;

import ifsp.edu.projeto.cortaai.userservice.dto.AuthResponseDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.CompleteProfileBarberDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.CompleteProfileCustomerDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.FirebaseAuthRequestDTO;
import ifsp.edu.projeto.cortaai.userservice.exception.ApiErrorResponse;
import ifsp.edu.projeto.cortaai.userservice.service.FirebaseAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints de autenticação com Firebase.
 *
 * <h2>Fluxo de login social (Google, Facebook, Apple, etc.)</h2>
 * <ol>
 *   <li>O app cliente autentica o usuário com o Firebase SDK (ex.: {@code signInWithPopup}).</li>
 *   <li>O app obtém o <strong>Firebase ID Token</strong>: {@code user.getIdToken()}.</li>
 *   <li>O app chama {@code POST /api/auth/verify} com o token e o tipo de usuário desejado.</li>
 *   <li>Se {@code profileComplete = false}, o app redireciona para a tela de dados complementares.</li>
 *   <li>O app chama {@code POST /api/auth/customers/complete-profile} ou
 *       {@code POST /api/auth/barbers/complete-profile} com os dados extras.</li>
 * </ol>
 *
 * <h2>Autenticação nas requisições subsequentes</h2>
 * <p>O app deve incluir o Firebase ID Token no header {@code Authorization: Bearer <token>}.
 * O API Gateway valida o token e injeta os headers {@code X-User-UID}, {@code X-User-Email},
 * {@code X-User-Name} e {@code X-User-Type} para os serviços downstream.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Autenticação via Firebase (social login, e-mail/senha e gerenciamento de perfil)")
public class AuthController {

    private final FirebaseAuthService firebaseAuthService;

    // ──────────────────────────────────────────────────────────────────────────
    // Rota pública — não exige token no header (o próprio corpo já contém o token)
    // ──────────────────────────────────────────────────────────────────────────

    @Operation(
            summary = "Verifica o token Firebase e auto-provisiona o usuário",
            description = """
                    Valida o Firebase ID Token obtido pelo SDK cliente.
                    
                    - Se o usuário ainda não existe na base, ele é criado automaticamente.
                    - Se o usuário existe (por UID ou e-mail), os dados são sincronizados.
                    - O campo `profileComplete` indica se dados extras (CPF, telefone) ainda precisam ser preenchidos.
                    
                    **Esta rota é pública** — o token vai no corpo da requisição, não no header.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autenticação realizada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token Firebase inválido ou expirado",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "400", description = "Corpo da requisição inválido",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @io.swagger.v3.oas.annotations.security.SecurityRequirements   // ← sem token no header aqui
    @PostMapping("/verify")
    public ResponseEntity<AuthResponseDTO> verify(
            @RequestBody @Valid FirebaseAuthRequestDTO request) {
        return ResponseEntity.ok(firebaseAuthService.verifyAndProvision(request));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Rotas protegidas — o Gateway injeta X-User-UID no header
    // ──────────────────────────────────────────────────────────────────────────

    @Operation(
            summary = "Retorna os dados do usuário autenticado",
            description = "Identifica o usuário pelo header `X-User-UID` injetado pelo API Gateway após validação do Firebase ID Token."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dados retornados com sucesso"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/me")
    public ResponseEntity<AuthResponseDTO> me(
            @Parameter(hidden = true) @RequestHeader("X-User-UID") String firebaseUid) {
        return ResponseEntity.ok(firebaseAuthService.getMe(firebaseUid));
    }

    @Operation(
            summary = "Completa o perfil do cliente após login social",
            description = "Preenche CPF e telefone do customer. Deve ser chamado quando `profileComplete = false`."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil atualizado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Customer não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/customers/complete-profile")
    public ResponseEntity<AuthResponseDTO> completeCustomerProfile(
            @Parameter(hidden = true) @RequestHeader("X-User-UID") String firebaseUid,
            @RequestBody @Valid CompleteProfileCustomerDTO dto) {
        return ResponseEntity.ok(firebaseAuthService.completeCustomerProfile(firebaseUid, dto));
    }

    @Operation(
            summary = "Completa o perfil do barbeiro após login social",
            description = "Preenche CPF, telefone e horários de trabalho do barbeiro. Deve ser chamado quando `profileComplete = false`."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil atualizado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Barbeiro não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/barbers/complete-profile")
    public ResponseEntity<AuthResponseDTO> completeBarberProfile(
            @Parameter(hidden = true) @RequestHeader("X-User-UID") String firebaseUid,
            @RequestBody @Valid CompleteProfileBarberDTO dto) {
        return ResponseEntity.ok(firebaseAuthService.completeBarberProfile(firebaseUid, dto));
    }
}
