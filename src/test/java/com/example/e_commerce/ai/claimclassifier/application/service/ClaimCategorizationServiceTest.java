package com.example.e_commerce.ai.claimclassifier.application.service;

import com.example.e_commerce.ai.claimclassifier.domain.model.ClaimAiMetadata;
import com.example.e_commerce.ai.claimclassifier.domain.repository.ClaimClassifier;
import com.example.e_commerce.claim.domain.enums.EnumStatus;
import com.example.e_commerce.claim.domain.model.Claim;
import com.example.e_commerce.claim.domain.repository.ClaimRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClaimCategorizationServiceTest {

    @Mock
    private ClaimRepository claimRepository;
    @Mock
    private ClaimClassifier claimClassifier;

    @InjectMocks
    private ClaimCategorizationService service;

    @Test
    void shouldCategorizeClaim_WhenValid() {
        // Given
        Claim claim = Claim.builder()
                .id(1L).orderId(100L).description("Item arrived damaged")
                .status(EnumStatus.PENDING).userId(10L).build();
        when(claimRepository.findById(1L)).thenReturn(Optional.of(claim));

        ClaimAiMetadata metadata = new ClaimAiMetadata("SHIPPING", "HIGH", "Producto danado en envio");
        when(claimClassifier.classify("Item arrived damaged")).thenReturn(metadata);

        // When
        service.categorize(1L, "Item arrived damaged");

        // Then
        assertEquals("SHIPPING", claim.getCategory());
        assertEquals("HIGH", claim.getUrgency());
        assertEquals("Producto danado en envio", claim.getSummary());
        verify(claimRepository).save(claim);
    }

    @Test
    void shouldDoNothing_WhenClaimNotFound() {
        // Given
        when(claimRepository.findById(99L)).thenReturn(Optional.empty());

        // When
        service.categorize(99L, "Some description");

        // Then
        verify(claimClassifier, never()).classify(anyString());
        verify(claimRepository, never()).save(any());
    }

    @Test
    void shouldPropagateException_WhenClassifierFails() {
        // Given
        Claim claim = Claim.builder()
                .id(1L).orderId(100L).description("Desc")
                .status(EnumStatus.PENDING).userId(10L).build();
        when(claimRepository.findById(1L)).thenReturn(Optional.of(claim));
        when(claimClassifier.classify("Desc")).thenThrow(new RuntimeException("Groq API error"));

        // When & Then
        assertThrows(RuntimeException.class, () -> service.categorize(1L, "Desc"));
        verify(claimRepository, never()).save(any());
    }
}
