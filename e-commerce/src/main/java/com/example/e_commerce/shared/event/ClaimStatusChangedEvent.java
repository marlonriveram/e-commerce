package com.example.e_commerce.shared.event;

import com.example.e_commerce.claim.domain.enums.EnumStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Evento de dominio que se publica cada vez que una reclamación cambia de estado.
 *
 * Este objeto NO es una entity de JPA ni un DTO de API. Es un "evento":
 * una representación inmutable de algo que ya ocurrió en el sistema.
 *
 * FLUJO:
 *   1. ClaimReviewService o ClaimRefundService cambia el estado del claim en la BD.
 *   2. Dentro de la misma transacción, publica este evento via ApplicationEventPublisher.
 *   3. Spring DEFIERE la entrega del evento hasta que la transacción haga COMMIT exitoso.
 *   4. Si hay ROLLBACK, este evento NUNCA se entrega.
 *   5. ClaimEventPublisher recibe el evento y lo envía a RabbitMQ.
 *
 * POR QUÉ ESTÁ EN shared/:
 *   Porque es cross-cutting: lo produce el módulo claim/ y lo consume el módulo notification/.
 *   shared/ es el lugar para componentes que cruzan límites de dominio.
 */
@Getter
@AllArgsConstructor
public class ClaimStatusChangedEvent {

    /** ID de la reclamación que cambió de estado */
    private Long claimId;

    /** ID del usuario que realizó el cambio (agente de soporte o finanzas) */
    private Long userId;

    /** Estado ANTES del cambio (ej: PENDING) */
    private EnumStatus previousStatus;

    /** Estado DESPUÉS del cambio (ej: IN_REVIEW) */
    private EnumStatus newStatus;

    /** Momento exacto en que ocurrió el cambio */
    private java.time.LocalDateTime timestamp;
}
