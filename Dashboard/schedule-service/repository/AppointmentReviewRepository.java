package ifsp.edu.projeto.cortaai.scheduleservice.repository;

import ifsp.edu.projeto.cortaai.scheduleservice.model.AppointmentReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppointmentReviewRepository extends JpaRepository<AppointmentReview, String> {
    // Permite salvar e buscar as avaliações físicas dos clientes
}