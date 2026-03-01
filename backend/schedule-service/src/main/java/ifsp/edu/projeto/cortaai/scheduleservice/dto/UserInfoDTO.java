package ifsp.edu.projeto.cortaai.scheduleservice.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
public class UserInfoDTO implements Serializable {
    private UUID id;
    private String name;
    private String email;
    private String userType;
    private String role;
    private UUID barbershopId;
    private LocalTime workStartTime;
    private LocalTime workEndTime;
    private String imageUrl;
}

