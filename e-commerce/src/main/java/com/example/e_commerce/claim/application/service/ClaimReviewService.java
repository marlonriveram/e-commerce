package com.example.e_commerce.claim.application.service;

import com.example.e_commerce.claim.domain.enums.EnumStatus;
import com.example.e_commerce.claim.domain.exception.ClaimNotFoundException;
import com.example.e_commerce.claim.domain.model.Claim;
import com.example.e_commerce.claim.domain.model.ClaimHistory;
import com.example.e_commerce.claim.domain.repository.ClaimHistoryRepository;
import com.example.e_commerce.claim.domain.repository.ClaimRepository;
import com.example.e_commerce.claim.domain.validator.ClaimValidator;
import com.example.e_commerce.shared.event.ClaimStatusChangedEvent;
import com.example.e_commerce.user.domain.exception.UserNotFoundException;
import com.example.e_commerce.user.domain.model.User;
import com.example.e_commerce.user.domain.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Servicio encargado de revisar claims (transiciones de estado para rol SUPPORT).
 *
 * RESPONSABILIDAD: UNA sola capacidad — revisar un claim y cambiar su estado.
 *
 * CAMBIO US-01:
 *   Se inyecta ApplicationEventPublisher para publicar un ClaimStatusChangedEvent
 *   después de persistir el cambio de estado. El evento se publica DENTRO del
 *   método @Transactional, por lo que Spring lo diferirá hasta después del COMMIT.
 *
 * FLUJO CON EVENTOS:
 *   1. Valida rol y transición
 *   2. Persiste el cambio de estado + ClaimHistory
 *   3. Publica el evento (Spring lo pone en cola interna)
 *   4. El método retorna OK → Spring hace COMMIT
 *   5. Spring entrega el evento a ClaimEventPublisher
 *   6. ClaimEventPublisher envía a RabbitMQ
 */
@Service
@RequiredArgsConstructor
public class ClaimReviewService {

    private final ClaimRepository claimRepository;
    private final ClaimHistoryRepository claimHistoryRepository;
    private final UserRepository userRepository;

    /** ApplicationEventPublisher: puerta de salida de Spring Application Events.
     *  No es RabbitMQ — es el sistema interno de eventos de Spring.
     *  Lo usamos para publicar eventos que luego ClaimEventPublisher
     *  capturará y enviará a RabbitMQ. */
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
     * Revisa un claim y cambia su estado.
     *
     * @Transactional: TODO este método se ejecuta dentro de una transacción de BD.
     *   - Si termina sin excepciones → COMMIT automático
     *   - Si lanza una excepción RuntimeException → ROLLBACK automático
     *   - Los eventos publicados con publishEvent() se diferirán hasta el COMMIT
     *
     * @param id            ID del claim a revisar
     * @param newStatus     nuevo estado (IN_REVIEW, APPROVED, o REJECTED)
     * @param changedByUser ID del usuario que realiza el cambio (debe ser SUPPORT)
     */
    @Transactional
    public Claim reviewClaim(Long id, EnumStatus newStatus, Long changedByUser) {
        Claim claim = findClaimById(id);
        User user = findUserById(changedByUser);

        // Validaciones: si alguna falla, lanza excepción → ROLLBACK → no se publica evento
        ClaimValidator.validateRoleForTransition(user.getRole(), newStatus);
        ClaimValidator.validateStatusTransition(claim.getStatus(), newStatus);

        // Capturamos el estado anterior ANTES de cambiarlo (lo necesitamos para el evento)
        EnumStatus previousStatus = claim.getStatus();

        // Persiste el cambio en BD + crea registro de auditoría
        Claim updated = updateStatus(claim, newStatus, changedByUser);

        // Publica el evento al Application Context de Spring.
        // NO va directo a RabbitMQ — Spring lo guarda internamente y lo entrega
        // a los @EventListener SOLO después de que esta transacción haga COMMIT.
        // Si hay excepción y la transacción se revierte, este evento se descarta.
        eventPublisher.publishEvent(new ClaimStatusChangedEvent(
                updated.getId(),        // ID del claim que cambió
                changedByUser,          // quién hizo el cambio
                previousStatus,         // estado anterior
                newStatus,              // nuevo estado
                java.time.LocalDateTime.now() // momento del cambio
        ));

        return updated;
    }

    private Claim updateStatus(Claim claim, EnumStatus newStatus, Long changedByUser) {
        EnumStatus previousStatus = claim.getStatus();
        claim.setStatus(newStatus);
        claimRepository.save(claim);

        ClaimHistory history = ClaimHistory.builder()
                .claimId(claim.getId())
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .changedByUser(changedByUser)
                .build();
        claimHistoryRepository.save(history);

        return claim;
    }
}
