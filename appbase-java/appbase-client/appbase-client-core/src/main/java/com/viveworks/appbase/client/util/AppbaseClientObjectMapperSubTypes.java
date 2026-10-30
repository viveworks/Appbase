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

package com.viveworks.appbase.client.util;

import com.viveworks.appbase.client.core.clientkey.DefaultMutableAppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.HostPortValue;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValue;
import com.viveworks.appbase.client.core.pool.DefaultAppbaseClientPoolConfiguration;
import com.viveworks.appbase.client.core.pool.manager.DefaultAppbaseClientPoolManagerConfiguration;
import com.viveworks.appbase.core.util.jackson.AppbaseCoreObjectMapperSubTypes;

/**
 * Used to register all subtypes with {@link com.fasterxml.jackson.databind.ObjectMapper}s.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public final class AppbaseClientObjectMapperSubTypes {
    private AppbaseClientObjectMapperSubTypes() {
    }

    /**
     * Register all {@code core} subtypes with {@link com.fasterxml.jackson.databind.ObjectMapper}s.
     */
    public static void register() {
        AppbaseCoreObjectMapperSubTypes.register(HostPortValue.class);
        AppbaseCoreObjectMapperSubTypes.register(TextValue.class);
        AppbaseCoreObjectMapperSubTypes.register(DefaultMutableAppbaseClientKey.class);
        AppbaseCoreObjectMapperSubTypes.register(DefaultAppbaseClientPoolConfiguration.class);
        AppbaseCoreObjectMapperSubTypes.register(DefaultAppbaseClientPoolManagerConfiguration.class);
    }
}
