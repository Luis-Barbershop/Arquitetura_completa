package ifsp.edu.projeto.cortaai.events;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;


@Getter
@AllArgsConstructor
public class BeforeDeleteCustomer {

    private UUID id;

}
