package com.superfercho.assistant.application.tool;

public enum ToolAccess {
    PUBLIC,
    CUSTOMER;

    public boolean covers(ToolAccess required) {
        return this == CUSTOMER || required == PUBLIC;
    }
}
