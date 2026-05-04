package ifsp.edu.projeto.cortaai.userservice.service;

import ifsp.edu.projeto.cortaai.userservice.dto.CustomerAcquisitionResponseDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.CustomerRetentionResponseDTO;
import ifsp.edu.projeto.cortaai.userservice.repository.analytics.VCustomerAcquisitionRepository;
import ifsp.edu.projeto.cortaai.userservice.repository.analytics.VCustomerRetentionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserAnalyticsService {

    private final VCustomerAcquisitionRepository vCustomerAcquisitionRepository;
    private final VCustomerRetentionRepository vCustomerRetentionRepository;

    public List<CustomerAcquisitionResponseDTO> getCustomerAcquisition() {
        log.info("Consultando aquisição de clientes por mês");
        return vCustomerAcquisitionRepository.findAll()
                .stream()
                .map(v -> new CustomerAcquisitionResponseDTO(v.getReferenceMonth(), v.getNewCustomers()))
                .toList();
    }

    public List<CustomerRetentionResponseDTO> getCustomerRetention() {
        log.info("Consultando retenção de clientes por mês");
        return vCustomerRetentionRepository.findAll()
                .stream()
                .map(v -> new CustomerRetentionResponseDTO(v.getReferenceMonth(), v.getReturningCustomers()))
                .toList();
    }
}
