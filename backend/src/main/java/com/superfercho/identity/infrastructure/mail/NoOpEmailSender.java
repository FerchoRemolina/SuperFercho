package com.superfercho.identity.infrastructure.mail;

import com.superfercho.identity.application.port.EmailMessage;
import com.superfercho.identity.application.port.EmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Placeholder for a real provider (Resend / Brevo / SMTP). Refuses to send until configured.
 */
public final class NoOpEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(NoOpEmailSender.class);

    @Override
    public void send(EmailMessage message) {
        log.warn(
                "Email provider is 'noop': message to {} was not delivered. Configure SUPERFERCHO_EMAIL_PROVIDER.",
                message.to());
    }
}
