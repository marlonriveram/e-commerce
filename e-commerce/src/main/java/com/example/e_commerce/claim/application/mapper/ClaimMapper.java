package com.example.e_commerce.claim.application.mapper;

import com.example.e_commerce.claim.application.dto.response.ClaimResponse;
import com.example.e_commerce.claim.domain.model.Claim;
import com.example.e_commerce.claim.infrastructure.entity.ClaimEntity;
import com.example.e_commerce.user.infrastructure.entity.UserEntity;

public class ClaimMapper {

    public static ClaimEntity toEntity(Claim domain) {
        if (domain == null) return null;
        return ClaimEntity.builder()
                .id(domain.getId())
                .orderId(domain.getOrderId())
                .description(domain.getDescription())
                .status(domain.getStatus())
                .user(UserEntity.builder().id(domain.getUserId()).build())
                .createdAt(domain.getCreatedAt())
                .build();
    }

    public static Claim toDomain(ClaimEntity entity) {
        if (entity == null) return null;
        return Claim.builder()
                .id(entity.getId())
                .orderId(entity.getOrderId())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .userId(entity.getUser().getId())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public static ClaimResponse toResponse(Claim domain) {
        if (domain == null) return null;
        return ClaimResponse.builder()
                .id(domain.getId())
                .orderId(domain.getOrderId())
                .description(domain.getDescription())
                .status(domain.getStatus())
                .userId(domain.getUserId())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
