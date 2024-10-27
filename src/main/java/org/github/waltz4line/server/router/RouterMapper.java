package org.github.waltz4line.server.router;

import java.lang.reflect.Method;

public interface RouterMapper {

    void requestGet(Object instance, Method method, RequestMapperAttr requestMapping);

    void requestPost(Object instance, Method method, RequestMapperAttr requestMapping);

    void requestPut(Object instance, Method method, RequestMapperAttr requestMapping);

    void requestDelete(Object instance, Method method, RequestMapperAttr requestMapping);

    void filterBefore(Object instance, Method method, String filterPath);

    void filterAfter(Object instance, Method method, String filterPath);

}
