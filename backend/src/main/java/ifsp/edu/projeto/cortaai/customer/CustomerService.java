package ifsp.edu.projeto.cortaai.customer;

import ifsp.edu.projeto.cortaai.events.BeforeDeleteCustomer;
import ifsp.edu.projeto.cortaai.util.NotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;


@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final ApplicationEventPublisher publisher;

    public CustomerService(final CustomerRepository customerRepository,
            final ApplicationEventPublisher publisher) {
        this.customerRepository = customerRepository;
        this.publisher = publisher;
    }

    public List<CustomerDTO> findAll() {
        final List<Customer> customers = customerRepository.findAll(Sort.by("id"));
        return customers.stream()
                .map(customer -> mapToDTO(customer, new CustomerDTO()))
                .toList();
    }

    public CustomerDTO get(final UUID id) {
        return customerRepository.findById(id)
                .map(customer -> mapToDTO(customer, new CustomerDTO()))
                .orElseThrow(NotFoundException::new);
    }

    public UUID create(final CustomerDTO customerDTO) {
        final Customer customer = new Customer();
        mapToEntity(customerDTO, customer);
        return customerRepository.save(customer).getId();
    }

    public void update(final UUID id, final CustomerDTO customerDTO) {
        final Customer customer = customerRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        mapToEntity(customerDTO, customer);
        customerRepository.save(customer);
    }

    public void delete(final UUID id) {
        final Customer customer = customerRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        publisher.publishEvent(new BeforeDeleteCustomer(id));
        customerRepository.delete(customer);
    }

    private CustomerDTO mapToDTO(final Customer customer, final CustomerDTO customerDTO) {
        customerDTO.setId(customer.getId());
        customerDTO.setName(customer.getName());
        customerDTO.setTell(customer.getTell());
        customerDTO.setEmail(customer.getEmail());
        customerDTO.setDocumentCPF(customer.getDocumentCPF());
        customerDTO.setPassword(customer.getPassword());
        customerDTO.setPreviousAppointment(customer.getPreviousAppointment());
        return customerDTO;
    }

    private Customer mapToEntity(final CustomerDTO customerDTO, final Customer customer) {
        customer.setName(customerDTO.getName());
        customer.setTell(customerDTO.getTell());
        customer.setEmail(customerDTO.getEmail());
        customer.setDocumentCPF(customerDTO.getDocumentCPF());
        customer.setPassword(customerDTO.getPassword());
        customer.setPreviousAppointment(customerDTO.getPreviousAppointment());
        return customer;
    }

    public boolean tellExists(final String tell) {
        return customerRepository.existsByTellIgnoreCase(tell);
    }

    public boolean emailExists(final String email) {
        return customerRepository.existsByEmailIgnoreCase(email);
    }

    public boolean documentCPFExists(final String documentCPF) {
        return customerRepository.existsByDocumentCPFIgnoreCase(documentCPF);
    }

    public boolean previousAppointmentExists(final String previousAppointment) {
        return customerRepository.existsByPreviousAppointmentIgnoreCase(previousAppointment);
    }

}
