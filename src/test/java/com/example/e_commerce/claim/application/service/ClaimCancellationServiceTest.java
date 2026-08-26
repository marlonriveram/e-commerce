package com.example.e_commerce.claim.application.service;

import com.example.e_commerce.claim.domain.enums.EnumStatus;
import com.example.e_commerce.claim.domain.exception.ClaimNotFoundException;
import com.example.e_commerce.claim.domain.exception.InvalidCancellationException;
import com.example.e_commerce.claim.domain.exception.NotClaimOwnerException;
import com.example.e_commerce.claim.domain.model.Claim;
import com.example.e_commerce.claim.domain.repository.ClaimRepository;
import com.example.e_commerce.shared.event.ClaimStatusChangedEvent;
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
class ClaimCancellationServiceTest {

    @Mock
    private ClaimRepository claimRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ClaimCancellationService service;

    private Claim pendingClaim(Long id, Long userId) {
        return Claim.builder()
                .id(id)
                .orderId(100L)
                .description("Desc")
                .status(EnumStatus.PENDING)
                .userId(userId)
                .build();
    }

    @Test
    void shouldCancelClaim_WhenValidRequest() {
        // Given
        Claim claim = pendingClaim(1L, 10L);
        User user = User.builder().id(10L).build();
        when(claimRepository.findById(1L)).thenReturn(Optional.of(claim));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));

        // When
        Claim result = service.cancelClaim(1L, 10L);

        // Then
        assertEquals(EnumStatus.CANCELLED, result.getStatus());
        verify(claimRepository).save(any(Claim.class));
        // prepara una caja vacia para caturar un argumento con captor.caputre()
        ArgumentCaptor<ClaimStatusChangedEvent> captor = ArgumentCaptor.forClass(ClaimStatusChangedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        ClaimStatusChangedEvent event = captor.getValue();
        assertEquals(1L, event.getClaimId());
        assertEquals(EnumStatus.PENDING, event.getPreviousStatus());
        assertEquals(EnumStatus.CANCELLED, event.getNewStatus());
    }

    @Test
    void shouldThrow_WhenClaimNotFound() {
        // Given
        when(claimRepository.findById(99L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ClaimNotFoundException.class, () -> service.cancelClaim(99L, 10L));
        verify(claimRepository, never()).save(any());
    }

    @Test
    void shouldThrow_WhenClaimNotPending() {
        // Given
        Claim claim = Claim.builder()
                .id(1L).orderId(100L).description("Desc")
                .status(EnumStatus.IN_REVIEW).userId(10L).build();
        User user = User.builder().id(10L).build();
        when(claimRepository.findById(1L)).thenReturn(Optional.of(claim));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));

        // When & Then
        assertThrows(InvalidCancellationException.class, () -> service.cancelClaim(1L, 10L));
        verify(claimRepository, never()).save(any());
    }

    @Test
    void shouldThrow_WhenUserNotOwner() {
        // Given
        Claim claim = pendingClaim(1L, 10L);
        User user = User.builder().id(20L).build();
        when(claimRepository.findById(1L)).thenReturn(Optional.of(claim));
        when(userRepository.findById(20L)).thenReturn(Optional.of(user));

        // When & Then
        assertThrows(NotClaimOwnerException.class, () -> service.cancelClaim(1L, 20L));
        verify(claimRepository, never()).save(any());
    }
}
