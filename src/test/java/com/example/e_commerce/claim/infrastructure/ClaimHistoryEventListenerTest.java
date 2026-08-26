package com.example.e_commerce.claim.infrastructure;

import com.example.e_commerce.claim.domain.model.ClaimHistory;
import com.example.e_commerce.claim.domain.repository.ClaimHistoryRepository;
import com.example.e_commerce.claim.domain.enums.EnumStatus;
import com.example.e_commerce.shared.event.ClaimStatusChangedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClaimHistoryEventListenerTest {

    @Mock
    private ClaimHistoryRepository claimHistoryRepository;

    @InjectMocks
    private ClaimHistoryEventListener listener;

    @Test
    void shouldSaveHistory_WhenStatusChanged() {
        // Given
        ClaimStatusChangedEvent event = new ClaimStatusChangedEvent(
                5L, 10L, 3L,
                EnumStatus.PENDING, EnumStatus.IN_REVIEW,
                LocalDateTime.of(2026, 8, 3, 13, 15)
        );

        // When
        listener.onClaimStatusChanged(event);

        // Then
        ArgumentCaptor<ClaimHistory> captor = ArgumentCaptor.forClass(ClaimHistory.class);
        verify(claimHistoryRepository).save(captor.capture());

        ClaimHistory saved = captor.getValue();
        assertEquals(5L, saved.getClaimId());
        assertEquals(EnumStatus.PENDING, saved.getPreviousStatus());
        assertEquals(EnumStatus.IN_REVIEW, saved.getNewStatus());
        assertEquals(3L, saved.getChangedByUser());
    }
}
