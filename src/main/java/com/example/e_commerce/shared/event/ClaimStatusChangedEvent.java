package com.example.e_commerce.shared.event;

import com.example.e_commerce.claim.domain.enums.EnumStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ClaimStatusChangedEvent {

    private Long claimId;

    private Long customerUserId;

    private Long changedByUser;

    private EnumStatus previousStatus;

    private EnumStatus newStatus;

    private java.time.LocalDateTime timestamp;
}
