package ifsp.edu.projeto.cortaai.notificationservice.feign;

import java.util.UUID;

/**
 * DTO mínimo para resolver o UUID do banco pelo Firebase UID.
 */
public class UserInfoDTO {
    private UUID id;
    private String firebaseUid;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getFirebaseUid() { return firebaseUid; }
    public void setFirebaseUid(String firebaseUid) { this.firebaseUid = firebaseUid; }
}
