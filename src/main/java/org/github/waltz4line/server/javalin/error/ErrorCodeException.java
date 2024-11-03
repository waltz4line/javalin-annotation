package org.github.waltz4line.server.javalin.error;

public class ErrorCodeException extends RuntimeException {
    public ErrorCodeException(String message) {
        super(message);
    }
}
