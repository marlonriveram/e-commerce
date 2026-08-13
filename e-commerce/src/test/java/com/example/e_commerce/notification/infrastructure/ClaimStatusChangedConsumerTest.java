package com.example.e_commerce.notification.infrastructure;

import com.example.e_commerce.claim.domain.enums.EnumStatus;
import com.example.e_commerce.notification.domain.NotificationService;
import com.example.e_commerce.shared.event.ClaimStatusChangedEvent;
import com.example.e_commerce.user.domain.model.User;
import com.example.e_commerce.user.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de ClaimStatusChangedConsumer.
 *
 * ¿QUÉ SE PRUEBA AQUÍ?
 *   La RESOLUCIÓN de datos del consumidor: dado un evento, ¿encuentra al usuario
 *   (dueño del claim, ya traído en customerUserId) y notifica al correo correcto?
 *
 * NO se prueba aquí:
 *   - La entrega de RabbitMQ (eso lo hace el broker, no el unit test)
 *   - El envío real de la notificación (NotificationService está mockeado)
 *   - La deserialización JSON (es de RabbitMQJsonConfig)
 *
 * Son 2 escenarios:
 *   1. El usuario dueño del claim existe → se notifica al correo del cliente
 *   2. El usuario no existe → se consume sin notificar
 */
@ExtendWith(MockitoExtension.class)
class ClaimStatusChangedConsumerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ClaimStatusChangedConsumer consumer;

    private ClaimStatusChangedEvent sampleEvent() {
        return new ClaimStatusChangedEvent(
                5L,                                     // claimId
                10L,                                    // customerUserId (cliente dueño del claim)
                3L,                                     // changedByUser (agente de soporte)
                EnumStatus.PENDING,                     // previousStatus
                EnumStatus.IN_REVIEW,                   // newStatus
                LocalDateTime.of(2026, 8, 3, 13, 15)    // timestamp
        );
    }

    @Test
    void shouldSendNotificationToCustomer_WhenCustomerUserExists() {
        // Given
        ClaimStatusChangedEvent event = sampleEvent();
        User customer = User.builder().id(10L).name("Ana").email("ana@mail.com").build();
        when(userRepository.findById(10L)).thenReturn(Optional.of(customer));

        // When
        consumer.onClaimStatusChanged(event);

        // Then
        verify(notificationService).sendClaimStatusNotification("ana@mail.com", event);
    }

    @Test
    void shouldNotSendNotification_WhenCustomerUserNotFound() {
        // Given
        ClaimStatusChangedEvent event = sampleEvent();
        when(userRepository.findById(10L)).thenReturn(Optional.empty());

        // When
        consumer.onClaimStatusChanged(event);

        // Then
        verify(notificationService, never())
                .sendClaimStatusNotification(anyString(), any(ClaimStatusChangedEvent.class));
    }
}
