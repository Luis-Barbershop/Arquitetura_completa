package ifsp.edu.projeto.cortaai.listener;

import ifsp.edu.projeto.cortaai.events.BeforeDeleteBarber;
import ifsp.edu.projeto.cortaai.events.BeforeDeleteCustomer;
import ifsp.edu.projeto.cortaai.exception.ReferenceException;
import ifsp.edu.projeto.cortaai.model.Appointments;
import ifsp.edu.projeto.cortaai.repository.AppointmentsRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component // Anotação para que o Spring gerencie esta classe e detecte os listeners
public class EntityDeletionListener {

    private final AppointmentsRepository appointmentsRepository;

    public EntityDeletionListener(final AppointmentsRepository appointmentsRepository) {
        this.appointmentsRepository = appointmentsRepository;
    }

    @EventListener(BeforeDeleteBarber.class)
    public void on(final BeforeDeleteBarber event) {
        final Appointments barberAppointments = appointmentsRepository.findFirstByBarberId(event.getId());
        if (barberAppointments != null) {
            final ReferenceException referencedException = new ReferenceException();
            referencedException.setKey("barber.appointments.barber.referenced");
            referencedException.addParam(barberAppointments.getId());
            throw referencedException;
        }
    }

    @EventListener(BeforeDeleteCustomer.class)
    public void on(final BeforeDeleteCustomer event) {
        final Appointments customerAppointments = appointmentsRepository.findFirstByCustomerId(event.getId());
        if (customerAppointments != null) {
            final ReferenceException referencedException = new ReferenceException();
            referencedException.setKey("customer.appointments.customer.referenced");
            referencedException.addParam(customerAppointments.getId());
            throw referencedException;
        }
    }
}