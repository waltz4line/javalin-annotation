package org.github.waltz4line.server.javalin.error;

public class ServerInitializeException extends Exception {

    public ServerInitializeException(String message) {
        super(message);
    }

    public ServerInitializeException(String message, Throwable cause) {
        super(message, cause);
    }

    public ServerInitializeException(Throwable cause) {
        super(cause);
    }
}
