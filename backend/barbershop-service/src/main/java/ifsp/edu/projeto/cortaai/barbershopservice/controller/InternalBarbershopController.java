package ifsp.edu.projeto.cortaai.barbershopservice.controller;

import ifsp.edu.projeto.cortaai.barbershopservice.dto.ActivityInfoDTO;
import ifsp.edu.projeto.cortaai.barbershopservice.dto.BarbershopInfoDTO;
import ifsp.edu.projeto.cortaai.barbershopservice.model.Activity;
import ifsp.edu.projeto.cortaai.barbershopservice.model.Barbershop;
import ifsp.edu.projeto.cortaai.barbershopservice.repository.ActivityRepository;
import ifsp.edu.projeto.cortaai.barbershopservice.repository.BarbershopRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Internal - Barbershops", description = "Endpoints internos consumidos pelo schedule-service via Feign (NÃO expostos pelo Gateway)")
public class InternalBarbershopController {

    private final BarbershopRepository barbershopRepository;
    private final ActivityRepository activityRepository;

    @Operation(summary = "Busca barbearia por ID", description = "Retorna dados resumidos de uma barbearia pelo UUID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Barbearia encontrada"),
            @ApiResponse(responseCode = "404", description = "Barbearia não encontrada")
    })
    @GetMapping("/{id}")
    public ResponseEntity<BarbershopInfoDTO> getBarbershopById(
            @Parameter(description = "UUID da barbearia") @PathVariable UUID id) {
        Barbershop shop = barbershopRepository.findById(id).orElse(null);
        if (shop == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new BarbershopInfoDTO(
                shop.getId(), shop.getOwnerId(), shop.getName(),
                shop.getCnpj(), shop.getAddress()
        ));
    }

    @Operation(summary = "Busca atividades por IDs", description = "Retorna uma lista de atividades filtradas pelos UUIDs informados no parâmetro 'ids'.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de atividades retornada")
    })
    @GetMapping("/{shopId}/activities")
    public ResponseEntity<List<ActivityInfoDTO>> getActivitiesByIds(
            @Parameter(description = "UUID da barbearia") @PathVariable UUID shopId,
            @Parameter(description = "Lista de UUIDs das atividades") @RequestParam("ids") List<UUID> ids) {

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

