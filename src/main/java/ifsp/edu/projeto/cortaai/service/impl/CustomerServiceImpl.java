package ifsp.edu.projeto.cortaai.service.impl;

import ifsp.edu.projeto.cortaai.dto.CustomerDTO;
import ifsp.edu.projeto.cortaai.events.BeforeDeleteCustomer;
import ifsp.edu.projeto.cortaai.exception.NotFoundException;
import ifsp.edu.projeto.cortaai.mapper.CustomerMapper; // Importa o novo Mapper
import ifsp.edu.projeto.cortaai.model.Customer;
import ifsp.edu.projeto.cortaai.repository.CustomerRepository;
import ifsp.edu.projeto.cortaai.service.CustomerService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final ApplicationEventPublisher publisher;
    private final CustomerMapper customerMapper; // Injeta o Mapper

    public CustomerServiceImpl(final CustomerRepository customerRepository,
                               final ApplicationEventPublisher publisher,
                               final CustomerMapper customerMapper) { // Adiciona no construtor
        this.customerRepository = customerRepository;
        this.publisher = publisher;
        this.customerMapper = customerMapper;
    }

    @Override
    public List<CustomerDTO> findAll() {
        final List<Customer> customers = customerRepository.findAll(Sort.by("id"));
        return customers.stream()
                .map(customerMapper::toDTO) // Usa o mapper
                .toList();
    }

    @Override
    public CustomerDTO get(final UUID id) {
        return customerRepository.findById(id)
                .map(customerMapper::toDTO) // Usa o mapper
                .orElseThrow(NotFoundException::new);
    }

    @Override
    public UUID create(final CustomerDTO customerDTO) {
        final Customer customer = customerMapper.toEntity(customerDTO);
        return customerRepository.save(customer).getId();
    }

    @Override
    public void update(final UUID id, final CustomerDTO customerDTO) {
        final Customer customer = customerRepository.findById(id)
                .orElseThrow(NotFoundException::new);

        Customer updatedCustomer = customerMapper.toEntity(customerDTO);
        updatedCustomer.setId(customer.getId()); // Garante que o ID seja mantido

        customerRepository.save(updatedCustomer);
    }

    @Override
    public void delete(final UUID id) {
        final Customer customer = customerRepository.findById(id)
                .orElseThrow(NotFoundException::new);

        // Dispara o evento para que o listener possa verificar as dependências
        publisher.publishEvent(new BeforeDeleteCustomer(id));

        customerRepository.delete(customer);
    }

    // MÉTODOS DE MAPPER FORAM REMOVIDOS

    @Override
    public boolean tellExists(final String tell) {
        return customerRepository.existsByTellIgnoreCase(tell);
    }

    @Override
    public boolean emailExists(final String email) {
        return customerRepository.existsByEmailIgnoreCase(email);
    }

    @Override
    public boolean documentCPFExists(final String documentCPF) {
        return customerRepository.existsByDocumentCPFIgnoreCase(documentCPF);
    }

    @Override
    public boolean previousAppointmentExists(final String previousAppointment) {
        return customerRepository.existsByPreviousAppointmentIgnoreCase(previousAppointment);
    }
}