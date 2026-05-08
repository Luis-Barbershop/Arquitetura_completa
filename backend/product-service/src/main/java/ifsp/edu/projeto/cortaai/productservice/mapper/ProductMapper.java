package ifsp.edu.projeto.cortaai.productservice.mapper;

import ifsp.edu.projeto.cortaai.productservice.dto.ProductDTO;
import ifsp.edu.projeto.cortaai.productservice.model.Product;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    default ProductDTO toDTO(Product product) {
        if (product == null) {
            return null;
        }
        return new ProductDTO(
                product.getId(),
                product.getBarbershopId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getDynamicCategory() != null ? product.getDynamicCategory().getId() : null,
                product.getDynamicCategory() != null ? product.getDynamicCategory().getName() : null,
                product.getCategory(),
                product.getStockQuantity(),
                product.getMinStockQuantity(),
                product.getImageUrl(),
                product.isActive(),
                product.getCreatedAt()
        );
    }
}
