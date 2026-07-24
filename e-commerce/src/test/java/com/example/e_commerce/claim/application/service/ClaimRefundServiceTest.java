package com.example.e_commerce.claim.application.service;

import com.example.e_commerce.claim.domain.enums.EnumStatus;
import com.example.e_commerce.claim.domain.exception.ClaimNotFoundException;
import com.example.e_commerce.claim.domain.exception.InvalidRoleForTransitionException;
import com.example.e_commerce.claim.domain.exception.InvalidStatusTransitionException;
import com.example.e_commerce.claim.domain.model.Claim;
import com.example.e_commerce.claim.domain.model.ClaimHistory;
import com.example.e_commerce.claim.domain.repository.ClaimHistoryRepository;
import com.example.e_commerce.claim.domain.repository.ClaimRepository;
import com.example.e_commerce.user.domain.enums.EnumRole;
import com.example.e_commerce.user.domain.exception.UserNotFoundException;
import com.example.e_commerce.user.domain.model.User;
import com.example.e_commerce.user.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClaimRefundServiceTest {

    @Mock
    private ClaimRepository claimRepository;
    @Mock
    private ClaimHistoryRepository claimHistoryRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ClaimRefundService service;

    private Claim approvedClaim() {
        return Claim.builder().id(1L).status(EnumStatus.APPROVED).userId(10L).build();
    }

    private User financeUser() {
        return User.builder().id(5L).role(EnumRole.FINANCE).build();
    }

    @Test
    void shouldRefundClaim_WhenFinanceRefundsApprovedClaim() {
        // Given
        when(claimRepository.findById(1L)).thenReturn(Optional.of(approvedClaim()));
        when(userRepository.findById(5L)).thenReturn(Optional.of(financeUser()));


        when(claimRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(claimHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        Claim result = service.refundClaim(1L, 5L);

        // Then
        assertEquals(EnumStatus.REFUNDED, result.getStatus());
        verify(claimRepository).save(any());
        verify(claimHistoryRepository).save(any());
    }

    @Test
    void shouldSaveCorrectHistoryRecord() {
        // Given
        when(claimRepository.findById(1L)).thenReturn(Optional.of(approvedClaim()));
        when(userRepository.findById(5L)).thenReturn(Optional.of(financeUser()));
        when(claimRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ArgumentCaptor<ClaimHistory> captor = ArgumentCaptor.forClass(ClaimHistory.class);
        when(claimHistoryRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        // When
        service.refundClaim(1L, 5L);

        // Then
        ClaimHistory history = captor.getValue();
        assertEquals(1L, history.getClaimId());
        assertEquals(EnumStatus.APPROVED, history.getPreviousStatus());
        assertEquals(EnumStatus.REFUNDED, history.getNewStatus());
        assertEquals(5L, history.getChangedByUser());
    }

    @Test
    void shouldThrow_WhenClaimNotFound() {
        when(claimRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ClaimNotFoundException.class, () -> service.refundClaim(99L, 5L));
    }

    @Test
    void shouldThrow_WhenUserNotFound() {
        when(claimRepository.findById(1L)).thenReturn(Optional.of(approvedClaim()));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> service.refundClaim(1L, 99L));
    }

    @Test
    void shouldThrow_WhenSupportTriesToRefund() {
        User support = User.builder().id(2L).role(EnumRole.SUPPORT).build();
        when(claimRepository.findById(1L)).thenReturn(Optional.of(approvedClaim()));
        when(userRepository.findById(2L)).thenReturn(Optional.of(support));

        assertThrows(InvalidRoleForTransitionException.class, () -> service.refundClaim(1L, 2L));
    }

    @Test
    void shouldThrow_WhenPendingClaimRefunded() {
        Claim pending = Claim.builder().id(2L).status(EnumStatus.PENDING).userId(10L).build();
        when(claimRepository.findById(2L)).thenReturn(Optional.of(pending));
        when(userRepository.findById(5L)).thenReturn(Optional.of(financeUser()));

        assertThrows(InvalidStatusTransitionException.class, () -> service.refundClaim(2L, 5L));
    }

    @Test
    void shouldThrow_WhenInReviewClaimRefunded() {
        Claim inReview = Claim.builder().id(3L).status(EnumStatus.IN_REVIEW).userId(10L).build();
        when(claimRepository.findById(3L)).thenReturn(Optional.of(inReview));
        when(userRepository.findById(5L)).thenReturn(Optional.of(financeUser()));

        assertThrows(InvalidStatusTransitionException.class, () -> service.refundClaim(3L, 5L));
    }
}
