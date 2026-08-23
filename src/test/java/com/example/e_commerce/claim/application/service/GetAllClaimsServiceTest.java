package com.example.e_commerce.claim.application.service;

import com.example.e_commerce.claim.domain.enums.EnumStatus;
import com.example.e_commerce.claim.domain.exception.ClaimNotFoundException;
import com.example.e_commerce.claim.domain.model.Claim;
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
class GetAllClaimsServiceTest {

    @Mock
    private ClaimRepository claimRepository;

    @InjectMocks
    private GetAllClaimsService service;

    @Test
    void shouldReturnAllClaims_WhenStatusIsNull() {
        // Given
        List<Claim> allClaims = List.of(
                Claim.builder().id(1L).status(EnumStatus.PENDING).build(),
                Claim.builder().id(2L).status(EnumStatus.REJECTED).build()
        );
        when(claimRepository.findAll()).thenReturn(allClaims);

        // When
        List<Claim> result = service.findAll(null);

        // Then
        assertEquals(2, result.size());
        verify(claimRepository).findAll();
        verify(claimRepository, never()).findByStatus(any());
    }

    @Test
    void shouldReturnFilteredClaims_WhenStatusProvided() {
        // Given
        List<Claim> pendingClaims = List.of(
                Claim.builder().id(1L).status(EnumStatus.PENDING).build()
        );
        when(claimRepository.findByStatus(EnumStatus.PENDING)).thenReturn(pendingClaims);

        // When
        List<Claim> result = service.findAll(EnumStatus.PENDING);

        // Then
        assertEquals(1, result.size());
        verify(claimRepository).findByStatus(EnumStatus.PENDING);
        verify(claimRepository, never()).findAll();
    }

    @Test
    void shouldReturnClaim_WhenFoundById() {
        // Given
        Claim claim = Claim.builder().id(1L).status(EnumStatus.APPROVED).build();
        when(claimRepository.findById(1L)).thenReturn(Optional.of(claim));

        // When
        Claim result = service.findById(1L);

        // Then
        assertEquals(1L, result.getId());
        assertEquals(EnumStatus.APPROVED, result.getStatus());
    }

    @Test
    void shouldThrow_WhenClaimNotFoundById() {
        when(claimRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ClaimNotFoundException.class, () -> service.findById(99L));
    }

    @Test
    void shouldReturnEmptyList_WhenNoClaimsForStatus() {
        when(claimRepository.findByStatus(EnumStatus.REFUNDED)).thenReturn(List.of());

        List<Claim> result = service.findAll(EnumStatus.REFUNDED);

        assertTrue(result.isEmpty());
    }
}
