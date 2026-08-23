package com.example.e_commerce.claim.application.mapper;

import com.example.e_commerce.claim.application.dto.response.ClaimHistoryResponse;
import com.example.e_commerce.claim.domain.enums.EnumStatus;
import com.example.e_commerce.claim.domain.model.ClaimHistory;
import com.example.e_commerce.claim.infrastructure.entity.ClaimEntity;
import com.example.e_commerce.claim.infrastructure.entity.ClaimHistoryEntity;
import com.example.e_commerce.user.infrastructure.entity.UserEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ClaimHistoryMapperTest {

    @Test
    void shouldMapToEntity_WhenValidDomain() {
        // Given
        ClaimHistory domain = ClaimHistory.builder()
                .id(1L).claimId(10L).previousStatus(EnumStatus.PENDING)
                .newStatus(EnumStatus.IN_REVIEW).changedByUser(5L)
                .changedAt(LocalDateTime.now()).build();

        // When
        ClaimHistoryEntity entity = ClaimHistoryMapper.toEntity(domain);

        // Then
        assertEquals(1L, entity.getId());
        assertEquals(10L, entity.getClaim().getId());
        assertEquals(EnumStatus.PENDING, entity.getPreviousStatus());
        assertEquals(EnumStatus.IN_REVIEW, entity.getNewStatus());
        assertEquals(5L, entity.getChangedByUser().getId());
    }

    @Test
    void shouldReturnNull_WhenToEntityWithNull() {
        assertNull(ClaimHistoryMapper.toEntity(null));
    }

    @Test
    void shouldMapToDomain_WhenValidEntity() {
        // Given
        ClaimHistoryEntity entity = ClaimHistoryEntity.builder()
                .id(2L)
                .claim(ClaimEntity.builder().id(20L).build())
                .previousStatus(EnumStatus.IN_REVIEW)
                .newStatus(EnumStatus.APPROVED)
                .changedByUser(UserEntity.builder().id(7L).build())
                .changedAt(LocalDateTime.now()).build();

        // When
        ClaimHistory domain = ClaimHistoryMapper.toDomain(entity);

        // Then
        assertEquals(2L, domain.getId());
        assertEquals(20L, domain.getClaimId());
        assertEquals(EnumStatus.IN_REVIEW, domain.getPreviousStatus());
        assertEquals(EnumStatus.APPROVED, domain.getNewStatus());
        assertEquals(7L, domain.getChangedByUser());
    }

    @Test
    void shouldReturnNull_WhenToDomainWithNull() {
        assertNull(ClaimHistoryMapper.toDomain(null));
    }

    @Test
    void shouldMapToResponse_WhenValidDomain() {
        // Given
        ClaimHistory domain = ClaimHistory.builder()
                .id(3L).claimId(30L).previousStatus(EnumStatus.APPROVED)
                .newStatus(EnumStatus.REFUNDED).changedByUser(9L)
                .changedAt(LocalDateTime.now()).build();

        // When
        ClaimHistoryResponse response = ClaimHistoryMapper.toResponse(domain);

        // Then
        assertEquals(3L, response.getId());
        assertEquals(30L, response.getClaimId());
        assertEquals(EnumStatus.APPROVED, response.getPreviousStatus());
        assertEquals(EnumStatus.REFUNDED, response.getNewStatus());
        assertEquals(9L, response.getChangedByUser());
    }

    @Test
    void shouldReturnNull_WhenToResponseWithNull() {
        assertNull(ClaimHistoryMapper.toResponse(null));
    }

    @Test
    void shouldExtractNestedIds_WhenMappingEntityToDomain() {
        // Given
        ClaimHistoryEntity entity = ClaimHistoryEntity.builder()
                .id(1L)
                .claim(ClaimEntity.builder().id(42L).build())
                .changedByUser(UserEntity.builder().id(99L).build())
                .newStatus(EnumStatus.IN_REVIEW).build();

        // When
        ClaimHistory domain = ClaimHistoryMapper.toDomain(entity);

        // Then
        assertEquals(42L, domain.getClaimId());
        assertEquals(99L, domain.getChangedByUser());
    }
}
