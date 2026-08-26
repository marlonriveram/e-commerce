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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Servicio encargado de procesar reembolsos de claims (rol FINANCE).
 *
 * RESPONSABILIDAD: UNA sola capacidad — reembolsar un claim (siempre a estado REFUNDED).
 *
 * CAMBIO US-01:
 *   Igual que ClaimReviewService, se inyecta ApplicationEventPublisher
 *   para notificar cuando un claim pasa a estado REFUNDED.
 *
 */
@Service
@RequiredArgsConstructor
public class ClaimRefundService {

    private final ClaimRepository claimRepository;
    private final UserRepository userRepository;

    /** ApplicationEventPublisher: mismo mecanismo que en ClaimReviewService */
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

     *
     * @param idClaim      ID del claim a reembolsar
     * @param changedByUser ID del usuario que realiza el cambio (debe ser FINANCE)
     */
    @Transactional
    public Claim refundClaim(Long idClaim, Long changedByUser) {
        Claim claim = findClaimById(idClaim);
        User user = findUserById(changedByUser);


        ClaimValidator.validateRoleForTransition(user.getRole(), EnumStatus.REFUNDED);
        ClaimValidator.validateStatusTransition(claim.getStatus(), EnumStatus.REFUNDED);


        EnumStatus previousStatus = claim.getStatus();


        Claim updated = updateStatus(claim, EnumStatus.REFUNDED);

        // Publica el evento
        eventPublisher.publishEvent(new ClaimStatusChangedEvent(
                updated.getId(),           // ID del claim que cambió
                updated.getUserId(),       // ID del CLIENTE dueño del claim (a quien notificar)
                changedByUser,             // quién hizo el cambio (agente FINANCE)
                previousStatus,            // estado anterior
                EnumStatus.REFUNDED,       // nuevo estado
                java.time.LocalDateTime.now() // momento del cambio
        ));

        return updated;
    }

    private Claim updateStatus(Claim claim, EnumStatus newStatus) {
        claim.setStatus(newStatus);
        claimRepository.save(claim);
        return claim;
    }
}
