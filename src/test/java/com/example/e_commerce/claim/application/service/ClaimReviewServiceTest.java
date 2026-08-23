package com.example.e_commerce.claim.application.service;

import com.example.e_commerce.claim.domain.enums.EnumStatus;
import com.example.e_commerce.claim.domain.exception.ClaimNotFoundException;
import com.example.e_commerce.claim.domain.exception.InvalidRoleForTransitionException;
import com.example.e_commerce.claim.domain.exception.InvalidStatusTransitionException;
import com.example.e_commerce.claim.domain.model.Claim;
import com.example.e_commerce.claim.domain.model.ClaimHistory;
import com.example.e_commerce.claim.domain.repository.ClaimHistoryRepository;
import com.example.e_commerce.claim.domain.repository.ClaimRepository;
import com.example.e_commerce.shared.event.ClaimStatusChangedEvent;
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
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClaimReviewServiceTest {

    @Mock
    private ClaimRepository claimRepository;
    @Mock
    private ClaimHistoryRepository claimHistoryRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ClaimReviewService service;

    private Claim pendingClaim() {
        return Claim.builder().id(1L).status(EnumStatus.PENDING).build();
    }

    private User supportUser() {
        return User.builder().id(2L).role(EnumRole.SUPPORT).build();
    }

    @Test
    void shouldReviewClaim_WhenSupportMovesPendingToInReview() {
        // Given

        when(claimRepository.findById(1L)).thenReturn(Optional.of(pendingClaim()));
        when(userRepository.findById(2L)).thenReturn(Optional.of(supportUser()));
        when(claimRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(claimHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        Claim result = service.reviewClaim(1L, EnumStatus.IN_REVIEW, 2L);

        // Then
        assertEquals(EnumStatus.IN_REVIEW, result.getStatus());
        verify(claimRepository).save(any());
        verify(claimHistoryRepository).save(any());
        // Verifica que se publicó el evento de cambio de estado al Application Context
        verify(eventPublisher).publishEvent(any(ClaimStatusChangedEvent.class));
    }

    @Test
    void shouldSaveCorrectHistoryRecord() {
        // Given
        when(claimRepository.findById(1L)).thenReturn(Optional.of(pendingClaim()));
        when(userRepository.findById(2L)).thenReturn(Optional.of(supportUser()));
        when(claimRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        ArgumentCaptor<ClaimHistory> captor = ArgumentCaptor.forClass(ClaimHistory.class);
        when(claimHistoryRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        // When
        service.reviewClaim(1L, EnumStatus.IN_REVIEW, 2L);

        // Then
        ClaimHistory history = captor.getValue();
        assertEquals(1L, history.getClaimId());
        assertEquals(EnumStatus.PENDING, history.getPreviousStatus());
        assertEquals(EnumStatus.IN_REVIEW, history.getNewStatus());
        assertEquals(2L, history.getChangedByUser());
    }

    @Test
    void shouldThrow_WhenClaimNotFound() {
        when(claimRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ClaimNotFoundException.class, () ->
                service.reviewClaim(99L, EnumStatus.IN_REVIEW, 2L));
    }

    @Test
    void shouldThrow_WhenUserNotFound() {
        when(claimRepository.findById(1L)).thenReturn(Optional.of(pendingClaim()));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () ->
                service.reviewClaim(1L, EnumStatus.IN_REVIEW, 99L));
    }

    @Test
    void shouldThrow_WhenCustomerTriesToReview() {
        User customer = User.builder().id(3L).role(EnumRole.CUSTOMER).build();
        when(claimRepository.findById(1L)).thenReturn(Optional.of(pendingClaim()));
        when(userRepository.findById(3L)).thenReturn(Optional.of(customer));

        assertThrows(InvalidRoleForTransitionException.class, () ->
                service.reviewClaim(1L, EnumStatus.IN_REVIEW, 3L));
    }

    @Test
    void shouldThrow_WhenInvalidTransition() {
        User support = User.builder().id(2L).role(EnumRole.SUPPORT).build();
        when(claimRepository.findById(1L)).thenReturn(Optional.of(pendingClaim()));
        when(userRepository.findById(2L)).thenReturn(Optional.of(support));

        assertThrows(InvalidStatusTransitionException.class, () ->
                service.reviewClaim(1L, EnumStatus.APPROVED, 2L));
    }
}
