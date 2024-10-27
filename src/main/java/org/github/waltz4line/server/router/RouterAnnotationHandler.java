package org.github.waltz4line.router;

import io.github.classgraph.*;
import org.apache.commons.lang3.StringUtils;
import org.github.waltz4line.router.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Router(path = "/router/annotation/:id", tag = "XXX-GROUP")
public final class RouterAnnotationHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RouterAnnotationHandler.class);

    private static final String KEY_PATH = "path";
    private static final String KEY_TAG = "tag";
    private static final String KEY_DESCRIPTION = "description";
    private static final String KEY_PATH_PARAMS = "pathParams";
    private static final String KEY_QUERY_PARAMS = "queryParams";
    private static final String KEY_REQUEST_BODY = "requestBody";
    private static final String KEY_RESPONSE_BODY = "responseBody";
    private static final String KEY_ERROR_BODY = "errorBody";
    private static final String KEY_TYPE = "type";

    private RouterAnnotationHandler() {
    }

    public static void handle(List<Object> instances, RouterMapper routerMapper) {
        Objects.requireNonNull(instances, "instance must not be null");
        for (Object instance : instances) {
//            handle(instance, routerMapper);
        }
    }

    public static void handle(Object instance) {
        Objects.requireNonNull(instance, "instance must not be null");
//        Objects.requireNonNull(routerMapper, "routerMapper must not be null");

        Class<?> clazz = instance.getClass();
        try (ScanResult scanResult = new ClassGraph().enableAllInfo()
                .acceptClasses(clazz.getName())
                .scan()) {

            ClassInfo classInfo = scanResult.getClassInfo(clazz.getName());
            if (classInfo == null) {
                LOGGER.warn("Class {} not found", clazz.getName());
                return;
            }
            Optional<RouterAttr> routerAttr = parseRouterAttr(classInfo, Router.class);
            routerAttr.ifPresent(attr -> parseEndpoints(instance, classInfo, attr));

            Optional<RouterAttr> filterAttr = parseRouterAttr(classInfo, RouterFilter.class);
            filterAttr.ifPresent(attr -> parseFilters(instance, classInfo, attr));
        }
    }

    private static void parseEndpoints(Object instance, ClassInfo classInfo, RouterAttr routerAttr) {
        MethodInfoList methodInfos = classInfo.getMethodInfo();
        var instanceName = classInfo.getSimpleName();
        methodInfos.stream()
                .filter(m -> isEndpointMethod(m) || isFilterMethod(m))
                .forEach(m -> {
                    if (m.hasAnnotation(GetMapping.class)) {

                    } else if (m.hasAnnotation(PostMapping.class)) {

                    } else if (m.hasAnnotation(PutMapping.class)) {

                    } else {

                    }
                });
    }


    private static void parseFilters(Object instance, ClassInfo classInfo, RouterAttr routerAttr) {

    }

    private static Optional<RequestMappingAttr> parseRequestMappingAttr(RouterAttr routerAttr, MethodInfo methodInfo, Class<? extends Annotation> annotation) {
        if (!methodInfo.hasAnnotation(annotation)) {
            return Optional.empty();
        }
        AnnotationParameterValueList parameterValues = methodInfo.getAnnotationInfo(annotation).getParameterValues();
        String path = (String) parameterValues.getValue(KEY_PATH);
        RequestMappingAttr requestMapping = new RequestMappingAttr(routerAttr.path() + path, routerAttr.tag());
        if (parameterValues.containsName(KEY_DESCRIPTION)) {
            requestMapping.setDescription((String) parameterValues.getValue(KEY_DESCRIPTION));
        }
        if (parameterValues.containsName(KEY_PATH_PARAMS)) {
            requestMapping.addPathParameters(parseParameterDescription(parameterValues, KEY_PATH_PARAMS));
        }
        if (parameterValues.containsName(KEY_QUERY_PARAMS)) {
            requestMapping.addQueryParameters(parseParameterDescription(parameterValues, KEY_QUERY_PARAMS));
        }
        if (parameterValues.containsName(KEY_REQUEST_BODY)) {
            requestMapping.setRequestBody((Class<?>) parameterValues.getValue(KEY_REQUEST_BODY));
        }
        if (parameterValues.containsName(KEY_RESPONSE_BODY)) {
            requestMapping.setResponseBody((Class<?>) parameterValues.getValue(KEY_RESPONSE_BODY));
        }
        if (parameterValues.containsName(KEY_ERROR_BODY)) {
            requestMapping.addErrorDescriptions(parseErrorDescription(parameterValues));
        }
        return Optional.of(requestMapping);
    }

    private static List<RequestMappingAttr.ParameterDescription> parseParameterDescription(AnnotationParameterValueList parameterValues, String paramName) {

    }

    private static List<RequestMappingAttr.ErrorDescription> parseErrorDescription(AnnotationParameterValueList parameterValues) {

    }


    private static Optional<RouterAttr> parseRouterAttr(ClassInfo classInfo, Class<? extends Annotation> annotation) {
        if (!classInfo.hasAnnotation(annotation)) {
            return Optional.empty();
        }
        AnnotationParameterValueList parameterValues = classInfo.getAnnotationInfo(annotation).getParameterValues();
        var path = (String) parameterValues.getValue(KEY_PATH);
        if (parameterValues.containsName(KEY_TAG)) {
            String tag = (String) parameterValues.getValue(KEY_TAG);
            if (StringUtils.isEmpty(tag)) {
                tag = classInfo.getSimpleName();
            }
            return Optional.of(RouterAttr.of(path, tag));
        }
        return Optional.of(RouterAttr.of(path));
    }

    private static boolean isEndpointMethod(MethodInfo methodInfo) {
        return methodInfo.hasAnnotation(GetMapping.class) ||
                methodInfo.hasAnnotation(PostMapping.class) ||
                methodInfo.hasAnnotation(PutMapping.class) ||
                methodInfo.hasAnnotation(DeleteMapping.class);
    }

    private static boolean isFilterMethod(MethodInfo methodInfo) {
        return methodInfo.hasAnnotation(BeforeFilter.class) || methodInfo.hasAnnotation(AfterFilter.class);
    }

    public static void main(String[] args) {
        RouterAnnotationHandler routerAnnotationHandler = new RouterAnnotationHandler();
        handle(routerAnnotationHandler);
    }

}
