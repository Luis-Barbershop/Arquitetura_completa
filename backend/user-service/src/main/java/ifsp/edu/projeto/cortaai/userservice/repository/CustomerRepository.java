package ifsp.edu.projeto.cortaai.userservice.repository;

import ifsp.edu.projeto.cortaai.userservice.model.Customer;
import ifsp.edu.projeto.cortaai.userservice.security.crypto.PrivacyHash;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    default Optional<Customer> findByEmail(String email) {
        String normalized = PrivacyHash.normalizeEmail(email);
        if (normalized == null) {
            return Optional.empty();
        }
        String hash = PrivacyHash.emailHash(normalized);
        return findByEmailHash(hash)
                .or(() -> findByEmailEncrypted(normalized))
                .or(() -> findByEmailRaw(normalized));
    }

    Optional<Customer> findByEmailHash(String emailHash);

    @Query("SELECT c FROM Customer c WHERE c.email = :email")
    Optional<Customer> findByEmailEncrypted(@Param("email") String email);

    @Query(value = "SELECT * FROM customers WHERE LOWER(email) = LOWER(:email) LIMIT 1", nativeQuery = true)
    Optional<Customer> findByEmailRaw(@Param("email") String email);

    @Query("SELECT c FROM Customer c WHERE c.firebaseUid = :firebaseUid")
    Optional<Customer> findByFirebaseUid(@Param("firebaseUid") String firebaseUid);

    default boolean existsByEmail(String email) {
        return findByEmail(email).isPresent();
    }

    default boolean existsByEmailIgnoreCase(String email) {
        return existsByEmail(email);
    }

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Customer c WHERE c.firebaseUid = :firebaseUid")
    boolean existsByFirebaseUid(@Param("firebaseUid") String firebaseUid);

    default boolean existsByDocumentCPF(String documentCPF) {
        return existsByDocumentCPFEncrypted(documentCPF) || countByDocumentCPFRaw(documentCPF) > 0;
    }

    default boolean existsByDocumentCPFIgnoreCase(String documentCPF) {
        return existsByDocumentCPF(documentCPF);
    }

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Customer c WHERE c.documentCPF = :documentCPF")
    boolean existsByDocumentCPFEncrypted(@Param("documentCPF") String documentCPF);

    @Query(value = "SELECT COUNT(*) FROM customers WHERE document_cpf = :documentCPF", nativeQuery = true)
    long countByDocumentCPFRaw(@Param("documentCPF") String documentCPF);

    @Query(value = """
            SELECT * FROM customers
            WHERE (document_cpf IS NOT NULL AND document_cpf <> '' AND document_cpf NOT LIKE 'enc:v1:%')
               OR (tell IS NOT NULL AND tell <> '' AND tell NOT LIKE 'enc:v1:%')
               OR (email IS NOT NULL AND email <> '' AND email NOT LIKE 'enc:v1:%')
               OR (birth_date IS NOT NULL AND birth_date <> '' AND birth_date NOT LIKE 'enc:v1:%')
               OR (email_hash IS NULL AND email IS NOT NULL AND email <> '')
            """, nativeQuery = true)
    List<Customer> findWithLegacyPlainSensitiveData();

    default boolean existsByTell(String tell) {
        return existsByTellEncrypted(tell) || countByTellRaw(tell) > 0;
    }

    default boolean existsByTellIgnoreCase(String tell) {
        return existsByTell(tell);
    }

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Customer c WHERE c.tell = :tell")
    boolean existsByTellEncrypted(@Param("tell") String tell);

    @Query(value = "SELECT COUNT(*) FROM customers WHERE tell = :tell", nativeQuery = true)
    long countByTellRaw(@Param("tell") String tell);
}
