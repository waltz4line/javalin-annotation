package org.github.waltz4line.server.javalin.event;

@FunctionalInterface
public interface LifecycleEvent {

    void handleEvent();

}
