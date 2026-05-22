package ifsp.edu.projeto.cortaai.userservice.validator;

import ifsp.edu.projeto.cortaai.userservice.dto.BarberDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.CustomerDTO;
import ifsp.edu.projeto.cortaai.userservice.service.impl.BarberServiceImpl;
import ifsp.edu.projeto.cortaai.userservice.service.impl.CustomerServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.HandlerMapping;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UniqueValidatorsTest {

    @Mock
    private CustomerServiceImpl customerService;
    @Mock
    private BarberServiceImpl barberService;
    @Mock
    private HttpServletRequest request;

    private UUID currentId;

    @BeforeEach
    void setUp() {
        currentId = UUID.randomUUID();
        when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
                .thenReturn(Map.of("id", currentId.toString()));
    }

    @Test
    void customerValidatorsShouldAcceptNullAndCurrentRecordValues() {
        CustomerDTO current = customer("ana@example.com", "11999999999", "12345678909");
        when(customerService.get(currentId)).thenReturn(current);

        var email = new CustomerEmailUnique.CustomerEmailUniqueValidator(customerService, request);
        var tell = new CustomerTellUnique.CustomerTellUniqueValidator(customerService, request);
        var cpf = new CustomerDocumentCPFUnique.CustomerDocumentCPFUniqueValidator(customerService, request);

        assertThat(email.isValid(null, null)).isTrue();
        assertThat(email.isValid(" ANA@EXAMPLE.COM ", null)).isTrue();
        assertThat(tell.isValid("11999999999", null)).isTrue();
        assertThat(cpf.isValid("123.456.789-09", null)).isTrue();
        verify(customerService, never()).emailExists(" ANA@EXAMPLE.COM ");
    }

    @Test
    void customerValidatorsShouldRejectExistingValuesAndAcceptNewOnes() {
        CustomerDTO current = customer("old@example.com", "11888888888", "98765432100");
        when(customerService.get(currentId)).thenReturn(current);
        when(customerService.emailExists("novo@example.com")).thenReturn(false);
        when(customerService.tellExists("11999999999")).thenReturn(true);
        when(customerService.documentCPFExists("12345678909")).thenReturn(true);

        var email = new CustomerEmailUnique.CustomerEmailUniqueValidator(customerService, request);
        var tell = new CustomerTellUnique.CustomerTellUniqueValidator(customerService, request);
        var cpf = new CustomerDocumentCPFUnique.CustomerDocumentCPFUniqueValidator(customerService, request);

        assertThat(email.isValid("novo@example.com", null)).isTrue();
        assertThat(tell.isValid("11999999999", null)).isFalse();
        assertThat(cpf.isValid("12345678909", null)).isFalse();
    }

    @Test
    void barberValidatorsShouldAcceptCurrentRecordValues() {
        BarberDTO current = barber("barber@example.com", "11999999999", "12345678909");
        when(barberService.get(currentId)).thenReturn(current);

        var email = new BarberEmailUnique.BarberEmailUniqueValidator(barberService, request);
        var tell = new BarberTellUnique.BarberTellUniqueValidator(barberService, request);
        var cpf = new BarberDocumentCPFUnique.BarberDocumentCPFUniqueValidator(barberService, request);

        assertThat(email.isValid(null, null)).isTrue();
        assertThat(email.isValid(" BARBER@EXAMPLE.COM ", null)).isTrue();
        assertThat(tell.isValid("11999999999", null)).isTrue();
        assertThat(cpf.isValid("123.456.789-09", null)).isTrue();
        verify(barberService, never()).documentCPFExists("123.456.789-09");
    }

    @Test
    void barberValidatorsShouldDelegateUniquenessChecksForDifferentValues() {
        BarberDTO current = barber("old@example.com", "11888888888", "98765432100");
        when(barberService.get(currentId)).thenReturn(current);
        when(barberService.emailExists("novo@example.com")).thenReturn(true);
        when(barberService.tellExists("11999999999")).thenReturn(false);
        when(barberService.documentCPFExists("12345678909")).thenReturn(false);

        var email = new BarberEmailUnique.BarberEmailUniqueValidator(barberService, request);
        var tell = new BarberTellUnique.BarberTellUniqueValidator(barberService, request);
        var cpf = new BarberDocumentCPFUnique.BarberDocumentCPFUniqueValidator(barberService, request);

        assertThat(email.isValid("novo@example.com", null)).isFalse();
        assertThat(tell.isValid("11999999999", null)).isTrue();
        assertThat(cpf.isValid("12345678909", null)).isTrue();
    }

    private CustomerDTO customer(String email, String tell, String cpf) {
        CustomerDTO dto = new CustomerDTO();
        dto.setId(currentId);
        dto.setName("Cliente");
        dto.setEmail(email);
        dto.setTell(tell);
        dto.setDocumentCPF(cpf);
        dto.setBirthDate(LocalDate.of(1990, 1, 1));
        return dto;
    }

    private BarberDTO barber(String email, String tell, String cpf) {
        return new BarberDTO(
                currentId,
                "Barbeiro",
                email,
                tell,
                cpf,
                LocalDate.of(1990, 1, 1),
                false,
                true,
                UUID.randomUUID(),
                null,
                null,
                null,
                Set.of()
        );
    }
}
