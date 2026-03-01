package ifsp.edu.projeto.cortaai.barbershopservice.controller;

import ifsp.edu.projeto.cortaai.barbershopservice.dto.*;
import ifsp.edu.projeto.cortaai.barbershopservice.service.BarbershopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/barbershops", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class BarbershopController {

    private final BarbershopService barbershopService;

    // ========== LEITURA PÚBLICA ==========

    @GetMapping
    public ResponseEntity<List<BarbershopDTO>> listAllBarbershops() {
        return ResponseEntity.ok(barbershopService.listBarbershops());
    }

    @GetMapping("/{shopId}")
    public ResponseEntity<BarbershopDTO> getBarbershop(@PathVariable UUID shopId) {
        return ResponseEntity.ok(barbershopService.getBarbershop(shopId));
    }

    @GetMapping("/{shopId}/activities")
    public ResponseEntity<List<ActivityDTO>> listActivities(@PathVariable UUID shopId) {
        return ResponseEntity.ok(barbershopService.listActivities(shopId));
    }

    // ========== FLUXO 1: GESTÃO DO DONO (OWNER) ==========

    @PostMapping(value = "/register-my-shop", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BarbershopDTO> createBarbershop(
            Principal principal,
            @RequestPart("shop") @Valid CreateBarbershopDTO dto,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        try {
            BarbershopDTO created = barbershopService.createBarbershop(principal.getName(), dto, file);
            return new ResponseEntity<>(created, HttpStatus.CREATED);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/my-shop")
    public ResponseEntity<BarbershopDTO> updateBarbershop(
            Principal principal,
            @RequestBody @Valid UpdateBarbershopDTO dto) {
        return ResponseEntity.ok(barbershopService.updateBarbershop(principal.getName(), dto));
    }

    @PostMapping("/my-shop/activities")
    public ResponseEntity<ActivityDTO> createActivity(
            Principal principal,
            @RequestBody @Valid CreateActivityDTO dto) {
        ActivityDTO created = barbershopService.createActivity(principal.getName(), dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/my-shop/activities/{activityId}")
    public ResponseEntity<ActivityDTO> updateActivity(
            Principal principal,
            @PathVariable UUID activityId,
            @RequestBody @Valid UpdateActivityDTO dto) {
        return ResponseEntity.ok(barbershopService.updateActivity(principal.getName(), activityId, dto));
    }

    @DeleteMapping("/my-shop/activities/{activityId}")
    public ResponseEntity<Void> deleteActivity(Principal principal, @PathVariable UUID activityId) {
        barbershopService.deleteActivity(principal.getName(), activityId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/my-shop/remove-barber/{barberId}")
    public ResponseEntity<Void> removeBarber(Principal principal, @PathVariable UUID barberId) {
        barbershopService.removeBarber(principal.getName(), barberId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/my-shop/close")
    public ResponseEntity<Void> closeBarbershop(
            Principal principal,
            @RequestBody @Valid CloseBarbershopRequestDTO dto) {
        barbershopService.closeBarbershop(principal.getName(), dto);
        return ResponseEntity.noContent().build();
    }

    // ========== FLUXO 2: JOIN REQUESTS ==========

    @PostMapping("/join-request")
    public ResponseEntity<Void> requestToJoin(
            Principal principal,
            @RequestBody @Valid BarberJoinRequestDTO dto) {
        barbershopService.requestToJoinBarbershop(principal.getName(), dto.getCnpj());
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/my-shop/pending-requests")
    public ResponseEntity<List<JoinRequestDTO>> getPendingRequests(Principal principal) {
        return ResponseEntity.ok(barbershopService.getPendingJoinRequests(principal.getName()));
    }

    @PostMapping("/my-shop/approve-request/{requestId}")
    public ResponseEntity<Void> approveJoinRequest(
            Principal principal,
            @PathVariable UUID requestId) {
        barbershopService.approveJoinRequest(principal.getName(), requestId);
        return ResponseEntity.noContent().build();
    }

    // ========== FLUXO 3: SAIR DA LOJA ==========

    @PostMapping("/leave-shop")
    public ResponseEntity<Void> leaveShop(Principal principal) {
        barbershopService.freeBarber(principal.getName());
        return ResponseEntity.noContent().build();
    }

    // ========== FLUXO 4: GESTÃO DE IMAGENS ==========

    @PostMapping("/my-shop/upload-logo")
    public ResponseEntity<String> uploadLogo(Principal principal, @RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.ok(barbershopService.updateBarbershopLogo(principal.getName(), file));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Falha no upload: " + e.getMessage());
        }
    }

    @PostMapping("/my-shop/upload-banner")
    public ResponseEntity<String> uploadBanner(Principal principal, @RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.ok(barbershopService.updateBarbershopBanner(principal.getName(), file));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Falha no upload: " + e.getMessage());
        }
    }

    @PostMapping("/my-shop/activities/{activityId}/upload-photo")
    public ResponseEntity<String> uploadActivityPhoto(
            Principal principal,
            @PathVariable UUID activityId,
            @RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.ok(barbershopService.updateActivityPhoto(principal.getName(), activityId, file));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Falha no upload: " + e.getMessage());
        }
    }

    @PostMapping("/my-shop/highlights")
    public ResponseEntity<String> addHighlight(Principal principal, @RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    barbershopService.addBarbershopHighlight(principal.getName(), file));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Falha no upload: " + e.getMessage());
        }
    }

    @DeleteMapping("/my-shop/highlights/{highlightId}")
    public ResponseEntity<Void> deleteHighlight(Principal principal, @PathVariable UUID highlightId) {
        barbershopService.deleteBarbershopHighlight(principal.getName(), highlightId);
        return ResponseEntity.noContent().build();
    }
}

