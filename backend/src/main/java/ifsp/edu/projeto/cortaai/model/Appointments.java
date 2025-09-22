package ifsp.edu.projeto.cortaai.model;

import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;


@Document("appointmentses")
@Getter
@Setter
public class Appointments {

    @Id
    private Long id;

    private OffsetDateTime datetime;

    @DocumentReference(lazy = true)
    private Barber barber;

    @DocumentReference(lazy = true)
    private Customer customer;

    @CreatedDate
    private OffsetDateTime dateCreated;

    @LastModifiedDate
    private OffsetDateTime lastUpdated;

    @Version
    private Integer version;

}
