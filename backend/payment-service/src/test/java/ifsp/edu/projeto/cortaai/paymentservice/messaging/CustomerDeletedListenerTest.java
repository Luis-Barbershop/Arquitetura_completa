package ifsp.edu.projeto.cortaai.paymentservice.messaging;

import ifsp.edu.projeto.cortaai.paymentservice.model.Transaction;
import ifsp.edu.projeto.cortaai.paymentservice.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerDeletedListenerTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private CustomerDeletedListener listener;

    @Test
    void shouldAnonymizeCustomerTransactions() {
        UUID customerId = UUID.randomUUID();
        Transaction first = Transaction.builder().id(UUID.randomUUID()).customerId(customerId).build();
        Transaction second = Transaction.builder().id(UUID.randomUUID()).customerId(customerId).build();

        when(transactionRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)).thenReturn(List.of(first, second));

        listener.onCustomerDeleted(Map.of("customerId", customerId.toString()));

        assertThat(first.getCustomerId()).isNull();
        assertThat(second.getCustomerId()).isNull();
        ArgumentCaptor<List<Transaction>> captor = ArgumentCaptor.forClass(List.class);
        verify(transactionRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).containsExactly(first, second);
    }

    @Test
    void shouldRethrowInvalidPayloadFailures() {
        assertThatThrownBy(() -> listener.onCustomerDeleted(Map.of("customerId", "invalid-uuid")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
