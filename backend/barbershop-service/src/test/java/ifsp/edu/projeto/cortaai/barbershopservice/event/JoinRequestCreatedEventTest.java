package ifsp.edu.projeto.cortaai.barbershopservice.event;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JoinRequestCreatedEventTest {

    @Test
    void shouldExposeConstructorValues() {
        UUID requestId = UUID.randomUUID();
        UUID barberId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        JoinRequestCreatedEvent event = new JoinRequestCreatedEvent(
                requestId,
                barberId,
                "Ana",
                "ana@example.com",
                shopId,
                "Barbearia Centro",
                ownerId,
                "JOIN"
        );

        assertThat(event.getRequestId()).isEqualTo(requestId);
        assertThat(event.getBarberId()).isEqualTo(barberId);
        assertThat(event.getBarberName()).isEqualTo("Ana");
        assertThat(event.getBarberEmail()).isEqualTo("ana@example.com");
        assertThat(event.getBarbershopId()).isEqualTo(shopId);
        assertThat(event.getBarbershopName()).isEqualTo("Barbearia Centro");
        assertThat(event.getOwnerId()).isEqualTo(ownerId);
        assertThat(event.getRequestType()).isEqualTo("JOIN");
    }

    @Test
    void shouldExposeMutatedValues() {
        UUID requestId = UUID.randomUUID();
        UUID barberId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        JoinRequestCreatedEvent event = new JoinRequestCreatedEvent();

        event.setRequestId(requestId);
        event.setBarberId(barberId);
        event.setBarberName("Bia");
        event.setBarberEmail("bia@example.com");
        event.setBarbershopId(shopId);
        event.setBarbershopName("Studio Bia");
        event.setOwnerId(ownerId);
        event.setRequestType("INVITE");

        assertThat(event.getRequestId()).isEqualTo(requestId);
        assertThat(event.getBarberId()).isEqualTo(barberId);
        assertThat(event.getBarberName()).isEqualTo("Bia");
        assertThat(event.getBarberEmail()).isEqualTo("bia@example.com");
        assertThat(event.getBarbershopId()).isEqualTo(shopId);
        assertThat(event.getBarbershopName()).isEqualTo("Studio Bia");
        assertThat(event.getOwnerId()).isEqualTo(ownerId);
        assertThat(event.getRequestType()).isEqualTo("INVITE");
    }
}
