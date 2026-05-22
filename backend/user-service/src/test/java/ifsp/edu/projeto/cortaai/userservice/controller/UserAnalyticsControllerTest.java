package ifsp.edu.projeto.cortaai.userservice.controller;

import ifsp.edu.projeto.cortaai.userservice.dto.CustomerAcquisitionResponseDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.CustomerRetentionResponseDTO;
import ifsp.edu.projeto.cortaai.userservice.service.UserAnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAnalyticsControllerTest {

    @Mock
    private UserAnalyticsService userAnalyticsService;

    private UserAnalyticsController controller;

    @BeforeEach
    void setUp() {
        controller = new UserAnalyticsController(userAnalyticsService);
    }

    @Test
    void shouldReturnCustomerAnalytics() {
        CustomerAcquisitionResponseDTO acquisition = new CustomerAcquisitionResponseDTO("2026-05", 12L);
        CustomerRetentionResponseDTO retention = new CustomerRetentionResponseDTO("2026-05", 7L);
        when(userAnalyticsService.getCustomerAcquisition()).thenReturn(List.of(acquisition));
        when(userAnalyticsService.getCustomerRetention()).thenReturn(List.of(retention));

        assertThat(controller.getCustomerAcquisition().getBody()).containsExactly(acquisition);
        assertThat(controller.getCustomerRetention().getBody()).containsExactly(retention);
    }
}
