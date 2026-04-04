package ifsp.edu.projeto.cortaai.paymentservice.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * DTO para informações básicas do usuário retornadas pelo user-service.
 * Usado pelo payment-service para enriquecer eventos com dados do customer.
 */
@Getter
@Setter
public class UserInfoDTO {
    private UUID id;
    private String name;
    private String email;
    private String userType;
}
