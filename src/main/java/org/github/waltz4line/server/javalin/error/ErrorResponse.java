package org.github.waltz4line.server.javalin.error;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(String errorCode, String errorDescription) {
}
