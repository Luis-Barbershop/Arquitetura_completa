package ifsp.edu.projeto.cortaai.scheduleservice.model.analytics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

import java.io.Serializable;
import java.time.LocalDate;

@Entity
@Immutable
@Table(name = "v_agenda_thermometer")
@IdClass(VAgendaThermometer.VAgendaThermometerId.class)
@Getter
public class VAgendaThermometer {

    @Id
    @Column(name = "agenda_date")
    private LocalDate agendaDate;

    @Id
    @Column(name = "barbershop_id")
    private String barbershopId;

    @Column(name = "total_appointments")
    private Long totalAppointments;

    @Column(name = "active_appointments")
    private Long activeAppointments;

    @Column(name = "lost_appointments")
    private Long lostAppointments;

    public static class VAgendaThermometerId implements Serializable {
        private LocalDate agendaDate;
        private String barbershopId;
    }
}
