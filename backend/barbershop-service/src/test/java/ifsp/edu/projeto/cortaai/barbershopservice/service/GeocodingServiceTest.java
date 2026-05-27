package ifsp.edu.projeto.cortaai.barbershopservice.service;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class GeocodingServiceTest {

    private final GeocodingService service = new GeocodingService();

    @Test
    void shouldReturnNullForNullOrBlankAddress() {
        assertThat(service.geocode(null)).isNull();
        assertThat(service.geocode("   ")).isNull();
    }

    @Test
    void shouldRemoveAddressComplementsAndNormalizeFormat() throws Exception {
        java.lang.reflect.Method clean = GeocodingService.class.getDeclaredMethod("clean", String.class);
        clean.setAccessible(true);

        // CEP removido
        String result = (String) clean.invoke(service, "CEP: 01310-100 Av Paulista, 1578, São Paulo");
        assertThat(result).doesNotContain("CEP");
        assertThat(result).doesNotContain("01310-100");

        // Complementos removidos
        result = (String) clean.invoke(service, "Rua das Flores, apto 12, Bloco B, São Paulo");
        assertThat(result).doesNotContain("apto");
        assertThat(result).doesNotContain("Bloco");

        // Apartamento com variações
        result = (String) clean.invoke(service, "Av Brasil, apt 5, andar 3, Rio de Janeiro");
        assertThat(result).doesNotContain("apt");
        assertThat(result).doesNotContain("andar");

        // Apart* e casa
        result = (String) clean.invoke(service, "Rua X, apartamento 10, casa 2, SP");
        assertThat(result).doesNotContain("apartamento");
        assertThat(result).doesNotContain("casa");

        // UF no final removida
        result = (String) clean.invoke(service, "Rua das Flores, 123 - SP");
        assertThat(result).doesNotContain("- SP");

        // Vírgulas duplicadas normalizadas
        result = (String) clean.invoke(service, "Rua X, , São Paulo");
        assertThat(result).doesNotContain(",,");

        // Vírgula no final removida
        result = (String) clean.invoke(service, "Rua X, São Paulo,");
        assertThat(result).doesNotEndWith(",");
    }

    @Test
    void shouldTruncateInputOver300Chars() throws Exception {
        java.lang.reflect.Method clean = GeocodingService.class.getDeclaredMethod("clean", String.class);
        clean.setAccessible(true);

        String longAddress = "A".repeat(500);
        String result = (String) clean.invoke(service, longAddress);
        assertThat(result.length()).isLessThanOrEqualTo(300);
    }
}
