package org.github.waltz4line.server.javalin;

import io.javalin.Javalin;
import org.github.waltz4line.server.javalin.openapi.DynamicDefinitionProcessor;
import org.github.waltz4line.server.javalin.openapi.RequestMapper;
import org.github.waltz4line.server.router.RequestMapperAttr;
import org.github.waltz4line.server.router.RouterMapper;

import java.lang.reflect.Method;

public class JavalinRouterMapper implements RouterMapper {

    private final Javalin javalin;

    private final boolean enableOpenApi;

    private final DynamicDefinitionProcessor definitionProcessor;

    public JavalinRouterMapper(Javalin javalin, DynamicDefinitionProcessor definitionProcessor) {
        this.javalin = javalin;
        this.enableOpenApi = definitionProcessor != null;
        this.definitionProcessor = definitionProcessor;
    }

    @Override
    public void requestGet(Object instance, Method method, RequestMapperAttr requestMapping) {
        javalin.get(requestMapping.getPath(), ctx -> RouterInvokeHandler.handle(ctx, instance, method));
        if (enableOpenApi) {
            definitionProcessor.addRequestMapper(RequestMapper.METHOD_GET, requestMapping);
        }
    }

    @Override
    public void requestPost(Object instance, Method method, RequestMapperAttr requestMapping) {
        javalin.post(requestMapping.getPath(), ctx -> RouterInvokeHandler.handle(ctx, instance, method));
        if (enableOpenApi) {
            definitionProcessor.addRequestMapper(RequestMapper.METHOD_POST, requestMapping);
        }
    }

    @Override
    public void requestPut(Object instance, Method method, RequestMapperAttr requestMapping) {
        javalin.put(requestMapping.getPath(), ctx -> RouterInvokeHandler.handle(ctx, instance, method));
        if (enableOpenApi) {
            definitionProcessor.addRequestMapper(RequestMapper.METHOD_PUT, requestMapping);
        }
    }

    @Override
    public void requestDelete(Object instance, Method method, RequestMapperAttr requestMapping) {
        javalin.delete(requestMapping.getPath(), ctx -> RouterInvokeHandler.handle(ctx, instance, method));
        if (enableOpenApi) {
            definitionProcessor.addRequestMapper(RequestMapper.METHOD_DELETE, requestMapping);
        }
    }

    @Override
    public void filterBefore(Object instance, Method method, String filterPath) {
        javalin.beforeMatched(filterPath, ctx -> RouterInvokeHandler.handle(ctx, instance, method));
    }

    @Override
    public void filterAfter(Object instance, Method method, String filterPath) {
        javalin.afterMatched(filterPath, ctx -> RouterInvokeHandler.handle(ctx, instance, method));
    }

}
