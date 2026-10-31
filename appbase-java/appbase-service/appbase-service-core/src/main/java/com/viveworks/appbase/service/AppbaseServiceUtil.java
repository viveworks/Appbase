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

package com.viveworks.appbase.service;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.configuration.PropertyCastException;
import com.viveworks.appbase.core.service.tracer.Traceable;
import com.viveworks.appbase.core.service.tracer.TracerService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.appbase.data.MapOfStringSetOfStringsValue;
import com.viveworks.appbase.service.appbase.data.SetOfStringsValue;
import com.viveworks.appbase.service.web.filter.AppbaseFilterConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Shared conversions and request helpers used by servlet and JAX-RS service adapters.
 * File-system operations are synchronous; the request accessors only inspect the supplied request.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class AppbaseServiceUtil {
    private AppbaseServiceUtil() {
        // no-op
    }

    /**
     * Creates {@code path} and any missing parent directories. When the immediate parent is newly
     * created, that parent (but not intermediate directories) is registered for best-effort deletion
     * when the JVM exits; a newly created file is also registered. Existing paths are unchanged.
     *
     * @param path file to create
     * @throws IOException if a required directory or file cannot be created
     * @throws NullPointerException if {@code path} has no parent
     * @throws com.viveworks.appbase.core.AppbaseRuntimeException if {@code path} is null
     */
    public static void createTemporaryFile(final Path path) throws IOException {
        AppbaseUtil.notNull(path, "path");

        final java.nio.file.Path parent = path.getParent();
        if (Files.notExists(parent)) {
            Files.createDirectories(parent);
            parent.toFile().deleteOnExit();
        }

        if (Files.notExists(path)) {
            Files.createFile(path);
            path.toFile().deleteOnExit();
        }
    }

    /**
     * Converts a nullable Boolean to the conventional SQL {@code TINYINT} representation.
     *
     * @param value Boolean to convert; {@code null} is treated as false
     * @return {@code 1} only for {@link Boolean#TRUE}, otherwise {@code 0}
     */
    public static int sqlTinyInt(final Boolean value) {
        return (value == null || !value) ? 0 : 1;
    }

    /**
     * Reads the UTC clock and discards sub-second precision.
     *
     * @return the current UTC local date-time, precise to whole seconds
     */
    public static LocalDateTime nowSeconds() {
        return LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);
    }

    /**
     * Discards fractional seconds without changing the date, time, or implicit time zone.
     *
     * @param localDateTime value whose nanoseconds are removed
     * @return the supplied date-time truncated to seconds
     * @throws com.viveworks.appbase.core.AppbaseRuntimeException if {@code localDateTime} is null
     */
    public static LocalDateTime truncateToSeconds(final LocalDateTime localDateTime) {
        AppbaseUtil.notNull(localDateTime, "localDateTime");

        return localDateTime.truncatedTo(ChronoUnit.SECONDS);
    }

    /**
     * Evaluates one filter invocation using the implementation's short-circuit order: empty rules enable;
     * a global blocked-path wildcard disables; a global allowed-path wildcard enables; filter/path blocks,
     * filter/path/method blocks, path/method blocks, and path blocks then disable; finally the corresponding
     * allow maps and allowed paths decide. Thus either global wildcard takes precedence over more-specific
     * rules. Method entries match the supplied spelling, uppercase, or lowercase.
     *
     * @param configurationService source of the typed filter configuration
     * @param filterType concrete filter class used as the per-filter rule key
     * @param path request path to evaluate
     * @param method HTTP method to evaluate
     * @return whether the filter should execute for this request
     * @throws com.viveworks.appbase.core.AppbaseRuntimeException if any argument is null
     */
    public static boolean isFilterEnabled(
            final ConfigurationService configurationService,
            final Class<?> filterType,
            final String path,
            final String method)
    {
        AppbaseUtil.notNull(configurationService, "configurationService");
        AppbaseUtil.notNull(filterType, "filterType");
        AppbaseUtil.notNull(path, "path");
        AppbaseUtil.notNull(method, "method");

        final AppbaseFilterConfig appbaseFilterConfig =
                configurationService.getTypedConfiguration(AppbaseFilterConfig.class);
        if (appbaseFilterConfig == null) {
            // If filter config is not found, allow filtering
            return true;
        }

        final Map<String, MapOfStringSetOfStringsValue> allowedMethodsByPathsByFilterTypeNameMap =
                appbaseFilterConfig.getAllowedMethodsByPathsByFilterTypeNameMap();
        final Map<String, SetOfStringsValue> allowedPathsByFilterTypeNameMap =
                appbaseFilterConfig.getAllowedPathsByFilterTypeNameMap();
        final Map<String, SetOfStringsValue> allowedMethodsByPathMap =
                appbaseFilterConfig.getAllowedMethodsByPathMap();
        final Set<String> allowedPaths = appbaseFilterConfig.getAllowedPaths();

        final Map<String, MapOfStringSetOfStringsValue> blockedMethodsByPathsByFilterTypeNameMap =
                appbaseFilterConfig.getBlockedMethodsByPathsByFilterTypeNameMap();
        final Map<String, SetOfStringsValue> blockedPathsByFilterTypeNameMap =
                appbaseFilterConfig.getBlockedPathsByFilterTypeNameMap();
        final Set<String> blockedPaths = appbaseFilterConfig.getBlockedPaths();
        final Map<String, SetOfStringsValue> blockedMethodsByPathMap =
                appbaseFilterConfig.getBlockedMethodsByPathMap();


        // If all empty, enabled
        if ((allowedPathsByFilterTypeNameMap == null || allowedPathsByFilterTypeNameMap.isEmpty()) &&
                (allowedMethodsByPathsByFilterTypeNameMap == null ||
                        allowedMethodsByPathsByFilterTypeNameMap.isEmpty()) &&
                (allowedMethodsByPathMap == null || allowedMethodsByPathMap.isEmpty()) &&
                (allowedPaths == null || allowedPaths.isEmpty()) &&
                (blockedMethodsByPathsByFilterTypeNameMap == null ||
                        blockedMethodsByPathsByFilterTypeNameMap.isEmpty()) &&
                (blockedPathsByFilterTypeNameMap == null || blockedPathsByFilterTypeNameMap.isEmpty()) &&
                (blockedPaths == null || blockedPaths.isEmpty()) &&
                (blockedMethodsByPathMap == null || blockedMethodsByPathMap.isEmpty()))
        {
            return true;
        }

        // Check if all paths are blocked
        if (blockedPaths != null && blockedPaths.contains(AppbaseServiceConstants.WILDCARD)) {
            return false;
        }


        // Check if all paths are allowed
        if (allowedPaths != null && allowedPaths.contains(AppbaseServiceConstants.WILDCARD)) {
            return true;
        }


        final String filterTypeName = filterType.getName();
        // Check blocked filter+path
        if (blockedPathsByFilterTypeNameMap != null && blockedPathsByFilterTypeNameMap.containsKey(filterTypeName)) {
            final SetOfStringsValue blockedPathsValue = blockedPathsByFilterTypeNameMap.get(filterTypeName);
            if (blockedPathsValue != null) {
                final Set<String> blockedPathSet = blockedPathsValue.getValue();
                if (blockedPathSet != null &&
                        (blockedPathSet.contains(AppbaseServiceConstants.WILDCARD) || blockedPathSet.contains(path)))
                {
                    return false;
                }
            }
        }


        // Check blocked filter+path+method
        if (blockedMethodsByPathsByFilterTypeNameMap != null &&
                blockedMethodsByPathsByFilterTypeNameMap.containsKey(filterTypeName))
        {
            final MapOfStringSetOfStringsValue blockedMethodsByPathsValue =
                    blockedMethodsByPathsByFilterTypeNameMap.get(filterTypeName);
            if (blockedMethodsByPathsValue != null) {
                final Map<String, SetOfStringsValue> blockedMethodsByPathsValueMap =
                        blockedMethodsByPathsValue.getValue();
                if (blockedMethodsByPathsValueMap != null && blockedMethodsByPathsValueMap.containsKey(path)) {
                    final SetOfStringsValue blockedMethodsValue = blockedMethodsByPathsValueMap.get(path);
                    if (blockedMethodsValue != null) {
                        final Set<String> blockedMethods = blockedMethodsValue.getValue();
                        if (blockedMethods != null) {
                            if (blockedMethods.contains(AppbaseServiceConstants.WILDCARD) ||
                                    blockedMethods.contains(method) ||
                                    blockedMethods.contains(method.toUpperCase()) ||
                                    blockedMethods.contains(method.toLowerCase()))
                            {
                                return false;
                            }
                        }
                    }
                }
            }
        }


        // Check blocked path+methods
        if (blockedMethodsByPathMap != null) {
            final SetOfStringsValue blockedMethodValues = blockedMethodsByPathMap.get(path);
            if (blockedMethodValues != null) {
                final Set<String> blockedMethods = blockedMethodValues.getValue();
                if (blockedMethods != null) {
                    if (blockedMethods.contains(AppbaseServiceConstants.WILDCARD) ||
                            blockedMethods.contains(method) ||
                            blockedMethods.contains(method.toUpperCase()) ||
                            blockedMethods.contains(method.toLowerCase()))
                    {
                        return false;
                    }
                }
            }
        }


        // Check blocked path
        if (blockedPaths != null &&
                (blockedPaths.contains(AppbaseServiceConstants.WILDCARD) || blockedPaths.contains(path)))
        {
            return false;
        }


        // Check allowed filter+path+methods
        if (allowedMethodsByPathsByFilterTypeNameMap != null &&
                allowedMethodsByPathsByFilterTypeNameMap.containsKey(filterTypeName))
        {
            final MapOfStringSetOfStringsValue allowedMethodsByPathsValue =
                    allowedMethodsByPathsByFilterTypeNameMap.get(filterTypeName);
            if (allowedMethodsByPathsValue != null) {
                final Map<String, SetOfStringsValue> allowedMethodsByPathsMap = allowedMethodsByPathsValue.getValue();
                if (allowedMethodsByPathsMap != null && allowedMethodsByPathsMap.containsKey(path)) {
                    final SetOfStringsValue allowedMethodsValue = allowedMethodsByPathsMap.get(path);
                    if (allowedMethodsValue != null) {
                        final Set<String> allowedMethods = allowedMethodsValue.getValue();
                        if (allowedMethods != null) {
                            return allowedMethods.contains(AppbaseServiceConstants.WILDCARD) ||
                                    allowedMethods.contains(method) ||
                                    allowedMethods.contains(method.toUpperCase()) ||
                                    allowedMethods.contains(method.toLowerCase());
                        }
                    }
                }

                return false;
            }
        }


        // Check allowed filter+path
        if (allowedPathsByFilterTypeNameMap != null && allowedPathsByFilterTypeNameMap.containsKey(filterTypeName)) {
            final SetOfStringsValue allowedPathsValue = allowedPathsByFilterTypeNameMap.get(filterTypeName);
            if (allowedPathsValue != null) {
                final Set<String> value = allowedPathsValue.getValue();
                return (value != null && (value.contains(AppbaseServiceConstants.WILDCARD) || value.contains(path)));
            }
        }


        // Check allowed path+methods
        if (allowedMethodsByPathMap != null) {
            final SetOfStringsValue allowedMethodValues = allowedMethodsByPathMap.get(path);
            if (allowedMethodValues != null) {
                final Set<String> allowedMethods = allowedMethodValues.getValue();
                return allowedMethods.contains(AppbaseServiceConstants.WILDCARD) ||
                        allowedMethods.contains(method) ||
                        allowedMethods.contains(method.toUpperCase()) ||
                        allowedMethods.contains(method.toLowerCase());
            }
        }


        // Check allowed paths
        return allowedPaths != null &&
                (allowedPaths.contains(path) || allowedPaths.contains(AppbaseServiceConstants.WILDCARD));
    }

    /**
     * Obtains the application core stored in the servlet context under
     * {@link AppbaseServiceConstants#APPBASE_CORE_ATTRIBUTE}.
     *
     * @param servletContext context populated during application startup
     * @return the stored core, or {@code null} when the attribute is absent
     * @throws ClassCastException when the attribute has a different type
     * @throws com.viveworks.appbase.core.AppbaseRuntimeException if {@code servletContext} is null
     */
    public static AppbaseCore appbaseCore(final ServletContext servletContext) {
        AppbaseUtil.notNull(servletContext, "servletContext");

        return (AppbaseCore) servletContext.getAttribute(AppbaseServiceConstants.APPBASE_CORE_ATTRIBUTE);
    }

    /**
     * Reads and UTF-8 URL-decodes a request parameter.
     *
     * @param name parameter name
     * @param httpServletRequest request to inspect
     * @return the decoded value, or {@code null} when the parameter is absent
     * @throws IllegalArgumentException if the value contains invalid percent encoding
     * @throws com.viveworks.appbase.core.AppbaseRuntimeException if either argument is null
     */
    @Nullable
    public static String parameter(
            final String name,
            final HttpServletRequest httpServletRequest)
    {
        AppbaseUtil.notNull(name, "name");
        AppbaseUtil.notNull(httpServletRequest, "httpServletRequest");

        final String parameter = httpServletRequest.getParameter(name);
        if (parameter != null) {
            return URLDecoder.decode(parameter, StandardCharsets.UTF_8);
        }

        return null;
    }

    /**
     * Reads and UTF-8 URL-decodes an HTTP header value.
     *
     * @param name header name
     * @param httpServletRequest request to inspect
     * @return the decoded header, or {@code null} when it is absent
     * @throws IllegalArgumentException if the value contains invalid percent encoding
     * @throws com.viveworks.appbase.core.AppbaseRuntimeException if either argument is null
     */
    @Nullable
    public static String header(
            final String name,
            final HttpServletRequest httpServletRequest)
    {
        AppbaseUtil.notNull(name, "name");
        AppbaseUtil.notNull(httpServletRequest, "httpServletRequest");

        final String header = httpServletRequest.getHeader(name);
        if (header != null) {
            return URLDecoder.decode(header, StandardCharsets.UTF_8);
        }

        return null;
    }

    /**
     * Finds the first request cookie whose name equals {@code name}. Cookie values are returned verbatim,
     * unlike parameters and headers, which this utility URL-decodes.
     *
     * @param name cookie name
     * @param httpServletRequest request to inspect
     * @return the matching cookie value, or {@code null} when no cookie matches
     * @throws com.viveworks.appbase.core.AppbaseRuntimeException if either argument is null
     */
    @Nullable
    public static String cookie(
            final String name,
            final HttpServletRequest httpServletRequest)
    {
        AppbaseUtil.notNull(name, "name");
        AppbaseUtil.notNull(httpServletRequest, "httpServletRequest");

        final Cookie[] cookies = httpServletRequest.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }

    /**
     * Reads the configured trace identifier property, falling back to
     * {@link TracerService#TRACE_ID_KEY} when the property is blank.
     *
     * @param appbaseCore core providing the configuration service
     * @return configured or default trace identifier key
     * @throws AppbaseServiceRuntimeException when the configured property is not a string
     * @throws com.viveworks.appbase.core.AppbaseRuntimeException if {@code appbaseCore} is null
     */
    public static String traceIdKey(final AppbaseCore appbaseCore) {
        AppbaseUtil.notNull(appbaseCore, "appbaseCore");

        String traceIdKey;
        try {
            traceIdKey = appbaseCore.getConfigurationService().getString(TracerService.TRACE_ID_KEY_PROPERTY);
        } catch (PropertyCastException e) {
            throw new AppbaseServiceRuntimeException("Cannot cast traceIdKey property to String");
        }

        if (AppbaseUtil.isBlank(traceIdKey)) {
            traceIdKey = TracerService.TRACE_ID_KEY;
        }

        return traceIdKey;
    }

    /**
     * Uses a non-blank incoming trace header, or generates a random UUID when none was supplied.
     *
     * @param tracerIdHeader candidate propagated trace identifier
     * @return the propagated identifier or a new UUID string
     */
    public static String traceId(final String tracerIdHeader) {
        if (!AppbaseUtil.isBlank(tracerIdHeader)) {
            return tracerIdHeader;
        }

        return UUID.randomUUID().toString();
    }

    /**
     * Uses the trace identifier carried by {@code traceable}, or generates a random UUID when it is blank.
     *
     * @param traceable object that may already carry a trace identifier
     * @return the carried identifier or a new UUID string
     */
    public static String traceId(final Traceable traceable) {
        if (!AppbaseUtil.isBlank(traceable.getTraceId())) {
            return traceable.getTraceId();
        }

        return UUID.randomUUID().toString();
    }

    /**
     * Selects a trace identifier in precedence order: non-blank header, non-blank value on
     * {@code traceable}, then a newly generated random UUID.
     *
     * @param tracerIdHeader candidate propagated HTTP trace identifier
     * @param traceable secondary source of a trace identifier
     * @return the first usable identifier, or a new UUID string
     */
    public static String traceId(final String tracerIdHeader, final Traceable traceable) {
        if (!AppbaseUtil.isBlank(tracerIdHeader)) {
            return tracerIdHeader;
        }

        if (!AppbaseUtil.isBlank(traceable.getTraceId())) {
            return traceable.getTraceId();
        }

        return UUID.randomUUID().toString();
    }
}
