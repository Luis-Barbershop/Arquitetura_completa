package ifsp.edu.projeto.cortaai.barbershopservice.model.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FixedExpenseCategoryTest {

    @Test
    void shouldExposeHumanReadableLabelsForAllCategories() {
        assertThat(FixedExpenseCategory.AGUA.getLabel()).isEqualTo("Água");
        assertThat(FixedExpenseCategory.LUZ.getLabel()).isEqualTo("Luz");
        assertThat(FixedExpenseCategory.ALUGUEL.getLabel()).isEqualTo("Aluguel");
        assertThat(FixedExpenseCategory.INTERNET.getLabel()).isEqualTo("Internet");
        assertThat(FixedExpenseCategory.ENERGIA.getLabel()).isEqualTo("Energia");
        assertThat(FixedExpenseCategory.FUNCIONARIOS.getLabel()).isEqualTo("Funcionários");
        assertThat(FixedExpenseCategory.MATERIAL.getLabel()).isEqualTo("Material");
        assertThat(FixedExpenseCategory.SISTEMA.getLabel()).isEqualTo("Sistema");
        assertThat(FixedExpenseCategory.CONTABILIDADE.getLabel()).isEqualTo("Contabilidade");
        assertThat(FixedExpenseCategory.MARKETING.getLabel()).isEqualTo("Marketing");
        assertThat(FixedExpenseCategory.MANUTENCAO.getLabel()).isEqualTo("Manutenção");
        assertThat(FixedExpenseCategory.OUTROS.getLabel()).isEqualTo("Outros");
    }
}
