package com.superfercho.assistant.application.exception;

public class ToolAccessDeniedException extends RuntimeException {

    public ToolAccessDeniedException(String message) {
        super(message);
    }
}
