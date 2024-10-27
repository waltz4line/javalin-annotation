package org.github.waltz4line.server.javalin.error;

public class ServerInitializeException extends RuntimeException {
  public ServerInitializeException(String message) {
    super(message);
  }
}
