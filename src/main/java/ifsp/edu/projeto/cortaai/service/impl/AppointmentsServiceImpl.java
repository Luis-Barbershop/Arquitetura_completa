package ifsp.edu.projeto.cortaai.service.impl;

import ifsp.edu.projeto.cortaai.dto.AppointmentsDTO;
import ifsp.edu.projeto.cortaai.exception.NotFoundException;
import ifsp.edu.projeto.cortaai.mapper.AppointmentMapper; // Importa o novo Mapper
import ifsp.edu.projeto.cortaai.model.Appointments;
import ifsp.edu.projeto.cortaai.model.Barber;
import ifsp.edu.projeto.cortaai.model.Customer;
import ifsp.edu.projeto.cortaai.repository.AppointmentsRepository;
import ifsp.edu.projeto.cortaai.repository.BarberRepository;
import ifsp.edu.projeto.cortaai.repository.CustomerRepository;
import ifsp.edu.projeto.cortaai.service.AppointmentsService;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AppointmentsServiceImpl implements AppointmentsService {

    private final AppointmentsRepository appointmentsRepository;
    private final BarberRepository barberRepository;
    private final CustomerRepository customerRepository;
    private final AppointmentMapper appointmentMapper; // Injeta o Mapper

    public AppointmentsServiceImpl(final AppointmentsRepository appointmentsRepository,
                                   final BarberRepository barberRepository,
                                   final CustomerRepository customerRepository,
                                   final AppointmentMapper appointmentMapper) { // Adiciona no construtor
        this.appointmentsRepository = appointmentsRepository;
        this.barberRepository = barberRepository;
        this.customerRepository = customerRepository;
        this.appointmentMapper = appointmentMapper;
    }

    @Override
    public List<AppointmentsDTO> findAll() {
        final List<Appointments> appointmentsList = appointmentsRepository.findAll(Sort.by("id"));
        return appointmentsList.stream()
                .map(appointmentMapper::toDTO) // Usa o mapper para converter
                .toList();
    }

    @Override
    public AppointmentsDTO get(final Long id) {
        return appointmentsRepository.findById(id)
                .map(appointmentMapper::toDTO) // Usa o mapper para converter
                .orElseThrow(NotFoundException::new);
    }

    @Override
    public Long create(final AppointmentsDTO appointmentsDTO) {
        // A lógica de buscar Barber e Customer continua aqui, pois é uma regra de negócio
        final Barber barber = barberRepository.findById(appointmentsDTO.getBarber())
                .orElseThrow(() -> new NotFoundException("barber not found"));
        final Customer customer = customerRepository.findById(appointmentsDTO.getCustomer())
                .orElseThrow(() -> new NotFoundException("customer not found"));

        final Appointments appointments = appointmentMapper.toEntity(appointmentsDTO, barber, customer);
        return appointmentsRepository.save(appointments).getId();
    }

    @Override
    public void update(final Long id, final AppointmentsDTO appointmentsDTO) {
        final Appointments appointments = appointmentsRepository.findById(id)
                .orElseThrow(NotFoundException::new);

        final Barber barber = barberRepository.findById(appointmentsDTO.getBarber())
                .orElseThrow(() -> new NotFoundException("barber not found"));
        final Customer customer = customerRepository.findById(appointmentsDTO.getCustomer())
                .orElseThrow(() -> new NotFoundException("customer not found"));

        // Atualiza a entidade existente
        appointments.setDatetime(appointmentsDTO.getDatetime());
        appointments.setBarber(barber);
        appointments.setCustomer(customer);

        appointmentsRepository.save(appointments);
    }

    @Override
    public void delete(final Long id) {
        // Validação da existência é importante antes de deletar
        if (!appointmentsRepository.existsById(id)) {
            throw new NotFoundException();
        }
        appointmentsRepository.deleteById(id);
    }

    // OS MÉTODOS DE EVENT LISTENER E MAPPER FORAM REMOVIDOS DAQUI
}