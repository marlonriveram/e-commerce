package com.example.e_commerce.notification.infrastructure;

import com.example.e_commerce.notification.domain.NotificationService;
import com.example.e_commerce.shared.config.RabbitMQConfig;
import com.example.e_commerce.shared.event.ClaimStatusChangedEvent;
import com.example.e_commerce.user.domain.model.User;
import com.example.e_commerce.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClaimStatusChangedConsumer {

    private final UserRepository userRepository;
    private final NotificationService notificationService;

    /**
     * Escucha la cola de eventos de cambio de estado.
     *
     * @RabbitListener: Spring crea un listener automáticamente para esta cola.
     * Si este método lanza una excepción, el mensaje se reintenta y cae al
     * Dead Letter Queue (claim.dlq) tras agotar los reintentos.
     */
    @RabbitListener(queues = RabbitMQConfig.CLAIM_STATUS_QUEUE)
    public void onClaimStatusChanged(ClaimStatusChangedEvent event) {
        log.info("Consumiendo evento de cambio de estado: claimId={}", event.getClaimId());

        User customer = userRepository.findById(event.getCustomerUserId()).orElse(null);
        if (customer == null) {
            log.warn("Usuario {} no encontrado. Se consume el mensaje sin notificar.", event.getCustomerUserId());
            return;
        }

        notificationService.sendClaimStatusNotification(customer.getEmail(), event);
    }
}
