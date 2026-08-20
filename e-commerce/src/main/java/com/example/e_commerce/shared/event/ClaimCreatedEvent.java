package com.example.e_commerce.shared.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ClaimCreatedEvent {

    private Long claimId;

    private Long userId;

    private Long orderId;

    /** Descripción del reclamo (entrada para el clasificador IA) */
    private String description;

    private java.time.LocalDateTime timestamp;
}
