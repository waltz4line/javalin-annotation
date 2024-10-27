package org.github.waltz4line.server.javalin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JavalinServerConfig(@JsonProperty(required = true)
                                  int port,
                                  JavalinRouter router,
                                  JavalinCors cors,
                                  @JsonProperty(defaultValue = "false")
                                  boolean enableOpenApi) {

    public record JavalinRouter(@JsonProperty(defaultValue = "/")
                                String contextPath,
                                @JsonProperty(defaultValue = "true")
                                boolean ignoreTrailingSlash,
                                @JsonProperty(defaultValue = "true")
                                boolean treatMultipleSlashesAsSingleSlash,
                                @JsonProperty(defaultValue = "false")
                                boolean caseInsensitiveRoutes,
                                @JsonProperty(defaultValue = "false")
                                boolean enableRouteOverview) {
    }

    public record JavalinCors(boolean enableCors,
                              List<String> allowHosts,
                              String exposedHeader) {
    }
}