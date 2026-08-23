package com.example.e_commerce.notification.infrastructure;

import com.example.e_commerce.claim.domain.enums.EnumStatus;
import com.example.e_commerce.shared.event.ClaimStatusChangedEvent;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Prueba unitaria de LogNotificationService (implementación mock).
 *
 * Solo verifica que el envío no lanza excepción: como la implementación
 * únicamente registra en consola, no hay más comportamiento que validar.
 */
class LogNotificationServiceTest {

    private final LogNotificationService service = new LogNotificationService();

    @Test
    void shouldNotThrow_WhenSendingNotification() {
        // Given
        ClaimStatusChangedEvent event = new ClaimStatusChangedEvent(
                5L,
                10L,
                3L,
                EnumStatus.PENDING,
                EnumStatus.IN_REVIEW,
                LocalDateTime.of(2026, 8, 3, 13, 15)
        );

        // When / Then
        assertDoesNotThrow(() -> service.sendClaimStatusNotification("ana@mail.com", event));
    }
}
