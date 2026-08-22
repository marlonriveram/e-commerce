package com.example.e_commerce.ai.claimclassifier.infrastructure;

import com.example.e_commerce.ai.claimclassifier.application.service.ClaimCategorizationService;
import com.example.e_commerce.shared.config.RabbitMQConfig;
import com.example.e_commerce.shared.event.ClaimCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClaimAiConsumer {

    private final ClaimCategorizationService claimCategorizationService;

    /**
     * Escucha la cola de eventos de creación de claims y categoriza el reclamo
     * con la IA (Groq). Si el análisis falla, el mensaje se reintenta y cae al
     * Dead Letter Queue (claim.dlq) tras agotar los reintentos.
     */
    @RabbitListener(queues = RabbitMQConfig.CLAIM_AI_QUEUE)
    public void onClaimCreated(ClaimCreatedEvent event) {
        log.info("Consumiendo evento de creación para categorizar: claimId={}", event.getClaimId());
        claimCategorizationService.categorize(event.getClaimId(), event.getDescription());
    }
}
