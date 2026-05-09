package ifsp.edu.projeto.cortaai.userservice.repository;

import ifsp.edu.projeto.cortaai.userservice.model.Barber;
import ifsp.edu.projeto.cortaai.userservice.security.crypto.PrivacyHash;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BarberRepository extends JpaRepository<Barber, UUID> {

    default Optional<Barber> findByEmail(String email) {
        String normalized = PrivacyHash.normalizeEmail(email);
        if (normalized == null) {
            return Optional.empty();
        }
        String hash = PrivacyHash.emailHash(normalized);
        return findByEmailHash(hash)
                .or(() -> findByEmailEncrypted(normalized))
                .or(() -> findByEmailRaw(normalized));
    }

    Optional<Barber> findByEmailHash(String emailHash);

    @Query("SELECT b FROM Barber b WHERE b.email = :email")
    Optional<Barber> findByEmailEncrypted(@Param("email") String email);

    @Query(value = "SELECT * FROM barbers WHERE LOWER(email) = LOWER(:email) LIMIT 1", nativeQuery = true)
    Optional<Barber> findByEmailRaw(@Param("email") String email);

    @Query("SELECT b FROM Barber b WHERE b.firebaseUid = :firebaseUid")
    Optional<Barber> findByFirebaseUid(@Param("firebaseUid") String firebaseUid);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Barber b WHERE b.firebaseUid = :firebaseUid")
    Optional<Barber> findByFirebaseUidForUpdate(@Param("firebaseUid") String firebaseUid);

    default boolean existsByEmail(String email) {
        return findByEmail(email).isPresent();
    }

    default boolean existsByEmailIgnoreCase(String email) {
        return existsByEmail(email);
    }

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Barber b WHERE b.firebaseUid = :firebaseUid")
    boolean existsByFirebaseUid(@Param("firebaseUid") String firebaseUid);

    default boolean existsByDocumentCPF(String documentCPF) {
        return existsByDocumentCPFEncrypted(documentCPF) || countByDocumentCPFRaw(documentCPF) > 0;
    }

    default boolean existsByDocumentCPFIgnoreCase(String documentCPF) {
        return existsByDocumentCPF(documentCPF);
    }

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Barber b WHERE b.documentCPF = :documentCPF")
    boolean existsByDocumentCPFEncrypted(@Param("documentCPF") String documentCPF);

    @Query(value = "SELECT COUNT(*) FROM barbers WHERE document_cpf = :documentCPF", nativeQuery = true)
    long countByDocumentCPFRaw(@Param("documentCPF") String documentCPF);

    default boolean existsByTellIgnoreCase(String tell) {
        return existsByTellEncrypted(tell) || countByTellRaw(tell) > 0;
    }

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Barber b WHERE b.tell = :tell")
    boolean existsByTellEncrypted(@Param("tell") String tell);

    @Query(value = "SELECT COUNT(*) FROM barbers WHERE tell = :tell", nativeQuery = true)
    long countByTellRaw(@Param("tell") String tell);

    @Query("SELECT b FROM Barber b WHERE b.barbershopId = :barbershopId")
    List<Barber> findByBarbershopId(@Param("barbershopId") UUID barbershopId);

    /**
     * Retorna barbeiros ativos de uma barbearia:
     * - Funcionários comuns (isOwner = false) sempre incluídos.
     * - Owners só incluídos se actAsBarber = true.
     */
    @Query("SELECT b FROM Barber b WHERE b.barbershopId = :barbershopId " +
           "AND (b.isOwner = false OR b.actAsBarber = true)")
    List<Barber> findActiveByBarbershopId(@Param("barbershopId") UUID barbershopId);

    default Optional<Barber> findByDocumentCPF(String cpf) {
        return findByDocumentCPFEncrypted(cpf).or(() -> findByDocumentCPFRaw(cpf));
    }

    @Query("SELECT b FROM Barber b WHERE b.documentCPF = :cpf")
    Optional<Barber> findByDocumentCPFEncrypted(@Param("cpf") String cpf);

    @Query(value = "SELECT * FROM barbers WHERE document_cpf = :cpf LIMIT 1", nativeQuery = true)
    Optional<Barber> findByDocumentCPFRaw(@Param("cpf") String cpf);

    @Query(value = """
            SELECT * FROM barbers
            WHERE (document_cpf IS NOT NULL AND document_cpf <> '' AND document_cpf NOT LIKE 'enc:v1:%')
               OR (tell IS NOT NULL AND tell <> '' AND tell NOT LIKE 'enc:v1:%')
               OR (email IS NOT NULL AND email <> '' AND email NOT LIKE 'enc:v1:%')
               OR (birth_date IS NOT NULL AND birth_date <> '' AND birth_date NOT LIKE 'enc:v1:%')
               OR (email_hash IS NULL AND email IS NOT NULL AND email <> '')
               OR (mp_access_token IS NOT NULL AND mp_access_token <> '' AND mp_access_token NOT LIKE 'enc:v1:%')
               OR (mp_refresh_token IS NOT NULL AND mp_refresh_token <> '' AND mp_refresh_token NOT LIKE 'enc:v1:%')
               OR (mp_user_id IS NOT NULL AND mp_user_id <> '' AND mp_user_id NOT LIKE 'enc:v1:%')
            """, nativeQuery = true)
    List<Barber> findWithLegacyPlainSensitiveData();
}
