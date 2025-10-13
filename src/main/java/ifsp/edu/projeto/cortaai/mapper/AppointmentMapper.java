package ifsp.edu.projeto.cortaai.mapper;

import ifsp.edu.projeto.cortaai.dto.AppointmentsDTO;
import ifsp.edu.projeto.cortaai.model.Appointments;
import ifsp.edu.projeto.cortaai.model.Barber;
import ifsp.edu.projeto.cortaai.model.Customer;
import org.springframework.stereotype.Component;

@Component
public class AppointmentMapper {

    public AppointmentsDTO toDTO(Appointments appointments) {
        if (appointments == null) {
            return null;
        }
        AppointmentsDTO appointmentsDTO = new AppointmentsDTO();
        appointmentsDTO.setId(appointments.getId());
        appointmentsDTO.setDatetime(appointments.getDatetime());
        appointmentsDTO.setBarber(appointments.getBarber() == null ? null : appointments.getBarber().getId());
        appointmentsDTO.setCustomer(appointments.getCustomer() == null ? null : appointments.getCustomer().getId());
        return appointmentsDTO;
    }

    public Appointments toEntity(AppointmentsDTO appointmentsDTO, Barber barber, Customer customer) {
        if (appointmentsDTO == null) {
            return null;
        }
        Appointments appointments = new Appointments();
        appointments.setDatetime(appointmentsDTO.getDatetime());
        appointments.setBarber(barber);
        appointments.setCustomer(customer);
        return appointments;
    }
}