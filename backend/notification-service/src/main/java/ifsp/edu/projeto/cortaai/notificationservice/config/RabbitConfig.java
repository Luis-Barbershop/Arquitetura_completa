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
    public static final String QUEUE_APPOINTMENT_RESCHEDULED = "notification.appointment.rescheduled";
    public static final String QUEUE_PAYMENT_APPROVED = "notification.payment.approved";
    public static final String QUEUE_JOIN_REQUEST_CREATED = "notification.barbershop.join-request.created";
    public static final String QUEUE_CUSTOMER_DELETED = "notification.customer.deleted";

    // Routing keys
    public static final String RK_APPOINTMENT_CREATED = "appointment.created";
    public static final String RK_APPOINTMENT_CANCELLED = "appointment.cancelled";
    public static final String RK_APPOINTMENT_CONCLUDED = "appointment.concluded";
    public static final String RK_APPOINTMENT_RESCHEDULED = "appointment.rescheduled";
    public static final String RK_PAYMENT_APPROVED = "payment.approved";
    public static final String RK_JOIN_REQUEST_CREATED = "barbershop.join-request.created";
    public static final String RK_CUSTOMER_DELETED = "customer.deleted";

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
    public Queue appointmentRescheduledQueue() {
        return QueueBuilder.durable(QUEUE_APPOINTMENT_RESCHEDULED).build();
    }

    @Bean
    public Queue paymentApprovedQueue() {
        return QueueBuilder.durable(QUEUE_PAYMENT_APPROVED).build();
    }

    @Bean
    public Queue joinRequestCreatedQueue() {
        return QueueBuilder.durable(QUEUE_JOIN_REQUEST_CREATED).build();
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
    public Binding bindAppointmentRescheduled() {
        return BindingBuilder.bind(appointmentRescheduledQueue()).to(exchange()).with(RK_APPOINTMENT_RESCHEDULED);
    }

    @Bean
    public Binding bindPaymentApproved() {
        return BindingBuilder.bind(paymentApprovedQueue()).to(exchange()).with(RK_PAYMENT_APPROVED);
    }

    @Bean
    public Binding bindJoinRequestCreated() {
        return BindingBuilder.bind(joinRequestCreatedQueue()).to(exchange()).with(RK_JOIN_REQUEST_CREATED);
    }

    @Bean
    public Queue customerDeletedQueue() {
        return QueueBuilder.durable(QUEUE_CUSTOMER_DELETED).build();
    }

    @Bean
    public Binding bindCustomerDeleted() {
        return BindingBuilder.bind(customerDeletedQueue()).to(exchange()).with(RK_CUSTOMER_DELETED);
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
