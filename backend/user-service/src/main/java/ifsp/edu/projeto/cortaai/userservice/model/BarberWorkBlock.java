package ifsp.edu.projeto.cortaai.userservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;

import java.sql.Types;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Representa um bloco de horário de trabalho de um barbeiro.
 * Exemplo: Segunda-feira, 09:00–12:00 e 13:00–18:00 são dois blocos.
 * Permite ao barbeiro configurar intervalos (almoço, pausa etc.) de forma flexível.
 */
@Entity
@Table(name = "barber_work_blocks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BarberWorkBlock {

    @Id
    @GeneratedValue
    @UuidGenerator
    @JdbcTypeCode(Types.VARCHAR)
    @Column(nullable = false, updatable = false, length = 36)
    private UUID id;

    @JdbcTypeCode(Types.VARCHAR)
    @Column(name = "barber_id", nullable = false, length = 36)
    private UUID barberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 10)
    private DayOfWeek dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;
}
