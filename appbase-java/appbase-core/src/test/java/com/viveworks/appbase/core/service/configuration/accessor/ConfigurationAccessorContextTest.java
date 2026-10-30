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

package com.viveworks.appbase.core.service.configuration.accessor;

import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.service.configuration.ConfigurationKey;
import com.viveworks.appbase.core.service.configuration.MimeType;
import com.viveworks.appbase.core.service.configuration.accessor.codec.ConfigurationCodec;
import com.viveworks.appbase.core.service.configuration.accessor.codec.PropertiesConfigurationCodec;
import com.viveworks.appbase.core.service.configuration.accessor.store.filesystem.ClasspathConfigurationStore;
import com.viveworks.appbase.core.service.mediator.DefaultMediatorServiceConfiguration;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import com.viveworks.appbase.core.util.AppbaseConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class ConfigurationAccessorContextTest {
    @Test
    public void testConfigurationAccessorContext() {
        final Map<MimeType, ConfigurationCodec> configurationCodecByMimeTypeMapping = new HashMap<>();
        final PropertiesConfigurationCodec propertiesConfigurationCodec = new PropertiesConfigurationCodec();
        configurationCodecByMimeTypeMapping.put(MimeType.PROPERTIES, propertiesConfigurationCodec);

        final Map<String, Class<? extends AppbaseConfiguration>>
                configurationTypeByRawConfigurationNameMapping = new HashMap<>();
        configurationTypeByRawConfigurationNameMapping.put(
                "mediatorServiceConfiguration",
                DefaultMediatorServiceConfiguration.class);

        final ClasspathConfigurationStore classpathConfigurationStore =
                new ClasspathConfigurationStore(
                        AppbaseConstants.CLASSPATH_FILE_PREFIX + "/appbase-core.yaml", MimeType.YAML);
        final Map<ConfigurationKey, Set<String>> writingConfigurationStoreIdsByKeyMapping = new ConcurrentHashMap<>();
        writingConfigurationStoreIdsByKeyMapping.put(
                CoreTestFixtures.getConfigurationKey(), Set.of(classpathConfigurationStore.getId()));

        final ConfigurationAccessorContext configurationAccessorContext = new ConfigurationAccessorContext(
                configurationCodecByMimeTypeMapping,
                configurationTypeByRawConfigurationNameMapping,
                writingConfigurationStoreIdsByKeyMapping);

        Assertions.assertNotNull(configurationAccessorContext.getConfigurationCodecByMimeTypeMapping());
        final ConfigurationCodec configurationCodec =
                configurationAccessorContext.findConfigurationCodec(MimeType.PROPERTIES);
        Assertions.assertNotNull(configurationCodec);
        Assertions.assertEquals(configurationCodec.getClass(), PropertiesConfigurationCodec.class);

        Assertions.assertNotNull(configurationAccessorContext.getConfigurationTypeByRawConfigurationNameMapping());
        final Class<? extends AppbaseConfiguration> type =
                configurationAccessorContext.findConfigurationType("mediatorServiceConfiguration");
        Assertions.assertNotNull(type);
        Assertions.assertEquals(type, DefaultMediatorServiceConfiguration.class);

        Assertions.assertNotNull(configurationAccessorContext.getRawConfigurationNameByConfigurationTypeNameMapping());
        Assertions.assertFalse(configurationAccessorContext
                .getRawConfigurationNameByConfigurationTypeNameMapping()
                .isEmpty());

        Assertions.assertNotNull(configurationAccessorContext.getWritingConfigurationStoreIdsByKeyMapping());
        final Set<String> writingConfigurationStoreIds =
                configurationAccessorContext.findWritingConfigurationStoreIds(CoreTestFixtures.getConfigurationKey());
        Assertions.assertNotNull(writingConfigurationStoreIds);


        final ConfigurationAccessorContext configurationAccessorContext1 = new ConfigurationAccessorContext(
                configurationCodecByMimeTypeMapping,
                configurationTypeByRawConfigurationNameMapping,
                writingConfigurationStoreIdsByKeyMapping);
        Assertions.assertEquals(configurationAccessorContext, configurationAccessorContext1);
        Assertions.assertEquals(configurationAccessorContext.hashCode(), configurationAccessorContext1.hashCode());
        Assertions.assertEquals(configurationAccessorContext.toString(), configurationAccessorContext1.toString());
    }
}
