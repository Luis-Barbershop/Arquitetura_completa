package ifsp.edu.projeto.cortaai.userservice.controller;

import ifsp.edu.projeto.cortaai.userservice.dto.AuthResponseDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.CompleteProfileBarberDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.CompleteProfileCustomerDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.FirebaseAuthRequestDTO;
import ifsp.edu.projeto.cortaai.userservice.service.FirebaseAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private FirebaseAuthService firebaseAuthService;

    private AuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthController(firebaseAuthService);
    }

    @Test
    void shouldVerifyTokenAndReturnAuthResponse() {
        FirebaseAuthRequestDTO request = new FirebaseAuthRequestDTO("id-token", "CUSTOMER");
        AuthResponseDTO auth = authResponse("CUSTOMER");
        when(firebaseAuthService.verifyAndProvision(request)).thenReturn(auth);

        ResponseEntity<AuthResponseDTO> response = controller.verify(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(auth);
    }

    @Test
    void shouldReturnAuthenticatedUserProfile() {
        AuthResponseDTO auth = authResponse("CUSTOMER");
        when(firebaseAuthService.getMe("firebase-uid")).thenReturn(auth);

        ResponseEntity<AuthResponseDTO> response = controller.me("firebase-uid");

        assertThat(response.getBody()).isEqualTo(auth);
    }

    @Test
    void shouldCompleteCustomerAndBarberProfiles() {
        CompleteProfileCustomerDTO customerDTO = new CompleteProfileCustomerDTO(
                "11999999999",
                "12345678909",
                "Cliente",
                LocalDate.of(1990, 1, 1)
        );
        CompleteProfileBarberDTO barberDTO = new CompleteProfileBarberDTO(
                "11988888888",
                "98765432100",
                "Barbeiro",
                LocalDate.of(1988, 2, 2),
                true
        );
        AuthResponseDTO customerAuth = authResponse("CUSTOMER");
        AuthResponseDTO barberAuth = authResponse("BARBER");
        when(firebaseAuthService.completeCustomerProfile("customer-uid", customerDTO)).thenReturn(customerAuth);
        when(firebaseAuthService.completeBarberProfile("barber-uid", barberDTO)).thenReturn(barberAuth);

        assertThat(controller.completeCustomerProfile("customer-uid", customerDTO).getBody()).isEqualTo(customerAuth);
        assertThat(controller.completeBarberProfile("barber-uid", barberDTO).getBody()).isEqualTo(barberAuth);

        verify(firebaseAuthService).completeCustomerProfile("customer-uid", customerDTO);
        verify(firebaseAuthService).completeBarberProfile("barber-uid", barberDTO);
    }

    private AuthResponseDTO authResponse(String userType) {
        return new AuthResponseDTO(
                UUID.randomUUID(),
                "Usuário",
                "user@example.com",
                "11999999999",
                "12345678909",
                "https://cdn/user.png",
                userType,
                "EMAIL",
                true,
                "BARBER".equals(userType) ? "ROLE_BARBER" : "ROLE_CUSTOMER",
                true,
                false,
                null,
                null,
                false,
                true
        );
    }
}
