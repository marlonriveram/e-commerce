package com.example.e_commerce.notification.infrastructure;

import com.example.e_commerce.claim.domain.enums.EnumStatus;
import com.example.e_commerce.notification.domain.exception.EmailNotificationException;
import com.example.e_commerce.shared.event.ClaimStatusChangedEvent;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailNotificationServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailNotificationService service;

    private ClaimStatusChangedEvent sampleEvent() {
        return new ClaimStatusChangedEvent(
                5L,                                     // claimId
                10L,                                    // customerUserId (cliente dueño del claim)
                3L,                                     // changedByUser (agente de soporte)
                EnumStatus.PENDING,                     // previousStatus
                EnumStatus.IN_REVIEW,                   // newStatus
                LocalDateTime.of(2026, 8, 3, 13, 15)    // timestamp
        );
    }

    @Test
    void shouldSendEmail_WhenValidEvent() {
        // Given
        MimeMessage message = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(message);
        ClaimStatusChangedEvent event = sampleEvent();

        // When
        service.sendClaimStatusNotification("ana@mail.com", event);

        // Then
        verify(mailSender).send(message);
    }

    @Test
    void shouldThrowEmailNotificationException_WhenSendFails() throws MessagingException {
        // Given
        MimeMessage message = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(message);
        // El helper (con multipart=true) invoca message.setContent(MimeMultipart),
        // que es donde el servicio captura MessagingException
        doThrow(new MessagingException("SMTP down")).when(message).setContent(any(jakarta.mail.Multipart.class));
        ClaimStatusChangedEvent event = sampleEvent();

        // When & Then
        assertThrows(EmailNotificationException.class,
                () -> service.sendClaimStatusNotification("ana@mail.com", event));
        verify(mailSender, never()).send(any(MimeMessage.class));
    }
}
