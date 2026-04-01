package ifsp.edu.projeto.cortaai.service;

import ifsp.edu.projeto.cortaai.dto.*;
import jakarta.validation.Valid;
import ifsp.edu.projeto.cortaai.dto.JoinRequestDTO;
import ifsp.edu.projeto.cortaai.dto.UpdateBarbershopDTO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate; // NOVO: IMPORT ADICIONADO
import java.time.LocalTime; // NOVO: IMPORT ADICIONADO
import java.util.List;
import java.util.UUID;

public interface BarberService {

    // --- Gestão de Barbeiros (Global) ---
    List<BarberDTO> findAll();
    BarberDTO getByEmail(String email);
    BarberDTO get(UUID id);
    UUID create(@Valid CreateBarberDTO createBarberDTO, MultipartFile file) throws IOException;
    void update(String email, BarberDTO barberDTO);
    void delete(String email);
    LoginResponseDTO login(LoginDTO loginDTO);

    // --- Gestão de Barbearias (Fluxo 1) ---
    BarbershopDTO createBarbershop(String ownerEmail, @Valid CreateBarbershopDTO createBarbershopDTO, MultipartFile file) throws IOException;
    BarbershopDTO updateBarbershop(String ownerEmail, UpdateBarbershopDTO updateBarbershopDTO);
    BarbershopDTO getBarbershop(UUID barbershopId);
    List<BarbershopDTO> listBarbershops();
    void closeBarbershop(String ownerEmail, CloseBarbershopRequestDTO closeBarbershopRequestDTO);
    ActivityDTO updateActivity(String ownerEmail, UUID activityId, UpdateActivityDTO updateActivityDTO);
    void deleteActivity(String ownerEmail, UUID activityId);

    // --- Gestão de Serviços (Fluxo 1) ---
    ActivityDTO createActivities(String ownerEmail, @Valid CreateActivityDTO createActivityDTO);
    List<ActivityDTO> listActivities(UUID barbershopId);
    List<BarberDTO> listBarbersByBarbershop(UUID barbershopId);
    List<ActivityDTO> listActivitiesByBarber(UUID barberId);

    // --- Gestão de Vínculos (Fluxos 2 e 3) ---
    void requestToJoinBarbershop(String barberEmail, String cnpj);
    void approveJoinRequest(String ownerEmail, Long requestId);
    void freeBarber(String barberEmail);
    void removeBarber(String ownerEmail, UUID barberIdToRemove);
    void rejectJoinRequest(String ownerEmail, Long requestId); // NOVO
    List<JoinRequestDTO> getPendingJoinRequests(String ownerEmail);

    // --- Gestão de Habilidades (Fluxo 2) ---
    void assignActivities(String barberEmail, @Valid BarberActivityAssignDTO barberActivityAssignDTO);
    List<ActivityDTO> getMyAssignedActivities(String barberEmail);
    void setWorkHours(String email, BarberWorkHoursDTO workHoursDTO);

    // -- Método para obter horários disponíveis de um barbeiro --
    List<LocalTime> getAvailableSlots(UUID barberId, LocalDate date, int durationInMinutes);
    List<JoinRequestHistoryDTO> getJoinRequestHistory(String barberEmail); // NOVO
    List<DailyAvailabilityDTO> getMonthlyAvailability(UUID barberId, int year, int month);

    // --- Métodos de validação ---
    boolean tellExists(String tell);
    boolean emailExists(String email);
    boolean documentCPFExists(String documentCPF);

    // --- MÉTODOS DE UPLOAD DE IMAGEM ---
    String updateBarberProfilePhoto(String email, MultipartFile file) throws IOException;
    String updateActivityPhoto(String ownerEmail, UUID activityId, MultipartFile file) throws IOException;
    String updateBarbershopLogo(String ownerEmail, MultipartFile file) throws IOException;
    String updateBarbershopBanner(String ownerEmail, MultipartFile file) throws IOException;
    String addBarbershopHighlight(String ownerEmail, MultipartFile file) throws IOException;
    void deleteBarbershopHighlight(String ownerEmail, UUID highlightId);


}