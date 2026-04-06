package ifsp.edu.projeto.cortaai.userservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(req -> req
                // Permite requisições de preflight CORS
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() 
                
                // CORREÇÃO: Liberta a rota de erro para não dar um falso 403
                .requestMatchers("/error").permitAll()
                
                // Swagger / Actuator
                .requestMatchers(
                        "/swagger-ui/**", "/swagger-ui.html",
                        "/v3/api-docs/**", "/v3/api-docs.yaml",
                        "/actuator/**"
                ).permitAll()
                // Auth público — TODAS as rotas de login e registo
                .requestMatchers(HttpMethod.POST, 
                        "/api/auth/verify", "/api/auth/verify/",
                        "/api/auth/email/login", "/api/auth/email/login/",
                        "/api/auth/email/verify-token", "/api/auth/email/verify-token/",
                        "/api/auth/email/register", "/api/auth/email/register/",
                        // Recuperação de senha — não exige token
                        "/api/auth/email/forgot-password", "/api/auth/email/forgot-password/",
                        "/api/auth/firebase-test/forgot-password", "/api/auth/firebase-test/forgot-password/",
                        "/api/auth/firebase-test/sign-in-email", "/api/auth/firebase-test/sign-in-email/",
                        "/api/auth/firebase-test/verify-id-token", "/api/auth/firebase-test/verify-id-token/",
                        "/api/auth/firebase-test/register-email", "/api/auth/firebase-test/register-email/",
                        "/api/customers/login", "/api/customers/login/",
                        "/api/barbers/login", "/api/barbers/login/",
                        "/api/customers/register", "/api/customers/register/",
                        "/api/barbers/register", "/api/barbers/register/"
                ).permitAll()
                // Complete-profile exige autenticação (Gateway injeta X-User-UID)
                .requestMatchers(HttpMethod.POST,
                        "/api/auth/customers/complete-profile", "/api/auth/customers/complete-profile/",
                        "/api/auth/barbers/complete-profile", "/api/auth/barbers/complete-profile/"
                ).authenticated()
                // Endpoints internos (Feign inter-serviço)
                .requestMatchers("/api/internal/**").permitAll()
                // Listagem pública de barbeiros
                .requestMatchers(HttpMethod.GET, "/api/barbers", "/api/barbers/**").permitAll()
                // Todo o resto exige UID injetado pelo Gateway
                .anyRequest().authenticated()
            )
            .addFilterBefore(firebaseHeaderFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public OncePerRequestFilter firebaseHeaderFilter() {
        return new OncePerRequestFilter() {

            private final Logger log = LoggerFactory.getLogger("FirebaseHeaderFilter");

            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain filterChain)
                    throws ServletException, IOException {

                String uid      = request.getHeader("X-User-UID");
                String email    = request.getHeader("X-User-Email");
                String userType = request.getHeader("X-User-Type");

                if (uid != null && !uid.isBlank()
                        && SecurityContextHolder.getContext().getAuthentication() == null) {

                    String role = "ROLE_" + (userType != null ? userType.toUpperCase() : "CUSTOMER");

                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(
                                    uid,    // principal = firebaseUid
                                    email,  // credentials = email
                                    List.of(new SimpleGrantedAuthority(role))
                            );
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);

                    log.debug("SecurityContext populado — uid={} role={}", uid, role);
                }

                filterChain.doFilter(request, response);
            }
        };
    }
}