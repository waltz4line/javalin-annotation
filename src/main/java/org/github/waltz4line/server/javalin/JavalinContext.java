package org.github.waltz4line.server.javalin;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.http.HttpStatus;
import io.javalin.http.RequestLogger;
import io.javalin.json.JavalinJackson;
import io.javalin.openapi.plugin.OpenApiPlugin;
import io.javalin.openapi.plugin.redoc.ReDocPlugin;
import org.github.waltz4line.server.WebServerContext;
import org.github.waltz4line.server.javalin.error.JettyErrorHandler;
import org.github.waltz4line.server.javalin.error.ServerInitializeException;
import org.github.waltz4line.server.javalin.event.AuthenticationHandler;
import org.github.waltz4line.server.javalin.event.LifecycleEvent;
import org.github.waltz4line.server.javalin.openapi.DynamicDefinitionProcessor;
import org.github.waltz4line.server.router.RouterAnnotationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class JavalinContext implements WebServerContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(JavalinContext.class);

    private final AtomicReference<Javalin> app = new AtomicReference<>();

    private final CountDownLatch shutdownLatch = new CountDownLatch(1);

    private final JavalinContextConfig javalinContextConfig;

    private final LifecycleEvent preparingEvent;

    private final LifecycleEvent serverStoppedEvent;

    private final List<Object> registeredRouterInstances;

    private final RequestLogger requestLogger;

    private final AuthenticationHandler authenticationHandler;

    private final DynamicDefinitionProcessor definitionProcessor;

    private JavalinContext(JavalinContextConfig javalinContextConfig,
                           LifecycleEvent preparingEvent,
                           LifecycleEvent serverStoppedEvent,
                           List<Object> registeredRouterInstances,
                           RequestLogger requestLogger,
                           AuthenticationHandler authenticationHandler) {
        this.javalinContextConfig = javalinContextConfig;
        this.preparingEvent = preparingEvent;
        this.serverStoppedEvent = serverStoppedEvent;
        this.registeredRouterInstances = registeredRouterInstances;
        this.requestLogger = requestLogger;
        this.authenticationHandler = authenticationHandler;
        this.definitionProcessor = javalinContextConfig.enableOpenApi() ? new DynamicDefinitionProcessor() : null;
    }

    @Override
    public void initialize() throws ServerInitializeException {
        try {
            if (preparingEvent != null) {
                preparingEvent.handleEvent();
            }
        } catch (Exception e) {
            throw new ServerInitializeException("Failed to preparing context ... ", e);
        }

        Javalin javalinApp = Javalin.create(config -> {
            configureRequestLogger(config);
            configureCors(config);
            configureRoutes(config);
            config.jetty.modifyServer(server -> server.setErrorHandler(new JettyErrorHandler()));
            config.jsonMapper(new JavalinJackson()
                    .updateMapper(mapper -> mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)));
            configureOpenApi(config);
        });

        javalinApp.before(authenticationHandler::handle);
        javalinApp.after(ctx -> {
            ctx.header("Server", " ");
            ctx.header("X-Content-Type-Options", "nosniff");
            ctx.header("X-Frame-Options", "DENY");
            ctx.header("X-XSS-Protection", "1; mode=block");
        });
        javalinApp.error(HttpStatus.NOT_FOUND, ctx -> {});
        RouterAnnotationHandler.handle(registeredRouterInstances,
                new JavalinRouterMapper(javalinApp, definitionProcessor));

        app.set(javalinApp);
        registerShutdownHook();
    }

    @Override
    public void start() throws ServerInitializeException {
        if (app.get() == null) {
            throw new ServerInitializeException("Javalin context has not been initialized");
        }

        app.get().start(javalinContextConfig.port());

        try {
            shutdownLatch.await();
        } catch (InterruptedException ignored) {
            // no op
        }
    }

    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            shutdownLatch.countDown();
            try {
                if (app.get() != null) {
                    app.get().stop();
                }

                if (serverStoppedEvent != null) {
                    serverStoppedEvent.handleEvent();
                }
            } catch (Exception e) {
                LOGGER.warn("Error occurred while shutting down Javalin context.", e);
            }
        }));
    }

    private void configureOpenApi(JavalinConfig config) {
        if (javalinContextConfig.enableOpenApi()) {
            config.registerPlugin(new OpenApiPlugin(pluginConfig -> {
                pluginConfig.withDefinitionConfiguration((version, definition) -> {
                    definition.withInfo(info -> info.setTitle("Javalin OpenAPI"));
                    definition.withDefinitionProcessor(definitionProcessor);
                });
            }));
            config.registerPlugin(new ReDocPlugin());
        }
    }

    private void configureRequestLogger(JavalinConfig config) {
        if (requestLogger != null) {
            config.requestLogger.http(requestLogger);
        }
    }

    private void configureCors(JavalinConfig config) {
        if (javalinContextConfig.cors() == null || !javalinContextConfig.cors().enableCors()) {
            return;
        }

        config.bundledPlugins.enableCors(cors -> {
            String exposeHeader = javalinContextConfig.cors().exposedHeader();
            List<String> allHosts = javalinContextConfig.cors().allowHosts();
            cors.addRule(it -> {
                if (allHosts == null || allHosts.isEmpty()) {
                    it.anyHost();
                } else {
                    String firstHost = allHosts.removeFirst();
                    if (allHosts.isEmpty()) {
                        it.allowHost(firstHost);
                    } else {
                        String[] otherHosts = allHosts.toArray(String[]::new);
                        it.allowHost(firstHost, otherHosts);
                    }
                }
                if (exposeHeader != null) {
                    it.exposeHeader(exposeHeader);
                }
            });
        });
    }

    private void configureRoutes(JavalinConfig config) {
        JavalinContextConfig.JavalinRouter router = javalinContextConfig.router();
        if (router == null) {
            return;
        }
        config.router.contextPath = router.contextPath();
        config.router.ignoreTrailingSlashes = router.ignoreTrailingSlash();
        config.router.treatMultipleSlashesAsSingleSlash = router.treatMultipleSlashesAsSingleSlash();
        config.router.caseInsensitiveRoutes = router.caseInsensitiveRoutes();
    }

    public static class Builder {

        private final JavalinContextConfig config;

        private LifecycleEvent preparingEvent;

        private LifecycleEvent serverStoppedEvent;

        private final List<Object> registeredRouterInstances = new ArrayList<>();

        private RequestLogger requestLogger;

        private AuthenticationHandler authenticationHandler = AuthenticationHandler.NOOP;

        public Builder(JavalinContextConfig config) {
            this.config = config;
        }

        public void onPreparingContext(LifecycleEvent preparingEvent) {
            Objects.requireNonNull(preparingEvent, "preparingEvent must not be null");
            this.preparingEvent = preparingEvent;
        }

        public void onServerStopped(LifecycleEvent serverStoppedEvent) {
            Objects.requireNonNull(serverStoppedEvent, "serverStoppedEvent must not be null");
            this.serverStoppedEvent = serverStoppedEvent;
        }

        public void registerRouter(Object router) {
            Objects.requireNonNull(router, "router must not be null");
            registeredRouterInstances.add(router);
        }

        public void registerRouters(Object... routers) {
            Objects.requireNonNull(routers, "routers must not be null");
            registeredRouterInstances.addAll(Arrays.asList(routers));
        }

        public void registerRequestLogger(RequestLogger requestLogger) {
            Objects.requireNonNull(requestLogger, "requestLogger must not be null");
            this.requestLogger = requestLogger;
        }

        public void registerAuthenticationHandler(AuthenticationHandler authenticationHandler) {
            Objects.requireNonNull(authenticationHandler, "authenticationHandler must not be null");
            this.authenticationHandler = authenticationHandler;
        }

        public WebServerContext build() {
            return new JavalinContext(config, preparingEvent, serverStoppedEvent, registeredRouterInstances, requestLogger, authenticationHandler);
        }

    }
}
