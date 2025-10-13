package ifsp.edu.projeto.cortaai.service.impl;

import ifsp.edu.projeto.cortaai.dto.BarberDTO;
import ifsp.edu.projeto.cortaai.dto.CreateBarberDTO; // Importe o novo DTO
import ifsp.edu.projeto.cortaai.events.BeforeDeleteBarber;
import ifsp.edu.projeto.cortaai.exception.NotFoundException;
import ifsp.edu.projeto.cortaai.mapper.BarberMapper;
import ifsp.edu.projeto.cortaai.model.Barber;
import ifsp.edu.projeto.cortaai.repository.BarberRepository;
import ifsp.edu.projeto.cortaai.service.BarberService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class BarberServiceImpl implements BarberService {

    private final BarberRepository barberRepository;
    private final ApplicationEventPublisher publisher;
    private final BarberMapper barberMapper;

    public BarberServiceImpl(final BarberRepository barberRepository,
                             final ApplicationEventPublisher publisher,
                             final BarberMapper barberMapper) {
        this.barberRepository = barberRepository;
        this.publisher = publisher;
        this.barberMapper = barberMapper;
    }

    @Override
    public List<BarberDTO> findAll() {
        final List<Barber> barbers = barberRepository.findAll(Sort.by("id"));
        return barbers.stream()
                .map(barberMapper::toDTO)
                .toList();
    }

    @Override
    public BarberDTO get(final UUID id) {
        return barberRepository.findById(id)
                .map(barberMapper::toDTO)
                .orElseThrow(NotFoundException::new);
    }

    @Override
    public UUID create(final CreateBarberDTO createBarberDTO) {
        final Barber barber = barberMapper.toEntity(createBarberDTO);
        return barberRepository.save(barber).getId();
    }

    @Override
    public void update(final UUID id, final BarberDTO barberDTO) {
        final Barber barber = barberRepository.findById(id)
                .orElseThrow(NotFoundException::new);

        Barber updatedBarber = barberMapper.toEntity(barberDTO);
        updatedBarber.setId(barber.getId());

        barberRepository.save(updatedBarber);
    }

    @Override
    public void delete(final UUID id) {
        final Barber barber = barberRepository.findById(id)
                .orElseThrow(NotFoundException::new);

        publisher.publishEvent(new BeforeDeleteBarber(id));

        barberRepository.delete(barber);
    }

    @Override
    public boolean tellExists(final String tell) {
        return barberRepository.existsByTellIgnoreCase(tell);
    }

    @Override
    public boolean emailExists(final String email) {
        return barberRepository.existsByEmailIgnoreCase(email);
    }

    @Override
    public boolean documentCPFExists(final String documentCPF) {
        return barberRepository.existsByDocumentCPFIgnoreCase(documentCPF);
    }
}