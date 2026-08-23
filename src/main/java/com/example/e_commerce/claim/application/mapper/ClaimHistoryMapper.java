package com.example.e_commerce.claim.application.mapper;

import com.example.e_commerce.claim.application.dto.response.ClaimHistoryResponse;
import com.example.e_commerce.claim.domain.model.ClaimHistory;
import com.example.e_commerce.claim.infrastructure.entity.ClaimEntity;
import com.example.e_commerce.claim.infrastructure.entity.ClaimHistoryEntity;
import com.example.e_commerce.user.infrastructure.entity.UserEntity;

public class ClaimHistoryMapper {

    public static ClaimHistoryEntity toEntity(ClaimHistory domain) {
        if (domain == null) return null;
        return ClaimHistoryEntity.builder()
                .id(domain.getId())
                .claim(ClaimEntity.builder().id(domain.getClaimId()).build())
                .previousStatus(domain.getPreviousStatus())
                .newStatus(domain.getNewStatus())
                .changedByUser(UserEntity.builder().id(domain.getChangedByUser()).build())
                .changedAt(domain.getChangedAt())
                .build();
    }

    public static ClaimHistory toDomain(ClaimHistoryEntity entity) {
        if (entity == null) return null;
        return ClaimHistory.builder()
                .id(entity.getId())
                .claimId(entity.getClaim().getId())
                .previousStatus(entity.getPreviousStatus())
                .newStatus(entity.getNewStatus())
                .changedByUser(entity.getChangedByUser().getId())
                .changedAt(entity.getChangedAt())
                .build();
    }

    public static ClaimHistoryResponse toResponse(ClaimHistory domain) {
        if (domain == null) return null;
        return ClaimHistoryResponse.builder()
                .id(domain.getId())
                .claimId(domain.getClaimId())
                .previousStatus(domain.getPreviousStatus())
                .newStatus(domain.getNewStatus())
                .changedByUser(domain.getChangedByUser())
                .changedAt(domain.getChangedAt())
                .build();
    }
}
