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
package com.viveworks.appbase.service.health.checkers;

import com.google.firebase.FirebaseApp;
import com.viveworks.appbase.client.service.firebase.app.FirebaseAppClientIntegrationService;
import com.viveworks.appbase.client.service.firebase.app.FirebaseAppClientService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.health.HealthCheck;
import com.viveworks.appbase.service.health.HealthChecker;

import java.util.Objects;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class FirebaseAppHealthChecker implements HealthChecker {
    private final FirebaseAppClientIntegrationService firebaseAppClientIntegrationService;
    private final String testAppName;

    public FirebaseAppHealthChecker(
            final FirebaseAppClientIntegrationService firebaseAppClientIntegrationService,
            final String testAppName)
    {
        AppbaseUtil.notNull(firebaseAppClientIntegrationService, "firebaseAppClientIntegrationService");
        AppbaseUtil.notBlank(testAppName, "testAppName");

        this.firebaseAppClientIntegrationService = firebaseAppClientIntegrationService;
        this.testAppName = testAppName;
    }

    @Override
    public HealthCheck runHealthCheck() {
        final FirebaseApp firebaseApp = firebaseAppClientIntegrationService.getClient();
        if (firebaseApp == null) {
            return HealthCheck.failed(FirebaseAppClientService.class.getName());
        }

        final String name = firebaseApp.getName();
        return  (Objects.equals(name, FirebaseApp.DEFAULT_APP_NAME) || Objects.equals(name, testAppName))
                ? HealthCheck.success(FirebaseAppClientService.class.getName())
                : HealthCheck.failed(FirebaseAppClientService.class.getName());
    }
}
