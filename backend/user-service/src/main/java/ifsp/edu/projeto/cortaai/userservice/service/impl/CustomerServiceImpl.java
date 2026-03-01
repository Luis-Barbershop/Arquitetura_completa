package ifsp.edu.projeto.cortaai.userservice.service.impl;

import ifsp.edu.projeto.cortaai.userservice.dto.*;
import ifsp.edu.projeto.cortaai.userservice.event.BeforeDeleteCustomer;
import ifsp.edu.projeto.cortaai.userservice.exception.NotFoundException;
import ifsp.edu.projeto.cortaai.userservice.mapper.CustomerMapper;
import ifsp.edu.projeto.cortaai.userservice.model.Customer;
import ifsp.edu.projeto.cortaai.userservice.repository.CustomerRepository;
import ifsp.edu.projeto.cortaai.userservice.service.CustomerService;
import ifsp.edu.projeto.cortaai.userservice.service.JwtTokenService;
import ifsp.edu.projeto.cortaai.userservice.service.storage.StorageService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder; // IMPORTANTE
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final ApplicationEventPublisher publisher;
    private final CustomerMapper customerMapper;
    private final PasswordEncoder passwordEncoder;
    private final StorageService storageService;
    private final JwtTokenService jwtTokenService; // NOVA DEPENDÊNCIA

    public CustomerServiceImpl(final CustomerRepository customerRepository,
                               final ApplicationEventPublisher publisher,
                               final CustomerMapper customerMapper,
                               final PasswordEncoder passwordEncoder,
                               final StorageService storageService,
                               final JwtTokenService jwtTokenService) { // ADICIONADO AO CONSTRUTOR
        this.customerRepository = customerRepository;
        this.publisher = publisher;
        this.customerMapper = customerMapper;
        this.passwordEncoder = passwordEncoder;
        this.storageService = storageService;
        this.jwtTokenService = jwtTokenService; // INJETADO
    }

    private Customer findCustomerByEmail(String email) {
        return customerRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Cliente (usuário autenticado) não encontrado"));
    }

    @Override
    public List<CustomerDTO> findAll() {
        final List<Customer> customers = customerRepository.findAll(Sort.by("id"));
        return customers.stream()
                .map(customerMapper::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponseDTO login(final LoginDTO loginDTO) { // TIPO DE RETORNO ALTERADO
        final Customer customer = customerRepository.findByEmail(loginDTO.getEmail())
                .orElseThrow(() -> new NotFoundException("Usuário ou senha inválidos"));

        // Compara a senha enviada com o hash salvo no banco
        if (!passwordEncoder.matches(loginDTO.getPassword(), customer.getPassword())) {
            throw new NotFoundException("Usuário ou senha inválidos");
        }

        // 1. Gera o token JWT para o cliente
        final String token = jwtTokenService.generateToken(customer);

        // 2. Mapeia o cliente para DTO
        final CustomerDTO customerDTO = customerMapper.toDTO(customer);

        // 3. Retorna o LoginResponseDTO (com token e dados do usuário)
        return LoginResponseDTO.builder()
                .token(token)
                .userData(customerDTO)
                .build();
    }

    @Override
    public CustomerDTO get(final UUID id) {
        return customerRepository.findById(id)
                .map(customerMapper::toDTO)
                .orElseThrow(NotFoundException::new);
    }

    // Método create alterado para aceitar o arquivo
    @Override
    @Transactional
    public UUID create(final CustomerCreateDTO customerCreateDTO, final MultipartFile file) throws IOException {
        final Customer customer = new Customer();
        customer.setName(customerCreateDTO.getName());
        customer.setTell(customerCreateDTO.getTell());
        customer.setEmail(customerCreateDTO.getEmail());
        customer.setDocumentCPF(customerCreateDTO.getDocumentCPF());
        customer.setPassword(passwordEncoder.encode(customerCreateDTO.getPassword()));

        // Salva o cliente primeiro para obter um ID
        final Customer savedCustomer = customerRepository.save(customer);

        // Se a foto foi enviada, faz o upload e atualiza
        if (file != null && !file.isEmpty()) {
            final UploadResultDTO uploadResult = storageService.uploadFile(file, "customer-profiles");
            savedCustomer.setImageUrl(uploadResult.getSecureUrl());
            savedCustomer.setImageUrlPublicId(uploadResult.getPublicId());
            customerRepository.save(savedCustomer); // Salva novamente com a URL da imagem
        }

        return savedCustomer.getId();
    }

    @Override
    @Transactional // Adicionada anotação
    public void update(final String email, final CustomerDTO customerDTO) { // ALTERADO
        // Busca o cliente pelo e-mail do token
        final Customer customer = findCustomerByEmail(email);

        customer.setName(customerDTO.getName());
        customer.setTell(customerDTO.getTell());
        customer.setEmail(customerDTO.getEmail());
        customer.setDocumentCPF(customerDTO.getDocumentCPF());

        if(customerDTO.getImageUrl() != null) {
            customer.setImageUrl(customerDTO.getImageUrl());
        }

        customerRepository.save(customer);
    }

    @Override
    @Transactional // Adicionada anotação
    public void delete(final String email) { // ALTERADO
        // Busca o cliente pelo e-mail do token
        final Customer customer = findCustomerByEmail(email);

        publisher.publishEvent(new BeforeDeleteCustomer(customer.getId()));
        customerRepository.delete(customer);
    }

    // --- Métodos de validação ---

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
    @Transactional
    public String updateProfilePhoto(String email, MultipartFile file) throws IOException {
        final Customer customer = findCustomerByEmail(email);

        // 1. Deletar foto antiga, se existir
        String oldPublicId = customer.getImageUrlPublicId();
        if (oldPublicId != null) {
            storageService.deleteFile(oldPublicId);
        }

        // 2. Faz o upload da nova foto
        final UploadResultDTO uploadResult = storageService.uploadFile(file, "customer-profiles");

        // 3. Salva a URL e o Public ID
        customer.setImageUrl(uploadResult.getSecureUrl());
        customer.setImageUrlPublicId(uploadResult.getPublicId());
        customerRepository.save(customer);

        return uploadResult.getSecureUrl(); // Retorna a nova URL
    }
}