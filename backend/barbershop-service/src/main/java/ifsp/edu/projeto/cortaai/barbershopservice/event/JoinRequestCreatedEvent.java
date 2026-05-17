package ifsp.edu.projeto.cortaai.barbershopservice.event;

import java.util.UUID;

/**
 * Evento publicado quando um barbeiro solicita entrada em uma barbearia.
 * O notification-service consume este evento e notifica o dono (owner) via IN_APP.
 */
public class JoinRequestCreatedEvent {

    private UUID requestId;
    private UUID barberId;
    private String barberName;
    private String barberEmail;
    private UUID barbershopId;
    private String barbershopName;
    private UUID ownerId;
    private String ownerEmail;
    /** "JOIN" = barbeiro pediu entrada; "INVITE" = owner convidou barbeiro. */
    private String requestType;

    public JoinRequestCreatedEvent() {
    }

    public JoinRequestCreatedEvent(UUID requestId, UUID barberId, String barberName, String barberEmail,
                                   UUID barbershopId, String barbershopName, UUID ownerId, String ownerEmail,
                                   String requestType) {
        this.requestId = requestId;
        this.barberId = barberId;
        this.barberName = barberName;
        this.barberEmail = barberEmail;
        this.barbershopId = barbershopId;
        this.barbershopName = barbershopName;
        this.ownerId = ownerId;
        this.ownerEmail = ownerEmail;
        this.requestType = requestType;
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

    public String getOwnerEmail() { return ownerEmail; }
    public void setOwnerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; }

    public String getRequestType() { return requestType; }
    public void setRequestType(String requestType) { this.requestType = requestType; }
}
