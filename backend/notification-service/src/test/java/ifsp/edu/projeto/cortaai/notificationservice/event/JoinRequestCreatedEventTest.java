package ifsp.edu.projeto.cortaai.notificationservice.event;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JoinRequestCreatedEventTest {

    @Test
    void shouldExposeValuesSetViaSetters() {
        UUID requestId = UUID.randomUUID();
        UUID barberId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        JoinRequestCreatedEvent event = new JoinRequestCreatedEvent();
        event.setRequestId(requestId);
        event.setBarberId(barberId);
        event.setBarberName("Carlos");
        event.setBarberEmail("carlos@example.com");
        event.setBarbershopId(shopId);
        event.setBarbershopName("Barbearia do Sul");
        event.setOwnerId(ownerId);
        event.setOwnerEmail("owner@example.com");
        event.setRequestType("JOIN");

        assertThat(event.getRequestId()).isEqualTo(requestId);
        assertThat(event.getBarberId()).isEqualTo(barberId);
        assertThat(event.getBarberName()).isEqualTo("Carlos");
        assertThat(event.getBarberEmail()).isEqualTo("carlos@example.com");
        assertThat(event.getBarbershopId()).isEqualTo(shopId);
        assertThat(event.getBarbershopName()).isEqualTo("Barbearia do Sul");
        assertThat(event.getOwnerId()).isEqualTo(ownerId);
        assertThat(event.getOwnerEmail()).isEqualTo("owner@example.com");
        assertThat(event.getRequestType()).isEqualTo("JOIN");
    }

    @Test
    void shouldReturnNullsForUninitializedEvent() {
        JoinRequestCreatedEvent event = new JoinRequestCreatedEvent();

        assertThat(event.getRequestId()).isNull();
        assertThat(event.getBarberId()).isNull();
        assertThat(event.getBarberName()).isNull();
        assertThat(event.getBarberEmail()).isNull();
        assertThat(event.getBarbershopId()).isNull();
        assertThat(event.getBarbershopName()).isNull();
        assertThat(event.getOwnerId()).isNull();
        assertThat(event.getOwnerEmail()).isNull();
        assertThat(event.getRequestType()).isNull();
    }

    @Test
    void shouldDistinguishJoinAndInviteRequestTypes() {
        JoinRequestCreatedEvent join = new JoinRequestCreatedEvent();
        join.setRequestType("JOIN");

        JoinRequestCreatedEvent invite = new JoinRequestCreatedEvent();
        invite.setRequestType("INVITE");

        assertThat(join.getRequestType()).isEqualTo("JOIN");
        assertThat(invite.getRequestType()).isEqualTo("INVITE");
        assertThat(join.getRequestType()).isNotEqualTo(invite.getRequestType());
    }
}
