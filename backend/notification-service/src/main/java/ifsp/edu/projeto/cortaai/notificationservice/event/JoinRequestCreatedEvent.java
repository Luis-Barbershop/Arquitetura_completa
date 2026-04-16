package ifsp.edu.projeto.cortaai.notificationservice.event;

import java.util.UUID;

/**
 * Evento recebido quando um barbeiro solicita entrada em uma barbearia (JOIN)
 * ou quando um owner convida um barbeiro (INVITE).
 * Publicado pelo barbershop-service; consumido pelo notification-service.
 */
public class JoinRequestCreatedEvent {

    private UUID requestId;
    private UUID barberId;
    private String barberName;
    private String barberEmail;
    private UUID barbershopId;
    private String barbershopName;
    private UUID ownerId;
    /** "JOIN" = barbeiro pediu entrada; "INVITE" = owner convidou barbeiro. */
    private String requestType;

    public JoinRequestCreatedEvent() {
    }

    public UUID getRequestId() { return requestId; }
    public void setRequestId(UUID requestId) { this.requestId = requestId; }

    public UUID getBarberId() { return barberId; }
    public void setBarberId(UUID barberId) { this.barberId = barberId; }

    public String getBarberName() { return barberName; }
    public void setBarberName(String barberName) { this.barberName = barberName; }

    public String getBarberEmail() { return barberEmail; }
    public void setBarberEmail(String barberEmail) { this.barberEmail = barberEmail; }

    public UUID getBarbershopId() { return barbershopId; }
    public void setBarbershopId(UUID barbershopId) { this.barbershopId = barbershopId; }

    public String getBarbershopName() { return barbershopName; }
    public void setBarbershopName(String barbershopName) { this.barbershopName = barbershopName; }

    public UUID getOwnerId() { return ownerId; }
    public void setOwnerId(UUID ownerId) { this.ownerId = ownerId; }

    public String getRequestType() { return requestType; }
    public void setRequestType(String requestType) { this.requestType = requestType; }
}
