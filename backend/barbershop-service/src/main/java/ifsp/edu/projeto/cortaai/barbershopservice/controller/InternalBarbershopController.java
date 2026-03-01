package ifsp.edu.projeto.cortaai.barbershopservice.controller;

import ifsp.edu.projeto.cortaai.barbershopservice.dto.ActivityInfoDTO;
import ifsp.edu.projeto.cortaai.barbershopservice.dto.BarbershopInfoDTO;
import ifsp.edu.projeto.cortaai.barbershopservice.model.Activity;
import ifsp.edu.projeto.cortaai.barbershopservice.model.Barbershop;
import ifsp.edu.projeto.cortaai.barbershopservice.repository.ActivityRepository;
import ifsp.edu.projeto.cortaai.barbershopservice.repository.BarbershopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Endpoints internos (inter-serviço).
 * Consumidos pelo schedule-service via Feign.
 * NÃO expostos no API Gateway.
 */
@RestController
@RequestMapping(value = "/api/internal/barbershops", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class InternalBarbershopController {

    private final BarbershopRepository barbershopRepository;
    private final ActivityRepository activityRepository;

    @GetMapping("/{id}")
    public ResponseEntity<BarbershopInfoDTO> getBarbershopById(@PathVariable UUID id) {
        Barbershop shop = barbershopRepository.findById(id).orElse(null);
        if (shop == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new BarbershopInfoDTO(
                shop.getId(), shop.getOwnerId(), shop.getName(),
                shop.getCnpj(), shop.getAddress()
        ));
    }

    @GetMapping("/{shopId}/activities")
    public ResponseEntity<List<ActivityInfoDTO>> getActivitiesByIds(
            @PathVariable UUID shopId,
            @RequestParam("ids") List<UUID> ids) {

        List<Activity> allActivities = activityRepository.findByBarbershopId(shopId);

        List<ActivityInfoDTO> filtered = allActivities.stream()
                .filter(a -> ids.contains(a.getId()))
                .map(a -> new ActivityInfoDTO(
                        a.getId(), a.getActivityName(), a.getPrice(),
                        a.getDurationMinutes(), shopId
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(filtered);
    }
}

