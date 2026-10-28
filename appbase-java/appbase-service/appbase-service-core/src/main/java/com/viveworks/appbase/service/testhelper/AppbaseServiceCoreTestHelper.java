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

package com.viveworks.appbase.service.testhelper;

import com.viveworks.appbase.core.service.AppbaseService;
import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.registry.AppbaseServiceEntry;
import com.viveworks.appbase.core.service.registry.ServiceRegistry;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class AppbaseServiceCoreTestHelper {
    public static AppbaseServiceEntry awaitServiceStartedStatus(
            final ServiceRegistry serviceRegistry,
            final Class<? extends AppbaseService> appbaseServiceType,
            final AppbaseServiceEntry appbaseServiceEntry)
            throws InterruptedException
    {
        AppbaseServiceEntry appbaseServiceEntry1 = appbaseServiceEntry;

        for (int i = 0; i < 5; ++i) {
            if (appbaseServiceEntry1.getServiceStatus() == ServiceStatus.STARTING) {
                serviceRegistry.pollAndUpdateStatuses();
                appbaseServiceEntry1 = serviceRegistry.getAppbaseService(appbaseServiceType);
                Thread.sleep(1000);
            } else {
                break;
            }
        }

        return appbaseServiceEntry1;
    }
}
