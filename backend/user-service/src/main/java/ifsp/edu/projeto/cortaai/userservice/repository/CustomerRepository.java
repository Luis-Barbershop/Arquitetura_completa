package ifsp.edu.projeto.cortaai.userservice.repository;

import ifsp.edu.projeto.cortaai.userservice.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    @Query("SELECT c FROM Customer c WHERE c.email = :email")
    Optional<Customer> findByEmail(@Param("email") String email);

    @Query("SELECT c FROM Customer c WHERE c.firebaseUid = :firebaseUid")
    Optional<Customer> findByFirebaseUid(@Param("firebaseUid") String firebaseUid);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Customer c WHERE c.email = :email")
    boolean existsByEmail(@Param("email") String email);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Customer c WHERE LOWER(c.email) = LOWER(:email)")
    boolean existsByEmailIgnoreCase(@Param("email") String email);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Customer c WHERE c.firebaseUid = :firebaseUid")
    boolean existsByFirebaseUid(@Param("firebaseUid") String firebaseUid);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Customer c WHERE c.documentCPF = :documentCPF")
    boolean existsByDocumentCPF(@Param("documentCPF") String documentCPF);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Customer c WHERE LOWER(c.documentCPF) = LOWER(:documentCPF)")
    boolean existsByDocumentCPFIgnoreCase(@Param("documentCPF") String documentCPF);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Customer c WHERE c.tell = :tell")
    boolean existsByTell(@Param("tell") String tell);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Customer c WHERE LOWER(c.tell) = LOWER(:tell)")
    boolean existsByTellIgnoreCase(@Param("tell") String tell);
}