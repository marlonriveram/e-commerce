package com.example.e_commerce.notification.infrastructure;

import com.example.e_commerce.notification.domain.NotificationService;
import com.example.e_commerce.notification.domain.exception.EmailNotificationException;
import com.example.e_commerce.shared.event.ClaimStatusChangedEvent;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Implementación real de NotificationService (US-02).
 *
 * Envía un correo electrónico al cliente cuyo claim cambió de estado.
 * Usa JavaMailSender con SMTP (Mailtrap en desarrollo).
 *
 * La interfaz NotificationService es la misma que usaba LogNotificationService,
 * por lo que ClaimStatusChangedConsumer NO cambia — solo se reemplaza la impl.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotificationService implements NotificationService {

    private final JavaMailSender mailSender; // abstraccion de spring para trabajar con correo electronico

    @Override
    public void sendClaimStatusNotification(String recipientEmail, ClaimStatusChangedEvent event) {
        try {
            MimeMessage message = mailSender.createMimeMessage(); // Permite crear el mensaje del correo

            // Facilita la construcion del email
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");


            helper.setTo(recipientEmail);
            helper.setSubject("Reclamo #" + event.getClaimId() + " - Estado actualizado");

                            // Permite generar html al correo y indica que true que contiene html
            helper.setText(buildHtmlBody(event), true);

            mailSender.send(message); // Envia el correo

            log.info("Email enviado a {} | Claim {} cambió de {} a {}",
                    recipientEmail,
                    event.getClaimId(),
                    event.getPreviousStatus(),
                    event.getNewStatus());

        } catch (MessagingException e) {
            log.error("Error al enviar email a {}: {}", recipientEmail, e.getMessage(), e);
            throw new EmailNotificationException(recipientEmail, e);
        }
    }

    private String buildHtmlBody(ClaimStatusChangedEvent event) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <h2 style="color: #2c3e50;">Notificación de Reclamo</h2>
                    <p>Hemos detectado un cambio en el estado de tu reclamo.</p>
                    <table style="border-collapse: collapse; width: 100%%; max-width: 500px;">
                        <tr>
                            <td style="padding: 8px; border: 1px solid #ddd; font-weight: bold;">Reclamo</td>
                            <td style="padding: 8px; border: 1px solid #ddd;">#%d</td>
                        </tr>
                        <tr>
                            <td style="padding: 8px; border: 1px solid #ddd; font-weight: bold;">Estado anterior</td>
                            <td style="padding: 8px; border: 1px solid #ddd;">%s</td>
                        </tr>
                        <tr>
                            <td style="padding: 8px; border: 1px solid #ddd; font-weight: bold;">Nuevo estado</td>
                            <td style="padding: 8px; border: 1px solid #ddd;">%s</td>
                        </tr>
                        <tr>
                            <td style="padding: 8px; border: 1px solid #ddd; font-weight: bold;">Fecha</td>
                            <td style="padding: 8px; border: 1px solid #ddd;">%s</td>
                        </tr>
                    </table>
                    <p style="margin-top: 20px; color: #7f8c8d; font-size: 12px;">
                        Este es un correo automático generado por el sistema de reclamos.
                    </p>
                </body>
                </html>
                """.formatted(
                event.getClaimId(),
                event.getPreviousStatus(),
                event.getNewStatus(),
                event.getTimestamp()
        );
    }
}
