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
public class ClaimResponse {

    private Long id;
    private Long orderId;
    private String description;
    private EnumStatus status;
    private Long userId;
    private LocalDateTime createdAt;
}
