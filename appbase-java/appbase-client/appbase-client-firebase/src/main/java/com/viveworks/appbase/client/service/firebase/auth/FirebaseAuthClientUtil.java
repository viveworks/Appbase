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

import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValue;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValueConfiguration;
import com.viveworks.appbase.client.service.firebase.app.FirebaseAppClientService;
import com.viveworks.appbase.client.service.firebase.auth.configuration.DefaultFirebaseAuthClientIntegrationServiceConfiguration;
import com.viveworks.appbase.client.util.AppbaseClientUtil;
import com.viveworks.appbase.core.util.AppbaseUtil;

/**
 * Utilities for this module.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public final class FirebaseAuthClientUtil {
    private FirebaseAuthClientUtil() {
    }

    /**
     * Generate {@link AppbaseClientKey} used by {@link FirebaseAuthClientService}.
     *
     * @param firebaseAppClientService {@link FirebaseAppClientService}
     * @param defaultFirebaseAuthClientIntegrationServiceConfiguration
     * {@link DefaultFirebaseAuthClientIntegrationServiceConfiguration}
     * @return {@link AppbaseClientKey}
     */
    public static AppbaseClientKey<TextValue> appbaseClientKey(
            final FirebaseAppClientService firebaseAppClientService,
            final DefaultFirebaseAuthClientIntegrationServiceConfiguration
                    defaultFirebaseAuthClientIntegrationServiceConfiguration)
    {
        AppbaseUtil.notNull(firebaseAppClientService, "firebaseAppClientService");
        AppbaseUtil.notNull(
                defaultFirebaseAuthClientIntegrationServiceConfiguration,
                "defaultFirebaseAuthClientIntegrationServiceConfiguration");

        final TextValueConfiguration textValueKeyConfiguration =
                defaultFirebaseAuthClientIntegrationServiceConfiguration.getTextValueKeyConfiguration();
        if (textValueKeyConfiguration != null) {
            return AppbaseClientUtil.textValueKey(textValueKeyConfiguration);
        }

        return firebaseAppClientService.getDefaultAppbaseClientKey();
    }
}
