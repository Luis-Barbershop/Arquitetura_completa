package ifsp.edu.projeto.cortaai.userservice.config;

import ifsp.edu.projeto.cortaai.userservice.service.JwtTokenService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class JwtAuthorizationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;

    public JwtAuthorizationFilter(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        final String authorizationHeader = request.getHeader("Authorization");

        // 1. Verifica se o header existe e está no formato "Bearer token"
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String token = authorizationHeader.substring(7); // Remove o "Bearer "
        final Claims claims = jwtTokenService.validateTokenAndGetClaims(token);

        // 2. Valida o token e extrai as permissões (claims)
        if (claims == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Cria a lista de Autoridades (Roles) com base nas claims
        List<GrantedAuthority> authorities = new ArrayList<>();
        String userType = claims.get("userType", String.class);

        if ("CUSTOMER".equals(userType)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
        } else if ("BARBER".equals(userType)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_BARBER"));
            Boolean isOwner = claims.get("isOwner", Boolean.class);
            if (isOwner != null && isOwner) {
                authorities.add(new SimpleGrantedAuthority("ROLE_OWNER"));
            }
        }

        // 4. Cria o objeto de Autenticação
        // O "principal" é o e-mail (subject), "credentials" é nulo, e passamos as autoridades
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                claims.getSubject(), // email
                null,
                authorities
        );

        // 5. Define o usuário como autenticado no contexto de segurança do Spring
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }
}