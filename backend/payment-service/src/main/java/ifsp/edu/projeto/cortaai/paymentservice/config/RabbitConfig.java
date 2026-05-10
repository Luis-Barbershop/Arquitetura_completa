package ifsp.edu.projeto.cortaai.paymentservice.config;

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
    public static final String RK_PAYMENT_APPROVED = "payment.approved";
    public static final String RK_CUSTOMER_DELETED = "customer.deleted";
    public static final String QUEUE_CUSTOMER_DELETED = "payment.customer.deleted";

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Queue customerDeletedQueue() {
        return QueueBuilder.durable(QUEUE_CUSTOMER_DELETED).build();
    }

    @Bean
    public Binding bindCustomerDeleted() {
        return BindingBuilder.bind(customerDeletedQueue()).to(exchange()).with(RK_CUSTOMER_DELETED);
    }

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
