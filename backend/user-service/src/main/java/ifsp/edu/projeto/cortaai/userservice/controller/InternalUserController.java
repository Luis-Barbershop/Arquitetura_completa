package ifsp.edu.projeto.cortaai.userservice.controller;

import ifsp.edu.projeto.cortaai.userservice.dto.UserInfoDTO;
import ifsp.edu.projeto.cortaai.userservice.model.Barber;
import ifsp.edu.projeto.cortaai.userservice.model.Customer;
import ifsp.edu.projeto.cortaai.userservice.repository.BarberRepository;
import ifsp.edu.projeto.cortaai.userservice.repository.CustomerRepository;
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

    /** Busca usuário por ID (Customer ou Barber). */
    @Operation(summary = "Busca usuário por ID", description = "Retorna o UserInfoDTO de um Customer ou Barber pelo UUID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário encontrado",
                    content = @Content(schema = @Schema(implementation = UserInfoDTO.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
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
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
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
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
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
            @ApiResponse(responseCode = "404", description = "Barbeiro não encontrado")
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

    // ── conversores ──────────────────────────────────────────────────────────

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

