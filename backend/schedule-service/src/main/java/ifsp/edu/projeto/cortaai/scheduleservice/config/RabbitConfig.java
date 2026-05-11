package ifsp.edu.projeto.cortaai.scheduleservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "cortaai.events";
    public static final String RK_CUSTOMER_DELETED = "customer.deleted";
    public static final String QUEUE_CUSTOMER_DELETED = "schedule.customer.deleted";
    public static final String RK_APPOINTMENT_REMINDER = "appointment.reminder";

    @Bean
    public TopicExchange cortaaiExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Queue customerDeletedQueue() {
        return QueueBuilder.durable(QUEUE_CUSTOMER_DELETED).build();
    }

    @Bean
    public Binding bindCustomerDeleted() {
        return BindingBuilder.bind(customerDeletedQueue()).to(cortaaiExchange()).with(RK_CUSTOMER_DELETED);
    }

    @Bean
    public MessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}

