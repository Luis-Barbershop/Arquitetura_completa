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
    @Transactional
    public AuthResponseDTO verifyAndProvision(FirebaseAuthRequestDTO request) {
        // 1. Valida o token Firebase
        FirebaseToken decoded = verifyToken(request.idToken());

        String uid      = decoded.getUid();
        String email    = decoded.getEmail();
        String name     = decoded.getName();
        String photoUrl = decoded.getPicture();
        String provider = extractProvider(decoded);

        log.info("Firebase auth OK — uid={} provider={} userType={}", uid, provider, request.userType());

        // 2. Determina o tipo de usuário
        String userType = resolveUserType(request.userType(), uid, email);

        // 3. Auto-provisiona ou recupera o usuário
        if ("BARBER".equalsIgnoreCase(userType)) {
            Barber barber = findOrCreateBarber(uid, email, name, photoUrl, provider);
            return toAuthResponse(barber);
        } else {
            Customer customer = findOrCreateCustomer(uid, email, name, photoUrl, provider);
            return toAuthResponse(customer);
        }
    }

    @Override
    @Transactional
    public AuthResponseDTO completeCustomerProfile(String firebaseUid, CompleteProfileCustomerDTO dto) {
        Customer customer = customerRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new NotFoundException("Customer não encontrado para o UID: " + firebaseUid));

        if (dto.name() != null && !dto.name().isBlank()) {
            customer.setName(dto.name());
        }
        customer.setTell(dto.tell());
        customer.setDocumentCPF(dto.documentCPF());

        customerRepository.saveAndFlush(customer);
        return toAuthResponse(customer);
    }

    @Override
    @Transactional
    public AuthResponseDTO completeBarberProfile(String firebaseUid, CompleteProfileBarberDTO dto) {
        Barber barber = barberRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new NotFoundException("Barbeiro não encontrado para o UID: " + firebaseUid));

        if (dto.name() != null && !dto.name().isBlank()) {
            barber.setName(dto.name());
        }
        barber.setTell(dto.tell());
        barber.setDocumentCPF(dto.documentCPF());
        barber.setWorkStartTime(dto.workStartTime());
        barber.setWorkEndTime(dto.workEndTime());
        barber.setOwner(dto.isOwner());

        barberRepository.saveAndFlush(barber);
        return toAuthResponse(barber);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponseDTO getMe(String firebaseUid) {
        // Tenta como Customer primeiro
        Optional<Customer> customer = customerRepository.findByFirebaseUid(firebaseUid);
        if (customer.isPresent()) {
            return toAuthResponse(customer.get());
        }

        // Depois tenta como Barber
        Optional<Barber> barber = barberRepository.findByFirebaseUid(firebaseUid);
        if (barber.isPresent()) {
            return toAuthResponse(barber.get());
        }

        throw new NotFoundException("Usuário não encontrado para o UID: " + firebaseUid);
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
     * Se o cliente não informou userType, tenta descobrir pela existência do registro.
     * Retorna "CUSTOMER" como padrão se nenhum registro for encontrado.
     */
    private String resolveUserType(String requested, String uid, String email) {
        if (requested != null && !requested.isBlank()) {
            return requested.toUpperCase();
        }

        if (barberRepository.existsByFirebaseUid(uid)) return "BARBER";
        if (customerRepository.existsByFirebaseUid(uid)) return "CUSTOMER";

        // Tenta por e-mail (usuário que tinha conta antes da migração)
        if (email != null) {
            if (barberRepository.existsByEmailIgnoreCase(email)) return "BARBER";
            if (customerRepository.existsByEmailIgnoreCase(email)) return "CUSTOMER";
        }

        // Padrão
        return "CUSTOMER";
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

    // ─── Customer provisioning ────────────────────────────────────────────────

    private Customer findOrCreateCustomer(String uid, String email, String name,
                                          String photoUrl, String provider) {
        // 1. Busca por UID
        Optional<Customer> byUid = customerRepository.findByFirebaseUid(uid);
        if (byUid.isPresent()) {
            return syncCustomerFromFirebase(byUid.get(), name, photoUrl, provider);
        }

        // 2. Busca por e-mail (migração de conta antiga)
        if (email != null) {
            Optional<Customer> byEmail = customerRepository.findByEmail(email);
            if (byEmail.isPresent()) {
                Customer existing = byEmail.get();
                try {
                    existing.setFirebaseUid(uid);
                    existing.setAuthProvider(provider);
                    if (photoUrl != null && existing.getImageUrl() == null) {
                        existing.setImageUrl(photoUrl);
                    }
                    return customerRepository.saveAndFlush(existing);
                } catch (Exception e) {
                    log.warn("Falha ao vincular Firebase UID ao customer existente (email={}): {}. Criando novo registro.",
                            email, e.getMessage());
                    // Se falhar (ex.: registro fantasma, schema antigo), cria um novo
                    // Mas antes, remove o registro inconsistente se possível
                    try {
                        customerRepository.delete(existing);
                        customerRepository.flush();
                    } catch (Exception deleteEx) {
                        log.warn("Não foi possível remover registro inconsistente: {}", deleteEx.getMessage());
                    }
                }
            }
        }

        // 3. Cria novo customer
        Customer newCustomer = new Customer();
        newCustomer.setFirebaseUid(uid);
        newCustomer.setEmail(email != null ? email : uid + "@firebase.local");
        newCustomer.setName(name != null ? name : "Usuário");
        newCustomer.setImageUrl(photoUrl);
        newCustomer.setAuthProvider(provider);
        // tell e documentCPF ficam null — perfil incompleto até o usuário completar
        log.info("Novo customer provisionado via Firebase: uid={}", uid);
        return customerRepository.saveAndFlush(newCustomer);
    }

    private Customer syncCustomerFromFirebase(Customer customer, String name, String photoUrl, String provider) {
        boolean changed = false;

        // Atualiza foto apenas se o usuário não tem uma foto própria (não do Cloudinary)
        if (photoUrl != null && customer.getImageUrl() == null) {
            customer.setImageUrl(photoUrl);
            changed = true;
        }
        if (!provider.equals(customer.getAuthProvider())) {
            customer.setAuthProvider(provider);
            changed = true;
        }

        return changed ? customerRepository.saveAndFlush(customer) : customer;
    }

    // ─── Barber provisioning ──────────────────────────────────────────────────

    private Barber findOrCreateBarber(String uid, String email, String name,
                                      String photoUrl, String provider) {
        // 1. Busca por UID
        Optional<Barber> byUid = barberRepository.findByFirebaseUid(uid);
        if (byUid.isPresent()) {
            return syncBarberFromFirebase(byUid.get(), photoUrl, provider);
        }

        // 2. Busca por e-mail (migração de conta antiga)
        if (email != null) {
            Optional<Barber> byEmail = barberRepository.findByEmail(email);
            if (byEmail.isPresent()) {
                Barber existing = byEmail.get();
                try {
                    existing.setFirebaseUid(uid);
                    existing.setAuthProvider(provider);
                    if (photoUrl != null && existing.getImageUrl() == null) {
                        existing.setImageUrl(photoUrl);
                    }
                    return barberRepository.saveAndFlush(existing);
                } catch (Exception e) {
                    log.warn("Falha ao vincular Firebase UID ao barbeiro existente (email={}): {}. Criando novo registro.",
                            email, e.getMessage());
                    try {
                        barberRepository.delete(existing);
                        barberRepository.flush();
                    } catch (Exception deleteEx) {
                        log.warn("Não foi possível remover registro inconsistente: {}", deleteEx.getMessage());
                    }
                }
            }
        }

        // 3. Cria novo barbeiro
        Barber newBarber = Barber.builder()
                .firebaseUid(uid)
                .email(email != null ? email : uid + "@firebase.local")
                .name(name != null ? name : "Barbeiro")
                .imageUrl(photoUrl)
                .authProvider(provider)
                .role("ROLE_BARBER")
                .isOwner(false)
                .build();

        log.info("Novo barbeiro provisionado via Firebase: uid={}", uid);
        return barberRepository.saveAndFlush(newBarber);
    }

    private Barber syncBarberFromFirebase(Barber barber, String photoUrl, String provider) {
        boolean changed = false;

        if (photoUrl != null && barber.getImageUrl() == null) {
            barber.setImageUrl(photoUrl);
            changed = true;
        }
        if (!provider.equals(barber.getAuthProvider())) {
            barber.setAuthProvider(provider);
            changed = true;
        }

        return changed ? barberRepository.saveAndFlush(barber) : barber;
    }

    // ─── Conversão para AuthResponseDTO ──────────────────────────────────────

    private AuthResponseDTO toAuthResponse(Customer customer) {
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
                "ROLE_CUSTOMER"
        );
    }

    private AuthResponseDTO toAuthResponse(Barber barber) {
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
                barber.getRole()
        );
    }
}
