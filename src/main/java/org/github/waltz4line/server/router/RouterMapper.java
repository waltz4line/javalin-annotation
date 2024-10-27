package org.github.waltz4line.router;

import java.lang.reflect.Method;

public interface RouterMapper {

    void requestGet(String path, Method method, Object instance);

    void requestPost(String path, Method method, Object instance);

    void requestPut(String path, Method method, Object instance);

    void requestDelete(String path, Method method, Object instance);

    void filterBefore(String path, Method method, Object instance);

    void filterAfter(String path, Method method, Object instance);

    void documentGet(String path, RouterDocument document);

    void documentPost(String path, RouterDocument document);

    void documentPut(String path, RouterDocument document);

    void documentDelete(String path, RouterDocument document);

}
