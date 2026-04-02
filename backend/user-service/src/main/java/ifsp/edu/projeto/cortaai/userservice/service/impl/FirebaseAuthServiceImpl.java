package ifsp.edu.projeto.cortaai.userservice.service.impl;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import ifsp.edu.projeto.cortaai.userservice.dto.AuthResponseDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.CompleteProfileBarberDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.CompleteProfileCustomerDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.FirebaseAuthRequestDTO;
import ifsp.edu.projeto.cortaai.userservice.exception.NotFoundException;
import ifsp.edu.projeto.cortaai.userservice.model.Barber;
import ifsp.edu.projeto.cortaai.userservice.model.Customer;
import ifsp.edu.projeto.cortaai.userservice.repository.BarberRepository;
import ifsp.edu.projeto.cortaai.userservice.repository.CustomerRepository;
import ifsp.edu.projeto.cortaai.userservice.service.FirebaseAuthService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FirebaseAuthServiceImpl implements FirebaseAuthService {

    private static final Logger log = LoggerFactory.getLogger(FirebaseAuthServiceImpl.class);

    private final FirebaseAuth firebaseAuth;
    private final CustomerRepository customerRepository;
    private final BarberRepository barberRepository;

    // ──────────────────────────────────────────────────────────────────────────
    // Implementações públicas
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public AuthResponseDTO verifyAndProvision(FirebaseAuthRequestDTO request) {
        // 1. Valida o token Firebase
        FirebaseToken decoded = verifyToken(request.idToken());

        String uid      = decoded.getUid();
        String email    = decoded.getEmail();
        String name     = decoded.getName();
        String photoUrl = decoded.getPicture();
        String provider = extractProvider(decoded);
        boolean emailVerified = extractEmailVerified(decoded);
        boolean verificationRequired = isEmailVerificationRequired(provider, emailVerified);

        log.info("Firebase auth OK — uid={} provider={} userType={}", uid, provider, request.userType());

        String resolvedType = (request.userType() != null && !request.userType().isBlank())
                ? request.userType().toUpperCase()
                : "CUSTOMER";
        String resolvedRole = "BARBER".equals(resolvedType) ? "ROLE_BARBER" : "ROLE_CUSTOMER";

        if (verificationRequired) {
            log.info("Login bloqueado: e-mail ainda nao verificado para uid={}", uid);
            return new AuthResponseDTO(
                    null,
                    name != null ? name : "Usuário",
                    email,
                    null,
                    photoUrl,
                    resolvedType,
                    provider,
                    false,
                    resolvedRole,
                    false,
                    true,
                    null,  // barbershopId
                    null   // isOwner
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
            log.info("Barber encontrado por UID={}", uid);
            return toAuthResponse(barberByUid.get(), emailVerified, false);
        }

        // Tenta como Customer (por UID)
        Optional<Customer> customerByUid = customerRepository.findByFirebaseUid(uid);
        if (customerByUid.isPresent()) {
            log.info("Customer encontrado por UID={}", uid);
            return toAuthResponse(customerByUid.get(), emailVerified, false);
        }

        // Tenta por e-mail (migração de conta antiga)
        // Apenas LEITURA — não tenta atualizar registros antigos aqui.
        // O complete-profile vai criar/atualizar corretamente depois.
        if (email != null) {
            Optional<Barber> barberByEmail = barberRepository.findByEmail(email);
            if (barberByEmail.isPresent()) {
                Barber existing = barberByEmail.get();
                log.info("Encontrado barber existente por email={} (migração). Retornando dados sem atualizar.", email);
                return toAuthResponse(existing, emailVerified, false);
            }
            Optional<Customer> customerByEmail = customerRepository.findByEmail(email);
            if (customerByEmail.isPresent()) {
                Customer existing = customerByEmail.get();
                log.info("Encontrado customer existente por email={} (migração). Retornando dados sem atualizar.", email);
                return toAuthResponse(existing, emailVerified, false);
            }
        }

        // Usuário NOVO — NÃO salva no banco ainda!
        // Retorna um DTO provisório com profileComplete=false para o frontend mostrar o modal.
        log.info("Usuário novo (não existe no banco). uid={} — aguardando complete-profile para salvar.", uid);
        return new AuthResponseDTO(
                null,                        // sem ID (ainda não foi salvo)
                name != null ? name : "Usuário",
                email,
                null,                        // sem telefone
                photoUrl,
                resolvedType,
                provider,
                false,                       // perfil NÃO completo
                resolvedRole,
                emailVerified,
                false,
                null,  // barbershopId
                null   // isOwner
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
                    log.info("Customer não encontrado para UID={}, criando novo registro no complete-profile.", firebaseUid);
                    Customer novo = new Customer();
                    novo.setFirebaseUid(firebaseUid);
                    return novo;
                });

        if (dto.name() != null && !dto.name().isBlank()) {
            customer.setName(dto.name());
        }
        if (customer.getEmail() == null || customer.getEmail().isBlank()) {
            if (email != null && !email.isBlank()) {
                customer.setEmail(email);
            } else {
                var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.getCredentials() instanceof String emailFromHeader) {
                    customer.setEmail(emailFromHeader);
                } else {
                    customer.setEmail(firebaseUid + "@firebase.local");
                }
            }
        }
        if (customer.getName() == null || customer.getName().isBlank()) {
            customer.setName("Usuário");
        }
        customer.setTell(dto.tell());
        customer.setDocumentCPF(dto.documentCPF());
        customer.setAuthProvider(customer.getAuthProvider() != null ? customer.getAuthProvider() : "EMAIL");

        customerRepository.saveAndFlush(customer);
        return toAuthResponse(customer, true, false);
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
                    log.info("Barber não encontrado para UID={}, criando novo registro no complete-profile.", firebaseUid);
                    return Barber.builder()
                            .firebaseUid(firebaseUid)
                            .role("ROLE_BARBER")
                            .isOwner(false)
                            .build();
                });

        if (dto.name() != null && !dto.name().isBlank()) {
            barber.setName(dto.name());
        }
        if (barber.getEmail() == null || barber.getEmail().isBlank()) {
            if (email != null && !email.isBlank()) {
                barber.setEmail(email);
            } else {
                var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.getCredentials() instanceof String emailFromHeader) {
                    barber.setEmail(emailFromHeader);
                } else {
                    barber.setEmail(firebaseUid + "@firebase.local");
                }
            }
        }
        if (barber.getName() == null || barber.getName().isBlank()) {
            barber.setName("Barbeiro");
        }
        barber.setTell(dto.tell());
        barber.setDocumentCPF(dto.documentCPF());
        barber.setWorkStartTime(dto.workStartTime());
        barber.setWorkEndTime(dto.workEndTime());
        barber.setOwner(dto.isOwner());
        barber.setAuthProvider(barber.getAuthProvider() != null ? barber.getAuthProvider() : "EMAIL");

        barberRepository.saveAndFlush(barber);
        return toAuthResponse(barber, true, false);
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
    public void setCustomUserClaims(String uid, String role, boolean isOwner) {
        try {
            Map<String, Object> claims = new HashMap<>();
            claims.put("role", role);
            claims.put("isOwner", isOwner);
            FirebaseAuth.getInstance().setCustomUserClaims(uid, claims);
            log.info("Custom claims atualizadas para UID {}: role={}, isOwner={}", uid, role, isOwner);
        } catch (FirebaseAuthException e) {
            log.error("Erro ao setar custom claims para o usuário {}: {}", uid, e.getMessage());
            throw new RuntimeException("Falha ao atualizar permissões do usuário", e);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers privados
    // ──────────────────────────────────────────────────────────────────────────

    private FirebaseToken verifyToken(String idToken) {
        try {
            return firebaseAuth.verifyIdToken(idToken);
        } catch (FirebaseAuthException e) {
            log.warn("Token Firebase inválido: {}", e.getMessage());
            throw new SecurityException("Token Firebase inválido ou expirado: " + e.getMessage());
        }
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

    // ─── Conversão para AuthResponseDTO ──────────────────────────────────────

    private AuthResponseDTO toAuthResponse(Customer customer, boolean emailVerified, boolean verificationRequired) {
        boolean complete = customer.getTell() != null && customer.getDocumentCPF() != null;
        return new AuthResponseDTO(
                customer.getId(),
                customer.getName(),
                customer.getEmail(),
                customer.getTell(),
                customer.getImageUrl(),
                "CUSTOMER",
                customer.getAuthProvider(),
                complete,
                "ROLE_CUSTOMER",
                emailVerified,
                verificationRequired,
                null,   // barbershopId — customers não têm
                null    // isOwner — customers não têm
        );
    }

    private AuthResponseDTO toAuthResponse(Barber barber, boolean emailVerified, boolean verificationRequired) {
        boolean complete = barber.getTell() != null && barber.getDocumentCPF() != null
                && barber.getWorkStartTime() != null && barber.getWorkEndTime() != null;
        return new AuthResponseDTO(
                barber.getId(),
                barber.getName(),
                barber.getEmail(),
                barber.getTell(),
                barber.getImageUrl(),
                "BARBER",
                barber.getAuthProvider(),
                complete,
                barber.getRole(),
                emailVerified,
                verificationRequired,
                barber.getBarbershopId(),   // ← campo chave para o frontend saber se tem barbearia
                barber.isOwner()            // ← campo chave para mostrar painel de dono
        );
    }
}
