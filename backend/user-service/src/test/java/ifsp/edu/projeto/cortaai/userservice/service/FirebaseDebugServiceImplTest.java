package ifsp.edu.projeto.cortaai.userservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import ifsp.edu.projeto.cortaai.userservice.dto.ChangePasswordRequestDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.FirebaseEmailRegisterRequestDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.FirebaseEmailSignInRequestDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.ForgotPasswordRequestDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.ResendVerificationRequestDTO;
import ifsp.edu.projeto.cortaai.userservice.repository.BarberRepository;
import ifsp.edu.projeto.cortaai.userservice.repository.CustomerRepository;
import ifsp.edu.projeto.cortaai.userservice.service.impl.FirebaseDebugServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FirebaseDebugServiceImplTest {

    @Mock
    private FirebaseAuthService firebaseAuthService;
    @Mock
    private BarberRepository barberRepository;
    @Mock
    private CustomerRepository customerRepository;

    private FirebaseDebugServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FirebaseDebugServiceImpl(
                null,
                firebaseAuthService,
                new ObjectMapper(),
                barberRepository,
                customerRepository
        );
    }

    @Test
    void shouldRejectFirebaseRestOperationsWhenApiKeyIsMissing() {
        assertThatThrownBy(() -> service.signInWithEmailPassword(new FirebaseEmailSignInRequestDTO("ana@example.com", "Secret123!")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("firebase.web-api-key");
        assertThatThrownBy(() -> service.registerWithEmailPassword(registerRequest()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("firebase.web-api-key");
        assertThatThrownBy(() -> service.forgotPassword(new ForgotPasswordRequestDTO("ana@example.com")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("firebase.web-api-key");
        assertThatThrownBy(() -> service.resendForgotPassword(new ForgotPasswordRequestDTO("ana@example.com")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("firebase.web-api-key");
        assertThatThrownBy(() -> service.changePassword(new ChangePasswordRequestDTO("token", "Secret123!")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("firebase.web-api-key");
        assertThatThrownBy(() -> service.resendVerificationEmail(new ResendVerificationRequestDTO("ana@example.com", "Secret123!")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("firebase.web-api-key");
    }

    @Test
    void shouldCheckEmailExistenceWithBarberPrecedence() {
        when(barberRepository.existsByEmailIgnoreCase("barber@example.com")).thenReturn(true);
        when(barberRepository.existsByEmailIgnoreCase("customer@example.com")).thenReturn(false);
        when(customerRepository.existsByEmailIgnoreCase("customer@example.com")).thenReturn(true);
        when(barberRepository.existsByEmailIgnoreCase("none@example.com")).thenReturn(false);
        when(customerRepository.existsByEmailIgnoreCase("none@example.com")).thenReturn(false);

        assertThat(service.checkEmailExists(" BARBER@Example.COM ").userType()).isEqualTo("BARBER");
        assertThat(service.checkEmailExists(" CUSTOMER@Example.COM ").userType()).isEqualTo("CUSTOMER");
        assertThat(service.checkEmailExists(" none@Example.COM ").exists()).isFalse();
    }

    @Test
    void shouldRejectBlankEmailExistenceCheck() {
        assertThatThrownBy(() -> service.checkEmailExists(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("E-mail não pode ser vazio");
    }

    @Test
    void shouldWrapInvalidTokenWhenFirebaseAuthIsUnavailableInUnitTest() {
        ReflectionTestUtils.setField(service, "firebaseWebApiKey", "fake-key");

        assertThatThrownBy(() -> service.verifyToken("id-token"))
                .isInstanceOf(NullPointerException.class);
    }

    private FirebaseEmailRegisterRequestDTO registerRequest() {
        return new FirebaseEmailRegisterRequestDTO(
                "ana@example.com",
                "Secret123!",
                "CUSTOMER",
                "Ana",
                "11999999999",
                "12345678909",
                LocalDate.of(1990, 1, 1),
                null,
                null,
                false
        );
    }
}
