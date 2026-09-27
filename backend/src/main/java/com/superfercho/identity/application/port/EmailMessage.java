package com.superfercho.identity.application.port;

public record EmailMessage(String to, String subject, String textBody) {

    public EmailMessage {
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("to cannot be blank");
        }
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("subject cannot be blank");
        }
        if (textBody == null || textBody.isBlank()) {
            throw new IllegalArgumentException("textBody cannot be blank");
        }
    }
}
