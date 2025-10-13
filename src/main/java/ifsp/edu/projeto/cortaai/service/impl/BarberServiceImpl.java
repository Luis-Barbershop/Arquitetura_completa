package ifsp.edu.projeto.cortaai.service.impl;

import ifsp.edu.projeto.cortaai.dto.BarberDTO;
import ifsp.edu.projeto.cortaai.dto.CreateBarberDTO;
import ifsp.edu.projeto.cortaai.dto.CustomerDTO;
import ifsp.edu.projeto.cortaai.events.BeforeDeleteBarber;
import ifsp.edu.projeto.cortaai.exception.NotFoundException;
import ifsp.edu.projeto.cortaai.mapper.BarberMapper; // Importa o novo Mapper
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
    private final BarberMapper barberMapper; // Injeta o Mapper

    public BarberServiceImpl(final BarberRepository barberRepository,
                             final ApplicationEventPublisher publisher,
                             final BarberMapper barberMapper) { // Adiciona no construtor
        this.barberRepository = barberRepository;
        this.publisher = publisher;
        this.barberMapper = barberMapper;
    }

    @Override
    public List<BarberDTO> findAll() {
        final List<Barber> barbers = barberRepository.findAll(Sort.by("id"));
        return barbers.stream()
                .map(barberMapper::toDTO) // Usa o mapper
                .toList();
    }

    @Override
    public BarberDTO get(final UUID id) {
        return barberRepository.findById(id)
                .map(barberMapper::toDTO) // Usa o mapper
                .orElseThrow(NotFoundException::new);
    }

    @Override
    public void update(final UUID id, final BarberDTO barberDTO) {
        final Barber barber = barberRepository.findById(id)
                .orElseThrow(NotFoundException::new);

        // Atualiza a entidade existente com os dados do DTO
        Barber updatedBarber = barberMapper.toEntity(barberDTO);
        updatedBarber.setId(barber.getId()); // Garante que o ID seja mantido

        barberRepository.save(updatedBarber);
    }

    @Override
    public void delete(final UUID id) {
        // Primeiro, verifica se o barbeiro existe
        final Barber barber = barberRepository.findById(id)
                .orElseThrow(NotFoundException::new);

        // Dispara o evento ANTES de deletar para que os listeners possam agir
        publisher.publishEvent(new BeforeDeleteBarber(id));

        // Se nenhum listener lançar uma exceção, a exclusão prossegue
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

    @Override
    public UUID create(CreateBarberDTO createBarberDTO) {
        return null;
    }

    @Override
    public List<CustomerDTO> findCustomerHistory(UUID id) {
        return List.of();
    }
}