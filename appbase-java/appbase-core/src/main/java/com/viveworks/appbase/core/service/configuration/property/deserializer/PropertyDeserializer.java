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

package com.viveworks.appbase.core.service.configuration.property.deserializer;

import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;

/**
 * Implementations of this class are used to deserialize properties to a configuration type by
 * {@link ConfigurationService}. They can be added using
 * {@link ConfigurationService#registerPropertyDeserializer(PropertyDeserializer)}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public interface PropertyDeserializer {
    /**
     * De-serialize property into an implementation of {@link AppbaseConfiguration}.
     *
     * @param propertyValue Property value
     * @param configurationType Configuration type
     * @param <T> Configuration type
     * @return Configuration
     * @throws PropertyDeserializerException if there are any errors while de-serializing
     */
    <T extends AppbaseConfiguration> T deserialize(
            String propertyValue, Class<T> configurationType) throws PropertyDeserializerException;
}
