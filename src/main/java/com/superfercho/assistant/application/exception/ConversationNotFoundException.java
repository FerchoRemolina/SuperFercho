package com.superfercho.assistant.application.exception;

public class ConversationNotFoundException extends RuntimeException {

    public ConversationNotFoundException() {
        super("Conversation not found");
    }
}
