package org.github.waltz4line.server.javalin.openapi;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.javalin.http.ContentType;
import io.javalin.openapi.plugin.DefinitionProcessor;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.github.waltz4line.server.router.RequestMapperAttr;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.*;

public class DynamicDefinitionProcessor implements DefinitionProcessor {

    private static final String PARAM_TYPE_PATH = "path";

    private static final String PARAM_TYPE_QUERY = "query";

    private final Map<String, List<RequestMapper>> requestMappers = new HashMap<>(Byte.MAX_VALUE);

    @NotNull
    @Override
    public String process(@NotNull ObjectNode objectNode) {
        ObjectNode paths = (ObjectNode) objectNode.get("paths");
        for (Map.Entry<String, List<RequestMapper>> entry : requestMappers.entrySet()) {
            String path = entry.getKey();
            List<RequestMapper> docMappers = entry.getValue();
            ObjectNode pathNode = paths.putObject(path);
            for (RequestMapper docMapper : docMappers) {
                createEndpointDocument(pathNode, docMapper);
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
        ObjectNode methodNode = pathNode.putObject(docMapper.methodType());
        methodNode.put("summary", document.getDescription());
        methodNode.putArray("tags").add(document.getTag());
        methodNode.put("operationId", document.getEndpointId());
        addParameter(methodNode, PARAM_TYPE_PATH, document.getPathParameters());
        addParameter(methodNode, PARAM_TYPE_QUERY, document.getQueryParameters());
        addRequestBody(methodNode, document.getRequestBody());
        addResponseBody(methodNode, document.getResponseBody(), HttpStatus.OK_200);



        // GET /users/:userId 엔드포인트 정의
//        ObjectNode getUserPath = paths.putObject("/users/{userId}");
//        ObjectNode getUserMethod = getUserPath.putObject("get");
//        getUserMethod.put("summary", "Get a user by ID");
//        getUserMethod.put("operationId", "getUserById");

//        // pathParam 설정
//        ObjectNode parameters = getUserMethod.putArray("parameters").addObject();
//        parameters.put("name", "userId");
//        parameters.put("in", "path");
//        parameters.put("required", true);
//        parameters.put("schema", createSchema("integer", null, "User ID"));
//
//        // queryParam 설정
//        ObjectNode queryParam = getUserMethod.putArray("parameters").addObject();
//        queryParam.put("name", "detail");
//        queryParam.put("in", "query");
//        queryParam.put("required", false);
//        queryParam.put("schema", createSchema("string", null, "Detail level"));

//        // 응답 설정
//        ObjectNode responses = getUserMethod.putObject("responses").putObject("200");
//        responses.put("description", "A user object");
//        ObjectNode content = responses.putObject("content").putObject("application/json").putObject("schema");
//        content.put("type", "object");
//        content.putObject("properties").set("id", createSchema("integer", null, "User ID"));
//
//        // POST /users 엔드포인트 정의
//        ObjectNode postUserPath = paths.putObject("/users");
//        ObjectNode postUserMethod = postUserPath.putObject("post");
//        postUserMethod.put("summary", "Create a new user");
//        postUserMethod.put("operationId", "createUser");
//
//        // requestBody 설정
//        ObjectNode requestBody = postUserMethod.putObject("requestBody").putObject("content").putObject("application/json");
//        requestBody.putObject("schema").put("type", "object").putObject("properties").set("name", createSchema("string", null, "User name"));
//
//        // 응답 설정
//        ObjectNode postResponses = postUserMethod.putObject("responses").putObject("201");
//        postResponses.put("description", "User created successfully");

    }

    private void addParameter(ObjectNode methodNode, String paramType, List<RequestMapperAttr.ParameterDescription> parameterDescriptions) {
        if (parameterDescriptions.isEmpty()) {
            return;
        }

        for (RequestMapperAttr.ParameterDescription parameterDescription : parameterDescriptions) {
            ObjectNode parameters = methodNode.putArray("parameters").addObject();
            parameters.put("name", parameterDescription.name());
            parameters.put("in", paramType);
            parameters.put("required", PARAM_TYPE_PATH.equals(paramType));
            addSchema(parameters, parameterDescription.type(), parameterDescription.description());
        }
    }

    private void addRequestBody(ObjectNode methodNode, Class<?> bodyClass) {
        if (bodyClass == null) {
            return;
        }

        ObjectNode requestBody = methodNode.putObject("requestBody");
        requestBody.put("required", true);
        ObjectNode mediaType = requestBody.putObject("content").putObject(ContentType.JSON);
        addSchema(mediaType, bodyClass, null);
        addExample(mediaType, bodyClass);


    }

    private void addResponseBody(ObjectNode methodNode, Class<?> bodyClass, int statusCode) {
//        ObjectNode responses = methodNode.putObject("responses").putObject(String.valueOf(statusCode));
//        ObjectNode content = responses.putObject("content")
//                .putObject("application/json")
//                .putObject("schema");
//        content.put("type", "object");
//        content.putObject("properties").set("id", createSchema("integer", null, "User ID"));

    }

    private void addSchema(ObjectNode parentNode, Class<?> type, String description) {
        if (type == null) {
            return;
        }

        ObjectNode schema  = parentNode.putObject("schema");
        schema.put("type", type.getSimpleName());
        if (StringUtils.isNotEmpty(description)) {
            schema.put("description", description);
        }

        if (type.isPrimitive() || type.isArray() || type.getPackageName().startsWith("java.lang")) {
            return;
        }
        addProperties(schema, type);
    }

    private void addProperties(ObjectNode parentNode, Class<?> type) {
        if (type == null) {
            return;
        }
        Field[] fields = type.getDeclaredFields();
        if (fields.length == 0) {
            return;
        }

        ObjectNode properties = parentNode.putObject("properties");
        for (Field field : fields) {

        }

    }

    private void addExample(ObjectNode parentNode, Class<?> type) {
//        mediaTypeNode.putObject("schema").put("type", "object").putObject("properties").set("name", createSchema("string", null, "User name"));
    }


    private ObjectNode createSchema(Class<?> type, String description) {
        ObjectNode schema = new ObjectNode(JsonNodeFactory.instance);
        if (type != null) {
            schema.put("type", type.getSimpleName());
        }
        schema.put("description", description);
        return schema;
    }

}
