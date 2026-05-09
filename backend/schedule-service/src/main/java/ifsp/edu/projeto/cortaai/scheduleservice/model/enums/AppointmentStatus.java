package ifsp.edu.projeto.cortaai.scheduleservice.model.enums;

public enum AppointmentStatus {
    SCHEDULED,
    PAYMENT_PENDING,
    EXPIRED,      // projeção lazy — PAYMENT_PENDING + startTime + 1h < now
    CONFIRMED,
    IN_PROGRESS,
    COMPLETED,
    WALK_IN,
    @Deprecated
    CONCLUDED,
    CANCELLED,
    NO_SHOW
}
