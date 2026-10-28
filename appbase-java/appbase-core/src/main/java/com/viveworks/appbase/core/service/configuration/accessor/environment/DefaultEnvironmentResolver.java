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

package com.viveworks.appbase.core.service.configuration.accessor.environment;

import com.viveworks.appbase.core.util.AppbaseConstants;
import com.viveworks.appbase.core.util.AppbaseUtil;

import javax.annotation.concurrent.ThreadSafe;

/**
 * Default implementation of {@link EnvironmentResolver}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@ThreadSafe
@SuppressWarnings("checkstyle:WeakerAccess")
public class DefaultEnvironmentResolver implements EnvironmentResolver {
    @Override
    public String resolveEnvironment() {
        String environment = System.getProperty(AppbaseConstants.APPBASE_SYSTEM_PROPERTY);
        if (AppbaseUtil.isBlank(environment)) {
            environment = System.getenv(AppbaseConstants.APPBASE_ENVIRONMENT_VARIABLE);
        }

        if (AppbaseUtil.isBlank(environment)) {
            return AppbaseConstants.DEFAULT_ENVIRONMENT;
        }

        return environment;
    }
}
