package com.example.e_commerce.claim.application.service;

import com.example.e_commerce.claim.domain.enums.EnumStatus;
import com.example.e_commerce.claim.domain.exception.ClaimNotFoundException;
import com.example.e_commerce.claim.domain.model.Claim;
import com.example.e_commerce.claim.domain.model.ClaimHistory;
import com.example.e_commerce.claim.domain.repository.ClaimHistoryRepository;
import com.example.e_commerce.claim.domain.repository.ClaimRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetAuditHistoryServiceTest {

    @Mock
    private ClaimHistoryRepository claimHistoryRepository;
    @Mock
    private ClaimRepository claimRepository;

    @InjectMocks
    private GetAuditHistoryService service;

    @Test
    void shouldReturnHistory_WhenClaimExists() {
        // Given
        Claim claim = Claim.builder().id(1L).build();
        List<ClaimHistory> history = List.of(
                ClaimHistory.builder().id(1L).claimId(1L)
                        .previousStatus(EnumStatus.PENDING).newStatus(EnumStatus.IN_REVIEW).build(),
                ClaimHistory.builder().id(2L).claimId(1L)
                        .previousStatus(EnumStatus.IN_REVIEW).newStatus(EnumStatus.APPROVED).build()
        );
        when(claimRepository.findById(1L)).thenReturn(Optional.of(claim));
        when(claimHistoryRepository.findByClaimId(1L)).thenReturn(history);

        // When
        List<ClaimHistory> result = service.findHistoryByClaimId(1L);

        // Then
        assertEquals(2, result.size());
        assertEquals(EnumStatus.PENDING, result.get(0).getPreviousStatus());
        assertEquals(EnumStatus.APPROVED, result.get(1).getNewStatus());
    }

    @Test
    void shouldThrow_WhenClaimNotFound() {
        when(claimRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ClaimNotFoundException.class, () -> service.findHistoryByClaimId(99L));
    }

    @Test
    void shouldReturnEmptyList_WhenNoHistory() {
        // Given
        Claim claim = Claim.builder().id(1L).build();
        when(claimRepository.findById(1L)).thenReturn(Optional.of(claim));
        when(claimHistoryRepository.findByClaimId(1L)).thenReturn(List.of());

        // When
        List<ClaimHistory> result = service.findHistoryByClaimId(1L);

        // Then
        assertTrue(result.isEmpty());
    }
}
