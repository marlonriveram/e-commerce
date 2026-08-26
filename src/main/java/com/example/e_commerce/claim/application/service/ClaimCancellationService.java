package com.example.e_commerce.claim.application.service;

import com.example.e_commerce.claim.domain.enums.EnumStatus;
import com.example.e_commerce.claim.domain.exception.ClaimNotFoundException;
import com.example.e_commerce.claim.domain.model.Claim;
import com.example.e_commerce.claim.domain.repository.ClaimRepository;
import com.example.e_commerce.claim.domain.validator.ClaimValidator;
import com.example.e_commerce.shared.event.ClaimStatusChangedEvent;
import com.example.e_commerce.user.domain.exception.UserNotFoundException;
import com.example.e_commerce.user.domain.model.User;
import com.example.e_commerce.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio encargado de cancelar claims (US-03).
 *
 * RESPONSABILIDAD: UNA sola capacidad — cancelar un claim que esté en PENDING.
 *
 * REGLAS DE NEGOCIO:
 *   - Solo se puede cancelar un claim en estado PENDING (si no → InvalidCancellationException, 400)
 *   - Solo el DUEÑO del claim puede cancelarlo (si no → NotClaimOwnerException, 403)
 *   - Al cancelar: el claim pasa a estado CANCELLED (terminal) y se registra en ClaimHistory
 *   - Se publica ClaimStatusChangedEvent para que US-02 notifique al cliente
 *
 * FLUJO CON EVENTOS:
 *   1. Busca el claim y valida que exista
 *   2. Busca el usuario que cancela y valida que exista
 *   3. Valida que el claim esté en PENDING
 *   4. Valida que el usuario sea el dueño del claim
 *   5. Persiste CANCELLED + ClaimHistory
 *   6. Publica el evento (Spring lo difiere hasta el COMMIT)
 *   7. Retorna OK → COMMIT → el evento llega a RabbitMQ (US-01) → notificación (US-02)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClaimCancellationService {

    private final ClaimRepository claimRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    private Claim findClaimById(Long id) {
        return claimRepository.findById(id)
                .orElseThrow(() -> new ClaimNotFoundException(id));
    }

    private User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    /**
     * Cancela un claim que esté en estado PENDING.
     *
     * @Transactional: si algo falla (validación, excepción) → ROLLBACK y el evento no se publica.
     *
     * @param claimId       ID del claim a cancelar
     * @param changedByUser ID del usuario que cancela (debe ser el dueño del claim)
     */
    @Transactional
    public Claim cancelClaim(Long claimId, Long changedByUser) {
        Claim claim = findClaimById(claimId);
        User user = findUserById(changedByUser);

        // Validaciones: si alguna falla, lanza excepción → ROLLBACK → no se publica evento
        ClaimValidator.validateClaimCanBeCancelled(claim.getStatus());
        ClaimValidator.validateUserIsClaimOwner(claimId, claim.getUserId(), changedByUser);

        EnumStatus previousStatus = claim.getStatus();

        Claim updated = updateStatusToCancelled(claim);

        log.info("Claim {} cancelado por el usuario {}", claimId, changedByUser);

        eventPublisher.publishEvent(new ClaimStatusChangedEvent(
                updated.getId(),           // claimId
                updated.getUserId(),       // customerUserId (el dueño, que es quien cancela)
                changedByUser,             // quién hizo el cambio
                previousStatus,            // PENDING
                EnumStatus.CANCELLED,      // nuevo estado
                java.time.LocalDateTime.now() // momento del cambio
        ));

        return updated;
    }

    private Claim updateStatusToCancelled(Claim claim) {
        claim.setStatus(EnumStatus.CANCELLED);
        claimRepository.save(claim);
        return claim;
    }
}
