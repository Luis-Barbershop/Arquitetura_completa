package ifsp.edu.projeto.cortaai.userservice.service.impl;

import ifsp.edu.projeto.cortaai.userservice.dto.AssignActivitiesDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.BarberDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.UploadResultDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.UpdateBarberDTO;
import ifsp.edu.projeto.cortaai.userservice.mapper.BarberMapper;
import ifsp.edu.projeto.cortaai.userservice.model.Barber;
import ifsp.edu.projeto.cortaai.userservice.repository.BarberRepository;
import ifsp.edu.projeto.cortaai.userservice.service.BarberService;
import ifsp.edu.projeto.cortaai.userservice.service.FirebaseAuthService;
import ifsp.edu.projeto.cortaai.userservice.service.storage.StorageService;
import ifsp.edu.projeto.cortaai.userservice.exception.ExternalServiceUnavailableException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@lombok.extern.slf4j.Slf4j
public class BarberServiceImpl implements BarberService {
    

    private final BarberRepository barberRepository;
    private final BarberMapper barberMapper;
    private final FirebaseAuthService firebaseAuthService;
    private final StorageService storageService;

    @Override
    public BarberDTO update(UUID id, UpdateBarberDTO dto) {
        Barber barber = barberRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Barbeiro não encontrado."));

        if (dto.getName() != null)  barber.setName(dto.getName());
        if (dto.getTell() != null)  barber.setTell(dto.getTell());
        if (dto.getEmail() != null) barber.setEmail(dto.getEmail());
        if (dto.getBirthDate() != null) barber.setBirthDate(dto.getBirthDate());

        // Horários de expediente — editáveis por qualquer barbeiro (owner, colaborador ou sem barbearia)
        if (dto.getWorkStartTime() != null) {
            barber.setWorkStartTime(dto.getWorkStartTime());
        }
        if (dto.getWorkEndTime() != null) {
            barber.setWorkEndTime(dto.getWorkEndTime());
        }

        // actAsBarber só é relevante para owners; barbeiros comuns sempre atuam como barbeiro
        if (dto.getActAsBarber() != null) {
            barber.setActAsBarber(barber.isOwner() ? dto.getActAsBarber() : true);
        }

        // 1. Salva no banco e guarda a referência
        Barber savedBarber = barberRepository.save(barber);

        // 2. Atualiza as claims do Firebase — só se o barbeiro tiver UID Firebase
        if (savedBarber.getFirebaseUid() != null) {
            firebaseAuthService.setCustomUserClaims(savedBarber.getFirebaseUid(), "BARBER", false);
        }

        // 3. Retorna o DTO
        return barberMapper.toDTO(savedBarber);
    }

    @Override
    public BarberDTO findById(UUID id) {
        Barber barber = barberRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Barbeiro não encontrado."));
        return barberMapper.toDTO(barber);
    }

    @Override
    public BarberDTO get(UUID id) {
        return findById(id);
    }

    @Override
    public List<BarberDTO> findAll() {
        return barberRepository.findAll().stream()
                .map(barberMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<BarberDTO> findByBarbershopId(UUID barbershopId) {
        List<Barber> result = barberRepository.findActiveByBarbershopId(barbershopId);
        log.info("event=find-active-barbers barbershopId={} found={}", barbershopId, result.size());
        result.forEach(b -> log.info("  barber id={} name={} isOwner={} actAsBarber={} barbershopId={}",
                b.getId(), b.getName(), b.isOwner(), b.isActAsBarber(), b.getBarbershopId()));
        return result.stream()
                .map(barberMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public boolean emailExists(String email) {
        return barberRepository.existsByEmailIgnoreCase(email);
    }

    @Override
    public boolean documentCPFExists(String documentCPF) {
        return barberRepository.existsByDocumentCPFIgnoreCase(documentCPF);
    }

    @Override
    public boolean tellExists(String tell) {
        return barberRepository.existsByTellIgnoreCase(tell);
    }

    @Override
    public String updateProfilePhotoByFirebaseUid(String firebaseUid, MultipartFile file) {
        Barber barber = barberRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new EntityNotFoundException("Barbeiro não encontrado."));

        try {
            String oldPublicId = barber.getImageUrlPublicId();
            if (oldPublicId != null) {
                storageService.deleteFile(oldPublicId);
            }
            UploadResultDTO uploadResult = storageService.uploadFile(file, "barber-profiles");
            barber.setImageUrl(uploadResult.getSecureUrl());
            barber.setImageUrlPublicId(uploadResult.getPublicId());
            barberRepository.save(barber);
            return uploadResult.getSecureUrl();
        } catch (IOException e) {
            throw new ExternalServiceUnavailableException("Falha ao fazer upload da foto: " + e.getMessage());
        }
    }

    // ========== HABILIDADES ==========

    @Override
    public Set<UUID> getAssignedActivityIds(String firebaseUid) {
        Barber barber = barberRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new EntityNotFoundException("Barbeiro não encontrado."));
        return barber.getAssignedActivityIds();
    }

    @Override
    public Set<UUID> getAssignedActivityIdsById(UUID barberId) {
        Barber barber = barberRepository.findById(barberId)
                .orElseThrow(() -> new EntityNotFoundException("Barbeiro não encontrado."));
        return barber.getAssignedActivityIds();
    }

    @Override
    public Set<UUID> assignActivities(String firebaseUid, AssignActivitiesDTO dto) {
        Barber barber = barberRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new EntityNotFoundException("Barbeiro não encontrado."));
        // Substitui completamente a seleção anterior
        barber.getAssignedActivityIds().clear();
        if (dto.activityIds() != null) {
            barber.getAssignedActivityIds().addAll(dto.activityIds());
        }
        barberRepository.save(barber);
        return barber.getAssignedActivityIds();
    }
}
