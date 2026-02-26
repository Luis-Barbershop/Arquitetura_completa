package ifsp.edu.projeto.cortaai.barbershop.service.impl;

import ifsp.edu.projeto.cortaai.barbershop.dto.JoinRequestDTO;
import ifsp.edu.projeto.cortaai.barbershop.exception.DuplicateResourceException;
import ifsp.edu.projeto.cortaai.barbershop.exception.ForbiddenException;
import ifsp.edu.projeto.cortaai.barbershop.exception.NotFoundException;
import ifsp.edu.projeto.cortaai.barbershop.mapper.JoinRequestMapper;
import ifsp.edu.projeto.cortaai.barbershop.model.Barbershop;
import ifsp.edu.projeto.cortaai.barbershop.model.BarbershopJoinRequest;
import ifsp.edu.projeto.cortaai.barbershop.model.enums.JoinRequestStatus;
import ifsp.edu.projeto.cortaai.barbershop.repository.BarbershopJoinRequestRepository;
import ifsp.edu.projeto.cortaai.barbershop.repository.BarbershopRepository;
import ifsp.edu.projeto.cortaai.barbershop.service.JoinRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class JoinRequestServiceImpl implements JoinRequestService {

    private final BarbershopJoinRequestRepository joinRequestRepository;
    private final BarbershopRepository barbershopRepository;
    private final JoinRequestMapper joinRequestMapper;

    @Override
    public void requestToJoin(UUID barberId, String cnpj) {
        Barbershop barbershop = barbershopRepository.findByCnpj(cnpj)
                .orElseThrow(() -> new NotFoundException("Barbearia não encontrada com CNPJ: " + cnpj));

        // Check if already has pending request
        if (joinRequestRepository.existsByBarberIdAndStatus(barberId, JoinRequestStatus.PENDING)) {
            throw new DuplicateResourceException("Já existe uma solicitação pendente para este barbeiro");
        }

        BarbershopJoinRequest request = new BarbershopJoinRequest();
        request.setBarbershop(barbershop);
        request.setBarberId(barberId);
        request.setStatus(JoinRequestStatus.PENDING);

        joinRequestRepository.save(request);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JoinRequestDTO> getPendingRequests(UUID barbershopId, UUID requesterId) {
        Barbershop barbershop = barbershopRepository.findById(barbershopId)
                .orElseThrow(() -> new NotFoundException("Barbearia", barbershopId));

        validateOwnership(barbershop, requesterId);

        List<BarbershopJoinRequest> requests = joinRequestRepository
                .findByBarbershopIdAndStatus(barbershopId, JoinRequestStatus.PENDING);

        return joinRequestMapper.toDTOList(requests);
    }

    @Override
    public void approveRequest(Long requestId, UUID requesterId) {
        BarbershopJoinRequest request = joinRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Solicitação", requestId));

        validateOwnership(request.getBarbershop(), requesterId);

        request.setStatus(JoinRequestStatus.APPROVED);
        joinRequestRepository.save(request);

        // Note: The user-service will be notified via messaging or Feign client
        // to update the barber's barbershopId
    }

    @Override
    public void rejectRequest(Long requestId, UUID requesterId) {
        BarbershopJoinRequest request = joinRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Solicitação", requestId));

        validateOwnership(request.getBarbershop(), requesterId);

        request.setStatus(JoinRequestStatus.REJECTED);
        joinRequestRepository.save(request);
    }

    @Override
    public void removeBarberFromShop(UUID barberId, UUID requesterId) {
        // This would need to communicate with user-service to remove the barber
        // For now, we just update the join request status
        List<BarbershopJoinRequest> requests = joinRequestRepository.findByBarberId(barberId);
        
        for (BarbershopJoinRequest request : requests) {
            if (request.getStatus() == JoinRequestStatus.APPROVED) {
                validateOwnership(request.getBarbershop(), requesterId);
                request.setStatus(JoinRequestStatus.REJECTED);
                joinRequestRepository.save(request);
            }
        }
    }

    private void validateOwnership(Barbershop barbershop, UUID requesterId) {
        if (!barbershop.getOwnerId().equals(requesterId)) {
            throw new ForbiddenException("Você não tem permissão para gerenciar esta barbearia");
        }
    }
}
