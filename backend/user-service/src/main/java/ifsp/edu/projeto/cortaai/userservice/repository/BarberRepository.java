package ifsp.edu.projeto.cortaai.userservice.repository;

import ifsp.edu.projeto.cortaai.userservice.model.Barber;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;


public interface BarberRepository extends JpaRepository<Barber, UUID> {

    boolean existsByTellIgnoreCase(String tell);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByDocumentCPFIgnoreCase(String documentCPF);

    /**

     Encontra todos os barbeiros vinculados a uma barbearia específica.
     Essencial para o "Fluxo 4: Agendamento pelo Cliente".*/
    List<Barber> findByBarbershopId(UUID barbershopId);

    Optional<Barber> findByEmail(String email);

}