package ifsp.edu.projeto.cortaai.productservice.repository;

import ifsp.edu.projeto.cortaai.productservice.model.ServiceRecipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ServiceRecipeRepository extends JpaRepository<ServiceRecipe, String> {
    List<ServiceRecipe> findByActivityId(String activityId);
}