package org.github.waltz4line.server;

import org.github.waltz4line.server.javalin.error.ServerInitializeException;

public interface WebServerContext {

    void initialize() throws ServerInitializeException;

    void start() throws ServerInitializeException, InterruptedException;

}
