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

package com.viveworks.appbase.service.web.endpoint.jaxrs.appbase;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.configuration.ConfigurationValue;
import com.viveworks.appbase.core.service.configuration.PropertyCastException;
import com.viveworks.appbase.core.service.configuration.jmx.JmxConfigurationKey;
import com.viveworks.appbase.core.service.configuration.jmx.JmxConfigurationService;
import com.viveworks.appbase.core.service.configuration.jmx.JmxConfigurationServiceMXBean;
import com.viveworks.appbase.core.service.configuration.util.ConfigurationsJson;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.appbase.data.SetOfStringsValue;
import com.viveworks.appbase.service.web.endpoint.jaxrs.AbstractAppbaseJaxrsEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.ServletContext;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.metrics.annotation.Timed;

import javax.management.MBeanException;
import java.net.URI;
import java.util.Set;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Path(AppbaseServiceConstants.DEFAULT_CONFIGURATION_RESTAPI_PATH)
public class ConfigurationJaxrsResource extends AbstractAppbaseJaxrsEntity {
    @GET
    @Path("/properties/{propertyKey}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Read property")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = ConfigurationValue.class)),
            responseCode = "200")
    @Timed
    public Response getProperty(
            @NotEmpty
            @PathParam("propertyKey")
            final String propertyKey,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        try {
            final String value = jmxConfigurationServiceMXBean(servletContext).getString(propertyKey);
            if (value == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            return Response.ok().entity(new ConfigurationValue<>(value)).build();
        } catch (PropertyCastException e) {
            throw new BadRequestException("The value of property key cannot be converted to string", e);
        }
    }

    @POST
    @Path("/properties/{propertyKey}/{propertyValue}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Set property")
    @ApiResponse(responseCode = "201")
    @Timed
    public Response setProperty(
            @NotEmpty
            @PathParam("propertyKey")
            final String propertyKey,
            @NotNull
            @PathParam("propertyValue")
            final String propertyValue,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        jmxConfigurationServiceMXBean(servletContext).putString(propertyKey, propertyValue);
        return Response.created(URI.create("/properties/" + propertyKey)).build();
    }

    @DELETE
    @Path("/properties/{propertyKey}")
    @Operation(summary = "Remove property")
    @ApiResponse(responseCode = "204")
    @Timed
    public Response removeProperty(
            @NotEmpty
            @PathParam("propertyKey")
            final String propertyKey,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        jmxConfigurationServiceMXBean(servletContext).removeString(propertyKey);
        return Response.noContent().build();
    }

    @HEAD
    @Path("/properties/{propertyKey}")
    @Operation(summary = "Check if a property exists")
    @ApiResponse(
            description = "Returns 200 if the property exists, otherwise 404",
            responseCode = "200")
    @Timed
    public Response containsProperty(
            @NotEmpty
            @PathParam("propertyKey")
            final String propertyKey,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        if (jmxConfigurationServiceMXBean(servletContext).containsProperty(propertyKey)) {
            return Response.ok().build();
        }

        return Response.status(Response.Status.NOT_FOUND).build();
    }

    @GET
    @Path("/typedConfigurations/{typeName}")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(summary = "Get typed configuration")
    @ApiResponse(description = "YAML on success, 404 if not found", responseCode = "200")
    @Timed
    public Response getTypedConfigurationYaml(
            @NotEmpty
            @PathParam("typeName")
            final String typeName,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        try {
            final String typedConfigurationYaml = jmxConfigurationServiceMXBean(servletContext)
                    .getTypedConfigurationYaml(typeName);
            if (typedConfigurationYaml == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            return Response.ok(typedConfigurationYaml).build();
        } catch (MBeanException e) {
            final Exception targetException = e.getTargetException();
            if (!ClassNotFoundException.class.isAssignableFrom(targetException.getClass())) {
                throw new InternalServerErrorException(e);
            }
        }

        return Response.status(Response.Status.NOT_FOUND).build();
    }


    @POST
    @Path("/typedConfigurations/{typeName}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Operation(summary = "Set typed configuration")
    @ApiResponse(responseCode = "201")
    @Timed
    public Response setTypedConfigurationYaml(
            @NotEmpty
            @PathParam("typeName")
            final String typeName,
            @NotNull
            final String yaml,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        try {
            jmxConfigurationServiceMXBean(servletContext).putTypedConfigurationYaml(yaml, typeName);
            return Response
                    .created(URI.create("/typedConfiguration/" + typeName))
                    .build();
        } catch (MBeanException e) {
            throw new InternalServerErrorException(e);
        }
    }

    @DELETE
    @Path("/typedConfigurations/{typeName}")
    @Operation(summary = "Set typed configuration")
    @ApiResponse(responseCode = "204")
    @Timed
    public Response removeTypedConfigurationYaml(
            @NotEmpty
            @PathParam("typeName")
            final String typeName,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        try {
            jmxConfigurationServiceMXBean(servletContext).removeTypedConfigurationYaml(typeName);
            return Response.noContent().build();
        } catch (MBeanException e) {
            throw new InternalServerErrorException(e);
        }
    }

    @HEAD
    @Path("/typedConfigurations/{typeName}")
    @Operation(summary = "Check if a property exists in the configuration service")
    @ApiResponse(
            description = "200 if the typed configuration exists, otherwise 404",
            responseCode = "200")
    @Timed
    public Response containsTypedConfiguration(
            @NotEmpty
            @PathParam("typeName")
            final String typeName,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        try {
            if (jmxConfigurationServiceMXBean(servletContext).containsTypedConfiguration(typeName)) {
                return Response.ok().build();
            }
        } catch (MBeanException e) {
            final Exception targetException = e.getTargetException();
            if (!ClassNotFoundException.class.isAssignableFrom(targetException.getClass())) {
                throw new InternalServerErrorException(e);
            }
        }

        return Response.status(Response.Status.NOT_FOUND).build();
    }

    @DELETE
    @Path("/propertyDeserializers/configurationTypes/{propertyKey}")
    @Operation(summary = "Unregister property deserializer and configuration types by property key")
    @ApiResponse(responseCode = "204")
    @Timed
    public Response unregisterPropertyDeserializerAndConfigurationTypesByPropertyKey(
            @NotEmpty
            @PathParam("propertyKey")
            final String propertyKey,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        jmxConfigurationServiceMXBean(servletContext)
                .unregisterPropertyDeserializerAndConfigurationTypesByPropertyKey(propertyKey);
        return Response.noContent().build();
    }

    @DELETE
    @Path("/propertyDeserializers/configurationTypes/")
    @Operation(summary = "Unregister property deserializer and configuration types map")
    @ApiResponse(responseCode = "204")
    @Timed
    public Response clearPropertyDeserializerAndConfigurationTypesByPropertyKeyMap(
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        jmxConfigurationServiceMXBean(servletContext)
                .clearPropertyDeserializerAndConfigurationTypesByPropertyKeyMap();
        return Response.noContent().build();
    }

    @DELETE
    @Path("/propertyDeserializers/{typeName}")
    @Operation(summary = "Unregister property deserializer by type name")
    @ApiResponse(responseCode = "204")
    @Timed
    public Response unregisterPropertyDeserializer(
            @NotEmpty
            @PathParam("typeName")
            final String typeName,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        try {
            jmxConfigurationServiceMXBean(servletContext).unregisterPropertyDeserializer(typeName);
            return Response.noContent().build();
        } catch (MBeanException e) {
            throw new InternalServerErrorException(e);
        }
    }

    @DELETE
    @Path("/propertyDeserializers")
    @Operation(summary = "Clear all property deserializers")
    @ApiResponse(responseCode = "204")
    @Timed
    public Response clearPropertyDeserializers(
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        jmxConfigurationServiceMXBean(servletContext).clearPropertyDeserializers();
        return Response.noContent().build();
    }

    @GET
    @Path("/nonValidatingTypes")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get non validating type names")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = SetOfStringsValue.class)),
            responseCode = "200")
    @Timed
    public Response getNonValidatingTypes(
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        final Set<String> nonValidatingTypeNames =
                jmxConfigurationServiceMXBean(servletContext).getNonValidatingTypeNames();
        final SetOfStringsValue setOfStringsValue = new SetOfStringsValue(nonValidatingTypeNames);
        return Response.ok(setOfStringsValue).build();
    }

    @DELETE
    @Path("/nonValidatingTypes/{typeName}")
    @Operation(summary = "Unregister non validating type")
    @ApiResponse(responseCode = "204")
    @Timed
    public Response unregisterNonValidatingType(
            @NotEmpty
            @PathParam("typeName")
            final String typeName,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        try {
            jmxConfigurationServiceMXBean(servletContext).unregisterNonValidatingTypeName(typeName);
            return Response.noContent().build();
        } catch (MBeanException e) {
            throw new InternalServerErrorException(e);
        }
    }

    @DELETE
    @Path("/nonValidatingTypes")
    @Operation(summary = "Clear non validating types")
    @ApiResponse(responseCode = "204")
    @Timed
    public Response clearNonValidatingTypes(
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        jmxConfigurationServiceMXBean(servletContext).clearNonValidatingTypeNames();
        return Response.noContent().build();
    }

    @GET
    @Path("/securePropertyKeys")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get secure property keys")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = SetOfStringsValue.class)),
            responseCode = "200")
    @Timed
    public Response getSecurePropertyKeys(
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        final Set<String> securePropertyKeys =
                jmxConfigurationServiceMXBean(servletContext).getSecurePropertyKeys();
        final SetOfStringsValue setOfStringsValue = new SetOfStringsValue(securePropertyKeys);
        return Response.ok(setOfStringsValue).build();
    }

    @DELETE
    @Path("/securePropertyKeys/{propertyKey}")
    @Operation(summary = "Unregister secure property keys")
    @ApiResponse(responseCode = "204")
    @Timed
    public Response unregisterSecurePropertyKey(
            @NotEmpty
            @PathParam("propertyKey")
            final String propertyKey,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        jmxConfigurationServiceMXBean(servletContext).unregisterSecurePropertyKey(propertyKey);
        return Response.noContent().build();
    }

    @DELETE
    @Path("/securePropertyKeys")
    @Operation(summary = "Clear secure property keys")
    @ApiResponse(responseCode = "204")
    @Timed
    public Response clearSecurePropertyKeys(
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        jmxConfigurationServiceMXBean(servletContext).clearSecurePropertyKeys();
        return Response.noContent().build();
    }

    @GET
    @Path("/secureTypes")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get secure types")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = SetOfStringsValue.class)),
            responseCode = "200")
    @Timed
    public Response getSecureTypes(
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        final Set<String> secureTypeNames =
                jmxConfigurationServiceMXBean(servletContext).getSecureTypeNames();
        final SetOfStringsValue setOfStringsValue = new SetOfStringsValue(secureTypeNames);
        return Response.ok(setOfStringsValue).build();
    }

    @DELETE
    @Path("/secureTypes/{typeName}")
    @Operation(summary = "Unregister secure type")
    @ApiResponse(responseCode = "204")
    @Timed
    public Response unregisterSecureType(
            @NotEmpty
            @PathParam("typeName")
            final String typeName,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        try {
            jmxConfigurationServiceMXBean(servletContext).unregisterSecureType(typeName);
        } catch (MBeanException e) {
            throw new InternalServerErrorException(e);
        }
        return Response.noContent().build();
    }

    @DELETE
    @Path("/secureTypes")
    @Operation(summary = "Clear secure types")
    @ApiResponse(responseCode = "204")
    @Timed
    public Response clearSecureTypes(
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        jmxConfigurationServiceMXBean(servletContext).clearSecureTypes();
        return Response.noContent().build();
    }

    @DELETE
    @Path("/secureFields/{fieldName}/configurationType/{typeName}")
    @Operation(summary = "Unregister secure field by configuration type")
    @ApiResponse(responseCode = "204")
    @Timed
    public Response unregisterSecureFieldByConfigurationType(
            @NotEmpty
            @PathParam("typeName")
            final String typeName,
            @NotEmpty
            @PathParam("fieldName")
            final String fieldName,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        try {
            jmxConfigurationServiceMXBean(servletContext)
                    .unregisterSecureFieldByConfigurationType(typeName, fieldName);
            return Response.noContent().build();
        } catch (MBeanException e) {
            throw new InternalServerErrorException(e);
        }
    }

    @DELETE
    @Path("/secureFields/configurationType")
    @Operation(summary = "Clear secure field by configuration type map")
    @ApiResponse(responseCode = "204")
    @Timed
    public Response clearSecureFieldByConfigurationTypeMap(
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        jmxConfigurationServiceMXBean(servletContext).clearSecureFieldByConfigurationTypeMap();
        return Response.noContent().build();
    }

    @DELETE
    @Path("/globalConfigurationListeners/{typeName}")
    @Operation(summary = "Unregister global configuration listener")
    @ApiResponse(responseCode = "204")
    @Timed
    public Response unregisterGlobalConfigurationListener(
            @NotEmpty
            @PathParam("typeName")
            final String typeName,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        try {
            jmxConfigurationServiceMXBean(servletContext).unregisterGlobalConfigurationListener(typeName);
            return Response.noContent().build();
        } catch (MBeanException e) {
            throw new InternalServerErrorException(e);
        }
    }

    @DELETE
    @Path("/globalConfigurationListeners")
    @Operation(summary = "Clear global configuration listener")
    @ApiResponse(responseCode = "204")
    @Timed
    public Response clearGlobalConfigurationListeners(
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        jmxConfigurationServiceMXBean(servletContext).clearGlobalConfigurationListeners();
        return Response.noContent().build();
    }

    @DELETE
    @Path("/configurationListeners/propertyKey/{propertyKey}")
    @Operation(summary = "Unregister configuration listener by property key")
    @ApiResponse(responseCode = "204")
    @Timed
    public Response unregisterConfigurationListenerByPropertyKey(
            @NotEmpty
            @PathParam("propertyKey")
            final String propertyKey,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        try {
            final JmxConfigurationKey jmxConfigurationKey = new JmxConfigurationKey(propertyKey, null);
            jmxConfigurationServiceMXBean(servletContext).unregisterConfigurationListenerByKey(jmxConfigurationKey);
            return Response.noContent().build();
        } catch (MBeanException e) {
            throw new InternalServerErrorException(e);
        }
    }

    @DELETE
    @Path("/configurationListeners/appbaseServiceConfigurationType/{typeName}")
    @Operation(summary = "Unregister configuration listener by type name")
    @ApiResponse(responseCode = "204")
    @Timed
    public Response unregisterConfigurationListenerByTypeName(
            @NotEmpty
            @PathParam("typeName")
            final String typeName,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        try {
            final JmxConfigurationKey jmxConfigurationKey = new JmxConfigurationKey(null, typeName);
            jmxConfigurationServiceMXBean(servletContext).unregisterConfigurationListenerByKey(jmxConfigurationKey);
            return Response.noContent().build();
        } catch (MBeanException e) {
            throw new InternalServerErrorException(e);
        }
    }

    @DELETE
    @Path("/configurationListeners")
    @Operation(summary = "Clear configuration listeners")
    @ApiResponse(responseCode = "204")
    @Timed
    public Response clearConfigurationListeners(
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        jmxConfigurationServiceMXBean(servletContext).clearConfigurationListenerByKeyMap();
        return Response.noContent().build();
    }

    @GET
    @Path("/configurations")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Read all configurations")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = ConfigurationsJson.class)),
            description = "Returns all configurations from the configuration service",
            responseCode = "200")
    @Timed
    public Response getConfigurationsAsJson(
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        final ConfigurationsJson configurationsJson =
                jmxConfigurationServiceMXBean(servletContext).getConfigurationsJson();
        if (configurationsJson != null) {
            return Response.ok(configurationsJson).build();
        }

        return Response.ok().build();
    }

    @POST
    @Path("/load")
    @Operation(summary = "Trigger configurations load")
    @ApiResponse(responseCode = "204")
    @Timed
    public Response load(
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        jmxConfigurationServiceMXBean(servletContext).load();
        return Response.noContent().build();
    }

    @GET
    @Path("/status")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(summary = "Get configuration service status")
    @ApiResponse(responseCode = "200")
    @Timed
    public Response status(
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        final ServiceStatus status = jmxConfigurationServiceMXBean(servletContext).getStatus();
        return Response.ok(status.name()).build();
    }

    @PUT
    @Path("/reload")
    @Operation(summary = "Reload configuration service using original configuration")
    @ApiResponse(responseCode = "204")
    @Timed
    public Response reload(
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        try {
            jmxConfigurationServiceMXBean(servletContext).reload();
            return Response.noContent().build();
        } catch (MBeanException e) {
            throw new InternalServerErrorException(e);
        }
    }

    @POST
    @Path("/reload")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Reload configuration service using new configuration")
    @ApiResponse(responseCode = "204")
    @Timed
    public Response reload(
            final String appbaseServiceConfigurationYaml,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        try {
            jmxConfigurationServiceMXBean(servletContext).reload(appbaseServiceConfigurationYaml);
            return Response.noContent().build();
        } catch (MBeanException e) {
            throw new InternalServerErrorException(e);
        }
    }

    @POST
    @Path("/shutdown")
    @Operation(summary = "Shutdown configuration service")
    @ApiResponse(responseCode = "204")
    @Timed
    public Response shutdown(
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        jmxConfigurationServiceMXBean(servletContext).shutdown();
        return Response.noContent().build();
    }

    private JmxConfigurationServiceMXBean jmxConfigurationServiceMXBean(final ServletContext servletContext) {
        final AppbaseCore appbaseCore = appbaseCore(servletContext);
        final JmxConfigurationServiceMXBean jmxConfigurationServiceMXBean =
                appbaseCore.getJmxService().getMXBean(JmxConfigurationService.class);

        if (jmxConfigurationServiceMXBean == null) {
            appbaseCore
                    .getLogService()
                    .getLogger(getClass())
                    .error("{} is not initialized", JmxConfigurationService.class.getName());
            throw new InternalServerErrorException(
                    JmxConfigurationService.class.getName() + " is not initialized");
        }

        return jmxConfigurationServiceMXBean;
    }
}
