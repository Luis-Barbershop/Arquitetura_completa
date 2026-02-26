package ifsp.edu.projeto.cortaai.barbershop.service.impl;

import ifsp.edu.projeto.cortaai.barbershop.dto.ActivityDTO;
import ifsp.edu.projeto.cortaai.barbershop.dto.CreateActivityDTO;
import ifsp.edu.projeto.cortaai.barbershop.dto.UpdateActivityDTO;
import ifsp.edu.projeto.cortaai.barbershop.dto.UploadResultDTO;
import ifsp.edu.projeto.cortaai.barbershop.exception.ForbiddenException;
import ifsp.edu.projeto.cortaai.barbershop.exception.NotFoundException;
import ifsp.edu.projeto.cortaai.barbershop.mapper.ActivityMapper;
import ifsp.edu.projeto.cortaai.barbershop.model.Activity;
import ifsp.edu.projeto.cortaai.barbershop.model.Barbershop;
import ifsp.edu.projeto.cortaai.barbershop.repository.ActivityRepository;
import ifsp.edu.projeto.cortaai.barbershop.repository.BarbershopRepository;
import ifsp.edu.projeto.cortaai.barbershop.service.ActivityService;
import ifsp.edu.projeto.cortaai.barbershop.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ActivityServiceImpl implements ActivityService {

    private final ActivityRepository activityRepository;
    private final BarbershopRepository barbershopRepository;
    private final ActivityMapper activityMapper;
    private final StorageService storageService;

    @Override
    @Transactional(readOnly = true)
    public List<ActivityDTO> findByBarbershopId(UUID barbershopId) {
        return activityMapper.toDTOList(activityRepository.findByBarbershopId(barbershopId));
    }

    @Override
    @Transactional(readOnly = true)
    public ActivityDTO findById(UUID id) {
        return activityRepository.findById(id)
                .map(activityMapper::toDTO)
                .orElseThrow(() -> new NotFoundException("Serviço", id));
    }

    @Override
    public ActivityDTO create(UUID barbershopId, CreateActivityDTO dto, UUID requesterId) {
        Barbershop barbershop = barbershopRepository.findById(barbershopId)
                .orElseThrow(() -> new NotFoundException("Barbearia", barbershopId));

        validateOwnership(barbershop, requesterId);

        Activity activity = activityMapper.toEntity(dto);
        activity.setBarbershop(barbershop);

        return activityMapper.toDTO(activityRepository.save(activity));
    }

    @Override
    public ActivityDTO update(UUID activityId, UpdateActivityDTO dto, UUID requesterId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new NotFoundException("Serviço", activityId));

        validateOwnership(activity.getBarbershop(), requesterId);

        activityMapper.updateEntityFromDTO(activity, dto);
        return activityMapper.toDTO(activityRepository.save(activity));
    }

    @Override
    public void delete(UUID activityId, UUID requesterId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new NotFoundException("Serviço", activityId));

        validateOwnership(activity.getBarbershop(), requesterId);

        if (activity.getImageUrlPublicId() != null) {
            storageService.delete(activity.getImageUrlPublicId());
        }

        activityRepository.delete(activity);
    }

    @Override
    public String updatePhoto(UUID activityId, MultipartFile file, UUID requesterId) throws IOException {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new NotFoundException("Serviço", activityId));

        validateOwnership(activity.getBarbershop(), requesterId);

        // Delete old image if exists
        if (activity.getImageUrlPublicId() != null) {
            storageService.delete(activity.getImageUrlPublicId());
        }

        UploadResultDTO uploadResult = storageService.upload(file, "activities");
        activity.setImageUrl(uploadResult.getUrl());
        activity.setImageUrlPublicId(uploadResult.getPublicId());
        activityRepository.save(activity);

        return uploadResult.getUrl();
    }

    private void validateOwnership(Barbershop barbershop, UUID requesterId) {
        if (!barbershop.getOwnerId().equals(requesterId)) {
            throw new ForbiddenException("Você não tem permissão para modificar este serviço");
        }
    }
}
