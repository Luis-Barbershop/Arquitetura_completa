package ifsp.edu.projeto.cortaai.productservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigTest {

    @Test
    void shouldBuildProductServiceOpenApiMetadata() {
        OpenAPI openAPI = new OpenApiConfig().customOpenAPI();

        assertThat(openAPI.getInfo().getTitle()).isEqualTo("Product Service API");
        assertThat(openAPI.getInfo().getDescription()).contains("catálogo");
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("1.0");
        assertThat(openAPI.getInfo().getContact().getName()).isEqualTo("CortaAí");
        assertThat(openAPI.getInfo().getContact().getUrl()).isEqualTo("https://github.com/AppCortaAi");
    }
}
