package com.superfercho.assistant.domain.exception;

public class InvalidConversationException extends RuntimeException {

    public InvalidConversationException(String message) {
        super(message);
    }
}
