package org.github.waltz4line.server.javalin;

import io.javalin.http.Context;
import org.github.waltz4line.server.javalin.error.ErrorCodeException;

import java.lang.reflect.Method;
import java.util.Optional;

public final class RouterInvokeHandler {

    private RouterInvokeHandler() {
    }

    public static void handle(Context context, Object instance, Method method) {
        try {
            method.invoke(instance, context);
        } catch (Throwable e) {
            Optional<ErrorCodeException> errorCodeException = findCause(e);
            if (errorCodeException.isPresent()) {
                throw errorCodeException.get();
            }
            throw new ErrorCodeException("TODO");
        }
    }

    public static Optional<ErrorCodeException> findCause(Throwable throwable) {
        while (throwable != null) {
            if (throwable instanceof ErrorCodeException errorCodeException) {
                return Optional.of(errorCodeException);
            }
            throwable = throwable.getCause();
        }
        return Optional.empty();
    }

}
