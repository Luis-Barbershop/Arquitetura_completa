package ifsp.edu.projeto.cortaai.scheduleservice.controller;

import ifsp.edu.projeto.cortaai.scheduleservice.dto.BarberBlockDTO;
import ifsp.edu.projeto.cortaai.scheduleservice.dto.CreateBarberBlockDTO;
import ifsp.edu.projeto.cortaai.scheduleservice.service.BarberBlockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/appointments/barber-blocks", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Barber Blocks", description = "Novo recurso: Endpoints para criação e gestão de bloqueios na agenda de barbeiros (ex: horários de almoço, folgas, férias)")
public class BarberBlockController {

    private final BarberBlockService barberBlockService;

    @Operation(summary = "Criar bloqueio de agenda", description = "Adiciona um bloqueio em um ou mais horários na agenda do barbeiro, impedindo novos agendamentos naquele intervalo.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Bloqueio criado com sucesso")
    })
    @PostMapping
    public ResponseEntity<BarberBlockDTO> createBlock(
            @Parameter(hidden = true) Principal principal,
            @Parameter(description = "Dados do bloqueio (horários e motivo)") @RequestBody @Valid CreateBarberBlockDTO dto) {
        BarberBlockDTO created = barberBlockService.createBlock(principal.getName(), dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @Operation(summary = "Listar bloqueios do barbeiro", description = "Busca todos os bloqueios ativos na agenda de um barbeiro para uma determinada data.")
    @GetMapping
    public ResponseEntity<List<BarberBlockDTO>> getBlocks(
            @Parameter(description = "UUID do barbeiro") @RequestParam UUID barberId,
            @Parameter(description = "Data da consulta (formato ISO: YYYY-MM-DD)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(barberBlockService.getBlocks(barberId, date));
    }

    @Operation(summary = "Remover bloqueio", description = "Exclui um bloqueio de agenda previamente configurado pelo barbeiro ou dono da barbearia.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBlock(
            @Parameter(hidden = true) Principal principal,
            @Parameter(description = "UUID do bloqueio") @PathVariable UUID id) {
        barberBlockService.deleteBlock(principal.getName(), id);
        return ResponseEntity.noContent().build();
    }
}