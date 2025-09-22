package ifsp.edu.projeto.cortaai.customer;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;


@Document("customers")
@Getter
@Setter
public class Customer {

    @Id
    private UUID id;

    @NotNull
    @Size(max = 70)
    private String name;

    @Indexed(unique = true)
    @NotNull
    @Size(max = 11)
    private String tell;

    @Indexed(unique = true)
    @NotNull
    @Size(max = 70)
    private String email;

    @Indexed(unique = true)
    @NotNull
    @Size(max = 11)
    private String documentCPF;

    @NotNull
    @Size(max = 70)
    private String password;

    @Indexed(unique = true)
    @Size(max = 255)
    private String previousAppointment;

    @CreatedDate
    private OffsetDateTime dateCreated;

    @LastModifiedDate
    private OffsetDateTime lastUpdated;

    @Version
    private Integer version;

}
