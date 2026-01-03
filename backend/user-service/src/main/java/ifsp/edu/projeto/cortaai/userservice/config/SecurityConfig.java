package ifsp.edu.projeto.cortaai.userservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.Customizer;

@Configuration
public class SecurityConfig {

    private final JwtAuthorizationFilter jwtAuthorizationFilter;

    public SecurityConfig(JwtAuthorizationFilter jwtAuthorizationFilter) {
        this.jwtAuthorizationFilter = jwtAuthorizationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(authorize -> authorize
                        // 1. Endpoints PÚBLICOS (Registro, Login, Swagger, Listagens)
                        .requestMatchers(
                                "/api/customers/register",
                                "/api/customers/login",
                                "/api/barbers/register",
                                "/api/barbers/login"
                        ).permitAll()
                        .requestMatchers(
                                "/",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/barbershops",
                                "/api/barbershops/{shopId}/activities",
                                "/api/barbershops/{shopId}/barbers",
                                "/api/barbers/{id}/availability" // Consulta de disponibilidade
                        ).permitAll()

                        // 2. Endpoints de CLIENTE (ROLE_CUSTOMER)
                        .requestMatchers("/api/customers/me/**").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.GET, "/api/appointments/customer/me").hasRole("CUSTOMER")

                        // 3. Endpoints de AGENDAMENTO (Qualquer usuário autenticado)
                        // A lógica de quem pode fazer o quê (cliente, dono)
                        // já está segura dentro do AppointmentsServiceImpl (Passo 4.1)
                        .requestMatchers("/api/appointments/**").authenticated()

                        // 4. Endpoints de BARBEIRO (ROLE_BARBER)
                        // Ações de perfil e ações de "staff" (entrar/sair da loja)
                        .requestMatchers(
                                "/api/barbers/me/**",
                                "/api/barbershops/join-request",
                                "/api/barbershops/leave-shop"
                        ).hasRole("BARBER")

                        // 5. Endpoints de DONO (ROLE_OWNER)
                        // Um BARBEIRO pode se *tornar* dono
                        .requestMatchers("/api/barbershops/register-my-shop").hasRole("BARBER")
                        // Mas apenas um DONO pode gerenciar a loja
                        .requestMatchers("/api/barbershops/my-shop/**").hasRole("OWNER")

                        // 6. Qualquer outra requisição deve ser autenticada
                        .anyRequest().authenticated()
                )

                .addFilterBefore(jwtAuthorizationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

}
