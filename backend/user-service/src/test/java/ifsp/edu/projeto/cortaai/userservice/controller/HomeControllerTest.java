package ifsp.edu.projeto.cortaai.userservice.controller;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HomeControllerTest {

    @Test
    void shouldRedirectRootToSwaggerUi() {
        assertThat(new HomeController().index()).isEqualTo("redirect:/swagger-ui/index.html");
    }
}
