package ifsp.edu.projeto.cortaai.barbershopservice.model.enums;

public enum FixedExpenseCategory {
    AGUA,
    LUZ,
    ALUGUEL,
    INTERNET,
    ENERGIA,
    FUNCIONARIOS,
    MATERIAL,
    SISTEMA,
    CONTABILIDADE,
    MARKETING,
    MANUTENCAO,
    OUTROS;

    public String getLabel() {
        return switch (this) {
            case AGUA        -> "Água";
            case LUZ         -> "Luz";
            case ALUGUEL     -> "Aluguel";
            case INTERNET    -> "Internet";
            case ENERGIA     -> "Energia";
            case FUNCIONARIOS -> "Funcionários";
            case MATERIAL    -> "Material";
            case SISTEMA     -> "Sistema";
            case CONTABILIDADE -> "Contabilidade";
            case MARKETING   -> "Marketing";
            case MANUTENCAO  -> "Manutenção";
            case OUTROS      -> "Outros";
        };
    }
}
