package com.example.e_commerce.notification.domain.exception;

public class EmailNotificationException extends RuntimeException {

    public EmailNotificationException(String recipientEmail, Throwable cause) {
        super("Failed to send email notification to: " + recipientEmail, cause);
    }
}