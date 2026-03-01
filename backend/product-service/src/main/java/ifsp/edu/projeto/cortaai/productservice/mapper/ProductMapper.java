package ifsp.edu.projeto.cortaai.productservice.mapper;

import ifsp.edu.projeto.cortaai.productservice.dto.ProductDTO;
import ifsp.edu.projeto.cortaai.productservice.model.Product;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    ProductDTO toDTO(Product product);
}
