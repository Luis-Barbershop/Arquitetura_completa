package ifsp.edu.projeto.cortaai.apigateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

/**
 * CORS agora é gerenciado inteiramente pelo application.yml (spring.cloud.gateway.globalcors).
 * Esta classe foi desativada para evitar conflito de headers duplicados.
 *
 * O Bean CorsWebFilter que existia aqui causava duplicação de
 * Access-Control-Allow-Origin quando combinado com globalcors ou com o Nginx,
 * resultando em "Network Error" no browser.
 */
@Configuration
public class CorsConfig {

    private static final Logger log = LoggerFactory.getLogger(CorsConfig.class);

    // Bean CorsWebFilter REMOVIDO — CORS via globalcors no application.yml
    // Se precisar reativar, use UM ou OUTRO, nunca os dois juntos.
}