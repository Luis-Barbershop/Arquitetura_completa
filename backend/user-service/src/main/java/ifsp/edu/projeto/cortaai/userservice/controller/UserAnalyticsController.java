package ifsp.edu.projeto.cortaai.userservice.controller;

import ifsp.edu.projeto.cortaai.userservice.dto.CustomerAcquisitionResponseDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.CustomerRetentionResponseDTO;
import ifsp.edu.projeto.cortaai.userservice.service.UserAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/users/analytics", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class UserAnalyticsController {

    private final UserAnalyticsService userAnalyticsService;

    @GetMapping("/customer-acquisition")
    public ResponseEntity<List<CustomerAcquisitionResponseDTO>> getCustomerAcquisition() {
        return ResponseEntity.ok(userAnalyticsService.getCustomerAcquisition());
    }

    @GetMapping("/customer-retention")
    public ResponseEntity<List<CustomerRetentionResponseDTO>> getCustomerRetention() {
        return ResponseEntity.ok(userAnalyticsService.getCustomerRetention());
    }
}
