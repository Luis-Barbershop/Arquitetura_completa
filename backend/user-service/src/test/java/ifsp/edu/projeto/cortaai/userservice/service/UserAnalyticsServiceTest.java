package ifsp.edu.projeto.cortaai.userservice.service;

import ifsp.edu.projeto.cortaai.userservice.model.analytics.VCustomerAcquisition;
import ifsp.edu.projeto.cortaai.userservice.model.analytics.VCustomerRetention;
import ifsp.edu.projeto.cortaai.userservice.repository.analytics.VCustomerAcquisitionRepository;
import ifsp.edu.projeto.cortaai.userservice.repository.analytics.VCustomerRetentionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAnalyticsServiceTest {

    @Mock
    private VCustomerAcquisitionRepository acquisitionRepository;
    @Mock
    private VCustomerRetentionRepository retentionRepository;

    private UserAnalyticsService service;

    @BeforeEach
    void setUp() {
        service = new UserAnalyticsService(acquisitionRepository, retentionRepository);
    }

    @Test
    void shouldMapCustomerAcquisitionViewRowsToDtos() {
        VCustomerAcquisition view = new VCustomerAcquisition();
        ReflectionTestUtils.setField(view, "referenceMonth", "2026-05");
        ReflectionTestUtils.setField(view, "newCustomers", 12L);
        when(acquisitionRepository.findAll()).thenReturn(List.of(view));

        var result = service.getCustomerAcquisition();

        assertThat(result).singleElement().satisfies(dto -> {
            assertThat(dto.referenceMonth()).isEqualTo("2026-05");
            assertThat(dto.newCustomers()).isEqualTo(12L);
        });
    }

    @Test
    void shouldMapCustomerRetentionViewRowsToDtos() {
        VCustomerRetention view = new VCustomerRetention();
        ReflectionTestUtils.setField(view, "referenceMonth", "2026-05");
        ReflectionTestUtils.setField(view, "returningCustomers", 7L);
        when(retentionRepository.findAll()).thenReturn(List.of(view));

        var result = service.getCustomerRetention();

        assertThat(result).singleElement().satisfies(dto -> {
            assertThat(dto.referenceMonth()).isEqualTo("2026-05");
            assertThat(dto.returningCustomers()).isEqualTo(7L);
        });
    }
}
