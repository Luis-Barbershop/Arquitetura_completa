package ifsp.edu.projeto.cortaai.userservice.repository;

import ifsp.edu.projeto.cortaai.userservice.model.Barber;
import ifsp.edu.projeto.cortaai.userservice.model.Customer;
import ifsp.edu.projeto.cortaai.userservice.security.crypto.DataCrypto;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RepositoryDefaultMethodsTest {

    private static final String TEST_KEY = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    @BeforeAll
    static void configureCrypto() {
        DataCrypto.configure(TEST_KEY);
    }

    @Test
    void customerRepositoryDefaultsShouldNormalizeEmailAndCheckLegacySensitiveFields() {
        CustomerRepository repository = mock(CustomerRepository.class, CALLS_REAL_METHODS);
        Customer customer = new Customer();
        when(repository.findByEmailHash(anyString())).thenReturn(Optional.of(customer));
        when(repository.existsByDocumentCPFEncrypted("12345678909")).thenReturn(false);
        when(repository.countByDocumentCPFRaw("12345678909")).thenReturn(1L);
        when(repository.existsByTellEncrypted("11999999999")).thenReturn(true);

        assertThat(repository.findByEmail(" ANA@Example.COM ")).contains(customer);
        assertThat(repository.findByEmail(null)).isEmpty();
        assertThat(repository.existsByEmail("ana@example.com")).isTrue();
        assertThat(repository.existsByEmailIgnoreCase("ana@example.com")).isTrue();
        assertThat(repository.existsByDocumentCPF("12345678909")).isTrue();
        assertThat(repository.existsByDocumentCPFIgnoreCase("12345678909")).isTrue();
        assertThat(repository.existsByTell("11999999999")).isTrue();
        assertThat(repository.existsByTellIgnoreCase("11999999999")).isTrue();
    }

    @Test
    void barberRepositoryDefaultsShouldFallbackAcrossSensitiveLookupStrategies() {
        BarberRepository repository = mock(BarberRepository.class, CALLS_REAL_METHODS);
        Barber barber = Barber.builder().email("barber@example.com").build();
        when(repository.findByEmailHash(anyString())).thenReturn(Optional.empty());
        when(repository.findByEmailEncrypted("barber@example.com")).thenReturn(Optional.empty());
        when(repository.findByEmailRaw("barber@example.com")).thenReturn(Optional.of(barber));
        when(repository.existsByDocumentCPFEncrypted("12345678909")).thenReturn(false);
        when(repository.countByDocumentCPFRaw("12345678909")).thenReturn(1L);
        when(repository.existsByTellEncrypted("11999999999")).thenReturn(false);
        when(repository.countByTellRaw("11999999999")).thenReturn(1L);
        when(repository.findByDocumentCPFEncrypted("12345678909")).thenReturn(Optional.empty());
        when(repository.findByDocumentCPFRaw("12345678909")).thenReturn(Optional.of(barber));

        assertThat(repository.findByEmail(" BARBER@Example.COM ")).contains(barber);
        assertThat(repository.findByEmail(null)).isEmpty();
        assertThat(repository.existsByEmail("barber@example.com")).isTrue();
        assertThat(repository.existsByEmailIgnoreCase("barber@example.com")).isTrue();
        assertThat(repository.existsByDocumentCPF("12345678909")).isTrue();
        assertThat(repository.existsByDocumentCPFIgnoreCase("12345678909")).isTrue();
        assertThat(repository.existsByTellIgnoreCase("11999999999")).isTrue();
        assertThat(repository.findByDocumentCPF("12345678909")).contains(barber);
    }
}
