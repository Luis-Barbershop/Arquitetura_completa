package ifsp.edu.projeto.cortaai.listener;

import java.util.UUID;

import ifsp.edu.projeto.cortaai.model.Barber;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import org.springframework.stereotype.Component;


@Component
public class BarberListener extends AbstractMongoEventListener<Barber> {

    @Override
    public void onBeforeConvert(final BeforeConvertEvent<Barber> event) {
        if (event.getSource().getId() == null) {
            event.getSource().setId(UUID.randomUUID());
        }
    }

}
