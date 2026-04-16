package ifsp.edu.projeto.cortaai.notificationservice.model;

/**
 * Tipos de notificação do sistema.
 */
public enum NotificationType {
    APPOINTMENT_CREATED,
    APPOINTMENT_CANCELLED,
    APPOINTMENT_CONCLUDED,
    PAYMENT_APPROVED,
    JOIN_REQUEST_RECEIVED,  // barbeiro solicitou entrada na barbearia do dono
    INVITE_RECEIVED         // owner convidou o barbeiro para a barbearia
}
