package ifsp.edu.projeto.cortaai.userservice.messaging.event;

import java.util.UUID;

public record CustomerDeletedEvent(UUID customerId) {}
