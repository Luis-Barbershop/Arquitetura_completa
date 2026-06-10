-- Sincroniza enum de notifications.type para suportar lembretes de agendamento.
-- Aplicar no notification_db.
ALTER TABLE notifications
MODIFY type ENUM(
  'APPOINTMENT_CANCELLED',
  'APPOINTMENT_CONCLUDED',
  'APPOINTMENT_CREATED',
  'APPOINTMENT_RESCHEDULED',
  'INVITE_RECEIVED',
  'JOIN_REQUEST_RECEIVED',
  'PAYMENT_APPROVED',
  'APPOINTMENT_REMINDER'
) NOT NULL;
