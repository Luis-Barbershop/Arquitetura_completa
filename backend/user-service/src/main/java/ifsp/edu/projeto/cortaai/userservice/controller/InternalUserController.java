package ifsp.edu.projeto.cortaai.userservice.controller;

import ifsp.edu.projeto.cortaai.userservice.dto.DayScheduleDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.UserInfoDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.SaveMpCredentialsDTO;
import ifsp.edu.projeto.cortaai.userservice.exception.ApiErrorResponse;
import ifsp.edu.projeto.cortaai.userservice.model.Barber;
import ifsp.edu.projeto.cortaai.userservice.model.Customer;
import ifsp.edu.projeto.cortaai.userservice.repository.BarberRepository;
import ifsp.edu.projeto.cortaai.userservice.repository.CustomerRepository;
import ifsp.edu.projeto.cortaai.userservice.service.BarberWorkScheduleService;
import ifsp.edu.projeto.cortaai.userservice.service.FirebaseAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import java.util.Set;

/**
 * Endpoints internos para comunicação inter-serviço.
 * NÃO devem ser expostos pelo API Gateway.
 */
@RestController
@RequestMapping("/api/internal/users")
@RequiredArgsConstructor
@Tag(name = "Internal - Users", description = "Endpoints internos para comunicação inter-serviço (NÃO expostos pelo Gateway)")
public class InternalUserController {

    private static final Logger log = LoggerFactory.getLogger(InternalUserController.class);

    private final CustomerRepository customerRepository;
    private final BarberRepository barberRepository;
    private final FirebaseAuthService firebaseAuthService;
    private final BarberWorkScheduleService workScheduleService;

    /** Busca usuário por ID (Customer ou Barber). */
    @Operation(summary = "Busca usuário por ID", description = "Retorna o UserInfoDTO de um Customer ou Barber pelo UUID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário encontrado",
                    content = @Content(schema = @Schema(implementation = UserInfoDTO.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<UserInfoDTO> getUserById(
            @Parameter(description = "UUID do usuário") @PathVariable UUID id) {
        Optional<Customer> customer = customerRepository.findById(id);
        if (customer.isPresent()) return ResponseEntity.ok(toUserInfoDTO(customer.get()));

        Optional<Barber> barber = barberRepository.findById(id);
        if (barber.isPresent()) return ResponseEntity.ok(toUserInfoDTO(barber.get()));

        return ResponseEntity.notFound().build();
    }

    /** Busca usuário por e-mail (Customer ou Barber). */
    @Operation(summary = "Busca usuário por e-mail", description = "Retorna o UserInfoDTO de um Customer ou Barber pelo endereço de e-mail.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário encontrado",
                    content = @Content(schema = @Schema(implementation = UserInfoDTO.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/by-email/{email}")
    public ResponseEntity<UserInfoDTO> getUserByEmail(
            @Parameter(description = "E-mail do usuário") @PathVariable String email) {
        Optional<Customer> customer = customerRepository.findByEmail(email);
        if (customer.isPresent()) return ResponseEntity.ok(toUserInfoDTO(customer.get()));

        Optional<Barber> barber = barberRepository.findByEmail(email);
        if (barber.isPresent()) return ResponseEntity.ok(toUserInfoDTO(barber.get()));

        return ResponseEntity.notFound().build();
    }

    /** Busca usuário pelo Firebase UID (Customer ou Barber). */
    @Operation(summary = "Busca usuário por Firebase UID", description = "Retorna o UserInfoDTO de um Customer ou Barber pelo UID do Firebase.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário encontrado",
                    content = @Content(schema = @Schema(implementation = UserInfoDTO.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/by-firebase-uid/{uid}")
    public ResponseEntity<UserInfoDTO> getUserByFirebaseUid(
            @Parameter(description = "UID do Firebase") @PathVariable String uid) {
        Optional<Customer> customer = customerRepository.findByFirebaseUid(uid);
        if (customer.isPresent()) return ResponseEntity.ok(toUserInfoDTO(customer.get()));

        Optional<Barber> barber = barberRepository.findByFirebaseUid(uid);
        if (barber.isPresent()) return ResponseEntity.ok(toUserInfoDTO(barber.get()));

        return ResponseEntity.notFound().build();
    }

    /** Lista barbeiros vinculados a uma barbearia específica. */
    @Operation(summary = "Lista barbeiros por barbearia (interno)",
               description = "Retorna barbeiros ativos da barbearia: funcionários (isOwner=false) sempre incluídos; owners apenas se actAsBarber=true.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de barbeiros retornada com sucesso")
    })
    @GetMapping("/barbers/by-barbershop/{barbershopId}")
    public ResponseEntity<List<UserInfoDTO>> getBarbersByBarbershop(
            @Parameter(description = "UUID da barbearia") @PathVariable UUID barbershopId) {
        List<UserInfoDTO> barbers = barberRepository.findActiveByBarbershopId(barbershopId)
                .stream()
                .map(this::toUserInfoDTO)
                .toList();
        return ResponseEntity.ok(barbers);
    }

    /** Lista os IDs das atividades atribuídas a um barbeiro (uso interno). */
    @Operation(summary = "Lista atividades atribuídas do barbeiro (interno)")
    @GetMapping("/barbers/{id}/activities")
    public ResponseEntity<Set<UUID>> getBarberAssignedActivities(
            @Parameter(description = "UUID do barbeiro") @PathVariable UUID id) {
        return barberRepository.findById(id)
                .map(barber -> ResponseEntity.ok(barber.getAssignedActivityIds()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Atualiza o barbershopId de um barbeiro.
     * Usado pelo barbershop-service quando um JoinRequest é aprovado ou uma barbearia é criada.
     * 
     * Aceita o body como:
     *   - JSON object: {"barbershopId": "uuid-string"} 
     *   - JSON object com null: {"barbershopId": null}  (para desvincular)
     */
    @Operation(
            summary = "Atualiza o barbershopId de um barbeiro",
            description = "Vincula ou desvincula um barbeiro de uma barbearia. Envie `{\"barbershopId\": \"uuid\"}` para vincular ou `{\"barbershopId\": null}` para desvincular."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "barbershopId atualizado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Barbeiro não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PutMapping("/{id}/barbershop")
    public ResponseEntity<Void> updateUserBarbershopId(
            @Parameter(description = "UUID do barbeiro") @PathVariable UUID id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "JSON com o campo barbershopId (UUID string ou null)",
                    required = true,
                    content = @Content(schema = @Schema(
                            example = "{\"barbershopId\": \"d5face7e-4b81-4681-b80f-673b2c59a312\"}"
                    ))
            )
            @RequestBody Map<String, String> body) {

        log.info("PUT /api/internal/users/{}/barbershop — body={}", id, body);

        Optional<Barber> barber = barberRepository.findById(id);
        if (barber.isEmpty()) {
            log.warn("Barber NOT FOUND by id={}", id);
            // Fallback: tenta procurar por todas as formas possíveis
            log.info("Listing all barbers for debug:");
            barberRepository.findAll().forEach(b -> 
                log.info("  barber: id={} email={} firebaseUid={}", b.getId(), b.getEmail(), b.getFirebaseUid())
            );
            return ResponseEntity.notFound().build();
        }

        String barbershopIdStr = body != null ? body.get("barbershopId") : null;
        UUID barbershopId = (barbershopIdStr != null && !barbershopIdStr.isBlank()) 
                ? UUID.fromString(barbershopIdStr) 
                : null;

        Barber b = barber.get();
        b.setBarbershopId(barbershopId);
        barberRepository.save(b);
        log.info("Barber {} barbershopId updated to {}", id, barbershopId);
        return ResponseEntity.ok().build();
    }
    @PutMapping("/make-owner/{uid}")
    public ResponseEntity<Void> makeBarberOwner(@PathVariable String uid) {
        // 1. Atualiza os custom claims do Firebase
        firebaseAuthService.setCustomUserClaims(uid, "BARBER", true);

        // 2. Persiste isOwner=true e role=ROLE_OWNER no banco de dados do barbeiro
        //    Sem isso, /auth/me sempre retornaria isOwner=false (lê do banco, não do Firebase)
        //    Também reforça actAsBarber=true: owner recém-promovido aparece na lista de
        //    profissionais da sua barbearia até que explicitamente desmarque isso.
        barberRepository.findByFirebaseUid(uid).ifPresent(barber -> {
            barber.setOwner(true);
            barber.setRole("ROLE_OWNER");
            barber.setActAsBarber(true);
            barberRepository.save(barber);
            log.info("Barber {} promovido a ROLE_OWNER (actAsBarber=true) no banco de dados.", uid);
        });

        return ResponseEntity.ok().build();
    }

    /**
     * Salva as credenciais do Mercado Pago OAuth no perfil do barbeiro.
     * Chamado pelo payment-service após o callback OAuth do MP.
     */
    @Operation(summary = "Salvar credenciais MP OAuth do barbeiro",
               description = "Endpoint interno chamado pelo payment-service após o OAuth do Mercado Pago. Persiste os tokens e IDs do barbeiro.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Credenciais salvas com sucesso"),
            @ApiResponse(responseCode = "404", description = "Barbeiro não encontrado")
    })
    @PutMapping("/barbers/{barberId}/mp-credentials")
    public ResponseEntity<Void> saveMpCredentials(
            @PathVariable UUID barberId,
            @RequestBody SaveMpCredentialsDTO dto) {
        Barber barber = barberRepository.findById(barberId)
                .orElseThrow(() -> new RuntimeException("Barbeiro não encontrado: " + barberId));
        barber.setMpAccessToken(dto.mpAccessToken());
        barber.setMpRefreshToken(dto.mpRefreshToken());
        barber.setMpUserId(dto.mpUserId());
        barber.setMpPublicKey(dto.mpPublicKey());
        barberRepository.save(barber);
        log.info("Credenciais MP salvas para barberId={}, mpUserId={}", barberId, dto.mpUserId());
        return ResponseEntity.ok().build();
    }

    /** Busca barbeiro por CPF (usado pelo barbershop-service para convites). */
    @Operation(summary = "Busca barbeiro por CPF (interno)",
               description = "Retorna o UserInfoDTO de um Barber pelo CPF. Usado pelo barbershop-service no fluxo de convite.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Barbeiro encontrado"),
            @ApiResponse(responseCode = "404", description = "Barbeiro não encontrado")
    })
    @GetMapping("/barbers/by-cpf/{cpf}")
    public ResponseEntity<UserInfoDTO> getBarberByCpf(
            @Parameter(description = "CPF do barbeiro (somente dígitos)") @PathVariable String cpf) {
        String cleanCpf = cpf.replaceAll("\\D", "");
        Optional<Barber> barber = barberRepository.findByDocumentCPF(cleanCpf);
        if (barber.isPresent()) return ResponseEntity.ok(toUserInfoDTO(barber.get()));
        return ResponseEntity.notFound().build();
    }


    // ── conversores ──────────────────────────────────────────────────────────

    // ── Agenda semanal (blocos de horário) ────────────────────────────────────
    @Operation(summary = "Retorna a agenda semanal de um barbeiro (inter-serviço)")
    @GetMapping("/barbers/{id}/work-schedule")
    public ResponseEntity<java.util.List<DayScheduleDTO>> getBarberWorkSchedule(
            @PathVariable UUID id) {
        return ResponseEntity.ok(workScheduleService.getScheduleByBarberId(id));
    }

    private UserInfoDTO toUserInfoDTO(Customer customer) {
        return new UserInfoDTO(
                customer.getId(),
                customer.getName(),
                customer.getEmail(),
                customer.getFirebaseUid(),
                "CUSTOMER",
                customer.getRole(),
                null,
                null,
                null,
                customer.getImageUrl()
        );
    }

    private UserInfoDTO toUserInfoDTO(Barber barber) {
        return new UserInfoDTO(
                barber.getId(),
                barber.getName(),
                barber.getEmail(),
                barber.getFirebaseUid(),
                "BARBER",
                barber.getRole(),
                barber.getBarbershopId(),
                barber.getWorkStartTime(),
                barber.getWorkEndTime(),
                barber.getImageUrl()
        );
    }
}

