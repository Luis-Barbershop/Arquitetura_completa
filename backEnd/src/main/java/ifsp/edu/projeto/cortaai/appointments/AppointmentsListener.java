package ifsp.edu.projeto.cortaai.appointments;

import ifsp.edu.projeto.cortaai.config.PrimarySequenceService;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import org.springframework.stereotype.Component;


@Component
public class AppointmentsListener extends AbstractMongoEventListener<Appointments> {

    private final PrimarySequenceService primarySequenceService;

    public AppointmentsListener(final PrimarySequenceService primarySequenceService) {
        this.primarySequenceService = primarySequenceService;
    }

    @Override
    public void onBeforeConvert(final BeforeConvertEvent<Appointments> event) {
        if (event.getSource().getId() == null) {
            event.getSource().setId(primarySequenceService.getNextValue());
        }
    }

}
