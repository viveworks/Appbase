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

package com.viveworks.appbase.core.service.configuration.util;

import com.viveworks.appbase.core.util.AppbaseUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

/**
 * Unit tests for {@link ConfigurationsJson}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class ConfigurationsJsonTest {

    private static final String CONFIGURATION_BY_PROPERTY_NAME_SENTINEL =
            "sentinel-configsjson-configbypropertyname-CFGLEAK07";
    private static final String LOADED_CONFIGURATION_VALUE_BY_PROPERTY_NAME_SENTINEL =
            "sentinel-configsjson-loadedvalbypropertyname-CFGLEAK07";
    private static final String CONFIGURATION_BY_TYPE_NAME_SENTINEL =
            "sentinel-configsjson-configbytypename-CFGLEAK07";
    private static final String LOADED_CONFIGURATION_VALUE_BY_TYPE_NAME_SENTINEL =
            "sentinel-configsjson-loadedvalbytypename-CFGLEAK07";

    private static ConfigurationsJson newConfigurationsJsonWithSentinels() {
        return new ConfigurationsJson(
                Set.of("some.secure.property.key"),
                Set.of("some.secure.type.name"),
                Map.of("some.secure.type.name", Set.of("someField")),
                Map.of("some.property.key", CONFIGURATION_BY_PROPERTY_NAME_SENTINEL),
                Map.of("some.property.key", LOADED_CONFIGURATION_VALUE_BY_PROPERTY_NAME_SENTINEL),
                Map.of("some.type.name", CONFIGURATION_BY_TYPE_NAME_SENTINEL),
                Map.of("some.type.name", LOADED_CONFIGURATION_VALUE_BY_TYPE_NAME_SENTINEL),
                Map.of("some.configuration.key", "com.example.SomeManagedService"),
                Set.of("com.example.SomeConfigSource"),
                Map.of(
                        "some.property.key",
                        new ConfigurationsJson.PropertyDeserializerAndConfigurationTypeNames(
                                "com.example.SomeDeserializer", "com.example.SomeConfigurationType")),
                Set.of("com.example.SomeNonValidatingType"),
                Set.of("com.example.SomeGlobalConfigurationListener"),
                Map.of("some.configuration.key", "com.example.SomeConfigurationListener"));
    }

    @Test
    void toStringDoesNotDiscloseResolvedConfigurationValueMaps() {
        // CFG-LEAK-07 (CWE-532, CWE-200): configurationByPropertyNameMap,
        // loadedConfigurationValueByPropertyNameMap, configurationByTypeNameMap and
        // loadedConfigurationValueByTypeNameMap carry resolved/raw configuration VALUES — including
        // unclassified secrets, since filterSecureConfig may be false at build() time. toString()
        // logged all four in plaintext. All four must be redacted in toString(), matching the
        // pattern DruidConnectionConfiguration already applies to its own open-ended/value-bearing
        // fields.
        //
        // The remaining toString() fields (securePropertyKeys, managedServiceClassNameByConfigurationKeyMap,
        // configurationListenerClassNameByKeyMap, configSourceClassNames,
        // propertyDeserializerAndConfigurationTypeNamesByPropertyKeyMap, nonValidatingTypeNames,
        // globalConfigurationListenerClassNames) carry only property/type KEY NAMES or Java
        // class/type names — never a resolved configuration VALUE — so they are left unredacted.
        final ConfigurationsJson configurationsJson = newConfigurationsJsonWithSentinels();

        final String result = configurationsJson.toString();

        Assertions.assertFalse(
                result.contains(CONFIGURATION_BY_PROPERTY_NAME_SENTINEL),
                "toString() must not contain configurationByPropertyNameMap's content");
        Assertions.assertFalse(
                result.contains(LOADED_CONFIGURATION_VALUE_BY_PROPERTY_NAME_SENTINEL),
                "toString() must not contain loadedConfigurationValueByPropertyNameMap's content");
        Assertions.assertFalse(
                result.contains(CONFIGURATION_BY_TYPE_NAME_SENTINEL),
                "toString() must not contain configurationByTypeNameMap's content");
        Assertions.assertFalse(
                result.contains(LOADED_CONFIGURATION_VALUE_BY_TYPE_NAME_SENTINEL),
                "toString() must not contain loadedConfigurationValueByTypeNameMap's content");
        Assertions.assertTrue(result.contains(AppbaseUtil.OBFUSCATE), "toString() must obfuscate the redacted fields");
    }

    @Test
    void gettersStillReturnTheRealValues() {
        final ConfigurationsJson configurationsJson = newConfigurationsJsonWithSentinels();

        Assertions.assertEquals(
                Map.of("some.property.key", CONFIGURATION_BY_PROPERTY_NAME_SENTINEL),
                configurationsJson.getConfigurationByPropertyNameMap());
        Assertions.assertEquals(
                Map.of("some.property.key", LOADED_CONFIGURATION_VALUE_BY_PROPERTY_NAME_SENTINEL),
                configurationsJson.getLoadedConfigurationValueByPropertyNameMap());
        Assertions.assertEquals(
                Map.of("some.type.name", CONFIGURATION_BY_TYPE_NAME_SENTINEL),
                configurationsJson.getConfigurationByTypeNameMap());
        Assertions.assertEquals(
                Map.of("some.type.name", LOADED_CONFIGURATION_VALUE_BY_TYPE_NAME_SENTINEL),
                configurationsJson.getLoadedConfigurationValueByTypeNameMap());
    }
}
