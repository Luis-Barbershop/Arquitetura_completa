package ifsp.edu.projeto.cortaai.barbershopservice.controller;

import ifsp.edu.projeto.cortaai.barbershopservice.dto.*;
import ifsp.edu.projeto.cortaai.barbershopservice.exception.ApiErrorResponse;
import ifsp.edu.projeto.cortaai.barbershopservice.service.BarbershopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/barbershops")
@RequiredArgsConstructor
@Tag(name = "Barbershops", description = "Endpoints para gerenciamento de barbearias, serviços, equipe e solicitações de vínculo")
public class BarbershopController {

    private final BarbershopService barbershopService;

    // ========== LEITURA PÚBLICA ==========

    @Operation(summary = "Lista todas as barbearias", description = "Retorna uma lista pública de todas as barbearias cadastradas.")
    @GetMapping
    public ResponseEntity<List<BarbershopDTO>> listAllBarbershops() {
        return ResponseEntity.ok(barbershopService.listBarbershops());
    }

    @Operation(summary = "Busca uma barbearia por ID", description = "Retorna os detalhes públicos de uma barbearia específica.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Barbearia encontrada"),
            @ApiResponse(responseCode = "404", description = "Barbearia não encontrada",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/{shopId}")
    public ResponseEntity<BarbershopDTO> getBarbershop(
            @Parameter(description = "UUID da barbearia") @PathVariable UUID shopId) {
        return ResponseEntity.ok(barbershopService.getBarbershop(shopId));
    }

    @Operation(summary = "Lista os serviços de uma barbearia", description = "Retorna todos os serviços (atividades) oferecidos por uma barbearia específica.")
    @GetMapping("/{shopId}/activities")
    public ResponseEntity<List<ActivityDTO>> listActivities(
            @Parameter(description = "UUID da barbearia") @PathVariable UUID shopId) {
        return ResponseEntity.ok(barbershopService.listActivities(shopId));
    }

    @Operation(summary = "Lista os barbeiros de uma barbearia", description = "Retorna os barbeiros vinculados a uma barbearia específica.")
    @GetMapping("/{shopId}/barbers")
    public ResponseEntity<List<BarberPublicDTO>> listBarbers(
            @Parameter(description = "UUID da barbearia") @PathVariable UUID shopId) {
        return ResponseEntity.ok(barbershopService.listBarbers(shopId));
    }

    @Operation(summary = "Avalia uma barbearia", description = "Permite que um cliente autenticado envie uma avaliação de 1 a 5 estrelas para uma barbearia.")
    @PostMapping("/{shopId}/reviews")
    public ResponseEntity<Void> createReview(
            @Parameter(hidden = true) Principal principal,
            @Parameter(description = "UUID da barbearia") @PathVariable UUID shopId,
            @Parameter(description = "Dados da avaliação") @RequestBody @Valid CreateBarbershopReviewDTO dto) {
        barbershopService.createReview(principal.getName(), shopId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // ========== FLUXO 1: GESTÃO DO DONO (OWNER) ==========

    @Operation(
        summary = "Registra uma nova barbearia",
        description = "Cria uma nova barbearia associada ao usuário autenticado. Envie os dados como FormData contendo 'shop' (JSON) e 'file' (Imagem)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Barbearia criada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token inválido ou ausente",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "CNPJ ou dados duplicados",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    // 1. ALTERADO: Agora consome MULTIPART_FORM_DATA_VALUE
    @PostMapping(value = "/register-my-shop", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BarbershopDTO> createBarbershop(
            @Parameter(hidden = true) Principal principal,
            // 2. ALTERADO: Usando @RequestPart em vez de @RequestBody
            @RequestPart("shop") @Valid CreateBarbershopDTO dto,
            @RequestPart(value = "file", required = false) MultipartFile file) throws IOException {
        
        // 3. ALTERADO: Passando o 'file' em vez de null
        BarbershopDTO created = barbershopService.createBarbershop(principal.getName(), dto, file);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @Operation(summary = "Atualiza os dados da própria barbearia", description = "Edita as informações da barbearia do dono autenticado (baseado no token JWT).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Barbearia atualizada"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token inválido ou ausente",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão para alterar esta barbearia",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Barbearia não encontrada",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PutMapping("/my-shop")
    public ResponseEntity<BarbershopDTO> updateBarbershop(
            @Parameter(hidden = true) Principal principal,
            @Parameter(description = "Novos dados da barbearia") @RequestBody @Valid UpdateBarbershopDTO dto) {
        return ResponseEntity.ok(barbershopService.updateBarbershop(principal.getName(), dto));
    }

    @Operation(summary = "Cria um novo serviço (atividade)", description = "Adiciona um novo serviço (ex: Corte, Barba) à barbearia do dono logado.")
    @PostMapping("/my-shop/activities")
    public ResponseEntity<ActivityDTO> createActivity(
            @Parameter(hidden = true) Principal principal,
            @Parameter(description = "Dados da nova atividade") @RequestBody @Valid CreateActivityDTO dto) {
        ActivityDTO created = barbershopService.createActivity(principal.getName(), dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @Operation(summary = "Atualiza um serviço (atividade)", description = "Altera os detalhes de um serviço existente na barbearia do dono logado.")
    @PutMapping("/my-shop/activities/{activityId}")
    public ResponseEntity<ActivityDTO> updateActivity(
            @Parameter(hidden = true) Principal principal,
            @Parameter(description = "UUID da atividade") @PathVariable UUID activityId,
            @Parameter(description = "Dados atualizados da atividade") @RequestBody @Valid UpdateActivityDTO dto) {
        return ResponseEntity.ok(barbershopService.updateActivity(principal.getName(), activityId, dto));
    }

    @Operation(summary = "Exclui um serviço (atividade)", description = "Remove um serviço específico da barbearia do dono logado.")
    @DeleteMapping("/my-shop/activities/{activityId}")
    public ResponseEntity<Void> deleteActivity(
            @Parameter(hidden = true) Principal principal,
            @Parameter(description = "UUID da atividade") @PathVariable UUID activityId) {
        barbershopService.deleteActivity(principal.getName(), activityId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Remove um barbeiro da equipe", description = "Ação exclusiva do dono: desvincula um barbeiro de sua barbearia.")
    @DeleteMapping("/my-shop/remove-barber/{barberId}")
    public ResponseEntity<Void> removeBarber(
            @Parameter(hidden = true) Principal principal,
            @Parameter(description = "UUID do barbeiro a ser removido") @PathVariable UUID barberId) {
        barbershopService.removeBarber(principal.getName(), barberId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Encerra as atividades da barbearia", description = "Exclui/Desativa permanentemente a barbearia do dono logado. Requer confirmação via DTO.")
    @DeleteMapping("/my-shop/close")
    public ResponseEntity<Void> closeBarbershop(
            @Parameter(hidden = true) Principal principal,
            @Parameter(description = "Confirmação de encerramento") @RequestBody @Valid CloseBarbershopRequestDTO dto) {
        barbershopService.closeBarbershop(principal.getName(), dto);
        return ResponseEntity.noContent().build();
    }

    // ========== FLUXO 2: JOIN REQUESTS ==========

    @Operation(summary = "Solicita entrada em uma barbearia", description = "Permite que um barbeiro autenticado solicite vínculo com uma barbearia informando o CNPJ da mesma.")
    @PostMapping("/join-request")
    public ResponseEntity<Void> requestToJoin(
            @Parameter(hidden = true) Principal principal,
            @Parameter(description = "Dados contendo o CNPJ da barbearia") @RequestBody @Valid BarberJoinRequestDTO dto) {
        barbershopService.requestToJoinBarbershop(principal.getName(), dto.getCnpj());
        return ResponseEntity.accepted().build();
    }

    @Operation(summary = "Lista solicitações pendentes de vínculo", description = "O dono da barbearia logada pode ver todos os barbeiros que pediram para entrar na equipe.")
    @GetMapping("/my-shop/pending-requests")
    public ResponseEntity<List<JoinRequestDTO>> getPendingRequests(@Parameter(hidden = true) Principal principal) {
        return ResponseEntity.ok(barbershopService.getPendingJoinRequests(principal.getName()));
    }

    @Operation(summary = "Aprova uma solicitação de vínculo", description = "O dono da barbearia aprova a entrada de um barbeiro específico na equipe.")
    @PostMapping("/my-shop/approve-request/{requestId}")
    public ResponseEntity<Void> approveJoinRequest(
            @Parameter(hidden = true) Principal principal,
            @Parameter(description = "UUID da solicitação de vínculo") @PathVariable UUID requestId) {
        barbershopService.approveJoinRequest(principal.getName(), requestId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Rejeita uma solicitação de vínculo", description = "O dono da barbearia rejeita a entrada de um barbeiro específico na equipe.")
    @PostMapping("/my-shop/reject-request/{requestId}")
    public ResponseEntity<Void> rejectJoinRequest(
            @Parameter(hidden = true) Principal principal,
            @Parameter(description = "UUID da solicitação de vínculo") @PathVariable UUID requestId) {
        barbershopService.rejectJoinRequest(principal.getName(), requestId);
        return ResponseEntity.noContent().build();
    }

    // ========== FLUXO 3: SAIR DA LOJA ==========

    @Operation(summary = "Sai da barbearia voluntariamente", description = "Permite que um barbeiro autenticado saia (desvincule-se) da barbearia em que trabalha atualmente.")
    @PostMapping("/leave-shop")
    public ResponseEntity<Void> leaveShop(@Parameter(hidden = true) Principal principal) {
        barbershopService.freeBarber(principal.getName());
        return ResponseEntity.noContent().build();
    }

    // ========== FLUXO 2B: CONVITE DO OWNER (INVITE) ==========

    @Operation(summary = "Convida um barbeiro pelo CPF", description = "O dono da barbearia informa o CPF de um barbeiro cadastrado para convidá-lo a fazer parte da equipe. O barbeiro verá o convite no perfil e poderá aceitar ou recusar.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Convite enviado com sucesso"),
            @ApiResponse(responseCode = "400", description = "CPF inválido",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Barbeiro não encontrado com este CPF",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Barbeiro já vinculado ou convite duplicado",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/my-shop/invite-barber")
    public ResponseEntity<Void> inviteBarber(
            @Parameter(hidden = true) Principal principal,
            @Parameter(description = "CPF do barbeiro a convidar") @RequestBody @Valid InviteBarberDTO dto) {
        barbershopService.inviteBarberByCpf(principal.getName(), dto.getCpf());
        return ResponseEntity.accepted().build();
    }

    @Operation(summary = "Lista convites pendentes do barbeiro autenticado", description = "Retorna os convites (INVITE) pendentes que o barbeiro recebeu de barbearias. Exibido na tela de perfil do barbeiro.")
    @GetMapping("/my-invites")
    public ResponseEntity<List<JoinRequestDTO>> getMyInvites(@Parameter(hidden = true) Principal principal) {
        return ResponseEntity.ok(barbershopService.getMyPendingInvites(principal.getName()));
    }

    @Operation(summary = "Aceita um convite de barbearia", description = "O barbeiro autenticado aceita o convite e fica vinculado à barbearia.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Convite aceito, barbeiro vinculado"),
            @ApiResponse(responseCode = "404", description = "Convite não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Convite não pertence a você",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Barbeiro já vinculado a outra barbearia",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/accept-invite/{requestId}")
    public ResponseEntity<Void> acceptInvite(
            @Parameter(hidden = true) Principal principal,
            @Parameter(description = "UUID do convite") @PathVariable UUID requestId) {
        barbershopService.acceptInvite(principal.getName(), requestId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Recusa um convite de barbearia", description = "O barbeiro autenticado recusa o convite.")
    @PostMapping("/reject-invite/{requestId}")
    public ResponseEntity<Void> rejectInvite(
            @Parameter(hidden = true) Principal principal,
            @Parameter(description = "UUID do convite") @PathVariable UUID requestId) {
        barbershopService.rejectInvite(principal.getName(), requestId);
        return ResponseEntity.noContent().build();
    }

    // ========== FLUXO 4: GESTÃO DE IMAGENS ==========

    @Operation(summary = "Faz o upload da logo da barbearia", description = "Atualiza a foto de logo da barbearia do dono logado.")
    @PostMapping(value = "/my-shop/upload-logo")
    public ResponseEntity<String> uploadLogo(
            @Parameter(hidden = true) Principal principal,
            @Parameter(description = "Arquivo da imagem da logo") @RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.ok(barbershopService.updateBarbershopLogo(principal.getName(), file));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Falha no upload: " + e.getMessage());
        }
    }

    @Operation(summary = "Faz o upload do banner da barbearia", description = "Atualiza a imagem de capa (banner) da barbearia do dono logado.")
    @PostMapping(value = "/my-shop/upload-banner")
    public ResponseEntity<String> uploadBanner(
            @Parameter(hidden = true) Principal principal,
            @Parameter(description = "Arquivo da imagem do banner") @RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.ok(barbershopService.updateBarbershopBanner(principal.getName(), file));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Falha no upload: " + e.getMessage());
        }
    }

    @Operation(summary = "Faz o upload da foto de um serviço", description = "Atualiza a imagem associada a um serviço/atividade específico na barbearia.")
    @PostMapping(value = "/my-shop/activities/{activityId}/upload-photo")
    public ResponseEntity<String> uploadActivityPhoto(
            @Parameter(hidden = true) Principal principal,
            @Parameter(description = "UUID da atividade") @PathVariable UUID activityId,
            @Parameter(description = "Arquivo da foto do serviço") @RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.ok(barbershopService.updateActivityPhoto(principal.getName(), activityId, file));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Falha no upload: " + e.getMessage());
        }
    }

    @Operation(summary = "Adiciona uma imagem aos destaques", description = "Faz o upload de uma nova imagem para o carrossel de destaques/portfólio da barbearia.")
    @PostMapping(value = "/my-shop/highlights")
    public ResponseEntity<String> addHighlight(
            @Parameter(hidden = true) Principal principal,
            @Parameter(description = "Arquivo da imagem destaque") @RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    barbershopService.addBarbershopHighlight(principal.getName(), file));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Falha no upload: " + e.getMessage());
        }
    }

    @Operation(summary = "Deleta uma imagem de destaque", description = "Remove uma imagem específica do carrossel de destaques da barbearia.")
    @DeleteMapping("/my-shop/highlights/{highlightId}")
    public ResponseEntity<Void> deleteHighlight(
            @Parameter(hidden = true) Principal principal,
            @Parameter(description = "UUID do destaque a ser removido") @PathVariable UUID highlightId) {
        barbershopService.deleteBarbershopHighlight(principal.getName(), highlightId);
        return ResponseEntity.noContent().build();
    }
}