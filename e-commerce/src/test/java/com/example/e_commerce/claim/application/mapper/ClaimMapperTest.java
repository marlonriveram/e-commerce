package com.example.e_commerce.claim.application.mapper;

import com.example.e_commerce.claim.application.dto.response.ClaimResponse;
import com.example.e_commerce.claim.domain.enums.EnumStatus;
import com.example.e_commerce.claim.domain.model.Claim;
import com.example.e_commerce.claim.infrastructure.entity.ClaimEntity;
import com.example.e_commerce.user.infrastructure.entity.UserEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ClaimMapperTest {

    @Test
    void shouldMapToEntity_WhenValidDomain() {
        // Given
        Claim domain = Claim.builder()
                .id(1L).orderId(100L).description("Damaged item")
                .status(EnumStatus.PENDING).userId(5L)
                .createdAt(LocalDateTime.now()).build();

        // When
        ClaimEntity entity = ClaimMapper.toEntity(domain);

        // Then
        assertEquals(1L, entity.getId());
        assertEquals(100L, entity.getOrderId());
        assertEquals("Damaged item", entity.getDescription());
        assertEquals(EnumStatus.PENDING, entity.getStatus());
        assertEquals(5L, entity.getUser().getId());
    }

    @Test
    void shouldReturnNull_WhenToEntityWithNull() {
        assertNull(ClaimMapper.toEntity(null));
    }

    @Test
    void shouldMapToDomain_WhenValidEntity() {
        // Given
        ClaimEntity entity = ClaimEntity.builder()
                .id(2L).orderId(200L).description("Wrong color")
                .status(EnumStatus.APPROVED)
                .user(UserEntity.builder().id(3L).build())
                .createdAt(LocalDateTime.now()).build();

        // When
        Claim domain = ClaimMapper.toDomain(entity);

        // Then
        assertEquals(2L, domain.getId());
        assertEquals(200L, domain.getOrderId());
        assertEquals("Wrong color", domain.getDescription());
        assertEquals(EnumStatus.APPROVED, domain.getStatus());
        assertEquals(3L, domain.getUserId());
    }

    @Test
    void shouldReturnNull_WhenToDomainWithNull() {
        assertNull(ClaimMapper.toDomain(null));
    }

    @Test
    void shouldMapToResponse_WhenValidDomain() {
        // Given
        Claim domain = Claim.builder()
                .id(4L).orderId(400L).description("Late delivery")
                .status(EnumStatus.REJECTED).userId(7L)
                .createdAt(LocalDateTime.now()).build();

        // When
        ClaimResponse response = ClaimMapper.toResponse(domain);

        // Then
        assertEquals(4L, response.getId());
        assertEquals(400L, response.getOrderId());
        assertEquals("Late delivery", response.getDescription());
        assertEquals(EnumStatus.REJECTED, response.getStatus());
        assertEquals(7L, response.getUserId());
    }

    @Test
    void shouldReturnNull_WhenToResponseWithNull() {
        assertNull(ClaimMapper.toResponse(null));
    }

    @Test
    void shouldExtractUserId_WhenMappingEntityToDomain() {
        // Given
        ClaimEntity entity = ClaimEntity.builder()
                .id(1L).user(UserEntity.builder().id(42L).build())
                .status(EnumStatus.PENDING).build();

        // When
        Claim domain = ClaimMapper.toDomain(entity);

        // Then
        assertEquals(42L, domain.getUserId());
        assertNull(domain.getOrderId());
    }
}
