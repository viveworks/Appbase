/*
 * Copyright 2019 The original author or authors of Appbase
 *
 * This project includes software developed by the author or authors of
 * Appbase (https://github.com/viveworks/Appbase).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.viveworks.appbase.bootstrap.servlet;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.web.endpoint.jaxrs.appbase.ConfigurationJaxrsResource;
import com.viveworks.appbase.service.web.endpoint.jaxrs.appbase.CoreJaxrsResource;
import com.viveworks.appbase.service.web.endpoint.jaxrs.appbase.ExceptionJaxrsResource;
import com.viveworks.appbase.service.web.endpoint.jaxrs.appbase.JmxJaxrsResource;
import com.viveworks.appbase.service.web.endpoint.jaxrs.appbase.LogJaxrsResource;
import com.viveworks.appbase.service.web.endpoint.jaxrs.appbase.MediatorJaxrsResource;
import com.viveworks.appbase.service.web.endpoint.jaxrs.appbase.MetricJaxrsResource;
import com.viveworks.appbase.service.web.endpoint.jaxrs.appbase.ServiceRegistryJaxrsResource;
import com.viveworks.appbase.service.web.endpoint.jaxrs.appbase.StatusJaxrsResource;
import com.viveworks.appbase.service.web.endpoint.servlet.appbase.ConfigurationServlet;
import com.viveworks.appbase.service.web.endpoint.servlet.appbase.CoreServlet;
import com.viveworks.appbase.service.web.endpoint.servlet.appbase.ExceptionServlet;
import com.viveworks.appbase.service.web.endpoint.servlet.appbase.JmxServlet;
import com.viveworks.appbase.service.web.endpoint.servlet.appbase.LogServlet;
import com.viveworks.appbase.service.web.endpoint.servlet.appbase.MediatorServlet;
import com.viveworks.appbase.service.web.endpoint.servlet.appbase.MetricServlet;
import com.viveworks.appbase.service.web.endpoint.servlet.appbase.ServiceRegistryServlet;
import com.viveworks.appbase.service.web.endpoint.servlet.appbase.StatusServlet;
import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import io.swagger.v3.oas.integration.OpenApiConfigurationException;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.integration.api.OpenApiContext;
import io.swagger.v3.oas.integration.api.OpenApiScanner;
import io.swagger.v3.oas.integration.api.OpenAPIConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import jakarta.servlet.ServletConfig;
import jakarta.ws.rs.Path;
import org.glassfish.jersey.server.ResourceConfig;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Registers the OpenAPI document for the nine standard Appbase core endpoints.
 *
 * <p>This is deliberately tied to the core-servlet bootstrap and does not include service-level
 * resources such as health, user-account, or support-message endpoints. The JAX-RS classes supply
 * OpenAPI annotations only; registering this document does not add a second live implementation of
 * the core endpoints alongside their servlet mappings.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public final class AppbaseCoreOpenApiRegistrar {

    private static final List<CoreEndpoint> CORE_ENDPOINTS = List.of(
        new CoreEndpoint(ConfigurationServlet.class, ConfigurationJaxrsResource.class,
            AppbaseServiceConstants.DEFAULT_CONFIGURATION_RESTAPI_PATH),
        new CoreEndpoint(CoreServlet.class, CoreJaxrsResource.class,
            AppbaseServiceConstants.DEFAULT_CORE_RESTAPI_PATH),
        new CoreEndpoint(ExceptionServlet.class, ExceptionJaxrsResource.class,
            AppbaseServiceConstants.DEFAULT_EXCEPTION_RESTAPI_PATH),
        new CoreEndpoint(JmxServlet.class, JmxJaxrsResource.class,
            AppbaseServiceConstants.DEFAULT_JMX_RESTAPI_PATH),
        new CoreEndpoint(LogServlet.class, LogJaxrsResource.class,
            AppbaseServiceConstants.DEFAULT_LOG_RESTAPI_PATH),
        new CoreEndpoint(MediatorServlet.class, MediatorJaxrsResource.class,
            AppbaseServiceConstants.DEFAULT_MEDIATOR_RESTAPI_PATH),
        new CoreEndpoint(MetricServlet.class, MetricJaxrsResource.class,
            AppbaseServiceConstants.DEFAULT_METRIC_RESTAPI_PATH),
        new CoreEndpoint(ServiceRegistryServlet.class, ServiceRegistryJaxrsResource.class,
            AppbaseServiceConstants.DEFAULT_SERVICE_REGISTRY_RESTAPI_PATH),
        new CoreEndpoint(StatusServlet.class, StatusJaxrsResource.class,
            AppbaseServiceConstants.DEFAULT_STATUS_RESTAPI_PATH));

    private AppbaseCoreOpenApiRegistrar() {}

    /**
     * Adds {@link OpenApiResource} to {@code resourceConfig} and initializes its core-only model.
     * Existing caller registrations are preserved.
     *
     * @param resourceConfig the Jersey configuration used by the servlet container
     * @return the same configuration instance
     */
    public static synchronized ResourceConfig register(final ResourceConfig resourceConfig) {
        return register(resourceConfig, Collections.emptySet(), Collections.emptyMap());
    }

    /**
     * Adds the core document for the effective servlet selection and paths.
     *
     * @param resourceConfig the Jersey configuration used by the servlet container
     * @param excludes core servlet types omitted from the deployment
     * @param pathOverrides effective paths keyed by core servlet type
     * @return the same configuration instance
     */
    public static synchronized ResourceConfig register(final ResourceConfig resourceConfig,
                                                        final Set<Class<?>> excludes,
                                                        final Map<Class<?>, String> pathOverrides) {
        final ResourceConfig config = Objects.requireNonNull(resourceConfig, "resourceConfig");
        final Set<Class<?>> effectiveExcludes = Objects.requireNonNull(excludes, "excludes");
        final Map<Class<?>, String> effectiveOverrides =
            Objects.requireNonNull(pathOverrides, "pathOverrides");
        final Set<String> resourceClasses = CORE_ENDPOINTS.stream()
            .filter(endpoint -> !effectiveExcludes.contains(endpoint.servletClass()))
            .map(endpoint -> endpoint.resourceClass().getName())
            .collect(Collectors.toUnmodifiableSet());
        try {
            final SwaggerConfiguration scanConfiguration = new SwaggerConfiguration()
                    .openAPI(new OpenAPI().info(new Info()
                        .title("Appbase Core REST API")
                        .version("1.0.0")))
                    .prettyPrint(true)
                    .resourceClasses(resourceClasses);
            final OpenApiContext scanContext = new JaxrsOpenApiContextBuilder<>()
                .ctxId("appbase-core-bootstrap-scan-" + UUID.randomUUID())
                .openApiConfiguration(scanConfiguration)
                .buildContext(true);
            final OpenAPI effectiveModel = scanContext.read();
            rewriteEffectivePaths(effectiveModel, effectiveExcludes, effectiveOverrides);
            final SwaggerConfiguration servingConfiguration = new SwaggerConfiguration()
                .openAPI(effectiveModel)
                .prettyPrint(true)
                .readAllResources(false)
                .scannerClass(FrozenModelScanner.class.getName())
                .resourceClasses(Collections.emptySet());
            return config.register(new CoreOpenApiResource(servingConfiguration));
        } catch (OpenApiConfigurationException e) {
            throw new AppbaseRuntimeException("Failed to initialize Appbase core OpenAPI", e);
        }
    }

    private static void rewriteEffectivePaths(final OpenAPI model,
                                              final Set<Class<?>> excludes,
                                              final Map<Class<?>, String> pathOverrides) {
        if (model.getPaths() == null) {
            return;
        }
        final Paths rewritten = new Paths();
        for (Map.Entry<String, PathItem> path : model.getPaths().entrySet()) {
            String effectivePath = path.getKey();
            for (CoreEndpoint endpoint : CORE_ENDPOINTS) {
                if (!excludes.contains(endpoint.servletClass())
                    && isEndpointPath(effectivePath, endpoint.defaultPath())) {
                    effectivePath = pathOverrides.getOrDefault(
                        endpoint.servletClass(), endpoint.defaultPath())
                        + effectivePath.substring(endpoint.defaultPath().length());
                    break;
                }
            }
            rewritten.addPathItem(effectivePath, path.getValue());
        }
        model.setPaths(rewritten);
    }

    private static boolean isEndpointPath(final String path, final String endpointPath) {
        return path.equals(endpointPath) || path.startsWith(endpointPath + "/");
    }

    /** Prevents the serving context from adding classpath resources to the frozen core model. */
    public static final class FrozenModelScanner implements OpenApiScanner {

        @Override
        public void setConfiguration(final OpenAPIConfiguration openApiConfiguration) {
            // The precomputed model is the complete source of truth for this context.
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

    /** Isolates each servlet container's frozen model from Swagger's global context registry. */
    @Path("/openapi.{type:json|yaml}")
    public static final class CoreOpenApiResource extends OpenApiResource {

        private final String contextId = "appbase-core-bootstrap-serve-" + UUID.randomUUID();

        CoreOpenApiResource(final OpenAPIConfiguration configuration) {
            openApiConfiguration(configuration);
        }

        @Override
        protected String getContextId(final ServletConfig servletConfig) {
            return contextId;
        }
    }

    private record CoreEndpoint(Class<?> servletClass, Class<?> resourceClass, String defaultPath) {}
}
