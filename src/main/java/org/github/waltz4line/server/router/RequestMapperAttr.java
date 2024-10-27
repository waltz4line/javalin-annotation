package org.github.waltz4line.server.router;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class RequestMapperAttr {

    private final String path;

    private final EndpointDocument endpointDocument;

    private RequestMapperAttr(String path, EndpointDocument endpointDocument) {
        this.path = path;
        this.endpointDocument = endpointDocument;
    }

    void setDescription(String description) {
        this.endpointDocument.description = description;
    }

    void addPathParameter(ParameterDescription parameterDescription) {
        this.endpointDocument.pathParameters.add(parameterDescription);
    }

    void addPathParameters(Collection<ParameterDescription> parameterDescriptions) {
        this.endpointDocument.pathParameters.addAll(parameterDescriptions);
    }

    void addQueryParameter(ParameterDescription parameterDescription) {
        this.endpointDocument.queryParameters.add(parameterDescription);
    }

    void addQueryParameters(Collection<ParameterDescription> parameterDescriptions) {
        this.endpointDocument.queryParameters.addAll(parameterDescriptions);
    }

    void setRequestBody(Class<?> requestBody) {
        this.endpointDocument.requestBody = requestBody;
    }

    void setResponseBody(Class<?> responseBody) {
        this.endpointDocument.responseBody = responseBody;
    }

    void addErrorDescription(ErrorDescription errorDescription) {
        this.endpointDocument.errorDescriptions.add(errorDescription);
    }

    void addErrorDescriptions(Collection<ErrorDescription> errorDescriptions) {
        this.endpointDocument.errorDescriptions.addAll(errorDescriptions);
    }

    public String getPath() {
        return path;
    }

    public EndpointDocument getEndpointDocument() {
        return endpointDocument;
    }

    public record ParameterDescription(String name, Class<?> type, String description) {
    }

    public record ErrorDescription(int statusCode, String errorCode, Class<?> errorResponse) {
    }

    public static final class EndpointDocument {
        private final String tag;

        private final String endpointId;

        private String description;

        private final List<ParameterDescription> pathParameters = new ArrayList<>();

        private final List<ParameterDescription> queryParameters = new ArrayList<>();

        private Class<?> requestBody;

        private Class<?> responseBody;

        private final List<ErrorDescription> errorDescriptions = new ArrayList<>();

        public EndpointDocument(String tag, String endpointId) {
            this.tag = tag;
            this.endpointId = endpointId;
        }

        public String getTag() {
            return tag;
        }

        public String getEndpointId() {
            return endpointId;
        }

        public String getDescription() {
            return description;
        }

        public List<ParameterDescription> getPathParameters() {
            return pathParameters;
        }

        public List<ParameterDescription> getQueryParameters() {
            return queryParameters;
        }

        public Class<?> getRequestBody() {
            return requestBody;
        }

        public Class<?> getResponseBody() {
            return responseBody;
        }

        public List<ErrorDescription> getErrorDescriptions() {
            return errorDescriptions;
        }
    }

    public static RequestMapperAttr of(String path, String tag, String endpointId) {
        return new RequestMapperAttr(path, new EndpointDocument(tag, endpointId));
    }
}