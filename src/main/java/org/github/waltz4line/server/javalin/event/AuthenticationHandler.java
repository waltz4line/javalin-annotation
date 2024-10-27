package org.github.waltz4line.server.javalin.event;


import io.javalin.http.Context;

@FunctionalInterface
public interface AuthenticationHandler {

    AuthenticationHandler NOOP = context ->  {};

    void handle(Context context);

}
