package com.superfercho.assistant.application.exception;

public class ToolNotAllowedException extends RuntimeException {

    public ToolNotAllowedException(String toolName) {
        super("Tool is not allowed: " + toolName);
    }
}
