package ifsp.edu.projeto.cortaai.paymentservice.messaging;

import ifsp.edu.projeto.cortaai.paymentservice.config.RabbitConfig;
import ifsp.edu.projeto.cortaai.paymentservice.model.Transaction;
import ifsp.edu.projeto.cortaai.paymentservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerDeletedListener {

    private final TransactionRepository transactionRepository;

    @RabbitListener(queues = RabbitConfig.QUEUE_CUSTOMER_DELETED)
    @Transactional
    public void onCustomerDeleted(Map<String, Object> payload) {
        try {
            UUID customerId = UUID.fromString(payload.get("customerId").toString());
            // Naturalmente idempotente: após a primeira execução, customerId é setado para null
            // nas transações. Redeliveries subsequentes retornam lista vazia → saveAll([]) → no-op.
            List<Transaction> transactions = transactionRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
            for (Transaction t : transactions) {
                t.setCustomerId(null);
            }
            transactionRepository.saveAll(transactions);
            log.info("Anonimizados {} pagamentos do customerId={}", transactions.size(), customerId);
        } catch (Exception e) {
            log.error("Erro ao anonimizar transações após exclusão de customer: {}", e.getMessage(), e);
            throw e;
        }
    }
}
