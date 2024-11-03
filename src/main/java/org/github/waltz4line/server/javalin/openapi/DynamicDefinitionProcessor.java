package org.github.waltz4line.server.javalin.openapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.javalin.openapi.plugin.DefinitionProcessor;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.github.waltz4line.server.router.RequestMapperAttr;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

public class DynamicDefinitionProcessor implements DefinitionProcessor {

    private static final String PARAM_TYPE_PATH = "path";
    private static final String PARAM_TYPE_QUERY = "query";
    private static final String NODE_PATHS = "paths";
    private static final String NODE_TAGS = "tags";
    private static final String NODE_OPERATION_ID = "operationId";
    private static final String NODE_SUMMARY = "summary";
    private static final String NODE_PARAMETERS = "parameters";
    private static final String NODE_NAME = "name";
    private static final String NODE_IN = "in";
    private static final String NODE_REQUIRED = "required";
    private static final String NODE_SCHEMA = "schema";
    private static final String NODE_TYPE = "type";
    private static final String NODE_DESCRIPTION = "description";
    private static final String NODE_PROPERTIES = "properties";
    private static final String NODE_ITEMS = "items";
    private static final String NODE_REQUEST_BODY = "requestBody";
    private static final String NODE_CONTENT = "content";
    private static final String NODE_APPLICATION_JSON = "application/json";
    private static final String NODE_RESPONSES = "responses";

    private static final String TYPE_VAL_BOOLEAN = "boolean";
    private static final String TYPE_VAL_INTEGER = "integer";
    private static final String TYPE_VAL_NUMERIC = "numeric";
    private static final String TYPE_VAL_STRING = "string";
    private static final String TYPE_VAL_OBJECT = "object";
    private static final String TYPE_VAL_ARRAY = "array";

    private final Map<String, List<RequestMapper>> requestMappers = new HashMap<>(Byte.MAX_VALUE);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @NotNull
    @Override
    public String process(@NotNull ObjectNode objectNode) {
        ObjectNode paths = objectNode.putObject(NODE_PATHS);
        for (Map.Entry<String, List<RequestMapper>> entry : requestMappers.entrySet()) {
            List<RequestMapper> docMappers = entry.getValue();
            ObjectNode path = paths.putObject(entry.getKey());
            for (RequestMapper docMapper : docMappers) {
                createEndpointDocument(path, docMapper);
            }
        }
        return objectNode.toString();
    }

    public void addRequestMapper(String methodType, RequestMapperAttr requestMapperAttr) {
        Objects.requireNonNull(methodType, "methodType must not be null");
        Objects.requireNonNull(requestMapperAttr, "requestMapperAttr must not be null");
        requestMappers.computeIfAbsent(requestMapperAttr.getPath(), k -> new ArrayList<>())
                .add(new RequestMapper(methodType, requestMapperAttr));
    }

    private void createEndpointDocument(ObjectNode pathNode, RequestMapper docMapper) {
        RequestMapperAttr.EndpointDocument document = docMapper.requestMapper().getEndpointDocument();
        ObjectNode method = pathNode.putObject(docMapper.methodType());
        method.put(NODE_OPERATION_ID, document.getEndpointId());
        method.putArray(NODE_TAGS).add(document.getTag());
        method.put(NODE_SUMMARY, document.getDescription());
        ArrayNode parameters = objectMapper.createArrayNode();
        addParameter(parameters, PARAM_TYPE_PATH, document.getPathParameters());
        addParameter(parameters, PARAM_TYPE_QUERY, document.getQueryParameters());
        if (!parameters.isEmpty()) {
            method.set(NODE_PARAMETERS, parameters);
        }
        addRequestBody(method, document.getRequestBody());
        ObjectNode responses = objectMapper.createObjectNode();
        addResponseBody(responses, document.getResponseBody(), HttpStatus.OK_200);
        addErrorBody(responses, document.getErrorDescriptions());
        method.set(NODE_RESPONSES, responses);
    }

    private void addParameter(ArrayNode parameters, String paramType, List<RequestMapperAttr.ParameterDescription> parameterDescriptions) {
        if (parameterDescriptions.isEmpty()) {
            return;
        }
        for (RequestMapperAttr.ParameterDescription parameterDescription : parameterDescriptions) {
            ObjectNode parameter = objectMapper.createObjectNode();
            parameter.put(NODE_NAME, parameterDescription.name());
            parameter.put(NODE_IN, paramType);
            parameter.put(NODE_REQUIRED, PARAM_TYPE_PATH.equals(paramType));
            parameter.set(NODE_SCHEMA, createSchema(parameterDescription.type(), parameterDescription.description()));
            parameters.add(parameter);
        }
    }

    private void addRequestBody(ObjectNode methodNode, Class<?> bodyClass) {
        if (bodyClass == null) {
            return;
        }
        ObjectNode requestBody = methodNode.putObject(NODE_REQUEST_BODY);
        putJsonBody(requestBody, bodyClass);
    }

    private void addResponseBody(ObjectNode responses, Class<?> bodyClass, int statusCode) {
        if (bodyClass == null) {
            return;
        }
        ObjectNode response = responses.putObject(String.valueOf(statusCode));
        putJsonBody(response, bodyClass);
    }

    private void addErrorBody(ObjectNode responses, List<RequestMapperAttr.ErrorDescription> errorDescriptions) {
        for (RequestMapperAttr.ErrorDescription errorDescription : errorDescriptions) {
            addResponseBody(responses, errorDescription.errorResponse(), errorDescription.statusCode());
        }
    }

    private void putJsonBody(ObjectNode bodyNode, Class<?> bodyClass) {
        ObjectNode jsonNode = bodyNode.putObject(NODE_CONTENT)
                .putObject(NODE_APPLICATION_JSON);
        jsonNode.set(NODE_SCHEMA, createSchema(bodyClass, bodyClass.getSimpleName()));
    }

    private ObjectNode createSchema(Class<?> type, String description) {
        ObjectNode schema = objectMapper.createObjectNode();
        String typeVal = parseType(type);
        schema.put(NODE_TYPE, typeVal);
        if (StringUtils.isNotEmpty(description)) {
            schema.put(NODE_DESCRIPTION, description);
        }
        if (TYPE_VAL_ARRAY.equals(typeVal)) {
            Class<?> component = type.getComponentType();
            schema.set(NODE_ITEMS, createSchema(component, component.getSimpleName()));
        } else if (TYPE_VAL_OBJECT.equals(typeVal)) {
            schema.set(NODE_PROPERTIES, createProperties(type));
        }
        return schema;
    }

    private ObjectNode createProperties(Class<?> type) {
        ObjectNode properties = objectMapper.createObjectNode();
        Field[] fields = type.getDeclaredFields();
        for (Field field : fields) {
            if (!Modifier.isStatic(field.getModifiers()) && !Modifier.isTransient(field.getModifiers())) {
                properties.set(field.getName(), createSchema(field.getType(), field.getName()));
            }
        }
        return properties;
    }

    private String parseType(Class<?> type) {
        Objects.requireNonNull(type, "type must not be null");
        if (boolean.class.equals(type) || Boolean.class.equals(type)) {
            return TYPE_VAL_BOOLEAN;
        } else if (byte.class.equals(type) || Byte.class.equals(type) ||
                short.class.equals(type) || Short.class.equals(type) ||
                int.class.equals(type) || Integer.class.equals(type) ||
                long.class.equals(type) || Long.class.equals(type)) {
            return TYPE_VAL_INTEGER;
        } else if (float.class.equals(type) || Float.class.equals(type) ||
                double.class.equals(type) || Double.class.equals(type)) {
            return TYPE_VAL_NUMERIC;
        } else if (String.class.equals(type)) {
            return TYPE_VAL_STRING;
        } else if (type.isArray()) {
            return TYPE_VAL_ARRAY;
        }
        return TYPE_VAL_OBJECT;
    }

}
