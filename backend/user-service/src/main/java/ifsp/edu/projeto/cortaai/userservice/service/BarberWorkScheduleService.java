package ifsp.edu.projeto.cortaai.userservice.service;

import ifsp.edu.projeto.cortaai.userservice.dto.DayScheduleDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.SaveWeekScheduleDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.WorkBlockDTO;
import ifsp.edu.projeto.cortaai.userservice.model.Barber;
import ifsp.edu.projeto.cortaai.userservice.model.BarberWorkBlock;
import ifsp.edu.projeto.cortaai.userservice.repository.BarberRepository;
import ifsp.edu.projeto.cortaai.userservice.repository.BarberWorkBlockRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BarberWorkScheduleService {

    private final BarberRepository barberRepository;
    private final BarberWorkBlockRepository workBlockRepository;

    /**
     * Retorna a agenda semanal do barbeiro autenticado (pelo firebaseUid).
     */
    public List<DayScheduleDTO> getSchedule(String firebaseUid) {
        Barber barber = barberRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new EntityNotFoundException("Barbeiro não encontrado."));
        return getScheduleByBarberId(barber.getId());
    }

    /**
     * Retorna a agenda semanal por ID do barbeiro (público/inter-serviço).
     */
    public List<DayScheduleDTO> getScheduleByBarberId(UUID barberId) {
        List<BarberWorkBlock> blocks = workBlockRepository.findByBarberIdOrderByDayOfWeekAscStartTimeAsc(barberId);

        // Agrupa por dia da semana
        Map<DayOfWeek, List<WorkBlockDTO>> grouped = new LinkedHashMap<>();
        for (BarberWorkBlock block : blocks) {
            grouped.computeIfAbsent(block.getDayOfWeek(), k -> new ArrayList<>())
                    .add(new WorkBlockDTO(block.getStartTime(), block.getEndTime()));
        }

        return grouped.entrySet().stream()
                .map(e -> new DayScheduleDTO(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * Substitui toda a agenda semanal do barbeiro.
     * Remove os blocos antigos e salva os novos.
     * Também atualiza workStartTime/workEndTime legados para retrocompatibilidade
     * com o schedule-service (pega o menor startTime e o maior endTime do dia).
     */
    public List<DayScheduleDTO> saveSchedule(String firebaseUid, SaveWeekScheduleDTO dto) {
        Barber barber = barberRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new EntityNotFoundException("Barbeiro não encontrado."));

        // Validação: cada bloco deve ter startTime < endTime
        for (DayScheduleDTO daySchedule : dto.getSchedule()) {
            for (WorkBlockDTO block : daySchedule.getBlocks()) {
                if (!block.getStartTime().isBefore(block.getEndTime())) {
                    throw new IllegalArgumentException(
                            "Horário de início deve ser anterior ao de término em " +
                            daySchedule.getDayOfWeek() + ": " + block.getStartTime() + "–" + block.getEndTime());
                }
            }
            // Valida sobreposição de blocos no mesmo dia
            List<WorkBlockDTO> sorted = daySchedule.getBlocks().stream()
                    .sorted(Comparator.comparing(WorkBlockDTO::getStartTime))
                    .collect(Collectors.toList());
            for (int i = 1; i < sorted.size(); i++) {
                if (sorted.get(i).getStartTime().isBefore(sorted.get(i - 1).getEndTime())) {
                    throw new IllegalArgumentException(
                            "Blocos de horário se sobrepõem em " + daySchedule.getDayOfWeek() + ": " +
                            sorted.get(i - 1).getStartTime() + "–" + sorted.get(i - 1).getEndTime() +
                            " e " + sorted.get(i).getStartTime() + "–" + sorted.get(i).getEndTime());
                }
            }
        }

        // Remove todos os blocos anteriores
        workBlockRepository.deleteByBarberId(barber.getId());

        // Salva novos blocos
        List<BarberWorkBlock> newBlocks = new ArrayList<>();
        for (DayScheduleDTO daySchedule : dto.getSchedule()) {
            for (WorkBlockDTO block : daySchedule.getBlocks()) {
                newBlocks.add(BarberWorkBlock.builder()
                        .barberId(barber.getId())
                        .dayOfWeek(daySchedule.getDayOfWeek())
                        .startTime(block.getStartTime())
                        .endTime(block.getEndTime())
                        .build());
            }
        }
        workBlockRepository.saveAll(newBlocks);

        // Retrocompatibilidade: atualiza workStartTime/workEndTime legado
        // com o range total (menor início / maior fim) para que o schedule-service
        // antigo ainda funcione enquanto não for migrado
        updateLegacyWorkHours(barber, dto);

        return getScheduleByBarberId(barber.getId());
    }

    private void updateLegacyWorkHours(Barber barber, SaveWeekScheduleDTO dto) {
        var allBlocks = dto.getSchedule().stream()
                .flatMap(d -> d.getBlocks().stream())
                .collect(Collectors.toList());

        if (allBlocks.isEmpty()) {
            barber.setWorkStartTime(null);
            barber.setWorkEndTime(null);
        } else {
            barber.setWorkStartTime(
                    allBlocks.stream().map(WorkBlockDTO::getStartTime).min(Comparator.naturalOrder()).orElse(null));
            barber.setWorkEndTime(
                    allBlocks.stream().map(WorkBlockDTO::getEndTime).max(Comparator.naturalOrder()).orElse(null));
        }
        barberRepository.save(barber);
    }
}
