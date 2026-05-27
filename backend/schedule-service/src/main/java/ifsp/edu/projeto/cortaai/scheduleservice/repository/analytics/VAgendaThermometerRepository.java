package ifsp.edu.projeto.cortaai.scheduleservice.repository.analytics;

import ifsp.edu.projeto.cortaai.scheduleservice.model.analytics.VAgendaThermometer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VAgendaThermometerRepository extends JpaRepository<VAgendaThermometer, String> {
}