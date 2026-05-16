package ifsp.edu.projeto.cortaai.scheduleservice.repository.projection;

import java.time.LocalDate;

public interface AgendaThermometerProjection {
    LocalDate getAgendaDate();
    String getBarbershopId();
    Long getTotalAppointments();
    Long getActiveAppointments();
    Long getWalkinAppointments();
    Long getPendingAppointments();
    Long getCompletedAppointments();
    Long getLostAppointments();
}
