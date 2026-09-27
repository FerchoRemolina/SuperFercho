package com.superfercho.identity.application.port;

/**
 * Outbound port for transactional email. Providers (console, Resend, SMTP) live in Infrastructure.
 */
public interface EmailSender {

    void send(EmailMessage message);
}
