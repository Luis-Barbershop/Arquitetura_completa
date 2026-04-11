package ifsp.edu.projeto.cortaai.userservice.repository;

import ifsp.edu.projeto.cortaai.userservice.model.Barber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BarberRepository extends JpaRepository<Barber, UUID> {

    @Query("SELECT b FROM Barber b WHERE b.email = :email")
    Optional<Barber> findByEmail(@Param("email") String email);

    @Query("SELECT b FROM Barber b WHERE b.firebaseUid = :firebaseUid")
    Optional<Barber> findByFirebaseUid(@Param("firebaseUid") String firebaseUid);

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Barber b WHERE b.email = :email")
    boolean existsByEmail(@Param("email") String email);

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Barber b WHERE LOWER(b.email) = LOWER(:email)")
    boolean existsByEmailIgnoreCase(@Param("email") String email);

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Barber b WHERE b.firebaseUid = :firebaseUid")
    boolean existsByFirebaseUid(@Param("firebaseUid") String firebaseUid);

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Barber b WHERE b.documentCPF = :documentCPF")
    boolean existsByDocumentCPF(@Param("documentCPF") String documentCPF);

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Barber b WHERE LOWER(b.documentCPF) = LOWER(:documentCPF)")
    boolean existsByDocumentCPFIgnoreCase(@Param("documentCPF") String documentCPF);

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Barber b WHERE LOWER(b.tell) = LOWER(:tell)")
    boolean existsByTellIgnoreCase(@Param("tell") String tell);

    @Query("SELECT b FROM Barber b WHERE b.barbershopId = :barbershopId")
    List<Barber> findByBarbershopId(@Param("barbershopId") UUID barbershopId);

    @Query("SELECT b FROM Barber b WHERE REPLACE(b.documentCPF, '.', '') = REPLACE(:cpf, '.', '')")
    Optional<Barber> findByDocumentCPF(@Param("cpf") String cpf);
}