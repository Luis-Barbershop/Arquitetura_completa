package ifsp.edu.projeto.cortaai.notificationservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "cortaai.events";

    // Queues
    public static final String QUEUE_APPOINTMENT_CREATED = "notification.appointment.created";
    public static final String QUEUE_APPOINTMENT_CANCELLED = "notification.appointment.cancelled";
    public static final String QUEUE_APPOINTMENT_CONCLUDED = "notification.appointment.concluded";
    public static final String QUEUE_PAYMENT_APPROVED = "notification.payment.approved";

    // Routing keys
    public static final String RK_APPOINTMENT_CREATED = "appointment.created";
    public static final String RK_APPOINTMENT_CANCELLED = "appointment.cancelled";
    public static final String RK_APPOINTMENT_CONCLUDED = "appointment.concluded";
    public static final String RK_PAYMENT_APPROVED = "payment.approved";

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE);
    }

    // --- Queues ---
    @Bean
    public Queue appointmentCreatedQueue() {
        return QueueBuilder.durable(QUEUE_APPOINTMENT_CREATED).build();
    }

    @Bean
    public Queue appointmentCancelledQueue() {
        return QueueBuilder.durable(QUEUE_APPOINTMENT_CANCELLED).build();
    }

    @Bean
    public Queue appointmentConcludedQueue() {
        return QueueBuilder.durable(QUEUE_APPOINTMENT_CONCLUDED).build();
    }

    @Bean
    public Queue paymentApprovedQueue() {
        return QueueBuilder.durable(QUEUE_PAYMENT_APPROVED).build();
    }

    // --- Bindings ---
    @Bean
    public Binding bindAppointmentCreated() {
        return BindingBuilder.bind(appointmentCreatedQueue()).to(exchange()).with(RK_APPOINTMENT_CREATED);
    }

    @Bean
    public Binding bindAppointmentCancelled() {
        return BindingBuilder.bind(appointmentCancelledQueue()).to(exchange()).with(RK_APPOINTMENT_CANCELLED);
    }

    @Bean
    public Binding bindAppointmentConcluded() {
        return BindingBuilder.bind(appointmentConcludedQueue()).to(exchange()).with(RK_APPOINTMENT_CONCLUDED);
    }

    @Bean
    public Binding bindPaymentApproved() {
        return BindingBuilder.bind(paymentApprovedQueue()).to(exchange()).with(RK_PAYMENT_APPROVED);
    }

    // --- JSON converter ---
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
