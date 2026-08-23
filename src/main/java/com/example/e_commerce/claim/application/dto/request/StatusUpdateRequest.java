package com.example.e_commerce.claim.application.dto.request;

import com.example.e_commerce.claim.domain.enums.EnumStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatusUpdateRequest {

    @NotNull
    private EnumStatus newStatus;

    @NotNull
    private Long changedByUser;
}
