package ifsp.edu.projeto.cortaai.userservice.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Payload enviado pelo cliente ao fazer login/registro via Firebase.
 *
 * <p>O campo {@code idToken} é o Firebase ID Token obtido pelo SDK cliente
 * (web, iOS, Android) após autenticar com qualquer provider (Google, Facebook,
 * Apple, GitHub, Twitter, email/senha, etc.).
 *
 * <p>O campo {@code userType} indica se o usuário quer entrar como
 * {@code CUSTOMER} ou {@code BARBER}. É obrigatório apenas no primeiro acesso;
 * nas subsequentes o sistema já conhece o tipo.
 */
public record FirebaseAuthRequestDTO(

        @NotBlank(message = "idToken é obrigatório")
        String idToken,

        /**
         * CUSTOMER | BARBER
         * Se omitido (null), o sistema tenta descobrir pelo registro existente.
         */
        String userType
) {}
