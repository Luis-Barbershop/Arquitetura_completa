package ifsp.edu.projeto.cortaai.barbershopservice.controller;

import ifsp.edu.projeto.cortaai.barbershopservice.dto.ActivityInfoDTO;
import ifsp.edu.projeto.cortaai.barbershopservice.dto.BarbershopInfoDTO;
import ifsp.edu.projeto.cortaai.barbershopservice.dto.CommissionRuleDTO;
import ifsp.edu.projeto.cortaai.barbershopservice.exception.ApiErrorResponse;
import ifsp.edu.projeto.cortaai.barbershopservice.model.Activity;
import ifsp.edu.projeto.cortaai.barbershopservice.model.Barbershop;
import ifsp.edu.projeto.cortaai.barbershopservice.repository.ActivityRepository;
import ifsp.edu.projeto.cortaai.barbershopservice.repository.BarberCommissionRuleRepository;
import ifsp.edu.projeto.cortaai.barbershopservice.repository.BarbershopRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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
    private final BarberCommissionRuleRepository commissionRuleRepository;

    @Operation(summary = "Busca barbearia por ID", description = "Retorna dados resumidos de uma barbearia pelo UUID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Barbearia encontrada"),
            @ApiResponse(responseCode = "404", description = "Barbearia não encontrada",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
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

    @Operation(summary = "Lista todas as atividades da barbearia", description = "Retorna todas as atividades ativas da barbearia. Usado pelo schedule-service para cruzar com a skill matrix.")
    @GetMapping("/{shopId}/activities/all")
    public ResponseEntity<List<ActivityInfoDTO>> getAllActivities(
            @Parameter(description = "UUID da barbearia") @PathVariable UUID shopId) {

        List<ActivityInfoDTO> activities = activityRepository.findByBarbershopId(shopId)
                .stream()
                .map(a -> new ActivityInfoDTO(
                        a.getId(), a.getActivityName(), a.getPrice(),
                        a.getDurationMinutes(), shopId
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(activities);
    }

    @Operation(summary = "Regras de comissão de um barbeiro (interno)", description = "Retorna as regras de comissão por atividade de um barbeiro em uma barbearia.")
    @GetMapping("/{shopId}/barbers/{barberId}/commissions")
    public ResponseEntity<List<CommissionRuleDTO>> getBarberCommissions(
            @PathVariable UUID shopId,
            @PathVariable UUID barberId) {
        List<CommissionRuleDTO> rules = commissionRuleRepository
                .findByBarbershopIdAndBarberId(shopId, barberId)
                .stream()
                .map(r -> new CommissionRuleDTO(
                        r.getId(),
                        r.getActivity().getId(),
                        r.getActivity().getActivityName(),
                        r.getPercentage()
                ))
                .toList();
        return ResponseEntity.ok(rules);
    }
}

