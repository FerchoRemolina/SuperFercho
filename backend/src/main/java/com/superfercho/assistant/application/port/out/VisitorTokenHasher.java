package com.superfercho.assistant.application.port.out;

public interface VisitorTokenHasher {

    String hash(String rawVisitorToken);
}
