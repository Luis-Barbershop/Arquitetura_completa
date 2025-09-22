package ifsp.edu.projeto.cortaai.repository;

import java.util.UUID;

import ifsp.edu.projeto.cortaai.model.Appointments;
import org.springframework.data.mongodb.repository.MongoRepository;


public interface AppointmentsRepository extends MongoRepository<Appointments, Long> {

    Appointments findFirstByBarberId(UUID id);

    Appointments findFirstByCustomerId(UUID id);

}
