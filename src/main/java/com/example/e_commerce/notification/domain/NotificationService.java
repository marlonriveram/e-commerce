package com.example.e_commerce.notification.domain;

import com.example.e_commerce.shared.event.ClaimStatusChangedEvent;


public interface NotificationService {

    // recipientEmail: Correo del usuario donde se envia la notificacion
    void sendClaimStatusNotification(String recipientEmail, ClaimStatusChangedEvent event);
}
