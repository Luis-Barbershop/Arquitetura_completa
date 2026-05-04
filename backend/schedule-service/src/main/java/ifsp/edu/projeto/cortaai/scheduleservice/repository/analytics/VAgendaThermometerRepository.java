package ifsp.edu.projeto.cortaai.scheduleservice.repository.analytics;

import ifsp.edu.projeto.cortaai.scheduleservice.model.analytics.VAgendaThermometer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface VAgendaThermometerRepository extends JpaRepository<VAgendaThermometer, VAgendaThermometer.VAgendaThermometerId> {

    List<VAgendaThermometer> findByBarbershopId(String barbershopId);

    List<VAgendaThermometer> findByBarbershopIdAndAgendaDateBetween(String barbershopId, LocalDate start, LocalDate end);
}
