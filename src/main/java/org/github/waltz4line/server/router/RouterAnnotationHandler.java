package org.github.waltz4line.server.router;

import io.github.classgraph.*;
import org.apache.commons.lang3.StringUtils;
import org.github.waltz4line.server.router.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;

public final class RouterAnnotationHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RouterAnnotationHandler.class);

    private static final char ENDPOINT_ID_DELIMITER = '.';
    private static final String KEY_PATH = "path";
    private static final String KEY_TAG = "tag";
    private static final String KEY_DESCRIPTION = "description";
    private static final String KEY_PATH_PARAMS = "pathParams";
    private static final String KEY_QUERY_PARAMS = "queryParams";
    private static final String KEY_REQUEST_BODY = "requestBody";
    private static final String KEY_RESPONSE_BODY = "responseBody";
    private static final String KEY_ERROR_BODY = "errorBody";

    private RouterAnnotationHandler() {
    }

    public static void handle(List<Object> instances, RouterMapper routerMapper) {
        Objects.requireNonNull(instances, "instance must not be null");
        for (Object instance : instances) {
            handle(instance, routerMapper);
        }
    }

    public static void handle(Object instance, RouterMapper routerMapper) {
        Objects.requireNonNull(instance, "instance must not be null");
        Objects.requireNonNull(routerMapper, "routerMapper must not be null");

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
            routerAttr.ifPresent(attr -> parseEndpoints(instance, classInfo.getMethodInfo(), attr, routerMapper));

            Optional<RouterAttr> filterAttr = parseRouterAttr(classInfo, RouterFilter.class);
            filterAttr.ifPresent(attr -> parseFilters(instance, classInfo.getMethodInfo(), attr, routerMapper));
        }
    }

    private static void parseEndpoints(Object instance, MethodInfoList methodInfos, RouterAttr routerAttr, RouterMapper routerMapper) {
        List<MethodInfo> filteredMethods = methodInfos.stream()
                .filter(m -> isEndpointMethod(m) || isFilterMethod(m)).toList();
        for (MethodInfo methodInfo : filteredMethods) {
            Method method = methodInfo.loadClassAndGetMethod();
            method.setAccessible(true);
            if (methodInfo.hasAnnotation(GetMapping.class)) {
                RequestMapperAttr requestMapping = parseRequestMappingAttr(routerAttr, methodInfo, GetMapping.class);
                routerMapper.requestGet(instance, method, requestMapping);
            } else if (methodInfo.hasAnnotation(PostMapping.class)) {
                RequestMapperAttr requestMapping = parseRequestMappingAttr(routerAttr, methodInfo, PostMapping.class);
                routerMapper.requestPost(instance, method, requestMapping);
            } else if (methodInfo.hasAnnotation(PutMapping.class)) {
                RequestMapperAttr requestMapping = parseRequestMappingAttr(routerAttr, methodInfo, PutMapping.class);
                routerMapper.requestPut(instance, method, requestMapping);
            } else if (methodInfo.hasAnnotation(DeleteMapping.class)) {
                RequestMapperAttr requestMapping = parseRequestMappingAttr(routerAttr, methodInfo, DeleteMapping.class);
                routerMapper.requestDelete(instance, method, requestMapping);
            } else {
                mappingFilter(methodInfo, method, instance, routerAttr, routerMapper);
            }
            LOGGER.info("Endpoint parsed for Class:{} Method:{}", instance.getClass().getName(), method.getName());
        }
    }

    private static void parseFilters(Object instance, MethodInfoList methodInfos, RouterAttr routerAttr, RouterMapper routerMapper) {
        List<MethodInfo> filteredMethods = methodInfos.stream()
                .filter(RouterAnnotationHandler::isFilterMethod).toList();
        for (MethodInfo methodInfo : filteredMethods) {
            Method method = methodInfo.loadClassAndGetMethod();
            mappingFilter(methodInfo, method, instance, routerAttr, routerMapper);
            LOGGER.info("Filter parsed for Class:{} Method:{}", instance.getClass().getName(), method.getName());
        }
    }

    private static void mappingFilter(MethodInfo methodInfo, Method method, Object instance, RouterAttr routerAttr, RouterMapper routerMapper) {
        if (methodInfo.hasAnnotation(BeforeFilter.class)) {
            String filterPath = parseFilterPath(routerAttr, methodInfo, BeforeFilter.class);
            routerMapper.filterBefore(instance, method, filterPath);
        } else {
            String filterPath = parseFilterPath(routerAttr, methodInfo, AfterFilter.class);
            routerMapper.filterBefore(instance, method, filterPath);
        }
    }

    private static String parseFilterPath(RouterAttr routerAttr, MethodInfo methodInfo, Class<? extends Annotation> annotation) {
        AnnotationParameterValueList parameterValues = methodInfo.getAnnotationInfo(annotation).getParameterValues();
        String path = (String) parameterValues.getValue(KEY_PATH);
        return routerAttr.pathConcat(path);
    }

    private static RequestMapperAttr parseRequestMappingAttr(RouterAttr routerAttr, MethodInfo methodInfo, Class<? extends Annotation> annotation) {
        AnnotationParameterValueList parameterValues = methodInfo.getAnnotationInfo(annotation).getParameterValues();
        String path = (String) parameterValues.getValue(KEY_PATH);
        String endpointId = methodInfo.getName();
        RequestMapperAttr requestMapping = RequestMapperAttr.of(routerAttr.pathConcat(path), routerAttr.tag(), routerAttr.tag() + ENDPOINT_ID_DELIMITER + endpointId);
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
        return requestMapping;
    }

    private static List<RequestMapperAttr.ParameterDescription> parseParameterDescription(AnnotationParameterValueList parameterValues, String paramName) {
        ParamDescriptor[] paramDescriptors = (ParamDescriptor[]) parameterValues.getValue(paramName);
        if (paramDescriptors == null || paramDescriptors.length == 0) {
            return Collections.emptyList();
        }
        List<RequestMapperAttr.ParameterDescription> parameterDescriptions = new ArrayList<>(paramDescriptors.length);
        for (ParamDescriptor paramDescriptor : paramDescriptors) {
            parameterDescriptions.add(new RequestMapperAttr.ParameterDescription(paramDescriptor.name(), paramDescriptor.type(), paramDescriptor.description()));
        }
        return parameterDescriptions;
    }

    private static List<RequestMapperAttr.ErrorDescription> parseErrorDescription(AnnotationParameterValueList parameterValues) {
        ErrorBody[] errorBodies = (ErrorBody[]) parameterValues.getValue(KEY_ERROR_BODY);
        if (errorBodies == null || errorBodies.length == 0) {
            return Collections.emptyList();
        }
        List<RequestMapperAttr.ErrorDescription> errorDescriptions = new ArrayList<>(errorBodies.length);
        for (ErrorBody errorBody : errorBodies) {
            errorDescriptions.add(new RequestMapperAttr.ErrorDescription(errorBody.statusCode(), errorBody.errorCode(), errorBody.responseClass()));
        }
        return errorDescriptions;
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

}
