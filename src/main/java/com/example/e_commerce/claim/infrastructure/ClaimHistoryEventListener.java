package com.example.e_commerce.claim.infrastructure;

import com.example.e_commerce.claim.domain.model.ClaimHistory;
import com.example.e_commerce.claim.domain.repository.ClaimHistoryRepository;
import com.example.e_commerce.shared.event.ClaimStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClaimHistoryEventListener {

    private final ClaimHistoryRepository claimHistoryRepository;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onClaimStatusChanged(ClaimStatusChangedEvent event) {
        log.info("Registrando historial: claimId={}, {} -> {}",
                event.getClaimId(),
                event.getPreviousStatus(),
                event.getNewStatus());

        ClaimHistory history = ClaimHistory.builder()
                .claimId(event.getClaimId())
                .previousStatus(event.getPreviousStatus())
                .newStatus(event.getNewStatus())
                .changedByUser(event.getChangedByUser())
                .build();

        claimHistoryRepository.save(history);
    }
}
