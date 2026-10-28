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

package com.viveworks.appbase.core.util.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.core.util.AppbaseUtil;

/**
 * Provides a configured JSON {@link ObjectMapper}, for use within this module.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public final class JsonUtil {
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private JsonUtil() {
    }

    static {
        ObjectMapperConfigurator.configure(JSON_MAPPER);
    }

    /**
     * Register subtypes with {@link #JSON_MAPPER}.
     *
     * @param subType Sub type
     */
    public static void registerSubType(final Class<?> subType) {
        AppbaseUtil.notNull(subType, "subType");

        ObjectMapperConfigurator.registerSubType(JSON_MAPPER, subType);
    }

    /**
     * Register subtypes with {@link #JSON_MAPPER}.
     *
     * @param subType Sub type
     * @param registerWithYamlMapper If it should be registered with other types of {@link ObjectMapper}s.
     */
    public static void registerSubType(final Class<?> subType, final boolean registerWithYamlMapper) {
        AppbaseUtil.notNull(subType, "subType");

        ObjectMapperConfigurator.registerSubType(JSON_MAPPER, subType);

        if (registerWithYamlMapper) {
            YamlUtil.registerSubType(subType, true);
        }
    }

    /**
     * Get the configured JSON object mapper.
     *
     * @return JSON object mapper
     */
    public static ObjectMapper getJsonMapper() {
        return JSON_MAPPER;
    }
}
