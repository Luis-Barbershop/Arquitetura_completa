package ifsp.edu.projeto.cortaai.barbershopservice.controller;

import ifsp.edu.projeto.cortaai.barbershopservice.dto.FixedExpenseRequestDTO;
import ifsp.edu.projeto.cortaai.barbershopservice.dto.FixedExpenseResponseDTO;
import ifsp.edu.projeto.cortaai.barbershopservice.model.enums.FixedExpenseCategory;
import ifsp.edu.projeto.cortaai.barbershopservice.service.FixedExpenseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FixedExpenseControllerTest {

    private FixedExpenseService service;
    private FixedExpenseController controller;
    private Principal principal;

    @BeforeEach
    void setUp() {
        service = mock(FixedExpenseService.class);
        controller = new FixedExpenseController(service);
        principal = () -> "owner-uid";
    }

    @Test
    void shouldListCreateAndDeleteFixedExpenses() {
        UUID expenseId = UUID.randomUUID();
        FixedExpenseRequestDTO request = new FixedExpenseRequestDTO(
                FixedExpenseCategory.ALUGUEL,
                null,
                BigDecimal.valueOf(1200),
                5,
                2026,
                true
        );
        FixedExpenseResponseDTO response = new FixedExpenseResponseDTO(
                expenseId,
                FixedExpenseCategory.ALUGUEL,
                "Aluguel",
                null,
                BigDecimal.valueOf(1200),
                5,
                2026,
                true,
                LocalDateTime.now()
        );
        when(service.list("owner-uid", 5, 2026)).thenReturn(List.of(response));
        when(service.create("owner-uid", request)).thenReturn(response);

        assertThat(controller.list(principal, 5, 2026).getBody()).containsExactly(response);
        assertThat(controller.create(principal, request).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(controller.create(principal, request).getBody()).isEqualTo(response);
        assertThat(controller.delete(principal, expenseId).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        verify(service).delete("owner-uid", expenseId);
    }
}
