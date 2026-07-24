package com.example.e_commerce.claim.application.service;

import com.example.e_commerce.claim.domain.enums.EnumStatus;
import com.example.e_commerce.claim.domain.model.Claim;
import com.example.e_commerce.claim.domain.repository.ClaimRepository;
import com.example.e_commerce.user.domain.exception.UserNotFoundException;
import com.example.e_commerce.user.domain.model.User;
import com.example.e_commerce.user.domain.repository.UserRepository;
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
class GetCustomerClaimsServiceTest {

    @Mock
    private ClaimRepository claimRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GetCustomerClaimsService service;

    @Test
    void shouldReturnClaims_WhenUserExists() {
        // Given
        User user = User.builder().id(1L).build();
        List<Claim> claims = List.of(
                Claim.builder().id(1L).userId(1L).status(EnumStatus.PENDING).build(),
                Claim.builder().id(2L).userId(1L).status(EnumStatus.IN_REVIEW).build()
        );
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(claimRepository.findByUserId(1L)).thenReturn(claims);

        // When
        List<Claim> result = service.findAllCustomersClaims(1L);

        // Then
        assertEquals(2, result.size());
        verify(claimRepository).findByUserId(1L);
    }

    @Test
    void shouldReturnEmptyList_WhenUserHasNoClaims() {
        // Given
        User user = User.builder().id(1L).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(claimRepository.findByUserId(1L)).thenReturn(List.of());

        // When
        List<Claim> result = service.findAllCustomersClaims(1L);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldThrow_WhenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> service.findAllCustomersClaims(99L));
    }
}
