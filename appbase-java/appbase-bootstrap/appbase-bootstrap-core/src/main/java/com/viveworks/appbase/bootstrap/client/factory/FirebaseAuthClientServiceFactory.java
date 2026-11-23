/*
 * Copyright 2019 The original author or authors of Appbase
 *
 * This project includes software developed by the author or authors of
 * Appbase (https://example.invalid/scratch-rehearsal).
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

package com.viveworks.appbase.bootstrap.client.factory;

import com.viveworks.appbase.bootstrap.client.AppbaseClientServiceFactory;
import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.bootstrap.client.ClientServiceOrder;
import com.viveworks.appbase.bootstrap.core.AppbaseCoreServices;
import com.viveworks.appbase.client.service.firebase.app.DefaultFirebaseAppClientService;
import com.viveworks.appbase.client.service.firebase.app.FirebaseAppClientService;
import com.viveworks.appbase.client.service.firebase.auth.DefaultFirebaseAuthClientService;
import com.viveworks.appbase.client.service.firebase.auth.FirebaseAuthClientService;
import com.viveworks.appbase.client.service.firebase.auth.configuration.DefaultFirebaseAuthClientServiceConfiguration;
import com.viveworks.appbase.core.AppbaseException;
import com.viveworks.appbase.core.service.configuration.MimeType;
import com.viveworks.appbase.core.service.configuration.accessor.ConfigurationAccessor;
import com.viveworks.appbase.core.service.configuration.accessor.store.ConfigurationStore;
import com.viveworks.appbase.core.service.configuration.accessor.store.filesystem.ClasspathConfigurationStore;
import com.viveworks.appbase.core.service.registry.AppbaseServiceEntry;
import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Built-in factory for {@link FirebaseAuthClientService}.
 *
 * <p>Four construction modes:
 * <ul>
 *   <li>Pre-built: {@link #FirebaseAuthClientServiceFactory(FirebaseAuthClientService)}</li>
 *   <li>Config-based: {@link #FirebaseAuthClientServiceFactory(DefaultFirebaseAuthClientServiceConfiguration,
 *       FirebaseAppClientService)} — requires a pre-built {@link FirebaseAppClientService}.</li>
 *   <li>Classpath-file: {@link #FirebaseAuthClientServiceFactory(String)} — reuses a registered
 *       {@link FirebaseAppClientService}, or creates a transient
 *       {@link DefaultFirebaseAppClientService} when no Firebase App service is registered.</li>
 *   <li>Custom store: {@link #FirebaseAuthClientServiceFactory(ConfigurationStore, String)} — supply
 *       any {@link ConfigurationStore} and its raw config name; reuses a registered Firebase App
 *       service or creates a transient {@link DefaultFirebaseAppClientService}.</li>
 * </ul>
 */
public final class FirebaseAuthClientServiceFactory
        implements AppbaseClientServiceFactory<FirebaseAuthClientService> {

    @Nullable private final FirebaseAuthClientService predefinedService;
    @Nullable private final DefaultFirebaseAuthClientServiceConfiguration config;
    @Nullable private final FirebaseAppClientService firebaseAppClientService;
    @Nullable private final ConfigurationStore configStore;
    @Nullable private final String rawConfigName;

    /** Wraps a pre-built {@link FirebaseAuthClientService} instance. */
    public FirebaseAuthClientServiceFactory(final FirebaseAuthClientService service) {
        this.predefinedService        = Objects.requireNonNull(service, "service");
        this.config                   = null;
        this.firebaseAppClientService = null;
        this.configStore              = null;
        this.rawConfigName            = null;
    }

    /**
     * Creates a {@link DefaultFirebaseAuthClientService} from config and an explicit Firebase App dependency.
     *
     * @param config                  Firebase Auth service configuration
     * @param firebaseAppClientService pre-built Firebase App client service
     */
    public FirebaseAuthClientServiceFactory(final DefaultFirebaseAuthClientServiceConfiguration config,
                                             final FirebaseAppClientService firebaseAppClientService) {
        this.config                   = Objects.requireNonNull(config, "config");
        this.firebaseAppClientService = Objects.requireNonNull(firebaseAppClientService, "firebaseAppClientService");
        this.predefinedService        = null;
        this.configStore              = null;
        this.rawConfigName            = null;
    }

    /**
     * Creates a {@link DefaultFirebaseAuthClientService} by loading config from a classpath YAML file.
     * A registered {@link FirebaseAppClientService} is reused when present; otherwise a transient
     * {@link DefaultFirebaseAppClientService} is created internally.
     *
     * @param configClasspathFile classpath URI of the Firebase Auth YAML config file
     */
    public FirebaseAuthClientServiceFactory(final String configClasspathFile) {
        this.configStore              = new ClasspathConfigurationStore(
            Objects.requireNonNull(configClasspathFile, "configClasspathFile"), MimeType.YAML);
        this.rawConfigName            = configClasspathFile;
        this.predefinedService        = null;
        this.config                   = null;
        this.firebaseAppClientService = null;
    }

    /**
     * Creates a {@link DefaultFirebaseAuthClientService} using config loaded from the supplied store.
     * A registered {@link FirebaseAppClientService} is reused when present; otherwise a transient
     * {@link DefaultFirebaseAppClientService} is created internally.
     * {@code rawConfigName} is the key used in
     * {@code registerConfigurationTypeByRawConfigurationName} — for a classpath store this is
     * the classpath URI; for other stores use their identifying name.
     *
     * @param configStore   the configuration store to register
     * @param rawConfigName the raw configuration name key
     */
    public FirebaseAuthClientServiceFactory(final ConfigurationStore configStore,
                                             final String rawConfigName) {
        this.configStore              = Objects.requireNonNull(configStore, "configStore");
        this.rawConfigName            = Objects.requireNonNull(rawConfigName, "rawConfigName");
        this.predefinedService        = null;
        this.config                   = null;
        this.firebaseAppClientService = null;
    }

    @Override
    public Class<FirebaseAuthClientService> serviceType() {
        return FirebaseAuthClientService.class;
    }

    @Override
    public int initializationOrder() {
        return ClientServiceOrder.FIREBASE_AUTH.order();
    }

    @Override
    public void registerConfiguration(final AppbaseCoreServices coreServices) {
        if (configStore == null) {
            return;
        }
        DefaultFirebaseAuthClientService.registerObjectMapperSubtypes();
        final ConfigurationAccessor accessor =
            coreServices.getConfigurationService().getConfigurationAccessor();
        accessor.registerConfigurationTypeByRawConfigurationName(
            DefaultFirebaseAuthClientServiceConfiguration.class, rawConfigName);
        accessor.registerConfigurationStore(configStore);
    }

    @Override
    public FirebaseAuthClientService create(final AppbaseCoreServices coreServices,
                                             final AppbaseClientCore appbaseClientCore) throws AppbaseException {
        if (predefinedService != null) {
            return predefinedService;
        }
        if (config != null) {
            coreServices.getConfigurationService().putTypedConfiguration(
                DefaultFirebaseAuthClientServiceConfiguration.class, config);
            return new DefaultFirebaseAuthClientService(appbaseClientCore, firebaseAppClientService);
        }
        final AppbaseServiceEntry appServiceEntry = coreServices.appbaseCore().getServiceRegistry()
            .getAppbaseService(FirebaseAppClientService.class);
        final FirebaseAppClientService appService = appServiceEntry == null
            ? new DefaultFirebaseAppClientService(appbaseClientCore)
            : (FirebaseAppClientService) appServiceEntry.getService();
        return new DefaultFirebaseAuthClientService(appbaseClientCore, appService);
    }
}
