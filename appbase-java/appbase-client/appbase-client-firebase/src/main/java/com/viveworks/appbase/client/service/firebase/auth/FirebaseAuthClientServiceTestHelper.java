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
package com.viveworks.appbase.client.service.firebase.auth;

import com.viveworks.appbase.client.core.ClientCoreTestHelper;
import com.viveworks.appbase.client.core.DefaultAppbaseClientCore;
import com.viveworks.appbase.client.service.firebase.app.DefaultFirebaseAppClientService;
import com.viveworks.appbase.client.service.firebase.app.FirebaseAppClientService;
import com.viveworks.appbase.client.service.firebase.app.FirebaseAppClientServiceTestHelper;
import com.viveworks.appbase.client.service.firebase.app.configuration.DefaultFirebaseAppClientServiceConfiguration;
import com.viveworks.appbase.client.service.firebase.auth.configuration.DefaultFirebaseAuthClientServiceConfiguration;
import com.viveworks.appbase.client.util.AppbaseClientObjectMapperSubTypes;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.configuration.MimeType;
import com.viveworks.appbase.core.service.configuration.accessor.ConfigurationAccessor;
import com.viveworks.appbase.core.service.configuration.accessor.store.filesystem.ClasspathConfigurationStore;
import com.viveworks.appbase.core.util.AppbaseConstants;
import com.viveworks.appbase.core.util.AppbaseUtil;

/**
 * Creates Firebase Auth client services for tests.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public final class FirebaseAuthClientServiceTestHelper {
    public static final String FIREBASE_AUTH_CONFIG_FILE_PROPERTY =
            AppbaseConstants.TEST_HELPER_CONFIG_FILE_PROPERTY_PREFIX + "firebaseAuth";
    private static final String FIREBASE_AUTH_CONFIG_CLASSPATH_FILE =
            AppbaseConstants.CLASSPATH_FILE_PREFIX + "/appbase-client-firebase-auth.yaml";

    private FirebaseAuthClientServiceTestHelper() {
    }

    /**
     * Creates a Firebase Auth client service from the default test configurations.
     *
     * @return the configured service
     */
    public static FirebaseAuthClientService firebaseAuthClientService() {
        final String firebaseAppConfigFile = System.getProperty(
                FirebaseAppClientServiceTestHelper.FIREBASE_APP_CONFIG_FILE_PROPERTY,
                FirebaseAppClientServiceTestHelper.FIREBASE_APP_CONFIG_CLASSPATH_FILE);
        final String firebaseAuthConfigFile = System.getProperty(
                FIREBASE_AUTH_CONFIG_FILE_PROPERTY,
                FIREBASE_AUTH_CONFIG_CLASSPATH_FILE);
        return firebaseAuthClientService(firebaseAppConfigFile, firebaseAuthConfigFile);
    }

    /**
     * Creates a Firebase Auth client service from the supplied test configurations.
     *
     * @param firebaseAppConfigFile Firebase App configuration location
     * @param firebaseAuthConfigFile Firebase Auth configuration location
     * @return the configured service
     */
    public static FirebaseAuthClientService firebaseAuthClientService(
            final String firebaseAppConfigFile,
            final String firebaseAuthConfigFile)
    {
        AppbaseUtil.notNull(firebaseAppConfigFile, "firebaseAppConfigFile");
        AppbaseUtil.notNull(firebaseAuthConfigFile, "firebaseAuthConfigFile");

        final AppbaseCore appbaseCore = appbaseCore(firebaseAppConfigFile, firebaseAuthConfigFile);
        final DefaultAppbaseClientCore defaultAppbaseClientCore =
                ClientCoreTestHelper.defaultAppbaseClientCore(appbaseCore);
        AppbaseUtil.notNull(defaultAppbaseClientCore, "defaultAppbaseClientCore");

        final FirebaseAppClientService firebaseAppClientService =
                new DefaultFirebaseAppClientService(defaultAppbaseClientCore);
        return new DefaultFirebaseAuthClientService(defaultAppbaseClientCore, firebaseAppClientService);
    }

    /**
     * Creates a Firebase Auth client service using an existing Appbase core.
     *
     * @param appbaseCore Appbase core used by the service
     * @return the configured service
     */
    public static FirebaseAuthClientService firebaseAuthClientService(final AppbaseCore appbaseCore) {
        final FirebaseServices firebaseServices = firebaseServices(appbaseCore);
        return firebaseServices.getFirebaseAuthClientService();
    }

    /**
     * Creates paired Firebase App and Auth services using an existing Appbase core.
     *
     * @param appbaseCore Appbase core used by the services
     * @return the paired services
     */
    public static FirebaseServices firebaseServices(final AppbaseCore appbaseCore) {
        final String firebaseAppConfigFile = System.getProperty(
                FirebaseAppClientServiceTestHelper.FIREBASE_APP_CONFIG_FILE_PROPERTY,
                FirebaseAppClientServiceTestHelper.FIREBASE_APP_CONFIG_CLASSPATH_FILE);
        final String firebaseAuthConfigFile = System.getProperty(
                FIREBASE_AUTH_CONFIG_FILE_PROPERTY,
                FIREBASE_AUTH_CONFIG_CLASSPATH_FILE);
        DefaultFirebaseAppClientService.registerObjectMapperSubtypes();
        DefaultFirebaseAuthClientService.registerObjectMapperSubtypes();
        AppbaseClientObjectMapperSubTypes.register();
        loadConfigurationService(appbaseCore, firebaseAppConfigFile, firebaseAuthConfigFile);

        final DefaultAppbaseClientCore defaultAppbaseClientCore =
                ClientCoreTestHelper.defaultAppbaseClientCore(appbaseCore);
        AppbaseUtil.notNull(defaultAppbaseClientCore, "defaultAppbaseClientCore");

        final FirebaseAppClientService firebaseAppClientService =
                new DefaultFirebaseAppClientService(defaultAppbaseClientCore);
        final DefaultFirebaseAuthClientService defaultFirebaseAuthClientService =
                new DefaultFirebaseAuthClientService(defaultAppbaseClientCore, firebaseAppClientService);
        return new FirebaseServices(firebaseAppClientService, defaultFirebaseAuthClientService);
    }

    /**
     * Creates an Appbase core from the default Firebase test configurations.
     *
     * @return the configured Appbase core
     */
    public static AppbaseCore appbaseCore() {
        final String firebaseAppConfigFile = System.getProperty(
                FirebaseAppClientServiceTestHelper.FIREBASE_APP_CONFIG_FILE_PROPERTY,
                FirebaseAppClientServiceTestHelper.FIREBASE_APP_CONFIG_CLASSPATH_FILE);
        final String firebaseAuthConfigFile = System.getProperty(
                FIREBASE_AUTH_CONFIG_FILE_PROPERTY,
                FIREBASE_AUTH_CONFIG_CLASSPATH_FILE);

        return appbaseCore(firebaseAppConfigFile, firebaseAuthConfigFile);
    }

    /**
     * Creates an Appbase core from the supplied Firebase test configurations.
     *
     * @param firebaseAppConfigFile Firebase App configuration location
     * @param firebaseAuthConfigFile Firebase Auth configuration location
     * @return the configured Appbase core
     */
    public static AppbaseCore appbaseCore(
            final String firebaseAppConfigFile,
            final String firebaseAuthConfigFile)
    {
        AppbaseUtil.notNull(firebaseAuthConfigFile, "firebaseAuthConfigFile");

        final AppbaseCore appbaseCore = ClientCoreTestHelper.appbaseCore();
        DefaultFirebaseAuthClientService.registerObjectMapperSubtypes();
        AppbaseClientObjectMapperSubTypes.register();

        loadConfigurationService(appbaseCore, firebaseAppConfigFile, firebaseAuthConfigFile);

        return appbaseCore;
    }

    private static void loadConfigurationService(
            final AppbaseCore appbaseCore,
            final String firebaseAppConfigFile,
            final String firebaseAuthConfigFile)
    {
        final ConfigurationService configurationService = appbaseCore.getConfigurationService();
        final ConfigurationAccessor configurationAccessor = configurationService.getConfigurationAccessor();

        configurationAccessor.registerConfigurationTypeByRawConfigurationName(
                DefaultFirebaseAppClientServiceConfiguration.class,
                firebaseAppConfigFile);
        final ClasspathConfigurationStore classpathConfigurationStore1 =
                new ClasspathConfigurationStore(firebaseAppConfigFile, MimeType.YAML);
        configurationAccessor.registerConfigurationStore(classpathConfigurationStore1);

        configurationAccessor.registerConfigurationTypeByRawConfigurationName(
                DefaultFirebaseAuthClientServiceConfiguration.class,
                firebaseAuthConfigFile);
        final ClasspathConfigurationStore classpathConfigurationStore2 =
                new ClasspathConfigurationStore(firebaseAuthConfigFile, MimeType.YAML);
        configurationAccessor.registerConfigurationStore(classpathConfigurationStore2);

        configurationService.load();
    }

    /** Holds the paired Firebase App and Auth services created for a test. */
    public static final class FirebaseServices {
        private final FirebaseAppClientService firebaseAppClientService;
        private final FirebaseAuthClientService firebaseAuthClientService;

        public FirebaseServices(
                final FirebaseAppClientService firebaseAppClientService,
                final FirebaseAuthClientService firebaseAuthClientService)
        {
            AppbaseUtil.notNull(firebaseAppClientService, "firebaseAppClientService");
            AppbaseUtil.notNull(firebaseAuthClientService, "firebaseAuthClientService");

            this.firebaseAppClientService = firebaseAppClientService;
            this.firebaseAuthClientService = firebaseAuthClientService;
        }

        public FirebaseAppClientService getFirebaseAppClientService() {
            return firebaseAppClientService;
        }

        public FirebaseAuthClientService getFirebaseAuthClientService() {
            return firebaseAuthClientService;
        }
    }
}
