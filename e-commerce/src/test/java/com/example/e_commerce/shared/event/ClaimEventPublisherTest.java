package com.example.e_commerce.shared.event;

import com.example.e_commerce.claim.domain.enums.EnumStatus;
import com.example.e_commerce.shared.config.RabbitMQConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;

import static org.mockito.Mockito.verify;


@ExtendWith(MockitoExtension.class)
class ClaimEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private ClaimEventPublisher publisher;

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
    void shouldSendEventWithAllDataIntact_WhenStatusChanges() {
        // Given
        ClaimStatusChangedEvent event = sampleEvent();

        // When
        publisher.onClaimStatusChanged(event);

        // Then
        // Mockito compara el objeto recibido con el enviado vía equals():
        // mismo objeto = misma referencia → datos intactos garantizados.
        verify(rabbitTemplate).convertAndSend(
                RabbitMQConfig.CLAIM_EXCHANGE,
                RabbitMQConfig.CLAIM_STATUS_ROUTING_KEY,
                event
        );
    }
}
