package ifsp.edu.projeto.cortaai.mapper;

import ifsp.edu.projeto.cortaai.dto.CustomerDTO;
import ifsp.edu.projeto.cortaai.model.Customer;
import org.springframework.stereotype.Component;

@Component
public class CustomerMapper {

    public CustomerDTO toDTO(Customer customer) {
        if (customer == null) {
            return null;
        }
        CustomerDTO customerDTO = new CustomerDTO();
        customerDTO.setId(customer.getId());
        customerDTO.setName(customer.getName());
        customerDTO.setTell(customer.getTell());
        customerDTO.setEmail(customer.getEmail());
        customerDTO.setDocumentCPF(customer.getDocumentCPF());
        customerDTO.setPassword(customer.getPassword());
        return customerDTO;
    }

    public Customer toEntity(CustomerDTO customerDTO) {
        if (customerDTO == null) {
            return null;
        }
        Customer customer = new Customer();
        customer.setName(customerDTO.getName());
        customer.setTell(customerDTO.getTell());
        customer.setEmail(customerDTO.getEmail());
        customer.setDocumentCPF(customerDTO.getDocumentCPF());
        customer.setPassword(customerDTO.getPassword());
        return customer;
    }
}