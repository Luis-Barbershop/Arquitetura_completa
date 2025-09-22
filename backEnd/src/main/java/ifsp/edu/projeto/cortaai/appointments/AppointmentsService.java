package ifsp.edu.projeto.cortaai.appointments;

import ifsp.edu.projeto.cortaai.barber.Barber;
import ifsp.edu.projeto.cortaai.barber.BarberRepository;
import ifsp.edu.projeto.cortaai.customer.Customer;
import ifsp.edu.projeto.cortaai.customer.CustomerRepository;
import ifsp.edu.projeto.cortaai.events.BeforeDeleteBarber;
import ifsp.edu.projeto.cortaai.events.BeforeDeleteCustomer;
import ifsp.edu.projeto.cortaai.util.NotFoundException;
import ifsp.edu.projeto.cortaai.util.ReferencedException;
import java.util.List;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;


@Service
public class AppointmentsService {

    private final AppointmentsRepository appointmentsRepository;
    private final BarberRepository barberRepository;
    private final CustomerRepository customerRepository;

    public AppointmentsService(final AppointmentsRepository appointmentsRepository,
            final BarberRepository barberRepository, final CustomerRepository customerRepository) {
        this.appointmentsRepository = appointmentsRepository;
        this.barberRepository = barberRepository;
        this.customerRepository = customerRepository;
    }

    public List<AppointmentsDTO> findAll() {
        final List<Appointments> appointmentses = appointmentsRepository.findAll(Sort.by("id"));
        return appointmentses.stream()
                .map(appointments -> mapToDTO(appointments, new AppointmentsDTO()))
                .toList();
    }

    public AppointmentsDTO get(final Long id) {
        return appointmentsRepository.findById(id)
                .map(appointments -> mapToDTO(appointments, new AppointmentsDTO()))
                .orElseThrow(NotFoundException::new);
    }

    public Long create(final AppointmentsDTO appointmentsDTO) {
        final Appointments appointments = new Appointments();
        mapToEntity(appointmentsDTO, appointments);
        return appointmentsRepository.save(appointments).getId();
    }

    public void update(final Long id, final AppointmentsDTO appointmentsDTO) {
        final Appointments appointments = appointmentsRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        mapToEntity(appointmentsDTO, appointments);
        appointmentsRepository.save(appointments);
    }

    public void delete(final Long id) {
        final Appointments appointments = appointmentsRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        appointmentsRepository.delete(appointments);
    }

    private AppointmentsDTO mapToDTO(final Appointments appointments,
            final AppointmentsDTO appointmentsDTO) {
        appointmentsDTO.setId(appointments.getId());
        appointmentsDTO.setDatetime(appointments.getDatetime());
        appointmentsDTO.setBarber(appointments.getBarber() == null ? null : appointments.getBarber().getId());
        appointmentsDTO.setCustomer(appointments.getCustomer() == null ? null : appointments.getCustomer().getId());
        return appointmentsDTO;
    }

    private Appointments mapToEntity(final AppointmentsDTO appointmentsDTO,
            final Appointments appointments) {
        appointments.setDatetime(appointmentsDTO.getDatetime());
        final Barber barber = appointmentsDTO.getBarber() == null ? null : barberRepository.findById(appointmentsDTO.getBarber())
                .orElseThrow(() -> new NotFoundException("barber not found"));
        appointments.setBarber(barber);
        final Customer customer = appointmentsDTO.getCustomer() == null ? null : customerRepository.findById(appointmentsDTO.getCustomer())
                .orElseThrow(() -> new NotFoundException("customer not found"));
        appointments.setCustomer(customer);
        return appointments;
    }

    @EventListener(BeforeDeleteBarber.class)
    public void on(final BeforeDeleteBarber event) {
        final ReferencedException referencedException = new ReferencedException();
        final Appointments barberAppointments = appointmentsRepository.findFirstByBarberId(event.getId());
        if (barberAppointments != null) {
            referencedException.setKey("barber.appointments.barber.referenced");
            referencedException.addParam(barberAppointments.getId());
            throw referencedException;
        }
    }

    @EventListener(BeforeDeleteCustomer.class)
    public void on(final BeforeDeleteCustomer event) {
        final ReferencedException referencedException = new ReferencedException();
        final Appointments customerAppointments = appointmentsRepository.findFirstByCustomerId(event.getId());
        if (customerAppointments != null) {
            referencedException.setKey("customer.appointments.customer.referenced");
            referencedException.addParam(customerAppointments.getId());
            throw referencedException;
        }
    }

}
