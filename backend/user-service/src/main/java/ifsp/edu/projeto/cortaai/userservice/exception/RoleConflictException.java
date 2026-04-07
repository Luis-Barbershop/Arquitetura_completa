package ifsp.edu.projeto.cortaai.userservice.exception;

/**
 * Lançada quando o usuário tenta autenticar com um userType incompatível
 * com o papel registrado na base de dados.
 * Ex: conta BARBER tentando entrar via portal de CUSTOMER.
 */
public class RoleConflictException extends RuntimeException {

    private final String actualRole;

    public RoleConflictException(String message, String actualRole) {
        super(message);
        this.actualRole = actualRole;
    }

    public String getActualRole() {
        return actualRole;
    }
}
