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
package com.viveworks.appbase.client.service.firebase.app;

import com.viveworks.appbase.client.core.ClientCoreTestHelper;
import com.viveworks.appbase.client.core.DefaultAppbaseClientCore;
import com.viveworks.appbase.client.service.firebase.app.configuration.DefaultFirebaseAppClientServiceConfiguration;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.configuration.MimeType;
import com.viveworks.appbase.core.service.configuration.accessor.ConfigurationAccessor;
import com.viveworks.appbase.core.service.configuration.accessor.store.filesystem.ClasspathConfigurationStore;
import com.viveworks.appbase.core.util.AppbaseConstants;
import com.viveworks.appbase.core.util.AppbaseUtil;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class FirebaseAppClientServiceTestHelper {
    public static final String FIREBASE_APP_CONFIG_FILE_PROPERTY =
            AppbaseConstants.TEST_HELPER_CONFIG_FILE_PROPERTY_PREFIX + "firebaseApp";
    public static final String FIREBASE_APP_CONFIG_CLASSPATH_FILE =
            AppbaseConstants.CLASSPATH_FILE_PREFIX + "/appbase-client-firebase-app.yaml";

    private FirebaseAppClientServiceTestHelper() {
    }

    public static FirebaseAppClientService firebaseAppClientService() {
        final String firebaseAppConfigFile = System.getProperty(
                FIREBASE_APP_CONFIG_FILE_PROPERTY,
                FIREBASE_APP_CONFIG_CLASSPATH_FILE);
        return firebaseAppClientService(firebaseAppConfigFile);
    }

    public static FirebaseAppClientService firebaseAppClientService(final String firebaseAppConfigFile) {
        final AppbaseCore appbaseCore = appbaseCore(firebaseAppConfigFile);
        final DefaultAppbaseClientCore defaultAppbaseClientCore =
                ClientCoreTestHelper.defaultAppbaseClientCore(appbaseCore);
        AppbaseUtil.notNull(defaultAppbaseClientCore, "defaultAppbaseClientCore");

        return new DefaultFirebaseAppClientService(defaultAppbaseClientCore);
    }

    public static AppbaseCore appbaseCore() {
        return appbaseCore(FIREBASE_APP_CONFIG_CLASSPATH_FILE);
    }

    public static AppbaseCore appbaseCore(final String firebaseAppConfigFile) {
        AppbaseUtil.notNull(firebaseAppConfigFile, "firebaseAppConfigFile");

        final AppbaseCore appbaseCore = ClientCoreTestHelper.appbaseCore();
        DefaultFirebaseAppClientService.registerObjectMapperSubtypes();

        final ConfigurationService configurationService = appbaseCore.getConfigurationService();
        final ConfigurationAccessor configurationAccessor = configurationService.getConfigurationAccessor();

        configurationAccessor.registerConfigurationTypeByRawConfigurationName(
                DefaultFirebaseAppClientServiceConfiguration.class,
                firebaseAppConfigFile);
        final ClasspathConfigurationStore classpathConfigurationStore =
                new ClasspathConfigurationStore(firebaseAppConfigFile, MimeType.YAML);
        configurationAccessor.registerConfigurationStore(classpathConfigurationStore);

        configurationService.load();

        return appbaseCore;
    }
}
