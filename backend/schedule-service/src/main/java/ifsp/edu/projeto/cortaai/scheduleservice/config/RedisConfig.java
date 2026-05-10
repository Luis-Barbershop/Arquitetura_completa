package ifsp.edu.projeto.cortaai.scheduleservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Configuration
@Slf4j
public class RedisConfig {

    @Bean
        public CacheManager cacheManager(@NonNull RedisConnectionFactory connectionFactory) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        GenericJackson2JsonRedisSerializer serializer =
                new GenericJackson2JsonRedisSerializer(mapper);

        Duration defaultTtl = Objects.requireNonNull(Duration.ofMinutes(5));
        Duration availabilityTtl = Objects.requireNonNull(Duration.ofMinutes(1));

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(defaultTtl)
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(serializer)
                )
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put("appointmentAvailability", config.entryTtl(availabilityTtl));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

        @Bean
        public CacheErrorHandler cacheErrorHandler() {
                return new CacheErrorHandler() {
                        @Override
                        public void handleCacheGetError(@NonNull RuntimeException exception, @NonNull Cache cache, @NonNull Object key) {
                                log.warn("Falha ao ler cache '{}' para chave '{}'. Seguindo sem cache.", cacheName(cache), key, exception);
                        }

                        @Override
                        public void handleCachePutError(@NonNull RuntimeException exception, @NonNull Cache cache, @NonNull Object key, @Nullable Object value) {
                                log.warn("Falha ao gravar cache '{}' para chave '{}'. Seguindo sem cache.", cacheName(cache), key, exception);
                        }

                        @Override
                        public void handleCacheEvictError(@NonNull RuntimeException exception, @NonNull Cache cache, @NonNull Object key) {
                                log.warn("Falha ao remover chave '{}' do cache '{}'.", key, cacheName(cache), exception);
                        }

                        @Override
                        public void handleCacheClearError(@NonNull RuntimeException exception, @NonNull Cache cache) {
                                log.warn("Falha ao limpar cache '{}'.", cacheName(cache), exception);
                        }

                        private String cacheName(Cache cache) {
                                return cache != null ? cache.getName() : "unknown";
                        }
                };
        }

    @Bean
    public RedisTemplate<String, String> stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        StringRedisSerializer serializer = new StringRedisSerializer();
        template.setKeySerializer(serializer);
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(serializer);
        template.setHashValueSerializer(serializer);
        template.afterPropertiesSet();
        return template;
    }
}

