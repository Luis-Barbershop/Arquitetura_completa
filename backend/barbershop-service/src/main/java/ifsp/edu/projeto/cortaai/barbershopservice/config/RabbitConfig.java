package ifsp.edu.projeto.cortaai.barbershopservice.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração do RabbitMQ para o barbershop-service.
 * Reutiliza o exchange cortaai.events já declarado pelo notification-service.
 */
@Configuration
public class RabbitConfig {

    /** Exchange compartilhado por todos os serviços. */
    public static final String EXCHANGE = "cortaai.events";

    /** Routing key para pedido de entrada de barbeiro na barbearia. */
    public static final String RK_JOIN_REQUEST_CREATED = "barbershop.join-request.created";

    /** Routing key para remoção de colaborador da barbearia. */
    public static final String RK_BARBER_REMOVED = "barber.removed";

    @Bean
    public TopicExchange cortaaiEventsExchange() {
        // durable=true, autoDelete=false — mesmos parâmetros do notification-service
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }
}
