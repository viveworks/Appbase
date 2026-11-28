package com.example.appbase.quickstart.jetty;

import com.viveworks.appbase.service.web.endpoint.jaxrs.appbase.ConfigurationJaxrsResource;
import com.viveworks.appbase.service.web.endpoint.jaxrs.appbase.CoreJaxrsResource;
import com.viveworks.appbase.service.web.endpoint.jaxrs.appbase.ExceptionJaxrsResource;
import com.viveworks.appbase.service.web.endpoint.jaxrs.appbase.JmxJaxrsResource;
import com.viveworks.appbase.service.web.endpoint.jaxrs.appbase.LogJaxrsResource;
import com.viveworks.appbase.service.web.endpoint.jaxrs.appbase.MediatorJaxrsResource;
import com.viveworks.appbase.service.web.endpoint.jaxrs.appbase.MetricJaxrsResource;
import com.viveworks.appbase.service.web.endpoint.jaxrs.appbase.ServiceRegistryJaxrsResource;
import com.viveworks.appbase.service.web.endpoint.jaxrs.appbase.StatusJaxrsResource;
import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import io.swagger.v3.oas.integration.OpenApiConfigurationException;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.integration.api.OpenAPIConfiguration;
import io.swagger.v3.oas.integration.api.OpenApiContext;
import io.swagger.v3.oas.integration.api.OpenApiScanner;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import jakarta.servlet.ServletConfig;
import jakarta.ws.rs.Path;
import org.glassfish.jersey.server.ResourceConfig;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Builds one combined OpenAPI document covering Appbase Core's own REST endpoints -- documented
 * the same way {@code com.viveworks.appbase.bootstrap.servlet.AppbaseCoreOpenApiRegistrar} does,
 * by scanning its JAX-RS-annotated resource classes for their OpenAPI shape only (those classes
 * are not registered as live resources here, matching that class's own javadoc: they supply
 * annotations only, Core's real endpoints are the servlets from withAppbaseCoreServlets()) --
 * together with this example's own live {@link TasksResource}, so both show up on one Swagger
 * UI page. This is example-only code; it does not modify Appbase itself.
 */
final class CombinedOpenApiSupport {

    private CombinedOpenApiSupport() {
    }

    static ResourceConfig register(final ResourceConfig resourceConfig) {
        final Set<String> resourceClasses = Set.of(
                ConfigurationJaxrsResource.class.getName(),
                CoreJaxrsResource.class.getName(),
                ExceptionJaxrsResource.class.getName(),
                JmxJaxrsResource.class.getName(),
                LogJaxrsResource.class.getName(),
                MediatorJaxrsResource.class.getName(),
                MetricJaxrsResource.class.getName(),
                ServiceRegistryJaxrsResource.class.getName(),
                StatusJaxrsResource.class.getName(),
                TasksResource.class.getName());
        try {
            final SwaggerConfiguration scanConfiguration = new SwaggerConfiguration()
                    .openAPI(new OpenAPI().info(new Info()
                            .title("Appbase Core + Tasks Quickstart (Jetty/Jersey) REST API")
                            .version("1.0.0")))
                    .prettyPrint(true)
                    .resourceClasses(resourceClasses);
            final OpenApiContext scanContext = new JaxrsOpenApiContextBuilder<>()
                    .ctxId("tasks-quickstart-jetty-openapi-scan-" + UUID.randomUUID())
                    .openApiConfiguration(scanConfiguration)
                    .buildContext(true);
            final OpenAPI model = scanContext.read();
            final SwaggerConfiguration servingConfiguration = new SwaggerConfiguration()
                    .openAPI(model)
                    .prettyPrint(true)
                    .readAllResources(false)
                    .scannerClass(FrozenModelScanner.class.getName())
                    .resourceClasses(Collections.emptySet());
            return resourceConfig.register(new CombinedOpenApiResource(servingConfiguration));
        } catch (final OpenApiConfigurationException exception) {
            throw new IllegalStateException("Failed to initialize combined OpenAPI document", exception);
        }
    }

    /** Prevents the serving context from rescanning the classpath; the precomputed model is frozen. */
    public static final class FrozenModelScanner implements OpenApiScanner {

        @Override
        public void setConfiguration(final OpenAPIConfiguration openApiConfiguration) {
        }

        @Override
        public Set<Class<?>> classes() {
            return Collections.emptySet();
        }

        @Override
        public Map<String, Object> resources() {
            return Collections.emptyMap();
        }
    }

    @Path("/openapi.{type:json|yaml}")
    public static final class CombinedOpenApiResource extends OpenApiResource {

        private final String contextId = "tasks-quickstart-jetty-openapi-serve-" + UUID.randomUUID();

        public CombinedOpenApiResource(final OpenAPIConfiguration configuration) {
            openApiConfiguration(configuration);
        }

        @Override
        protected String getContextId(final ServletConfig servletConfig) {
            return contextId;
        }
    }
}
