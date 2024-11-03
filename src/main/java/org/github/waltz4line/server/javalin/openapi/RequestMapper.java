package org.github.waltz4line.server.javalin.openapi;

import org.github.waltz4line.server.router.RequestMapperAttr;

public record RequestMapper(String methodType, RequestMapperAttr requestMapper) {

    public static final String METHOD_GET = "get";
    public static final String METHOD_POST = "post";
    public static final String METHOD_PUT = "put";
    public static final String METHOD_DELETE = "delete";

}
