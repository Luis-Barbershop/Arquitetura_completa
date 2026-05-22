package ifsp.edu.projeto.cortaai.userservice.config;

import com.cloudinary.Cloudinary;
import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class UserServiceConfigTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldCreateCloudinaryBeanWithConfiguredValues() {
        CloudinaryConfig config = new CloudinaryConfig();
        ReflectionTestUtils.setField(config, "cloudName", "demo-cloud");
        ReflectionTestUtils.setField(config, "apiKey", "api-key");
        ReflectionTestUtils.setField(config, "apiSecret", "api-secret");

        Cloudinary cloudinary = config.cloudinary();

        assertThat(cloudinary.config.cloudName).isEqualTo("demo-cloud");
        assertThat(cloudinary.config.apiKey).isEqualTo("api-key");
        assertThat(cloudinary.config.apiSecret).isEqualTo("api-secret");
        assertThat(cloudinary.config.secure).isTrue();
    }

    @Test
    void shouldCreateOpenApiMetadataAndSecurityScheme() {
        OpenAPI openAPI = new OpenApiConfig().customOpenAPI();

        assertThat(openAPI.getInfo().getTitle()).contains("User Service");
        assertThat(openAPI.getServers()).hasSize(2);
        assertThat(openAPI.getComponents().getSecuritySchemes()).containsKey("Firebase Bearer Token");
        assertThat(openAPI.getSecurity()).isNotEmpty();
    }

    @Test
    void shouldCreateRabbitBeans() {
        RabbitConfig config = new RabbitConfig();
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);

        TopicExchange exchange = config.cortaaiExchange();
        RabbitTemplate template = config.rabbitTemplate(connectionFactory);

        assertThat(exchange.getName()).isEqualTo(RabbitConfig.EXCHANGE);
        assertThat(config.jsonMessageConverter()).isInstanceOf(Jackson2JsonMessageConverter.class);
        assertThat(template.getMessageConverter()).isInstanceOf(Jackson2JsonMessageConverter.class);
    }

    @Test
    void shouldAddOctetStreamSupportToJacksonConverter() {
        MappingJackson2HttpMessageConverter jackson = new MappingJackson2HttpMessageConverter();
        List<HttpMessageConverter<?>> converters = new ArrayList<>();
        converters.add(jackson);

        new WebConfig().extendMessageConverters(converters);

        assertThat(jackson.getSupportedMediaTypes())
                .containsExactly(MediaType.APPLICATION_JSON, MediaType.APPLICATION_OCTET_STREAM);
    }

    @Test
    void firebaseHeaderFilterShouldPopulateSecurityContextFromGatewayHeaders() throws Exception {
        OncePerRequestFilter filter = new SecurityConfig().firebaseHeaderFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/customers/me");
        request.addHeader("X-User-UID", "firebase-uid");
        request.addHeader("X-User-Email", "ana@example.com");
        request.addHeader("X-User-Type", "barber");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication.getPrincipal()).isEqualTo("firebase-uid");
        assertThat(authentication.getCredentials()).isEqualTo("ana@example.com");
        assertThat(authentication.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_BARBER");
    }

    @Test
    void firebaseHeaderFilterShouldKeepExistingAuthenticationAndDefaultRoleWhenMissingType() throws Exception {
        OncePerRequestFilter filter = new SecurityConfig().firebaseHeaderFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/customers/me");
        request.addHeader("X-User-UID", "firebase-uid");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_CUSTOMER");
    }
}
