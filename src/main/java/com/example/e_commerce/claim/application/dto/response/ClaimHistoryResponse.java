package com.example.e_commerce.claim.application.dto.response;

import com.example.e_commerce.claim.domain.enums.EnumStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClaimHistoryResponse {

    private Long id;
    private Long claimId;
    private EnumStatus previousStatus;
    private EnumStatus newStatus;
    private Long changedByUser;
    private LocalDateTime changedAt;
}
