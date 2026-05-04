package ifsp.edu.projeto.cortaai.scheduleservice.service;

import ifsp.edu.projeto.cortaai.scheduleservice.dto.AgendaThermometerResponseDTO;
import ifsp.edu.projeto.cortaai.scheduleservice.dto.BarberSkillMatrixResponseDTO;
import ifsp.edu.projeto.cortaai.scheduleservice.repository.analytics.VAgendaThermometerRepository;
import ifsp.edu.projeto.cortaai.scheduleservice.repository.analytics.VBarberSkillMatrixRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AnalyticsService {

    private final VAgendaThermometerRepository vAgendaThermometerRepository;
    private final VBarberSkillMatrixRepository vBarberSkillMatrixRepository;

    public List<AgendaThermometerResponseDTO> getAgendaThermometer(String barbershopId) {
        log.info("Consultando termômetro de agenda para barbearia: {}", barbershopId);
        return vAgendaThermometerRepository.findByBarbershopId(barbershopId)
                .stream()
                .map(v -> new AgendaThermometerResponseDTO(
                        v.getAgendaDate(),
                        v.getBarbershopId(),
                        v.getTotalAppointments(),
                        v.getActiveAppointments(),
                        v.getLostAppointments()
                ))
                .toList();
    }

    public List<BarberSkillMatrixResponseDTO> getBarberSkillMatrix(String barbershopId) {
        log.info("Consultando matriz de habilidades para barbearia: {}", barbershopId);
        return vBarberSkillMatrixRepository.findByBarbershopId(barbershopId)
                .stream()
                .map(v -> new BarberSkillMatrixResponseDTO(
                        v.getBarberId(),
                        v.getBarberName(),
                        v.getActivityName(),
                        v.getTimesExecuted(),
                        v.getTotalGeneratedByActivity()
                ))
                .toList();
    }
}
