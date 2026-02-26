package ifsp.edu.projeto.cortaai.barbershop.service;

import ifsp.edu.projeto.cortaai.barbershop.dto.JoinRequestDTO;

import java.util.List;
import java.util.UUID;

public interface JoinRequestService {

    void requestToJoin(UUID barberId, String cnpj);
    
    List<JoinRequestDTO> getPendingRequests(UUID barbershopId, UUID requesterId);
    
    void approveRequest(Long requestId, UUID requesterId);
    
    void rejectRequest(Long requestId, UUID requesterId);
    
    void removeBarberFromShop(UUID barberId, UUID requesterId);
}
