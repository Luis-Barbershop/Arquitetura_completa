package ifsp.edu.projeto.cortaai.scheduleservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScheduleConfigTest {

    @Test
    void shouldCreateAiRestTemplate() {
        assertThat(new AiConfig().restTemplate()).isInstanceOf(RestTemplate.class);
    }

    @Test
    void shouldCreateOpenApiMetadata() {
        var api = new OpenApiConfig().customOpenAPI();

        assertThat(api.getInfo().getTitle()).isEqualTo("Schedule Service API");
        assertThat(api.getInfo().getVersion()).isEqualTo("1.0");
        assertThat(api.getInfo().getContact().getName()).isEqualTo("CortaAí");
    }

    @Test
    void shouldCreateRabbitComponents() {
        RabbitConfig config = new RabbitConfig();

        assertThat(config.cortaaiExchange().getName()).isEqualTo(RabbitConfig.EXCHANGE);
        assertThat(config.customerDeletedQueue().getName()).isEqualTo(RabbitConfig.QUEUE_CUSTOMER_DELETED);
        assertThat(config.customerDeletedQueue().isDurable()).isTrue();
        assertThat(config.bindCustomerDeleted().getExchange()).isEqualTo(RabbitConfig.EXCHANGE);
        assertThat(config.bindCustomerDeleted().getRoutingKey()).isEqualTo(RabbitConfig.RK_CUSTOMER_DELETED);
        assertThat(config.jackson2JsonMessageConverter()).isInstanceOf(Jackson2JsonMessageConverter.class);
    }

    @Test
    void shouldCreateRedisCacheManagerAndHandleCacheErrors() {
        RedisConfig config = new RedisConfig();
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
        Cache cache = mock(Cache.class);
        when(cache.getName()).thenReturn("appointments");

        assertThat(config.cacheManager(connectionFactory)).isNotNull();

        CacheErrorHandler errorHandler = config.cacheErrorHandler();
        errorHandler.handleCacheGetError(new RuntimeException("get"), cache, "key");
        errorHandler.handleCachePutError(new RuntimeException("put"), cache, "key", "value");
        errorHandler.handleCacheEvictError(new RuntimeException("evict"), cache, "key");
        errorHandler.handleCacheClearError(new RuntimeException("clear"), cache);
        errorHandler.handleCacheGetError(new RuntimeException("unknown"), null, "key");
    }
}
