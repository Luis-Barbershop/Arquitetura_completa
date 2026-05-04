package ifsp.edu.projeto.cortaai.userservice.service.impl;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import ifsp.edu.projeto.cortaai.userservice.dto.CustomerDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.UploadResultDTO;
import ifsp.edu.projeto.cortaai.userservice.event.BeforeDeleteCustomer;
import ifsp.edu.projeto.cortaai.userservice.exception.NotFoundException;
import ifsp.edu.projeto.cortaai.userservice.mapper.CustomerMapper;
import ifsp.edu.projeto.cortaai.userservice.model.Customer;
import ifsp.edu.projeto.cortaai.userservice.repository.CustomerRepository;
import ifsp.edu.projeto.cortaai.userservice.security.crypto.PrivacyHash;
import ifsp.edu.projeto.cortaai.userservice.service.CustomerService;
import ifsp.edu.projeto.cortaai.userservice.service.FirebaseAuthService;
import ifsp.edu.projeto.cortaai.userservice.service.storage.StorageService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final ApplicationEventPublisher publisher;
    private final CustomerMapper customerMapper;
    private final StorageService storageService;
    private final FirebaseAuthService firebaseAuthService;
    private final FirebaseAuth firebaseAuth;

    public CustomerServiceImpl(final CustomerRepository customerRepository,
                               final ApplicationEventPublisher publisher,
                               final CustomerMapper customerMapper,
                               final StorageService storageService,
                               final FirebaseAuthService firebaseAuthService,
                               final FirebaseAuth firebaseAuth
                               ) {
        this.customerRepository = customerRepository;
        this.publisher = publisher;
        this.customerMapper = customerMapper;
        this.storageService = storageService;
        this.firebaseAuthService = firebaseAuthService;
        this.firebaseAuth = firebaseAuth;
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Customer findByFirebaseUid(String firebaseUid) {
        return customerRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new NotFoundException("Customer não encontrado para o UID: " + firebaseUid));
    }

    /**
     * Retorna o Customer associado ao UID, criando um registro mínimo caso ainda não exista.
     * Usado nos endpoints de favoritos para evitar 404 em usuários com perfil incompleto.
     */
    @Transactional
    private Customer findOrCreateByFirebaseUid(String firebaseUid) {
        return customerRepository.findByFirebaseUid(firebaseUid).orElseGet(() -> {
            Customer novo = new Customer();
            novo.setFirebaseUid(firebaseUid);
            novo.setAuthProvider("FIREBASE");
            // Tenta buscar nome/email no Firebase Admin SDK
            try {
                UserRecord userRecord = firebaseAuth.getUser(firebaseUid);
                String name = userRecord.getDisplayName();
                String email = userRecord.getEmail();
                novo.setName((name != null && !name.isBlank()) ? name : "Usuário");
                novo.setEmail((email != null && !email.isBlank()) ? PrivacyHash.normalizeEmail(email) : (firebaseUid + "@firebase.local"));
            } catch (FirebaseAuthException e) {
                novo.setName("Usuário");
                novo.setEmail(firebaseUid + "@firebase.local");
            }
            return customerRepository.save(novo);
        });
    }

    // ── CustomerService ───────────────────────────────────────────────────────

    @Override
    public List<CustomerDTO> findAll() {
        return customerRepository.findAll(Sort.by("id"))
                .stream()
                .map(customerMapper::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerDTO get(final UUID id) {
        return customerRepository.findById(id)
                .map(customerMapper::toDTO)
                .orElseThrow(NotFoundException::new);
    }

    @Override
    @Transactional
    public void updateByFirebaseUid(final String firebaseUid, final CustomerDTO customerDTO) {
        final Customer customer = findByFirebaseUid(firebaseUid);

        customer.setName(customerDTO.getName());
        customer.setTell(customerDTO.getTell());
        customer.setEmail(PrivacyHash.normalizeEmail(customerDTO.getEmail()));
        customer.setDocumentCPF(onlyDigits(customerDTO.getDocumentCPF()));
        if (customerDTO.getBirthDate() != null) {
            customer.setBirthDate(customerDTO.getBirthDate());
        }

        if (customerDTO.getImageUrl() != null) {
            customer.setImageUrl(customerDTO.getImageUrl());
        }

        customerRepository.save(customer);
        firebaseAuthService.setCustomUserClaims(customer.getFirebaseUid(), "CUSTOMER", false);
    }

    @Override
    @Transactional
    public void deleteByFirebaseUid(final String firebaseUid) {
        final Customer customer = findByFirebaseUid(firebaseUid);
        publisher.publishEvent(new BeforeDeleteCustomer(customer.getId()));
        customerRepository.delete(customer);
    }

    @Override
    @Transactional
    public String updateProfilePhotoByFirebaseUid(final String firebaseUid, final MultipartFile file) throws IOException {
        final Customer customer = findByFirebaseUid(firebaseUid);

        // Remove foto antiga do Cloudinary se existir
        String oldPublicId = customer.getImageUrlPublicId();
        if (oldPublicId != null) {
            storageService.deleteFile(oldPublicId);
        }

        final UploadResultDTO uploadResult = storageService.uploadFile(file, "customer-profiles");
        customer.setImageUrl(uploadResult.getSecureUrl());
        customer.setImageUrlPublicId(uploadResult.getPublicId());
        customerRepository.save(customer);

        return uploadResult.getSecureUrl();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> listFavoriteBarbershopIdsByFirebaseUid(final String firebaseUid) {
        return customerRepository.findByFirebaseUid(firebaseUid)
                .map(c -> new ArrayList<>(c.getFavoriteBarbershopIds()))
                .orElse(new ArrayList<>());
    }

    @Override
    @Transactional
    public void addFavoriteBarbershopByFirebaseUid(final String firebaseUid, final UUID barbershopId) {
        final Customer customer = findOrCreateByFirebaseUid(firebaseUid);
        customer.getFavoriteBarbershopIds().add(barbershopId);
        customerRepository.save(customer);
    }

    @Override
    @Transactional
    public void removeFavoriteBarbershopByFirebaseUid(final String firebaseUid, final UUID barbershopId) {
        customerRepository.findByFirebaseUid(firebaseUid).ifPresent(customer -> {
            customer.getFavoriteBarbershopIds().remove(barbershopId);
            customerRepository.save(customer);
        });
    }

    // ── Validações ────────────────────────────────────────────────────────────

    @Override
    public boolean tellExists(final String tell) {
        return customerRepository.existsByTellIgnoreCase(tell);
    }

    @Override
    public boolean emailExists(final String email) {
        return customerRepository.existsByEmailIgnoreCase(PrivacyHash.normalizeEmail(email));
    }

    @Override
    public boolean documentCPFExists(final String documentCPF) {
        return customerRepository.existsByDocumentCPFIgnoreCase(onlyDigits(documentCPF));
    }

    private String onlyDigits(String value) {
        return value == null ? null : value.replaceAll("\\D", "");
    }
}
