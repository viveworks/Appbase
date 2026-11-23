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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.ThreadSafe;

/**
 * {@link PropertyDeserializer} implementation for <code>YAML</code>.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@ThreadSafe
@SuppressFBWarnings("IMC_IMMATURE_CLASS_NO_TOSTRING")
public class YamlPropertyDeserializer implements PropertyDeserializer {
    private final ObjectMapper yamlMapper;

    public YamlPropertyDeserializer() {
        this.yamlMapper = YamlUtil.getYamlMapper();
    }

    public YamlPropertyDeserializer(final ObjectMapper yamlMapper) {
        AppbaseUtil.notNull(yamlMapper, "yamlMapper");

        this.yamlMapper = yamlMapper;
    }

    @Override
    public <T extends AppbaseConfiguration> T deserialize(
            final String propertyValue, final Class<T> configurationType) throws PropertyDeserializerException
    {
        try {
            return yamlMapper
                    .readerFor(configurationType)
                    .readValue(propertyValue);
        } catch (JsonProcessingException jpe) {
            throw new PropertyDeserializerException(
                    "Error while de-serializing property into YAML " +
                            "[configurationType=" + configurationType +
                            ", propertyValue=" + propertyValue + "]",
                    jpe);
        }
    }
}
