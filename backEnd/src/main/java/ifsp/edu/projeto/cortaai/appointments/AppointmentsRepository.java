package ifsp.edu.projeto.cortaai.appointments;

import java.util.UUID;
import org.springframework.data.mongodb.repository.MongoRepository;


public interface AppointmentsRepository extends MongoRepository<Appointments, Long> {

    Appointments findFirstByBarberId(UUID id);

    Appointments findFirstByCustomerId(UUID id);

}
