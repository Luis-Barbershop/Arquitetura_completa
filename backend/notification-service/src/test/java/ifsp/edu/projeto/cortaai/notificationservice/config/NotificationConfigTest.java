package ifsp.edu.projeto.cortaai.notificationservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class NotificationConfigTest {

    @Test
    void shouldCreateOpenApiMetadata() {
        var api = new OpenApiConfig().customOpenAPI();

        assertThat(api.getInfo().getTitle()).isEqualTo("Notification Service API");
        assertThat(api.getInfo().getVersion()).isEqualTo("1.0");
        assertThat(api.getInfo().getContact().getUrl()).contains("github.com");
    }

    @Test
    void shouldCreateRedisTemplate() {
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);

        var template = new RedisConfig().stringRedisTemplate(connectionFactory);

        assertThat(template.getConnectionFactory()).isSameAs(connectionFactory);
    }

    @Test
    void shouldCreateRabbitQueuesBindingsAndTemplate() {
        RabbitConfig config = new RabbitConfig();

        assertQueue(config.appointmentCreatedQueue(), RabbitConfig.QUEUE_APPOINTMENT_CREATED);
        assertQueue(config.appointmentCancelledQueue(), RabbitConfig.QUEUE_APPOINTMENT_CANCELLED);
        assertQueue(config.appointmentConcludedQueue(), RabbitConfig.QUEUE_APPOINTMENT_CONCLUDED);
        assertQueue(config.appointmentRescheduledQueue(), RabbitConfig.QUEUE_APPOINTMENT_RESCHEDULED);
        assertQueue(config.paymentApprovedQueue(), RabbitConfig.QUEUE_PAYMENT_APPROVED);
        assertQueue(config.joinRequestCreatedQueue(), RabbitConfig.QUEUE_JOIN_REQUEST_CREATED);
        assertQueue(config.appointmentReminderQueue(), RabbitConfig.QUEUE_APPOINTMENT_REMINDER);
        assertQueue(config.customerDeletedQueue(), RabbitConfig.QUEUE_CUSTOMER_DELETED);

        assertBinding(config.bindAppointmentCreated(), RabbitConfig.RK_APPOINTMENT_CREATED);
        assertBinding(config.bindAppointmentCancelled(), RabbitConfig.RK_APPOINTMENT_CANCELLED);
        assertBinding(config.bindAppointmentConcluded(), RabbitConfig.RK_APPOINTMENT_CONCLUDED);
        assertBinding(config.bindAppointmentRescheduled(), RabbitConfig.RK_APPOINTMENT_RESCHEDULED);
        assertBinding(config.bindPaymentApproved(), RabbitConfig.RK_PAYMENT_APPROVED);
        assertBinding(config.bindJoinRequestCreated(), RabbitConfig.RK_JOIN_REQUEST_CREATED);
        assertBinding(config.bindAppointmentReminder(), RabbitConfig.RK_APPOINTMENT_REMINDER);
        assertBinding(config.bindCustomerDeleted(), RabbitConfig.RK_CUSTOMER_DELETED);

        assertThat(config.exchange().getName()).isEqualTo(RabbitConfig.EXCHANGE);
        assertThat(config.jsonMessageConverter()).isInstanceOf(Jackson2JsonMessageConverter.class);
        assertThat(config.rabbitTemplate(mock(ConnectionFactory.class)).getMessageConverter())
                .isInstanceOf(Jackson2JsonMessageConverter.class);
    }

    private static void assertQueue(Queue queue, String expectedName) {
        assertThat(queue.getName()).isEqualTo(expectedName);
        assertThat(queue.isDurable()).isTrue();
    }

    private static void assertBinding(Binding binding, String routingKey) {
        assertThat(binding.getExchange()).isEqualTo(RabbitConfig.EXCHANGE);
        assertThat(binding.getRoutingKey()).isEqualTo(routingKey);
    }
}
