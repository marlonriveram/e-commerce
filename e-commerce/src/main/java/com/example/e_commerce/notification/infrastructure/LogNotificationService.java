package com.example.e_commerce.notification.infrastructure;

import com.example.e_commerce.notification.domain.NotificationService;
import com.example.e_commerce.shared.event.ClaimStatusChangedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Implementación MOCK de NotificationService.
 *
 * Solo registra la notificación en consola (log.info) con el correo del cliente
 * y los datos del cambio. Sirve para validar el flujo completo de US-02 sin
 * infraestructura de email real.
 *
 * MÁS ADELANTE: se reemplaza por EmailNotificationService (JavaMailSender + SMTP)
 * implementando la MISMA interfaz — el consumidor no cambia.
 */
@Slf4j
@Component
public class LogNotificationService implements NotificationService {

    @Override
    public void sendClaimStatusNotification(String recipientEmail, ClaimStatusChangedEvent event) {
        log.info("NOTIFICACIÓN (mock): Para {} | Claim {} cambió de {} a {}",
                recipientEmail,
                event.getClaimId(),
                event.getPreviousStatus(),
                event.getNewStatus()
        );
    }
}
