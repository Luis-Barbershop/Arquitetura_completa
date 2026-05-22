package ifsp.edu.projeto.cortaai.userservice.controller;

import ifsp.edu.projeto.cortaai.userservice.dto.CustomerDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.UpdateCustomerDTO;
import ifsp.edu.projeto.cortaai.userservice.service.CustomerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerControllerTest {

    @Mock
    private CustomerService customerService;
    @Mock
    private MultipartFile file;

    private CustomerController controller;

    @BeforeEach
    void setUp() {
        controller = new CustomerController(customerService);
    }

    @Test
    void shouldListAndGetCustomers() {
        UUID id = UUID.randomUUID();
        CustomerDTO customer = customer(id);
        when(customerService.findAll()).thenReturn(List.of(customer));
        when(customerService.get(id)).thenReturn(customer);

        assertThat(controller.getAllCustomers().getBody()).containsExactly(customer);
        assertThat(controller.getCustomer(id).getBody()).isEqualTo(customer);
    }

    @Test
    void shouldUpdateAndDeleteAuthenticatedCustomer() {
        UpdateCustomerDTO dto = new UpdateCustomerDTO();
        dto.setName("Novo nome");

        ResponseEntity<Void> update = controller.updateCustomer("firebase-uid", dto);
        ResponseEntity<Void> delete = controller.deleteCustomer("firebase-uid");

        assertThat(update.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(delete.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(customerService).updateByFirebaseUid("firebase-uid", dto);
        verify(customerService).deleteByFirebaseUid("firebase-uid");
    }

    @Test
    void shouldManageFavoritesForAuthenticatedCustomer() {
        UUID barbershopId = UUID.randomUUID();
        when(customerService.listFavoriteBarbershopIdsByFirebaseUid("firebase-uid")).thenReturn(List.of(barbershopId));

        assertThat(controller.listMyFavorites("firebase-uid").getBody()).containsExactly(barbershopId);
        assertThat(controller.addFavorite("firebase-uid", barbershopId).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(controller.removeFavorite("firebase-uid", barbershopId).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        verify(customerService).addFavoriteBarbershopByFirebaseUid("firebase-uid", barbershopId);
        verify(customerService).removeFavoriteBarbershopByFirebaseUid("firebase-uid", barbershopId);
    }

    @Test
    void shouldUploadCustomerPhotoAndMapIoExceptionToServerError() throws IOException {
        when(customerService.updateProfilePhotoByFirebaseUid("firebase-uid", file)).thenReturn("https://cdn/customer.png");

        ResponseEntity<String> success = controller.uploadCustomerPhoto("firebase-uid", file);

        assertThat(success.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(success.getBody()).isEqualTo("https://cdn/customer.png");

        when(customerService.updateProfilePhotoByFirebaseUid("firebase-uid", file))
                .thenThrow(new IOException("storage down"));

        ResponseEntity<String> failure = controller.uploadCustomerPhoto("firebase-uid", file);

        assertThat(failure.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(failure.getBody()).isEqualTo("Falha no upload: storage down");
    }

    private CustomerDTO customer(UUID id) {
        CustomerDTO dto = new CustomerDTO();
        dto.setId(id);
        dto.setName("Cliente");
        dto.setEmail("cliente@example.com");
        dto.setTell("11999999999");
        dto.setDocumentCPF("12345678909");
        return dto;
    }
}
