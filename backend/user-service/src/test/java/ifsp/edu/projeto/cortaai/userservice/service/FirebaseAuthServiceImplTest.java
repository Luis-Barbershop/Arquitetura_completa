package ifsp.edu.projeto.cortaai.userservice.service;

import com.google.firebase.ErrorCode;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import ifsp.edu.projeto.cortaai.userservice.dto.AuthResponseDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.BarbershopInfoDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.CompleteProfileBarberDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.CompleteProfileCustomerDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.FirebaseAuthRequestDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.OnboardingPageProgressDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.OnboardingProgressDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.OnboardingRoleProgressDTO;
import ifsp.edu.projeto.cortaai.userservice.exception.ExternalServiceUnavailableException;
import ifsp.edu.projeto.cortaai.userservice.exception.NotFoundException;
import ifsp.edu.projeto.cortaai.userservice.exception.RoleConflictException;
import ifsp.edu.projeto.cortaai.userservice.feign.BarbershopServiceClient;
import ifsp.edu.projeto.cortaai.userservice.model.Barber;
import ifsp.edu.projeto.cortaai.userservice.model.Customer;
import ifsp.edu.projeto.cortaai.userservice.repository.BarberRepository;
import ifsp.edu.projeto.cortaai.userservice.repository.CustomerRepository;
import ifsp.edu.projeto.cortaai.userservice.service.impl.FirebaseAuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Constructor;
import java.time.LocalDate;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FirebaseAuthServiceImplTest {

    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private BarberRepository barberRepository;
    @Mock
    private BarbershopServiceClient barbershopServiceClient;

    private FirebaseAuthServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FirebaseAuthServiceImpl(null, customerRepository, barberRepository, barbershopServiceClient, idToken -> null);
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldBlockUnverifiedEmailPasswordLoginBeforeRepositoryLookup() throws Exception {
        service = serviceWithToken(firebaseToken("firebase-uid", "Ana", "ana@example.com", "password", false));

        AuthResponseDTO response = service.verifyAndProvision(new FirebaseAuthRequestDTO("id-token", "BARBER"));

        assertThat(response.id()).isNull();
        assertThat(response.userType()).isEqualTo("BARBER");
        assertThat(response.role()).isEqualTo("ROLE_BARBER");
        assertThat(response.emailVerified()).isFalse();
        assertThat(response.verificationRequired()).isTrue();
    }

    @Test
    void shouldVerifyAndReturnExistingBarberByFirebaseUid() throws Exception {
        UUID barbershopId = UUID.randomUUID();
        Barber barber = barber(UUID.randomUUID(), "barber-uid");
        barber.setBarbershopId(barbershopId);
        service = serviceWithToken(firebaseToken("barber-uid", "Bia", "BIA@Example.COM", "google.com", true));
        when(barberRepository.findByFirebaseUid("barber-uid")).thenReturn(Optional.of(barber));
        when(barbershopServiceClient.getBarbershopById(barbershopId))
                .thenReturn(new BarbershopInfoDTO(barbershopId, "Barbearia Central"));

        AuthResponseDTO response = service.verifyAndProvision(new FirebaseAuthRequestDTO("id-token", "BARBER"));

        assertThat(response.id()).isEqualTo(barber.getId());
        assertThat(response.userType()).isEqualTo("BARBER");
        assertThat(response.authProvider()).isEqualTo("EMAIL");
        assertThat(response.barbershopName()).isEqualTo("Barbearia Central");
    }

    @Test
    void shouldRejectLoginWhenRequestedRoleConflictsWithExistingCustomer() throws Exception {
        Customer customer = customer(UUID.randomUUID(), "customer-uid");
        service = serviceWithToken(firebaseToken("customer-uid", "Ana", "ana@example.com", "password", true));
        when(barberRepository.findByFirebaseUid("customer-uid")).thenReturn(Optional.empty());
        when(customerRepository.findByFirebaseUid("customer-uid")).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> service.verifyAndProvision(new FirebaseAuthRequestDTO("id-token", "BARBER")))
                .isInstanceOf(RoleConflictException.class)
                .hasMessageContaining("cliente");
    }

    @Test
    void shouldFindExistingCustomerByEmailDuringMigration() throws Exception {
        Customer customer = customer(UUID.randomUUID(), "old-uid");
        service = serviceWithToken(firebaseToken("new-uid", "Ana", "ANA@Example.COM", "github.com", true));
        when(barberRepository.findByFirebaseUid("new-uid")).thenReturn(Optional.empty());
        when(customerRepository.findByFirebaseUid("new-uid")).thenReturn(Optional.empty());
        when(barberRepository.findByEmail("ana@example.com")).thenReturn(Optional.empty());
        when(customerRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(customer));

        AuthResponseDTO response = service.verifyAndProvision(new FirebaseAuthRequestDTO("id-token", "CUSTOMER"));

        assertThat(response.id()).isEqualTo(customer.getId());
        assertThat(response.userType()).isEqualTo("CUSTOMER");
        assertThat(response.authProvider()).isEqualTo("EMAIL");
    }

    @Test
    void shouldReturnIncompleteDefaultCustomerForNewVerifiedFirebaseUser() throws Exception {
        service = serviceWithToken(firebaseToken("new-uid", null, "new@example.com", "unknown-provider", true));
        when(barberRepository.findByFirebaseUid("new-uid")).thenReturn(Optional.empty());
        when(customerRepository.findByFirebaseUid("new-uid")).thenReturn(Optional.empty());
        when(barberRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(customerRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());

        AuthResponseDTO response = service.verifyAndProvision(new FirebaseAuthRequestDTO("id-token", null));

        assertThat(response.id()).isNull();
        assertThat(response.name()).isEqualTo("Usuário");
        assertThat(response.userType()).isEqualTo("CUSTOMER");
        assertThat(response.authProvider()).isEqualTo("UNKNOWN-PROVIDER");
        assertThat(response.profileComplete()).isFalse();
    }

    @Test
    void shouldWrapFirebaseTokenVerificationFailures() {
        service = new FirebaseAuthServiceImpl(null, customerRepository, barberRepository, barbershopServiceClient,
                idToken -> {
                    throw new FirebaseAuthException(new FirebaseException(ErrorCode.UNAUTHENTICATED, "bad token", null));
                });

        assertThatThrownBy(() -> service.verifyAndProvision(new FirebaseAuthRequestDTO("bad-token", "CUSTOMER")))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Token Firebase inválido");
    }

    @Test
    void shouldCompleteNewCustomerProfileUsingExplicitEmail() {
        CompleteProfileCustomerDTO dto = new CompleteProfileCustomerDTO(
                "11999999999", "123.456.789-09", "Ana", LocalDate.of(1990, 1, 1));
        when(customerRepository.findByFirebaseUid("customer-uid")).thenReturn(Optional.empty());

        AuthResponseDTO response = service.completeCustomerProfile("customer-uid", dto, "ANA@Example.COM");

        assertThat(response.name()).isEqualTo("Ana");
        assertThat(response.email()).isEqualTo("ana@example.com");
        assertThat(response.documentCPF()).isEqualTo("12345678909");
        assertThat(response.profileComplete()).isTrue();
        verify(customerRepository).saveAndFlush(org.mockito.ArgumentMatchers.argThat(customer ->
                "customer-uid".equals(customer.getFirebaseUid())
                        && "ana@example.com".equals(customer.getEmail())
                        && "12345678909".equals(customer.getDocumentCPF())
        ));
    }

    @Test
    void shouldCompleteExistingCustomerUsingSecurityContextEmailFallbacks() {
        Customer customer = customer(UUID.randomUUID(), "customer-uid");
        customer.setEmail(null);
        customer.setName(null);
        when(customerRepository.findByFirebaseUid("customer-uid")).thenReturn(Optional.of(customer));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("customer-uid", "ctx@example.com"));

        AuthResponseDTO response = service.completeCustomerProfile("customer-uid",
                new CompleteProfileCustomerDTO("11999999999", "12345678909", "", LocalDate.of(1991, 2, 3)));

        assertThat(response.name()).isEqualTo("Usuário");
        assertThat(response.email()).isEqualTo("ctx@example.com");
        assertThat(response.profileComplete()).isTrue();
    }

    @Test
    void shouldCompleteNewBarberProfileAndResolveBarbershopNameWhenPresent() {
        UUID barbershopId = UUID.randomUUID();
        CompleteProfileBarberDTO dto = new CompleteProfileBarberDTO(
                "11988888888", "987.654.321-00", "Bia", LocalDate.of(1988, 2, 2), true);
        when(barberRepository.findByFirebaseUid("barber-uid")).thenReturn(Optional.empty());

        AuthResponseDTO response = service.completeBarberProfile("barber-uid", dto, "BIA@Example.COM");

        assertThat(response.name()).isEqualTo("Bia");
        assertThat(response.email()).isEqualTo("bia@example.com");
        assertThat(response.userType()).isEqualTo("BARBER");
        assertThat(response.isOwner()).isTrue();
        verify(barberRepository).saveAndFlush(org.mockito.ArgumentMatchers.argThat(barber ->
                "barber-uid".equals(barber.getFirebaseUid())
                        && barber.isOwner()
                        && "98765432100".equals(barber.getDocumentCPF())
        ));

        Barber barber = barber(UUID.randomUUID(), "barber-existing");
        barber.setBarbershopId(barbershopId);
        when(barberRepository.findByFirebaseUid("barber-existing")).thenReturn(Optional.of(barber));
        when(barbershopServiceClient.getBarbershopById(barbershopId))
                .thenReturn(new BarbershopInfoDTO(barbershopId, "Barbearia Central"));

        AuthResponseDTO existing = service.getMe("barber-existing");

        assertThat(existing.barbershopName()).isEqualTo("Barbearia Central");
    }

    @Test
    void shouldReturnMeForCustomerAndHandleMissingUser() {
        Customer customer = customer(UUID.randomUUID(), "customer-uid");
        when(customerRepository.findByFirebaseUid("customer-uid")).thenReturn(Optional.of(customer));

        AuthResponseDTO response = service.getMe("customer-uid");

        assertThat(response.userType()).isEqualTo("CUSTOMER");
        assertThat(response.role()).isEqualTo("ROLE_CUSTOMER");

        when(customerRepository.findByFirebaseUid("missing")).thenReturn(Optional.empty());
        when(barberRepository.findByFirebaseUid("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMe("missing"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void shouldIgnoreBarbershopLookupFailuresWhenBuildingBarberResponse() {
        UUID barbershopId = UUID.randomUUID();
        Barber barber = barber(UUID.randomUUID(), "barber-uid");
        barber.setBarbershopId(barbershopId);
        when(customerRepository.findByFirebaseUid("barber-uid")).thenReturn(Optional.empty());
        when(barberRepository.findByFirebaseUid("barber-uid")).thenReturn(Optional.of(barber));
        when(barbershopServiceClient.getBarbershopById(barbershopId))
                .thenThrow(new ExternalServiceUnavailableException("offline"));

        AuthResponseDTO response = service.getMe("barber-uid");

        assertThat(response.barbershopId()).isEqualTo(barbershopId);
        assertThat(response.barbershopName()).isNull();
    }

    @Test
    void shouldReturnDefaultOnboardingProgressWhenCustomerHasNoSnapshot() {
        Customer customer = customer(UUID.randomUUID(), "customer-uid");
        customer.setOnboardingProgressJson(null);
        when(customerRepository.findByFirebaseUid("customer-uid")).thenReturn(Optional.of(customer));

        OnboardingProgressDTO response = service.getOnboardingProgress("customer-uid");

        assertThat(response.version()).isEqualTo(1);
        assertThat(response.progressByRole()).isEmpty();
    }

    @Test
    void shouldPersistAndReturnOnboardingProgressForCustomer() {
        Customer customer = customer(UUID.randomUUID(), "customer-uid");
        when(customerRepository.findByFirebaseUid("customer-uid")).thenReturn(Optional.of(customer));

        Map<String, OnboardingRoleProgressDTO> progressByRole = new LinkedHashMap<>();
        progressByRole.put("owner", new OnboardingRoleProgressDTO(Map.of(
                "owner-manage-shop", new OnboardingPageProgressDTO("2026-06-03T21:00:00.000Z")
        )));

        OnboardingProgressDTO payload = new OnboardingProgressDTO(1, progressByRole);

        OnboardingProgressDTO updated = service.updateOnboardingProgress("customer-uid", payload);

        assertThat(updated.version()).isEqualTo(1);
        assertThat(updated.progressByRole()).containsKey("owner");
        assertThat(customer.getOnboardingProgressJson()).contains("owner-manage-shop");
        verify(customerRepository).save(customer);
    }

    @Test
    void shouldReadBarberOnboardingProgressFromStoredJson() {
        Barber barber = barber(UUID.randomUUID(), "barber-uid");
        barber.setOnboardingProgressJson("{\"version\":1,\"progressByRole\":{\"barber\":{\"completedPages\":{\"barber-home\":{\"completedAt\":\"2026-06-03T20:00:00.000Z\"}}}}}");
        when(customerRepository.findByFirebaseUid("barber-uid")).thenReturn(Optional.empty());
        when(barberRepository.findByFirebaseUid("barber-uid")).thenReturn(Optional.of(barber));

        OnboardingProgressDTO response = service.getOnboardingProgress("barber-uid");

        assertThat(response.version()).isEqualTo(1);
        assertThat(response.progressByRole()).containsKey("barber");
        assertThat(response.progressByRole().get("barber").completedPages()).containsKey("barber-home");
    }

    private Customer customer(UUID id, String uid) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setFirebaseUid(uid);
        customer.setName("Ana");
        customer.setEmail("ana@example.com");
        customer.setTell("11999999999");
        customer.setDocumentCPF("12345678909");
        customer.setBirthDate(LocalDate.of(1990, 1, 1));
        customer.setAuthProvider("EMAIL");
        return customer;
    }

    private Barber barber(UUID id, String uid) {
        return Barber.builder()
                .id(id)
                .firebaseUid(uid)
                .name("Bia")
                .email("bia@example.com")
                .tell("11988888888")
                .documentCPF("98765432100")
                .birthDate(LocalDate.of(1988, 2, 2))
                .authProvider("EMAIL")
                .role("ROLE_BARBER")
                .isOwner(false)
                .actAsBarber(true)
                .build();
    }

    private FirebaseAuthServiceImpl serviceWithToken(FirebaseToken token) {
        return new FirebaseAuthServiceImpl(null, customerRepository, barberRepository, barbershopServiceClient, idToken -> token);
    }

    private FirebaseToken firebaseToken(String uid,
                                        String name,
                                        String email,
                                        String provider,
                                        boolean emailVerified) throws Exception {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", uid);
        claims.put("user_id", uid);
        if (name != null) {
            claims.put("name", name);
        }
        if (email != null) {
            claims.put("email", email);
        }
        claims.put("picture", "https://cdn/avatar.png");
        claims.put("iss", "https://securetoken.google.com/cortaai");
        claims.put("aud", "cortaai");
        claims.put("iat", Instant.now().getEpochSecond());
        claims.put("exp", Instant.now().plusSeconds(3600).getEpochSecond());
        claims.put("email_verified", emailVerified);
        claims.put("firebase", Map.of("sign_in_provider", provider));

        Constructor<FirebaseToken> constructor = FirebaseToken.class.getDeclaredConstructor(Map.class);
        constructor.setAccessible(true);
        return constructor.newInstance(claims);
    }
}
