package ifsp.edu.projeto.cortaai.userservice.controller;

import ifsp.edu.projeto.cortaai.userservice.dto.UserInfoDTO;
import ifsp.edu.projeto.cortaai.userservice.model.Barber;
import ifsp.edu.projeto.cortaai.userservice.model.Customer;
import ifsp.edu.projeto.cortaai.userservice.repository.BarberRepository;
import ifsp.edu.projeto.cortaai.userservice.repository.CustomerRepository;
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
public class InternalUserController {

    private static final Logger log = LoggerFactory.getLogger(InternalUserController.class);

    private final CustomerRepository customerRepository;
    private final BarberRepository barberRepository;

    /** Busca usuário por ID (Customer ou Barber). */
    @GetMapping("/{id}")
    public ResponseEntity<UserInfoDTO> getUserById(@PathVariable UUID id) {
        Optional<Customer> customer = customerRepository.findById(id);
        if (customer.isPresent()) return ResponseEntity.ok(toUserInfoDTO(customer.get()));

        Optional<Barber> barber = barberRepository.findById(id);
        if (barber.isPresent()) return ResponseEntity.ok(toUserInfoDTO(barber.get()));

        return ResponseEntity.notFound().build();
    }

    /** Busca usuário por e-mail (Customer ou Barber). */
    @GetMapping("/by-email/{email}")
    public ResponseEntity<UserInfoDTO> getUserByEmail(@PathVariable String email) {
        Optional<Customer> customer = customerRepository.findByEmail(email);
        if (customer.isPresent()) return ResponseEntity.ok(toUserInfoDTO(customer.get()));

        Optional<Barber> barber = barberRepository.findByEmail(email);
        if (barber.isPresent()) return ResponseEntity.ok(toUserInfoDTO(barber.get()));

        return ResponseEntity.notFound().build();
    }

    /** Busca usuário pelo Firebase UID (Customer ou Barber). */
    @GetMapping("/by-firebase-uid/{uid}")
    public ResponseEntity<UserInfoDTO> getUserByFirebaseUid(@PathVariable String uid) {
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
    @PutMapping("/{id}/barbershop")
    public ResponseEntity<Void> updateUserBarbershopId(
            @PathVariable UUID id,
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

