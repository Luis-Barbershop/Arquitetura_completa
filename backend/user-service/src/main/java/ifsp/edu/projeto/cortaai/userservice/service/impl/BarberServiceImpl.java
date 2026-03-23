package ifsp.edu.projeto.cortaai.userservice.service.impl;

import ifsp.edu.projeto.cortaai.userservice.dto.BarberDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.UpdateBarberDTO;
import ifsp.edu.projeto.cortaai.userservice.mapper.BarberMapper;
import ifsp.edu.projeto.cortaai.userservice.model.Barber;
import ifsp.edu.projeto.cortaai.userservice.repository.BarberRepository;
import ifsp.edu.projeto.cortaai.userservice.service.BarberService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BarberServiceImpl implements BarberService {

    private final BarberRepository barberRepository;
    private final BarberMapper barberMapper;

    @Override
    public BarberDTO update(UUID id, UpdateBarberDTO dto) {
        Barber barber = barberRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Barbeiro não encontrado."));

        if (dto.getName() != null)  barber.setName(dto.getName());
        if (dto.getTell() != null)  barber.setTell(dto.getTell());
        if (dto.getEmail() != null) barber.setEmail(dto.getEmail());

        return barberMapper.toDTO(barberRepository.save(barber));
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
        return barberRepository.findByBarbershopId(barbershopId).stream()
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
}
