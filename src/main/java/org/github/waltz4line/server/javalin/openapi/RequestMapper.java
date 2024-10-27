package org.github.waltz4line.server.javalin.openapi;

import org.github.waltz4line.server.router.RequestMapperAttr;

public record RequestMapper(String methodType, RequestMapperAttr requestMapper) {

    public static final String METHOD_GET = "GET";
    public static final String METHOD_POST = "POST";
    public static final String METHOD_PUT = "PUT";
    public static final String METHOD_DELETE = "DELETE";

}
