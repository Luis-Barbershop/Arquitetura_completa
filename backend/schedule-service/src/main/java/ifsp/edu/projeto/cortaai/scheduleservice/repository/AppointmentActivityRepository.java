package ifsp.edu.projeto.cortaai.scheduleservice.repository;

import ifsp.edu.projeto.cortaai.scheduleservice.model.AppointmentActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AppointmentActivityRepository extends JpaRepository<AppointmentActivity, UUID> {
}

