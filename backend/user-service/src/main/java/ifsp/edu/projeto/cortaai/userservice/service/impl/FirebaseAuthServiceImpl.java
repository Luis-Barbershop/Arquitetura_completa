package ifsp.edu.projeto.cortaai.userservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
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
import ifsp.edu.projeto.cortaai.userservice.security.crypto.PrivacyHash;
import ifsp.edu.projeto.cortaai.userservice.service.FirebaseAuthService;
import feign.FeignException;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class FirebaseAuthServiceImpl implements FirebaseAuthService {

    private static final Logger log = LoggerFactory.getLogger(FirebaseAuthServiceImpl.class);
    private static final int ONBOARDING_VERSION = 1;

    private final FirebaseAuth firebaseAuth;
    private final CustomerRepository customerRepository;
    private final BarberRepository barberRepository;
    private final BarbershopServiceClient barbershopServiceClient;
    private final TokenVerifier tokenVerifier;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public FirebaseAuthServiceImpl(
            FirebaseAuth firebaseAuth,
            CustomerRepository customerRepository,
            BarberRepository barberRepository,
            BarbershopServiceClient barbershopServiceClient
    ) {
        this(firebaseAuth, customerRepository, barberRepository, barbershopServiceClient, firebaseAuth::verifyIdToken);
    }

    public FirebaseAuthServiceImpl(
            FirebaseAuth firebaseAuth,
            CustomerRepository customerRepository,
            BarberRepository barberRepository,
            BarbershopServiceClient barbershopServiceClient,
            TokenVerifier tokenVerifier
    ) {
        this.firebaseAuth = firebaseAuth;
        this.customerRepository = customerRepository;
        this.barberRepository = barberRepository;
        this.barbershopServiceClient = barbershopServiceClient;
        this.tokenVerifier = tokenVerifier;
    }

    @FunctionalInterface
    public interface TokenVerifier {
        FirebaseToken verify(String idToken) throws FirebaseAuthException;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Implementações públicas
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public AuthResponseDTO verifyAndProvision(FirebaseAuthRequestDTO request) {
        // 1. Valida o token Firebase
        FirebaseToken decoded = verifyToken(request.idToken());

        String uid      = decoded.getUid();
        String email    = PrivacyHash.normalizeEmail(decoded.getEmail());
        String name     = decoded.getName();
        String photoUrl = decoded.getPicture();
        String provider = extractProvider(decoded);
        boolean emailVerified = extractEmailVerified(decoded);
        boolean verificationRequired = isEmailVerificationRequired(provider, emailVerified);

    log.info("event=firebase-auth-ok uid={} provider={} userType={}", maskIdentifier(uid), provider, request.userType());

        String resolvedType = (request.userType() != null && !request.userType().isBlank())
                ? request.userType().toUpperCase()
                : "CUSTOMER";
        String resolvedRole = "BARBER".equals(resolvedType) ? "ROLE_BARBER" : "ROLE_CUSTOMER";

        if (verificationRequired) {
            log.info("event=login-blocked-email-not-verified uid={}", maskIdentifier(uid));
            return new AuthResponseDTO(
                    null,
                    name != null ? name : "Usuário",
                    email,
                    null,
                    null,
                    photoUrl,
                    resolvedType,
                    provider,
                    false,
                    resolvedRole,
                    false,
                    true,
                    null,  // barbershopId
                        null,  // barbershopName
                    null,  // isOwner
                    null   // actAsBarber
            );
        }

        // 2. Verifica se o usuário JÁ EXISTE no banco (por UID ou e-mail)
        //    Se existe e tem perfil completo, retorna direto.
        //    Se existe mas perfil incompleto, retorna com profileComplete=false.
        //    Se NÃO existe, retorna dados do Firebase com profileComplete=false
        //    SEM criar nada no banco — só salva quando o complete-profile for chamado.

        // Tenta como Barber primeiro (por UID)
        Optional<Barber> barberByUid = barberRepository.findByFirebaseUid(uid);
        if (barberByUid.isPresent()) {
            log.info("event=barber-found-by-uid uid={}", maskIdentifier(uid));
            assertRoleCompatible(request.userType(), "BARBER");
            return toAuthResponse(barberByUid.get(), emailVerified, false);
        }

        // Tenta como Customer (por UID)
        Optional<Customer> customerByUid = customerRepository.findByFirebaseUid(uid);
        if (customerByUid.isPresent()) {
            log.info("event=customer-found-by-uid uid={}", maskIdentifier(uid));
            assertRoleCompatible(request.userType(), "CUSTOMER");
            return toAuthResponse(customerByUid.get(), emailVerified, false);
        }

        // Tenta por e-mail (migração de conta antiga)
        // Apenas LEITURA — não tenta atualizar registros antigos aqui.
        // O complete-profile vai criar/atualizar corretamente depois.
        if (email != null) {
            Optional<Barber> barberByEmail = barberRepository.findByEmail(email);
            if (barberByEmail.isPresent()) {
                Barber existing = barberByEmail.get();
                log.info("event=barber-found-by-email-migration email={}", maskEmail(email));
                assertRoleCompatible(request.userType(), "BARBER");
                return toAuthResponse(existing, emailVerified, false);
            }
            Optional<Customer> customerByEmail = customerRepository.findByEmail(email);
            if (customerByEmail.isPresent()) {
                Customer existing = customerByEmail.get();
                log.info("event=customer-found-by-email-migration email={}", maskEmail(email));
                assertRoleCompatible(request.userType(), "CUSTOMER");
                return toAuthResponse(existing, emailVerified, false);
            }
        }

        // Usuário NOVO — NÃO salva no banco ainda!
        // Retorna um DTO provisório com profileComplete=false para o frontend mostrar o modal.
    log.info("event=user-not-found-awaiting-complete-profile uid={}", maskIdentifier(uid));
        return new AuthResponseDTO(
                null,                        // sem ID (ainda não foi salvo)
                name != null ? name : "Usuário",
                email,
                null,                        // sem telefone
            null,                        // sem CPF
                photoUrl,
                resolvedType,
                provider,
                false,                       // perfil NÃO completo
                resolvedRole,
                emailVerified,
                false,
                null,  // barbershopId
                null,  // barbershopName
                null,  // isOwner
                null   // actAsBarber
        );
    }

    @Override
    @Transactional
    public AuthResponseDTO completeCustomerProfile(String firebaseUid, CompleteProfileCustomerDTO dto) {
        return completeCustomerProfile(firebaseUid, dto, null);
    }

    @Override
    @Transactional
    public AuthResponseDTO completeCustomerProfile(String firebaseUid, CompleteProfileCustomerDTO dto, String email) {
        Customer customer = customerRepository.findByFirebaseUid(firebaseUid)
                .orElseGet(() -> {
                    log.info("event=customer-create-on-complete-profile uid={}", maskIdentifier(firebaseUid));
                    Customer novo = new Customer();
                    novo.setFirebaseUid(firebaseUid);
                    return novo;
                });

        if (dto.name() != null && !dto.name().isBlank()) {
            customer.setName(dto.name());
        }
        if (customer.getEmail() == null || customer.getEmail().isBlank()) {
            if (email != null && !email.isBlank()) {
                customer.setEmail(PrivacyHash.normalizeEmail(email));
            } else {
                var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.getCredentials() instanceof String emailFromHeader) {
                    customer.setEmail(PrivacyHash.normalizeEmail(emailFromHeader));
                } else {
                    customer.setEmail(firebaseUid + "@firebase.local");
                }
            }
        }
        if (customer.getName() == null || customer.getName().isBlank()) {
            customer.setName("Usuário");
        }
        customer.setTell(dto.tell());
        customer.setDocumentCPF(onlyDigits(dto.documentCPF()));
        if (dto.birthDate() != null) customer.setBirthDate(dto.birthDate());
        customer.setAuthProvider(customer.getAuthProvider() != null ? customer.getAuthProvider() : "EMAIL");

        customerRepository.saveAndFlush(customer);

        // Consulta o estado real de emailVerified no Firebase Admin SDK
        boolean emailVerified = true;
        try {
            UserRecord userRecord = firebaseAuth.getUser(firebaseUid);
            emailVerified = userRecord.isEmailVerified();
        } catch (Exception e) {
            log.warn("event=email-verified-check-failed uid={} reason={}", maskIdentifier(firebaseUid), sanitizeMessage(e.getMessage()));
        }
        return toAuthResponse(customer, emailVerified, false);
    }

    @Override
    @Transactional
    public AuthResponseDTO completeBarberProfile(String firebaseUid, CompleteProfileBarberDTO dto) {
        return completeBarberProfile(firebaseUid, dto, null);
    }

    @Override
    @Transactional
    public AuthResponseDTO completeBarberProfile(String firebaseUid, CompleteProfileBarberDTO dto, String email) {
        Barber barber = barberRepository.findByFirebaseUid(firebaseUid)
                .orElseGet(() -> {
                    log.info("event=barber-create-on-complete-profile uid={}", maskIdentifier(firebaseUid));
                    return Barber.builder()
                            .firebaseUid(firebaseUid)
                            .role("ROLE_BARBER")
                            .isOwner(false)
                            .actAsBarber(true)
                            .build();
                });

        if (dto.name() != null && !dto.name().isBlank()) {
            barber.setName(dto.name());
        }
        if (barber.getEmail() == null || barber.getEmail().isBlank()) {
            if (email != null && !email.isBlank()) {
                barber.setEmail(PrivacyHash.normalizeEmail(email));
            } else {
                var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.getCredentials() instanceof String emailFromHeader) {
                    barber.setEmail(PrivacyHash.normalizeEmail(emailFromHeader));
                } else {
                    barber.setEmail(firebaseUid + "@firebase.local");
                }
            }
        }
        if (barber.getName() == null || barber.getName().isBlank()) {
            barber.setName("Barbeiro");
        }
        barber.setTell(dto.tell());
        barber.setDocumentCPF(onlyDigits(dto.documentCPF()));
        if (dto.birthDate() != null) barber.setBirthDate(dto.birthDate());
        barber.setOwner(dto.isOwner());
        barber.setAuthProvider(barber.getAuthProvider() != null ? barber.getAuthProvider() : "EMAIL");

        barberRepository.saveAndFlush(barber);

        // Consulta o estado real de emailVerified no Firebase Admin SDK
        boolean emailVerified = true;
        try {
            UserRecord userRecord = firebaseAuth.getUser(firebaseUid);
            emailVerified = userRecord.isEmailVerified();
        } catch (Exception e) {
            log.warn("event=email-verified-check-failed uid={} reason={}", maskIdentifier(firebaseUid), sanitizeMessage(e.getMessage()));
        }
        return toAuthResponse(barber, emailVerified, false);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponseDTO getMe(String firebaseUid) {
        // Tenta como Customer primeiro
        Optional<Customer> customer = customerRepository.findByFirebaseUid(firebaseUid);
        if (customer.isPresent()) {
            return toAuthResponse(customer.get(), true, false);
        }

        // Depois tenta como Barber
        Optional<Barber> barber = barberRepository.findByFirebaseUid(firebaseUid);
        if (barber.isPresent()) {
            return toAuthResponse(barber.get(), true, false);
        }

        throw new NotFoundException("Usuário não encontrado para o UID: " + firebaseUid);
    }

    @Override
    @Transactional(readOnly = true)
    public OnboardingProgressDTO getOnboardingProgress(String firebaseUid) {
        Optional<Customer> customer = customerRepository.findByFirebaseUid(firebaseUid);
        if (customer.isPresent()) {
            return deserializeOnboarding(customer.get().getOnboardingProgressJson());
        }

        Optional<Barber> barber = barberRepository.findByFirebaseUid(firebaseUid);
        if (barber.isPresent()) {
            return deserializeOnboarding(barber.get().getOnboardingProgressJson());
        }

        throw new NotFoundException("Usuário não encontrado para o UID: " + firebaseUid);
    }

    @Override
    @Transactional
    public OnboardingProgressDTO updateOnboardingProgress(String firebaseUid, OnboardingProgressDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Payload de onboarding é obrigatório.");
        }

        OnboardingProgressDTO normalized = normalizeOnboarding(dto);
        String serialized = serializeOnboarding(normalized);

        Optional<Customer> customer = customerRepository.findByFirebaseUid(firebaseUid);
        if (customer.isPresent()) {
            Customer entity = customer.get();
            entity.setOnboardingProgressJson(serialized);
            customerRepository.save(entity);
            return normalized;
        }

        Optional<Barber> barber = barberRepository.findByFirebaseUid(firebaseUid);
        if (barber.isPresent()) {
            Barber entity = barber.get();
            entity.setOnboardingProgressJson(serialized);
            barberRepository.save(entity);
            return normalized;
        }

        throw new NotFoundException("Usuário não encontrado para o UID: " + firebaseUid);
    }


    @Override
    public void setCustomUserClaims(String uid, String role, boolean isOwner) {
        try {
            Map<String, Object> claims = new HashMap<>();
            claims.put("role", role);
            claims.put("isOwner", isOwner);
            FirebaseAuth.getInstance().setCustomUserClaims(uid, claims);
            log.info("event=custom-claims-updated uid={} role={} isOwner={}", maskIdentifier(uid), role, isOwner);
        } catch (FirebaseAuthException e) {
            log.error("event=custom-claims-update-failed uid={} reason={}", maskIdentifier(uid), sanitizeMessage(e.getMessage()));
            throw new IllegalStateException("Falha ao atualizar permissões do usuário", e);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers privados
    // ──────────────────────────────────────────────────────────────────────────

    private FirebaseToken verifyToken(String idToken) {
        try {
            return tokenVerifier.verify(idToken);
        } catch (FirebaseAuthException e) {
            log.warn("event=firebase-token-invalid reason={}", sanitizeMessage(e.getMessage()));
            throw new SecurityException("Token Firebase inválido ou expirado: " + e.getMessage());
        }
    }

    private String maskIdentifier(String value) {
        if (value == null || value.isBlank()) {
            return "***";
        }
        String normalized = value.trim();
        if (normalized.length() <= 6) {
            return "***";
        }
        return normalized.substring(0, 4) + "..." + normalized.substring(normalized.length() - 2);
    }

    private String maskEmail(String email) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            return "***";
        }
        String[] parts = email.split("@", 2);
        String local = parts[0];
        String domain = parts[1];
        String localMasked = local.length() <= 2 ? "***" : local.substring(0, 2) + "***";
        String domainMasked = domain.length() <= 3 ? "***" : domain.substring(0, 1) + "***";
        return localMasked + "@" + domainMasked;
    }

    private String sanitizeMessage(String value) {
        if (value == null || value.isBlank()) {
            return "n/a";
        }
        return value
                .replaceAll("(?i)bearer\\s+[a-z0-9._-]+", "bearer ***")
                .replaceAll("(?i)token[=:\\s]+[^\\s,;]+", "token=***")
                .replaceAll("(?i)authorization[^\\s]*", "authorization***");
    }

    private String onlyDigits(String value) {
        return value == null ? null : value.replaceAll("\\D", "");
    }

    private OnboardingProgressDTO defaultOnboarding() {
        return new OnboardingProgressDTO(ONBOARDING_VERSION, Map.of());
    }

    private OnboardingProgressDTO deserializeOnboarding(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return defaultOnboarding();
        }

        try {
            OnboardingProgressDTO parsed = objectMapper.readValue(rawJson, OnboardingProgressDTO.class);
            return normalizeOnboarding(parsed);
        } catch (JsonProcessingException ex) {
            log.warn("event=onboarding-progress-invalid-json reason={}", sanitizeMessage(ex.getMessage()));
            return defaultOnboarding();
        }
    }

    private String serializeOnboarding(OnboardingProgressDTO dto) {
        try {
            return objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Falha ao serializar progresso de onboarding.", ex);
        }
    }

    private OnboardingProgressDTO normalizeOnboarding(OnboardingProgressDTO dto) {
        int version = dto.version() == null ? ONBOARDING_VERSION : dto.version();
        if (version <= 0) {
            throw new IllegalArgumentException("Campo version deve ser maior que zero.");
        }

        Map<String, OnboardingRoleProgressDTO> sanitizedRoles = new LinkedHashMap<>();
        Map<String, OnboardingRoleProgressDTO> roles = dto.progressByRole() == null ? Map.of() : dto.progressByRole();

        for (Map.Entry<String, OnboardingRoleProgressDTO> roleEntry : roles.entrySet()) {
            String roleKey = roleEntry.getKey();
            if (roleKey == null || roleKey.isBlank()) {
                continue;
            }

            OnboardingRoleProgressDTO roleValue = roleEntry.getValue();
            Map<String, OnboardingPageProgressDTO> completedPages =
                    roleValue == null || roleValue.completedPages() == null ? Map.of() : roleValue.completedPages();

            Map<String, OnboardingPageProgressDTO> sanitizedPages = new LinkedHashMap<>();
            for (Map.Entry<String, OnboardingPageProgressDTO> pageEntry : completedPages.entrySet()) {
                String pageKey = pageEntry.getKey();
                if (pageKey == null || pageKey.isBlank()) {
                    continue;
                }

                OnboardingPageProgressDTO pageValue = pageEntry.getValue();
                String completedAt = pageValue == null ? null : pageValue.completedAt();
                sanitizedPages.put(pageKey, new OnboardingPageProgressDTO(completedAt));
            }

            sanitizedRoles.put(roleKey, new OnboardingRoleProgressDTO(sanitizedPages));
        }

        return new OnboardingProgressDTO(version, sanitizedRoles);
    }

    /**
     * Determina o auth provider a partir dos claims do token Firebase.
     * Ex.: google.com → GOOGLE, password → EMAIL, etc.
     */
    @SuppressWarnings("unchecked")
    private String extractProvider(FirebaseToken token) {
        try {
            Map<String, Object> firebase = (Map<String, Object>) token.getClaims().get("firebase");
            if (firebase != null) {
                String signInProvider = (String) firebase.get("sign_in_provider");
                if (signInProvider != null) {
                    return switch (signInProvider) {
                        case "google.com"   -> "GOOGLE";
                        case "facebook.com" -> "FACEBOOK";
                        case "apple.com"    -> "APPLE";
                        case "github.com"   -> "GITHUB";
                        case "twitter.com"  -> "TWITTER";
                        case "password"     -> "EMAIL";
                        case "phone"        -> "PHONE";
                        default             -> signInProvider.toUpperCase().replace(".COM", "");
                    };
                }
            }
        } catch (Exception e) {
            log.debug("Não foi possível extrair o provider do token: {}", e.getMessage());
        }
        return "EMAIL";
    }

    private boolean extractEmailVerified(FirebaseToken token) {
        Object claim = token.getClaims().get("email_verified");
        if (claim instanceof Boolean boolClaim) {
            return boolClaim;
        }
        if (claim instanceof String strClaim) {
            return Boolean.parseBoolean(strClaim);
        }
        return false;
    }

    private boolean isEmailVerificationRequired(String provider, boolean emailVerified) {
        return "EMAIL".equalsIgnoreCase(provider) && !emailVerified;
    }

    /**
     * Valida que o {@code requestedType} enviado pelo frontend é compatível
     * com o {@code actualType} registrado no banco.
     * <p>
     * Se forem incompatíveis → lança {@link RoleConflictException} (HTTP 403).
     * Se {@code requestedType} for null/blank, não há conflito (sem preferência declarada).
     */
    private void assertRoleCompatible(String requestedType, String actualType) {
        if (requestedType == null || requestedType.isBlank()) return;
        String normalized = requestedType.trim().toUpperCase();
        if (!normalized.equals(actualType.toUpperCase())) {
            String portal = "BARBER".equals(actualType) ? "barbeiro" : "cliente";
            throw new RoleConflictException(
                "Você possui uma conta de " + portal + ". Acesse o portal correto.",
                actualType
            );
        }
    }

    // ─── Conversão para AuthResponseDTO ──────────────────────────────────────

    private AuthResponseDTO toAuthResponse(Customer customer, boolean emailVerified, boolean verificationRequired) {
        boolean complete = customer.getTell() != null && customer.getDocumentCPF() != null
                && customer.getBirthDate() != null;
        return new AuthResponseDTO(
                customer.getId(),
                customer.getName(),
                customer.getEmail(),
                customer.getTell(),
            customer.getDocumentCPF(),
                customer.getImageUrl(),
                "CUSTOMER",
                customer.getAuthProvider(),
                complete,
                "ROLE_CUSTOMER",
                emailVerified,
                verificationRequired,
                null,   // barbershopId — customers não têm
                null,   // barbershopName — customers não têm
                null,   // isOwner — customers não têm
                null    // actAsBarber — customers não têm
        );
    }

    private AuthResponseDTO toAuthResponse(Barber barber, boolean emailVerified, boolean verificationRequired) {
        boolean complete = barber.getTell() != null && barber.getDocumentCPF() != null
                && barber.getBirthDate() != null;
            String barbershopName = resolveBarbershopName(barber.getBarbershopId());
        return new AuthResponseDTO(
                barber.getId(),
                barber.getName(),
                barber.getEmail(),
                barber.getTell(),
            barber.getDocumentCPF(),
                barber.getImageUrl(),
                "BARBER",
                barber.getAuthProvider(),
                complete,
                barber.getRole(),
                emailVerified,
                verificationRequired,
                barber.getBarbershopId(),   // ← campo chave para o frontend saber se tem barbearia
                barbershopName,
                barber.isOwner(),           // ← campo chave para mostrar painel de dono
                barber.isActAsBarber()      // ← se o owner aparece na lista de barbeiros
        );
    }

    private String resolveBarbershopName(java.util.UUID barbershopId) {
        if (barbershopId == null) {
            return null;
        }

        try {
            BarbershopInfoDTO shop = barbershopServiceClient.getBarbershopById(barbershopId);
            return shop != null ? shop.name() : null;
        } catch (FeignException.NotFound ex) {
            log.warn("event=barbershop-name-not-found id={}", maskIdentifier(String.valueOf(barbershopId)));
            return null;
        } catch (ExternalServiceUnavailableException ex) {
            log.warn("event=barbershop-name-unavailable id={} reason={}",
                    maskIdentifier(String.valueOf(barbershopId)),
                    sanitizeMessage(ex.getMessage()));
            return null;
        } catch (Exception ex) {
            log.warn("event=barbershop-name-resolve-failed id={} reason={}",
                    maskIdentifier(String.valueOf(barbershopId)),
                    sanitizeMessage(ex.getMessage()));
            return null;
        }
    }
}
