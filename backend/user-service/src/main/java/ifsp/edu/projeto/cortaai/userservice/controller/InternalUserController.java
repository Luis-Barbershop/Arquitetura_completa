package ifsp.edu.projeto.cortaai.userservice.controller;

import ifsp.edu.projeto.cortaai.userservice.dto.UserInfoDTO;
import ifsp.edu.projeto.cortaai.userservice.model.Barber;
import ifsp.edu.projeto.cortaai.userservice.model.Customer;
import ifsp.edu.projeto.cortaai.userservice.repository.BarberRepository;
import ifsp.edu.projeto.cortaai.userservice.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

/**
 * Endpoints internos para comunicação inter-serviço.
 * NÃO devem ser expostos pelo API Gateway.
 * Protegidos pelo header X-Internal-Token.
 */
@RestController
@RequestMapping("/api/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final CustomerRepository customerRepository;
    private final BarberRepository barberRepository;

    /**
     * Busca usuário por ID (pode ser Customer ou Barber).
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserInfoDTO> getUserById(@PathVariable UUID id) {
        // Tenta encontrar como Customer
        Optional<Customer> customer = customerRepository.findById(id);
        if (customer.isPresent()) {
            return ResponseEntity.ok(toUserInfoDTO(customer.get()));
        }

        // Tenta encontrar como Barber
        Optional<Barber> barber = barberRepository.findById(id);
        if (barber.isPresent()) {
            return ResponseEntity.ok(toUserInfoDTO(barber.get()));
        }

        return ResponseEntity.notFound().build();
    }

    /**
     * Busca usuário por email (pode ser Customer ou Barber).
     */
    @GetMapping("/by-email/{email}")
    public ResponseEntity<UserInfoDTO> getUserByEmail(@PathVariable String email) {
        // Tenta encontrar como Customer
        Optional<Customer> customer = customerRepository.findByEmail(email);
        if (customer.isPresent()) {
            return ResponseEntity.ok(toUserInfoDTO(customer.get()));
        }

        // Tenta encontrar como Barber
        Optional<Barber> barber = barberRepository.findByEmail(email);
        if (barber.isPresent()) {
            return ResponseEntity.ok(toUserInfoDTO(barber.get()));
        }

        return ResponseEntity.notFound().build();
    }

    /**
     * Atualiza o barbershopId de um barbeiro.
     * Usado pelo barbershop-service quando um JoinRequest é aprovado.
     */
    @PutMapping("/{id}/barbershop")
    public ResponseEntity<Void> updateUserBarbershopId(
            @PathVariable UUID id,
            @RequestBody UUID barbershopId) {

        Optional<Barber> barber = barberRepository.findById(id);
        if (barber.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Barber b = barber.get();
        b.setBarbershopId(barbershopId);
        barberRepository.save(b);

        return ResponseEntity.ok().build();
    }

    // --- Métodos auxiliares de conversão ---

    private UserInfoDTO toUserInfoDTO(Customer customer) {
        return new UserInfoDTO(
                customer.getId(),
                customer.getName(),
                customer.getEmail(),
                "CUSTOMER",
                customer.getRole(),
                null, // Customer não tem barbershopId
                null, // Customer não tem workStartTime
                null, // Customer não tem workEndTime
                customer.getImageUrl()
        );
    }

    private UserInfoDTO toUserInfoDTO(Barber barber) {
        return new UserInfoDTO(
                barber.getId(),
                barber.getName(),
                barber.getEmail(),
                "BARBER",
                barber.getRole(),
                barber.getBarbershopId(),
                barber.getWorkStartTime(),
                barber.getWorkEndTime(),
                barber.getImageUrl()
        );
    }
}

