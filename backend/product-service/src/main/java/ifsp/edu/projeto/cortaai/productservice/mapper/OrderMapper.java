package ifsp.edu.projeto.cortaai.productservice.mapper;

import ifsp.edu.projeto.cortaai.productservice.dto.OrderDTO;
import ifsp.edu.projeto.cortaai.productservice.dto.OrderItemDTO;
import ifsp.edu.projeto.cortaai.productservice.model.Order;
import ifsp.edu.projeto.cortaai.productservice.model.OrderItem;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    OrderDTO toDTO(Order order);

    OrderItemDTO toItemDTO(OrderItem item);

    List<OrderItemDTO> toItemDTOList(List<OrderItem> items);
}
