package ifsp.edu.projeto.cortaai.userservice.controller;

import ifsp.edu.projeto.cortaai.userservice.dto.ChangePasswordRequestDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.ChangePasswordResponseDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.EmailExistsResponseDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.FirebaseEmailRegisterRequestDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.FirebaseEmailRegisterResponseDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.FirebaseEmailSignInRequestDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.FirebaseEmailSignInResponseDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.FirebaseTokenDebugRequestDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.FirebaseTokenDebugResponseDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.ForgotPasswordRequestDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.ResendVerificationRequestDTO;
import ifsp.edu.projeto.cortaai.userservice.service.FirebaseDebugService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FirebaseTestControllerTest {

    @Mock
    private FirebaseDebugService firebaseDebugService;

    private FirebaseTestController controller;

    @BeforeEach
    void setUp() {
        controller = new FirebaseTestController(firebaseDebugService);
    }

    @Test
    void shouldDelegateEmailPasswordLoginAndTokenVerification() {
        FirebaseEmailSignInRequestDTO loginRequest = new FirebaseEmailSignInRequestDTO("ana@example.com", "Secret123!");
        FirebaseEmailSignInResponseDTO loginResponse = new FirebaseEmailSignInResponseDTO(
                "id-token", "refresh", "3600", "uid", "ana@example.com", true);
        FirebaseTokenDebugRequestDTO tokenRequest = new FirebaseTokenDebugRequestDTO("id-token");
        FirebaseTokenDebugResponseDTO tokenResponse = new FirebaseTokenDebugResponseDTO(
                "uid", "ana@example.com", "Ana", "issuer", "aud", "iat", "exp", Map.of("role", "CUSTOMER"));
        when(firebaseDebugService.signInWithEmailPassword(loginRequest)).thenReturn(loginResponse);
        when(firebaseDebugService.verifyToken("id-token")).thenReturn(tokenResponse);

        assertThat(controller.signInWithEmail(loginRequest).getBody()).isEqualTo(loginResponse);
        assertThat(controller.verifyIdToken(tokenRequest).getBody()).isEqualTo(tokenResponse);
    }

    @Test
    void shouldDelegateRegisterAndChangePassword() {
        FirebaseEmailRegisterRequestDTO registerRequest = new FirebaseEmailRegisterRequestDTO(
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
        FirebaseEmailRegisterResponseDTO registerResponse = new FirebaseEmailRegisterResponseDTO(
                "id-token", "refresh", "3600", "uid", null);
        ChangePasswordRequestDTO changeRequest = new ChangePasswordRequestDTO("old-token", "NewSecret123!");
        ChangePasswordResponseDTO changeResponse = new ChangePasswordResponseDTO("new-token", "new-refresh");
        when(firebaseDebugService.registerWithEmailPassword(registerRequest)).thenReturn(registerResponse);
        when(firebaseDebugService.changePassword(changeRequest)).thenReturn(changeResponse);

        assertThat(controller.registerWithEmail(registerRequest).getBody()).isEqualTo(registerResponse);
        assertThat(controller.changePassword(changeRequest).getBody()).isEqualTo(changeResponse);
    }

    @Test
    void shouldDelegatePasswordAndVerificationFlows() {
        ForgotPasswordRequestDTO forgotPassword = new ForgotPasswordRequestDTO("ana@example.com");
        ResendVerificationRequestDTO resendVerification = new ResendVerificationRequestDTO("ana@example.com", "Secret123!");

        assertThat(controller.forgotPassword(forgotPassword).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(controller.resendForgotPassword(forgotPassword).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(controller.resendVerification(resendVerification).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        verify(firebaseDebugService).forgotPassword(forgotPassword);
        verify(firebaseDebugService).resendForgotPassword(forgotPassword);
        verify(firebaseDebugService).resendVerificationEmail(resendVerification);
    }

    @Test
    void shouldCheckEmailExistence() {
        EmailExistsResponseDTO response = new EmailExistsResponseDTO(true, "BARBER");
        when(firebaseDebugService.checkEmailExists("barber@example.com")).thenReturn(response);

        assertThat(controller.emailExists("barber@example.com").getBody()).isEqualTo(response);
    }
}
