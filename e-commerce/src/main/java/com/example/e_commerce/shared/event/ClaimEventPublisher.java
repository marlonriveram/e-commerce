package com.example.e_commerce.shared.event;

import com.example.e_commerce.shared.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Publicador de eventos de claims hacia RabbitMQ.
 *
 * ¿QUÉ HACE ESTE COMPONENTE?
 *   Escucha los eventos de Spring Application Event (ClaimStatusChangedEvent)
 *   y los envía a RabbitMQ para que otros sistemas los consuman.
 *
 * FLUJO COMPLETO (paso a paso):
 *
 *   1. ClaimReviewService.reviewClaim() ejecuta updateStatus() → persiste en BD
 *   2. ClaimReviewService llama a eventPublisher.publishEvent(new ClaimStatusChangedEvent(...))
 *   3. Spring Application Event: el evento se pone en una cola de espera interna
 *   4. Spring verifica: ¿la transacción @Transactional ya hizo COMMIT?
 *      - SÍ → Spring entrega el evento a los @EventListener
 *      - NO → Spring espera a que termine la transacción
 *   5. Este componente (@EventListener) recibe el ClaimStatusChangedEvent
 *   6. RabbitTemplate.convertAndSend() envía el evento a RabbitMQ
 *   7. El mensaje queda en la cola claim.status.queue hasta que un consumidor lo lea
 *
 * ¿POR QUÉ @EventListener y NO @TransactionalEventListener?
 *   @EventListener simple escucha el evento cuando Spring lo entrega.
 *   La entrega YA está diferida por Spring hasta después del COMMIT
 *   (porque el evento se publicó dentro de un @Transactional).
 *   No necesitamos @TransactionalEventListener porque este componente
 *   NO accede a la BD — solo envía a RabbitMQ.
 *
 * ¿POR QUÉ no está en el módulo claim/?
 *   Porque es infraestructura de cross-cutting concern. El módulo claim/
 *   solo sabe que "algo" escucha sus eventos. Este componente es el
 *   "algo" que conecta claim/ con RabbitMQ. Segregación de responsabilidades.
 *
 * SLF4J (@Slf4J de Lombok):
 *   Genera un logger automático para registrar qué eventos se publican.
 *   Útil para debugging en producción.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClaimEventPublisher {

    /** RabbitTemplate: la herramienta de Spring para enviar mensajes a RabbitMQ.*/
    private final RabbitTemplate rabbitTemplate;

    /**
     * Escucha eventos ClaimStatusChangedEvent del Application Context de Spring.
     *
     * @EventListener: Spring Detecta que este método acepta ClaimStatusChangedEvent
     *   y lo llama automáticamente cada vez que alguien hace publishEvent() con
     *   ese tipo de evento.
     *
     * ¿POR QUÉ ESTE MÉTODO ES VOID?
     *   Porque el evento ya ocurrió. No hay nada que "retornar". Solo propagamos
     *   la notificación a RabbitMQ. Es como un altavoz: recibe y difunde.
     */
    @EventListener
    public void onClaimStatusChanged(ClaimStatusChangedEvent event) {
        // Registramos el evento para debugging
        log.info("Publicando evento de cambio de estado en RabbitMQ: " +
                 "claimId={}, {} -> {}",
                 event.getClaimId(),
                 event.getPreviousStatus(),
                 event.getNewStatus()
        );

        // RabbitTemplate.convertAndSend():
        //   1. Toma el ClaimStatusChangedEvent
        //   2. JacksonJsonMessageConverter lo convierte a JSON (Jackson 3)
        //   3. Envía el JSON a RabbitMQ al exchange y routing key especificados
        //   4. RabbitMQ lo enruta a la cola claim.status.queue (por el binding)
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.CLAIM_EXCHANGE,          // exchange destino
                RabbitMQConfig.CLAIM_STATUS_ROUTING_KEY, // routing key
                event                                    // payload (se convierte a JSON)
        );

        log.info("Evento publicado exitosamente: claimId={}", event.getClaimId());
    }
}
