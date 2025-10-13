package ifsp.edu.projeto.cortaai.service.impl;

import ifsp.edu.projeto.cortaai.dto.BarberDTO;
import ifsp.edu.projeto.cortaai.dto.CustomerDTO;
import ifsp.edu.projeto.cortaai.events.BeforeDeleteBarber;
import ifsp.edu.projeto.cortaai.exception.NotFoundException;
import ifsp.edu.projeto.cortaai.mapper.BarberMapper; // Importa o novo Mapper
import ifsp.edu.projeto.cortaai.mapper.CustomerMapper;
import ifsp.edu.projeto.cortaai.model.Appointments;
import ifsp.edu.projeto.cortaai.model.Barber;
import ifsp.edu.projeto.cortaai.repository.AppointmentsRepository;
import ifsp.edu.projeto.cortaai.repository.BarberRepository;
import ifsp.edu.projeto.cortaai.service.BarberService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BarberServiceImpl implements BarberService {

    private final BarberRepository barberRepository;
    private final ApplicationEventPublisher publisher;
    private final BarberMapper barberMapper; // Injeta o Mapper
    private final AppointmentsRepository appointmentsRepository; // INJETAR O REPOSITÓRIO DE AGENDAMENTOS
    private final CustomerMapper customerMapper; // INJETAR O MAPPER DE CLIENTE


    public BarberServiceImpl(final BarberRepository barberRepository,
                             final ApplicationEventPublisher publisher,
                             final BarberMapper barberMapper,
                             final AppointmentsRepository appointmentsRepository, // Adicionar no construtor
                             final CustomerMapper customerMapper) { // Adicionar no construtor
        this.barberRepository = barberRepository;
        this.publisher = publisher;
        this.barberMapper = barberMapper;
        this.appointmentsRepository = appointmentsRepository;
        this.customerMapper = customerMapper;
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
    public UUID create(final BarberDTO barberDTO) {
        final Barber barber = barberMapper.toEntity(barberDTO);
        return barberRepository.save(barber).getId();
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

    public List<CustomerDTO> findCustomerHistory(final UUID barberId) {
        // Busca todos os agendamentos do barbeiro
        final List<Appointments> appointments = appointmentsRepository.findAllByBarberId(barberId);

        // Mapeia os agendamentos para obter os clientes,
        // usa .stream().distinct() para garantir que cada cliente apareça apenas uma vez
        // e converte para DTO
        return appointments.stream()
                .map(Appointments::getCustomer)
                .distinct()
                .map(customerMapper::toDTO)
                .collect(Collectors.toList());
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