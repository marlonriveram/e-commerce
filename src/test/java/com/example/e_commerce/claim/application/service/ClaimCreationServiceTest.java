package com.example.e_commerce.claim.application.service;

import com.example.e_commerce.claim.application.dto.request.ClaimRequest;
import com.example.e_commerce.claim.domain.enums.EnumStatus;
import com.example.e_commerce.claim.domain.model.Claim;
import com.example.e_commerce.claim.domain.repository.ClaimRepository;
import com.example.e_commerce.user.domain.enums.EnumRole;
import com.example.e_commerce.user.domain.exception.UserNotFoundException;
import com.example.e_commerce.user.domain.model.User;
import com.example.e_commerce.user.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClaimCreationServiceTest {

    @Mock
    private ClaimRepository claimRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ClaimCreationService service;

    @Test
    void shouldCreateClaimWithPendingStatus_WhenValidRequest() {
        // Given
        ClaimRequest request = new ClaimRequest(100L, "Item arrived damaged", 1L);

        Optional<User> user = Optional.of(
                User.builder().id(1L).role(EnumRole.CUSTOMER).build());

        Claim claimResponse= Claim.builder()
                .description("Item arrived damaged")
                .status(EnumStatus.PENDING)
                .orderId(100L)
                .createdAt(LocalDateTime.now())
                .userId(1L)
                .build();

        when(userRepository.findById(1L)).thenReturn(user);
        when(claimRepository.save(any(Claim.class))).thenReturn(claimResponse);

        // When
        Claim result = service.createClaim(request);

        // Then
        assertEquals(EnumStatus.PENDING, result.getStatus());
        assertEquals(100L, result.getOrderId());
        assertEquals("Item arrived damaged", result.getDescription());
        assertEquals(1L, result.getUserId());
        assertNotNull(result.getCreatedAt());
        verify(claimRepository).save(any(Claim.class));
    }

    @Test
    void shouldThrow_WhenUserNotFound() {
        // Given
        ClaimRequest request = new ClaimRequest(100L, "Desc", 99L);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UserNotFoundException.class, () -> service.createClaim(request));
        verify(claimRepository, never()).save(any());
    }

    @Test
    void shouldSaveClaimWithCorrectFields() {
        // Given
        ClaimRequest request = new ClaimRequest(200L, "Wrong item", 5L);
        Claim claimResponse= Claim.builder()
                .description("Wrong item")
                .status(EnumStatus.PENDING)
                .orderId(200L)
                .userId(5L)
                .build();

        Optional<User> user = Optional.of(
                User.builder().id(5L).role(EnumRole.CUSTOMER).build());

        when(userRepository.findById(5L)).thenReturn(user);


        when(claimRepository.save(any(Claim.class))).thenReturn(claimResponse);

        // When
        Claim result = service.createClaim(request);

        // Then
        assertEquals(200L, result.getOrderId());
        assertEquals("Wrong item", result.getDescription());
        assertEquals(5L, result.getUserId());
        assertEquals(EnumStatus.PENDING, result.getStatus());
    }
}
