package ifsp.edu.projeto.cortaai.barbershop.service.impl;

import ifsp.edu.projeto.cortaai.barbershop.dto.*;
import ifsp.edu.projeto.cortaai.barbershop.exception.DuplicateResourceException;
import ifsp.edu.projeto.cortaai.barbershop.exception.ForbiddenException;
import ifsp.edu.projeto.cortaai.barbershop.exception.NotFoundException;
import ifsp.edu.projeto.cortaai.barbershop.mapper.BarbershopMapper;
import ifsp.edu.projeto.cortaai.barbershop.model.Barbershop;
import ifsp.edu.projeto.cortaai.barbershop.model.BarbershopHighlight;
import ifsp.edu.projeto.cortaai.barbershop.repository.BarbershopHighlightRepository;
import ifsp.edu.projeto.cortaai.barbershop.repository.BarbershopRepository;
import ifsp.edu.projeto.cortaai.barbershop.service.BarbershopService;
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
public class BarbershopServiceImpl implements BarbershopService {

    private final BarbershopRepository barbershopRepository;
    private final BarbershopHighlightRepository highlightRepository;
    private final BarbershopMapper barbershopMapper;
    private final StorageService storageService;

    @Override
    @Transactional(readOnly = true)
    public List<BarbershopDTO> findAll() {
        return barbershopMapper.toDTOList(barbershopRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public BarbershopDTO findById(UUID id) {
        return barbershopRepository.findById(id)
                .map(barbershopMapper::toDTO)
                .orElseThrow(() -> new NotFoundException("Barbearia", id));
    }

    @Override
    @Transactional(readOnly = true)
    public BarbershopDTO findByCnpj(String cnpj) {
        return barbershopRepository.findByCnpj(cnpj)
                .map(barbershopMapper::toDTO)
                .orElseThrow(() -> new NotFoundException("Barbearia não encontrada com CNPJ: " + cnpj));
    }

    @Override
    @Transactional(readOnly = true)
    public BarbershopDTO findByOwnerId(UUID ownerId) {
        return barbershopRepository.findByOwnerId(ownerId)
                .map(barbershopMapper::toDTO)
                .orElseThrow(() -> new NotFoundException("Barbearia não encontrada para o proprietário: " + ownerId));
    }

    @Override
    public BarbershopDTO create(CreateBarbershopDTO dto, UUID ownerId) throws IOException {
        return create(dto, ownerId, null);
    }

    @Override
    public BarbershopDTO create(CreateBarbershopDTO dto, UUID ownerId, MultipartFile logo) throws IOException {
        if (barbershopRepository.existsByCnpj(dto.getCnpj())) {
            throw new DuplicateResourceException("CNPJ já cadastrado: " + dto.getCnpj());
        }

        Barbershop barbershop = barbershopMapper.toEntity(dto);
        barbershop.setOwnerId(ownerId);

        if (logo != null && !logo.isEmpty()) {
            UploadResultDTO uploadResult = storageService.upload(logo, "barbershops/logos");
            barbershop.setLogoUrl(uploadResult.getUrl());
            barbershop.setLogoUrlPublicId(uploadResult.getPublicId());
        }

        return barbershopMapper.toDTO(barbershopRepository.save(barbershop));
    }

    @Override
    public BarbershopDTO update(UUID id, UpdateBarbershopDTO dto, UUID requesterId) {
        Barbershop barbershop = barbershopRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Barbearia", id));

        validateOwnership(barbershop, requesterId);

        barbershopMapper.updateEntityFromDTO(barbershop, dto);
        return barbershopMapper.toDTO(barbershopRepository.save(barbershop));
    }

    @Override
    public void delete(UUID id, UUID requesterId) {
        Barbershop barbershop = barbershopRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Barbearia", id));

        validateOwnership(barbershop, requesterId);

        // Delete images from storage
        if (barbershop.getLogoUrlPublicId() != null) {
            storageService.delete(barbershop.getLogoUrlPublicId());
        }
        if (barbershop.getBannerUrlPublicId() != null) {
            storageService.delete(barbershop.getBannerUrlPublicId());
        }
        barbershop.getHighlights().forEach(h -> {
            if (h.getImageUrlPublicId() != null) {
                storageService.delete(h.getImageUrlPublicId());
            }
        });

        barbershopRepository.delete(barbershop);
    }

    @Override
    public String updateLogo(UUID barbershopId, MultipartFile file, UUID requesterId) throws IOException {
        Barbershop barbershop = barbershopRepository.findById(barbershopId)
                .orElseThrow(() -> new NotFoundException("Barbearia", barbershopId));

        validateOwnership(barbershop, requesterId);

        // Delete old logo if exists
        if (barbershop.getLogoUrlPublicId() != null) {
            storageService.delete(barbershop.getLogoUrlPublicId());
        }

        UploadResultDTO uploadResult = storageService.upload(file, "barbershops/logos");
        barbershop.setLogoUrl(uploadResult.getUrl());
        barbershop.setLogoUrlPublicId(uploadResult.getPublicId());
        barbershopRepository.save(barbershop);

        return uploadResult.getUrl();
    }

    @Override
    public String updateBanner(UUID barbershopId, MultipartFile file, UUID requesterId) throws IOException {
        Barbershop barbershop = barbershopRepository.findById(barbershopId)
                .orElseThrow(() -> new NotFoundException("Barbearia", barbershopId));

        validateOwnership(barbershop, requesterId);

        // Delete old banner if exists
        if (barbershop.getBannerUrlPublicId() != null) {
            storageService.delete(barbershop.getBannerUrlPublicId());
        }

        UploadResultDTO uploadResult = storageService.upload(file, "barbershops/banners");
        barbershop.setBannerUrl(uploadResult.getUrl());
        barbershop.setBannerUrlPublicId(uploadResult.getPublicId());
        barbershopRepository.save(barbershop);

        return uploadResult.getUrl();
    }

    @Override
    public String addHighlight(UUID barbershopId, MultipartFile file, UUID requesterId) throws IOException {
        Barbershop barbershop = barbershopRepository.findById(barbershopId)
                .orElseThrow(() -> new NotFoundException("Barbearia", barbershopId));

        validateOwnership(barbershop, requesterId);

        UploadResultDTO uploadResult = storageService.upload(file, "barbershops/highlights");

        BarbershopHighlight highlight = new BarbershopHighlight();
        highlight.setBarbershop(barbershop);
        highlight.setImageUrl(uploadResult.getUrl());
        highlight.setImageUrlPublicId(uploadResult.getPublicId());
        highlightRepository.save(highlight);

        return uploadResult.getUrl();
    }

    @Override
    public void deleteHighlight(UUID barbershopId, UUID highlightId, UUID requesterId) {
        Barbershop barbershop = barbershopRepository.findById(barbershopId)
                .orElseThrow(() -> new NotFoundException("Barbearia", barbershopId));

        validateOwnership(barbershop, requesterId);

        BarbershopHighlight highlight = highlightRepository.findById(highlightId)
                .orElseThrow(() -> new NotFoundException("Destaque", highlightId));

        if (!highlight.getBarbershop().getId().equals(barbershopId)) {
            throw new ForbiddenException("Destaque não pertence a esta barbearia");
        }

        if (highlight.getImageUrlPublicId() != null) {
            storageService.delete(highlight.getImageUrlPublicId());
        }

        highlightRepository.delete(highlight);
    }

    private void validateOwnership(Barbershop barbershop, UUID requesterId) {
        if (!barbershop.getOwnerId().equals(requesterId)) {
            throw new ForbiddenException("Você não tem permissão para modificar esta barbearia");
        }
    }
}
