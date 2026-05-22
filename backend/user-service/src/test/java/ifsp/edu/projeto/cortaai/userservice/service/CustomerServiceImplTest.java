package ifsp.edu.projeto.cortaai.userservice.service;

import ifsp.edu.projeto.cortaai.userservice.dto.CustomerDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.UpdateCustomerDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.UploadResultDTO;
import ifsp.edu.projeto.cortaai.userservice.event.BeforeDeleteCustomer;
import ifsp.edu.projeto.cortaai.userservice.exception.NotFoundException;
import ifsp.edu.projeto.cortaai.userservice.mapper.CustomerMapper;
import ifsp.edu.projeto.cortaai.userservice.messaging.CustomerDeletedPublisher;
import ifsp.edu.projeto.cortaai.userservice.model.Customer;
import ifsp.edu.projeto.cortaai.userservice.repository.CustomerRepository;
import ifsp.edu.projeto.cortaai.userservice.service.impl.CustomerServiceImpl;
import ifsp.edu.projeto.cortaai.userservice.service.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceImplTest {

    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private ApplicationEventPublisher publisher;
    @Mock
    private StorageService storageService;
    @Mock
    private CustomerDeletedPublisher customerDeletedPublisher;

    private CustomerServiceImpl service;

    @BeforeEach
    void setUp() {
        CustomerMapper mapper = Mappers.getMapper(CustomerMapper.class);
        service = new CustomerServiceImpl(
                customerRepository,
                publisher,
                mapper,
                storageService,
                null,
                customerDeletedPublisher
        );
    }

    @Test
    void shouldFindAllCustomersSortedAndMapped() {
        Customer customer = customer(UUID.randomUUID(), "Ana Cliente");
        when(customerRepository.findAll(Sort.by("id"))).thenReturn(List.of(customer));

        List<CustomerDTO> result = service.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(customer.getId());
        assertThat(result.get(0).getName()).isEqualTo("Ana Cliente");
    }

    @Test
    void shouldGetCustomerByIdOrThrowWhenMissing() {
        UUID id = UUID.randomUUID();
        Customer customer = customer(id, "Cliente");
        when(customerRepository.findById(id)).thenReturn(Optional.of(customer));

        CustomerDTO result = service.get(id);

        assertThat(result.getId()).isEqualTo(id);

        UUID missingId = UUID.randomUUID();
        when(customerRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(missingId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void shouldUpdateCustomerByFirebaseUidWithOnlyProvidedFields() {
        Customer customer = customer(UUID.randomUUID(), "Antes");
        customer.setTell("11999999999");
        customer.setBirthDate(LocalDate.of(1990, 1, 1));
        UpdateCustomerDTO dto = new UpdateCustomerDTO();
        dto.setName("Depois");
        dto.setBirthDate(LocalDate.of(1991, 2, 3));
        when(customerRepository.findByFirebaseUid("customer-uid")).thenReturn(Optional.of(customer));

        service.updateByFirebaseUid("customer-uid", dto);

        assertThat(customer.getName()).isEqualTo("Depois");
        assertThat(customer.getTell()).isEqualTo("11999999999");
        assertThat(customer.getBirthDate()).isEqualTo(LocalDate.of(1991, 2, 3));
        verify(customerRepository).save(customer);
    }

    @Test
    void shouldDeleteCustomerPublishingDomainEventAndIntegrationEvent() {
        UUID customerId = UUID.randomUUID();
        Customer customer = customer(customerId, "Cliente");
        when(customerRepository.findByFirebaseUid("customer-uid")).thenReturn(Optional.of(customer));

        service.deleteByFirebaseUid("customer-uid");

        ArgumentCaptor<BeforeDeleteCustomer> eventCaptor = ArgumentCaptor.forClass(BeforeDeleteCustomer.class);
        verify(publisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getId()).isEqualTo(customerId);
        verify(customerRepository).delete(customer);
        verify(customerDeletedPublisher).publish(customerId);
    }

    @Test
    void shouldUpdateCustomerPhotoReplacingOldFile() throws IOException {
        Customer customer = customer(UUID.randomUUID(), "Cliente");
        customer.setImageUrlPublicId("old-photo");
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        when(customerRepository.findByFirebaseUid("customer-uid")).thenReturn(Optional.of(customer));
        when(storageService.uploadFile(file, "customer-profiles"))
                .thenReturn(new UploadResultDTO("new-photo", "https://cdn/customer.png"));

        String result = service.updateProfilePhotoByFirebaseUid("customer-uid", file);

        assertThat(result).isEqualTo("https://cdn/customer.png");
        assertThat(customer.getImageUrl()).isEqualTo("https://cdn/customer.png");
        assertThat(customer.getImageUrlPublicId()).isEqualTo("new-photo");
        verify(storageService).deleteFile("old-photo");
        verify(customerRepository).save(customer);
    }

    @Test
    void shouldListFavoriteIdsOrReturnEmptyWhenCustomerDoesNotExist() {
        UUID favorite = UUID.randomUUID();
        Customer customer = customer(UUID.randomUUID(), "Cliente");
        customer.getFavoriteBarbershopIds().add(favorite);
        when(customerRepository.findByFirebaseUid("customer-uid")).thenReturn(Optional.of(customer));
        when(customerRepository.findByFirebaseUid("missing-uid")).thenReturn(Optional.empty());

        assertThat(service.listFavoriteBarbershopIdsByFirebaseUid("customer-uid")).containsExactly(favorite);
        assertThat(service.listFavoriteBarbershopIdsByFirebaseUid("missing-uid")).isEmpty();
    }

    @Test
    void shouldAddAndRemoveFavoriteBarbershop() {
        UUID favorite = UUID.randomUUID();
        Customer customer = customer(UUID.randomUUID(), "Cliente");
        when(customerRepository.findByFirebaseUid("customer-uid")).thenReturn(Optional.of(customer));

        service.addFavoriteBarbershopByFirebaseUid("customer-uid", favorite);

        assertThat(customer.getFavoriteBarbershopIds()).containsExactly(favorite);
        verify(customerRepository).save(customer);

        service.removeFavoriteBarbershopByFirebaseUid("customer-uid", favorite);

        assertThat(customer.getFavoriteBarbershopIds()).isEmpty();
        verify(customerRepository, org.mockito.Mockito.times(2)).save(customer);
    }

    @Test
    void shouldIgnoreFavoriteRemovalWhenCustomerDoesNotExist() {
        when(customerRepository.findByFirebaseUid("missing-uid")).thenReturn(Optional.empty());

        service.removeFavoriteBarbershopByFirebaseUid("missing-uid", UUID.randomUUID());

        verify(customerRepository, never()).save(any());
    }

    @Test
    void shouldDelegateUniquenessChecksWithNormalizedInputs() {
        when(customerRepository.existsByTellIgnoreCase("11999999999")).thenReturn(true);
        when(customerRepository.existsByEmailIgnoreCase("ana@example.com")).thenReturn(true);
        when(customerRepository.existsByDocumentCPFIgnoreCase("12345678901")).thenReturn(true);

        assertThat(service.tellExists("11999999999")).isTrue();
        assertThat(service.emailExists(" Ana@Example.COM ")).isTrue();
        assertThat(service.documentCPFExists("123.456.789-01")).isTrue();
    }

    private Customer customer(UUID id, String name) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setFirebaseUid("firebase-" + id);
        customer.setName(name);
        customer.setEmail("cliente@example.com");
        customer.setTell("11999999999");
        return customer;
    }
}
