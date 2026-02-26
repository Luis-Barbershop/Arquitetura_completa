package ifsp.edu.projeto.cortaai.barbershop.controller;

import ifsp.edu.projeto.cortaai.barbershop.dto.*;
import ifsp.edu.projeto.cortaai.barbershop.service.ActivityService;
import ifsp.edu.projeto.cortaai.barbershop.service.BarbershopService;
import ifsp.edu.projeto.cortaai.barbershop.service.JoinRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/barbershops", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Barbershop", description = "Endpoints para gerenciamento de barbearias")
public class BarbershopController {

    private final BarbershopService barbershopService;
    private final ActivityService activityService;
    private final JoinRequestService joinRequestService;

    // ==================== Public Endpoints ====================

    @GetMapping
    @Operation(summary = "Lista todas as barbearias")
    public ResponseEntity<List<BarbershopDTO>> listAll() {
        return ResponseEntity.ok(barbershopService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca barbearia por ID")
    public ResponseEntity<BarbershopDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(barbershopService.findById(id));
    }

    @GetMapping("/cnpj/{cnpj}")
    @Operation(summary = "Busca barbearia por CNPJ")
    public ResponseEntity<BarbershopDTO> findByCnpj(@PathVariable String cnpj) {
        return ResponseEntity.ok(barbershopService.findByCnpj(cnpj));
    }

    @GetMapping("/{shopId}/activities")
    @Operation(summary = "Lista serviços de uma barbearia")
    public ResponseEntity<List<ActivityDTO>> listActivities(@PathVariable UUID shopId) {
        return ResponseEntity.ok(activityService.findByBarbershopId(shopId));
    }

    // ==================== Owner Endpoints ====================

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Cria uma nova barbearia")
    @ApiResponse(responseCode = "201")
    public ResponseEntity<BarbershopDTO> create(
            @RequestHeader("X-User-Id") UUID ownerId,
            @RequestPart("shop") @Valid CreateBarbershopDTO dto,
            @RequestPart(value = "logo", required = false) MultipartFile logo) throws IOException {
        BarbershopDTO created = barbershopService.create(dto, ownerId, logo);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza dados da barbearia")
    public ResponseEntity<BarbershopDTO> update(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID requesterId,
            @RequestBody @Valid UpdateBarbershopDTO dto) {
        return ResponseEntity.ok(barbershopService.update(id, dto, requesterId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Fecha/exclui uma barbearia")
    @ApiResponse(responseCode = "204")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID requesterId) {
        barbershopService.delete(id, requesterId);
        return ResponseEntity.noContent().build();
    }

    // ==================== Activity Management ====================

    @PostMapping("/{shopId}/activities")
    @Operation(summary = "Cria um novo serviço")
    @ApiResponse(responseCode = "201")
    public ResponseEntity<ActivityDTO> createActivity(
            @PathVariable UUID shopId,
            @RequestHeader("X-User-Id") UUID requesterId,
            @RequestBody @Valid CreateActivityDTO dto) {
        ActivityDTO created = activityService.create(shopId, dto, requesterId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/activities/{activityId}")
    @Operation(summary = "Atualiza um serviço")
    public ResponseEntity<ActivityDTO> updateActivity(
            @PathVariable UUID activityId,
            @RequestHeader("X-User-Id") UUID requesterId,
            @RequestBody @Valid UpdateActivityDTO dto) {
        return ResponseEntity.ok(activityService.update(activityId, dto, requesterId));
    }

    @DeleteMapping("/activities/{activityId}")
    @Operation(summary = "Exclui um serviço")
    @ApiResponse(responseCode = "204")
    public ResponseEntity<Void> deleteActivity(
            @PathVariable UUID activityId,
            @RequestHeader("X-User-Id") UUID requesterId) {
        activityService.delete(activityId, requesterId);
        return ResponseEntity.noContent().build();
    }

    // ==================== Image Management ====================

    @PostMapping(value = "/{id}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload do logo da barbearia")
    public ResponseEntity<String> uploadLogo(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID requesterId,
            @RequestParam("file") MultipartFile file) throws IOException {
        String url = barbershopService.updateLogo(id, file, requesterId);
        return ResponseEntity.ok(url);
    }

    @PostMapping(value = "/{id}/banner", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload do banner da barbearia")
    public ResponseEntity<String> uploadBanner(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID requesterId,
            @RequestParam("file") MultipartFile file) throws IOException {
        String url = barbershopService.updateBanner(id, file, requesterId);
        return ResponseEntity.ok(url);
    }

    @PostMapping(value = "/{id}/highlights", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Adiciona destaque da barbearia")
    @ApiResponse(responseCode = "201")
    public ResponseEntity<String> addHighlight(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID requesterId,
            @RequestParam("file") MultipartFile file) throws IOException {
        String url = barbershopService.addHighlight(id, file, requesterId);
        return ResponseEntity.status(HttpStatus.CREATED).body(url);
    }

    @DeleteMapping("/{shopId}/highlights/{highlightId}")
    @Operation(summary = "Remove destaque da barbearia")
    @ApiResponse(responseCode = "204")
    public ResponseEntity<Void> deleteHighlight(
            @PathVariable UUID shopId,
            @PathVariable UUID highlightId,
            @RequestHeader("X-User-Id") UUID requesterId) {
        barbershopService.deleteHighlight(shopId, highlightId, requesterId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/activities/{activityId}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload de foto do serviço")
    public ResponseEntity<String> uploadActivityPhoto(
            @PathVariable UUID activityId,
            @RequestHeader("X-User-Id") UUID requesterId,
            @RequestParam("file") MultipartFile file) throws IOException {
        String url = activityService.updatePhoto(activityId, file, requesterId);
        return ResponseEntity.ok(url);
    }

    // ==================== Join Request Management ====================

    @PostMapping("/join-request")
    @Operation(summary = "Solicita entrada em uma barbearia")
    @ApiResponse(responseCode = "202")
    public ResponseEntity<Void> requestToJoin(
            @RequestHeader("X-User-Id") UUID barberId,
            @RequestBody @Valid BarberJoinRequestDTO dto) {
        joinRequestService.requestToJoin(barberId, dto.getCnpj());
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/{shopId}/pending-requests")
    @Operation(summary = "Lista solicitações pendentes")
    public ResponseEntity<List<JoinRequestDTO>> getPendingRequests(
            @PathVariable UUID shopId,
            @RequestHeader("X-User-Id") UUID requesterId) {
        return ResponseEntity.ok(joinRequestService.getPendingRequests(shopId, requesterId));
    }

    @PostMapping("/requests/{requestId}/approve")
    @Operation(summary = "Aprova solicitação de entrada")
    @ApiResponse(responseCode = "204")
    public ResponseEntity<Void> approveRequest(
            @PathVariable Long requestId,
            @RequestHeader("X-User-Id") UUID requesterId) {
        joinRequestService.approveRequest(requestId, requesterId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/requests/{requestId}/reject")
    @Operation(summary = "Rejeita solicitação de entrada")
    @ApiResponse(responseCode = "204")
    public ResponseEntity<Void> rejectRequest(
            @PathVariable Long requestId,
            @RequestHeader("X-User-Id") UUID requesterId) {
        joinRequestService.rejectRequest(requestId, requesterId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{shopId}/barbers/{barberId}")
    @Operation(summary = "Remove barbeiro da barbearia")
    @ApiResponse(responseCode = "204")
    public ResponseEntity<Void> removeBarber(
            @PathVariable UUID shopId,
            @PathVariable UUID barberId,
            @RequestHeader("X-User-Id") UUID requesterId) {
        joinRequestService.removeBarberFromShop(barberId, requesterId);
        return ResponseEntity.noContent().build();
    }
}
