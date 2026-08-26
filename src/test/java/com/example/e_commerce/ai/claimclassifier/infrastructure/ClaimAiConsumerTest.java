package com.example.e_commerce.ai.claimclassifier.infrastructure;

import com.example.e_commerce.ai.claimclassifier.application.service.ClaimCategorizationService;
import com.example.e_commerce.shared.event.ClaimCreatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClaimAiConsumerTest {

    @Mock
    private ClaimCategorizationService claimCategorizationService;

    @InjectMocks
    private ClaimAiConsumer consumer;

    @Test
    void shouldDelegateToService_WhenEventReceived() {
        // Given
        ClaimCreatedEvent event = new ClaimCreatedEvent(
                1L, 10L, 200L, "Item arrived damaged",
                LocalDateTime.of(2026, 8, 3, 13, 15)
        );

        // When
        consumer.onClaimCreated(event);

        // Then
        verify(claimCategorizationService).categorize(1L, "Item arrived damaged");
    }
}
