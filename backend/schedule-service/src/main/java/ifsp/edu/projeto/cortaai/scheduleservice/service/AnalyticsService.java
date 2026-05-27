package ifsp.edu.projeto.cortaai.scheduleservice.service;

import ifsp.edu.projeto.cortaai.scheduleservice.dto.AgendaThermometerResponseDTO;
import ifsp.edu.projeto.cortaai.scheduleservice.dto.BarberSkillMatrixResponseDTO;
import ifsp.edu.projeto.cortaai.scheduleservice.dto.UserInfoDTO;
import ifsp.edu.projeto.cortaai.scheduleservice.feign.UserServiceClient;
import ifsp.edu.projeto.cortaai.scheduleservice.repository.AppointmentRepository;
import ifsp.edu.projeto.cortaai.scheduleservice.repository.analytics.VBarberSkillMatrixRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AnalyticsService {

    private final AppointmentRepository appointmentRepository;
    private final VBarberSkillMatrixRepository vBarberSkillMatrixRepository;
    private final UserServiceClient userServiceClient;

    public List<AgendaThermometerResponseDTO> getAgendaThermometer(String firebaseUid, String barbershopId) {
        requireOwnerAccess(firebaseUid, barbershopId);
        return getAgendaThermometer(barbershopId);
    }

    public List<AgendaThermometerResponseDTO> getAgendaThermometer(String barbershopId) {
        log.info("Consultando termômetro de agenda por status para barbearia: {}", barbershopId);
        return appointmentRepository.findAgendaThermometerByBarbershopId(barbershopId)
                .stream()
                .map(v -> new AgendaThermometerResponseDTO(
                        v.getAgendaDate(),
                        v.getBarbershopId(),
                        v.getTotalAppointments(),
                        v.getActiveAppointments(),
                        v.getWalkinAppointments(),
                        v.getPendingAppointments(),
                        v.getCompletedAppointments(),
                        v.getLostAppointments()
                ))
                .toList();
    }

    public List<BarberSkillMatrixResponseDTO> getBarberSkillMatrix(String firebaseUid, String barbershopId) {
        requireOwnerAccess(firebaseUid, barbershopId);
        return getBarberSkillMatrix(barbershopId);
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

    private void requireOwnerAccess(String firebaseUid, String barbershopId) {
        UserInfoDTO user = userServiceClient.getUserByFirebaseUid(firebaseUid);
        if (user == null || user.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário autenticado não encontrado.");
        }
        if (!"BARBER".equalsIgnoreCase(user.getUserType()) || !isOwner(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas o dono pode acessar os analytics da barbearia.");
        }
        String userShopId = user.getBarbershopId() == null ? "" : user.getBarbershopId().toString();
        if (barbershopId == null || !barbershopId.equals(userShopId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Sem permissão para acessar os analytics desta barbearia.");
        }
    }

    private boolean isOwner(UserInfoDTO user) {
        String role = user.getRole() == null ? "" : user.getRole().toUpperCase(Locale.ROOT);
        return role.contains("OWNER");
    }
}
