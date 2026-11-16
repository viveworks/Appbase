/*
 * Copyright 2019 The original author or authors of Appbase
 *
 * This project includes software developed by the author or authors of
 * Appbase (https://github.com/navaneeth-mysore/Appbase).
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

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Validates Jersey package-scanning compatibility before a servlet container starts.
 *
 * <p>Jersey 3.1.11 embeds ASM 9.8 and supports package scanning through Java 25. Jersey exposes the
 * property-driven scanner publicly, but {@link ResourceConfig#packages(String...)} records its
 * finder collection in Jersey's package-private configuration implementation. The narrowly isolated
 * reflective check below is therefore pinned to Jersey 3.1.11. It locates that state by its finder
 * accessor contract rather than by a private field name, so an internal field rename cannot block
 * explicit-only configurations. A future Jersey API-layout change fails closed only on an otherwise
 * unsupported future Java runtime; Java 25 and earlier never enter the reflective path.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public final class JerseyResourceConfigCompatibility {

    private static final int MAX_PACKAGE_SCAN_RUNTIME_FEATURE = 25;
    private static final String PACKAGE_NAMES_SCANNER =
        "org.glassfish.jersey.server.internal.scanning.PackageNamesScanner";

    private JerseyResourceConfigCompatibility() {
    }

    /**
     * Validates a resource configuration against the current Java runtime.
     *
     * @param resourceConfig Jersey configuration to validate
     * @throws IllegalStateException when package scanning is configured on an unsupported runtime,
     *                               or when future-Jersey compatibility cannot be inspected safely
     */
    public static void validate(final ResourceConfig resourceConfig) {
        validate(resourceConfig, Runtime.version().feature());
    }

    static void validate(final ResourceConfig resourceConfig, final int runtimeFeature) {
        Objects.requireNonNull(resourceConfig, "resourceConfig");
        if (runtimeFeature <= MAX_PACKAGE_SCAN_RUNTIME_FEATURE) {
            return;
        }
        if (resourceConfig.getProperty(ServerProperties.PROVIDER_PACKAGES) != null
                || hasDirectPackageScanner(resourceConfig, runtimeFeature)) {
            throw unsupportedPackageScan(runtimeFeature);
        }
    }

    private static boolean hasDirectPackageScanner(
            final ResourceConfig resourceConfig, final int runtimeFeature) {
        try {
            Object state = resourceFinderState(resourceConfig);
            Method getResourceFinders = state.getClass().getMethod("getResourceFinders");
            if (!getResourceFinders.trySetAccessible()) {
                throw inspectionFailure(runtimeFeature, null);
            }
            Object resourceFinders = getResourceFinders.invoke(state);
            if (!(resourceFinders instanceof Iterable<?> finders)) {
                throw inspectionFailure(runtimeFeature, null);
            }
            for (Object finder : finders) {
                if (finder != null && PACKAGE_NAMES_SCANNER.equals(finder.getClass().getName())) {
                    return true;
                }
            }
            return false;
        } catch (ReflectiveOperationException | SecurityException exception) {
            throw inspectionFailure(runtimeFeature, exception);
        }
    }

    private static Object resourceFinderState(final ResourceConfig resourceConfig)
            throws ReflectiveOperationException {
        for (Field candidate : ResourceConfig.class.getDeclaredFields()) {
            try {
                candidate.getType().getMethod("getResourceFinders");
            } catch (NoSuchMethodException exception) {
                continue;
            }
            if (!candidate.trySetAccessible()) {
                throw new IllegalAccessException("Jersey resource-finder state is inaccessible");
            }
            Object state = candidate.get(resourceConfig);
            if (state != null) {
                return state;
            }
        }
        throw new NoSuchFieldException("Jersey resource-finder state is unavailable");
    }

    private static IllegalStateException unsupportedPackageScan(final int runtimeFeature) {
        return new IllegalStateException("Jersey package scanning is supported through Java 25, "
            + "but this server is running on Java " + runtimeFeature + ". Register each JAX-RS "
            + "resource explicitly with ResourceConfig.register(Class), run on a supported Java "
            + "runtime, or upgrade Appbase/Jersey to a version that supports this runtime.");
    }

    private static IllegalStateException inspectionFailure(
            final int runtimeFeature, final Throwable cause) {
        return new IllegalStateException("Appbase could not verify Jersey package-scanning "
            + "compatibility on Java " + runtimeFeature + ". Register each JAX-RS resource "
            + "explicitly with ResourceConfig.register(Class), run on Java 25, or upgrade "
            + "Appbase/Jersey before starting this servlet container.", cause);
    }
}
