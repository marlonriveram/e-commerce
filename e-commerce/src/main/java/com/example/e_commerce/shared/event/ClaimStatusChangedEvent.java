package com.example.e_commerce.shared.event;

import com.example.e_commerce.claim.domain.enums.EnumStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ClaimStatusChangedEvent {

    /** ID de la reclamación que cambió de estado */
    private Long claimId;

    /** ID del CLIENTE dueño del claim (a quien hay que notificar) */
    private Long customerUserId;

    /** ID del usuario que realizó el cambio (agente de soporte o finanzas) */
    private Long changedByUser;

    /** Estado ANTES del cambio (ej: PENDING) */
    private EnumStatus previousStatus;

    /** Estado DESPUÉS del cambio (ej: IN_REVIEW) */
    private EnumStatus newStatus;

    /** Momento exacto en que ocurrió el cambio */
    private java.time.LocalDateTime timestamp;
}
