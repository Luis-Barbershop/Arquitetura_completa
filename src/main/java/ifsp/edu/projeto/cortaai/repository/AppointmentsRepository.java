package ifsp.edu.projeto.cortaai.repository;

import ifsp.edu.projeto.cortaai.model.Appointments;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;


public interface AppointmentsRepository extends JpaRepository<Appointments, Long> {

    Appointments findFirstByBarberId(UUID id);

    Appointments findFirstByCustomerId(UUID id);

}
