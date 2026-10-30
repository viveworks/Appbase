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

package com.viveworks.appbase.core.service.configuration.microprofile;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.configuration.ConfigurationKey;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.configuration.ConfigurationValue;
import com.viveworks.appbase.core.service.configuration.DefaultConfigurationServiceTest;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationAddedEvent;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationChangedEvent;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationRemovedEvent;
import com.viveworks.appbase.core.service.log.DefaultLogService;
import com.viveworks.appbase.core.service.log.DefaultLogServiceConfiguration;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.DefaultMediatorService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.metric.DefaultMetricService;
import com.viveworks.appbase.core.service.metric.DefaultMetricServiceConfiguration;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.validation.DefaultValidationService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.Optional;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MicroprofileConfigurationServiceImplTest {
    private static LogService logService;
    private static MetricService metricService;
    private static ValidationService validationService;
    private static MediatorService mediatorService;
    private static MicroprofileConfigurationServiceImpl microprofileConfigurationService;

    @BeforeAll
    static void setUp() {
        logService =
                new DefaultLogService(
                        CoreTestFixtures.getMediatorEventDelegatorRegistry(),
                        DefaultLogServiceConfiguration.builder().build());
        metricService = new DefaultMetricService(
                logService,
                DefaultMetricServiceConfiguration.builder().build());
        validationService = new DefaultValidationService(logService, metricService);
        mediatorService = new DefaultMediatorService(
                logService,
                metricService,
                CoreTestFixtures.getSharedDisruptorService(),
                CoreTestFixtures.getMediatorEventDelegatorRegistry(),
                validationService);
        final ConfigurationService wrappedConfigurationService =
                DefaultConfigurationServiceTest.newConfigurationService(
                        logService, metricService, mediatorService, validationService, true);
        microprofileConfigurationService = new MicroprofileConfigurationServiceImpl(wrappedConfigurationService);

    }

    @AfterAll
    static void tearDown() throws Exception {
        if (microprofileConfigurationService != null) {
            microprofileConfigurationService.shutdown();
        }

        if (metricService != null) {
            metricService.shutdown();
        }

        if (validationService != null) {
            validationService.shutdown();
        }

        if (mediatorService != null) {
            mediatorService.shutdown();
        }

        if (logService != null) {
            logService.shutdown();
        }
    }

    @Test
    @Order(1)
    void testMicroprofileMethods() {
        final Iterable<ConfigSource> configSources = microprofileConfigurationService.getConfigSources();
        Assertions.assertNotNull(configSources);
        boolean found1 = false;
        for (final ConfigSource configSource : configSources) {
            Assertions.assertNotNull(configSource);
            found1 = true;
        }
        Assertions.assertTrue(found1);

        final Iterable<String> propertyNames = microprofileConfigurationService.getPropertyNames();
        Assertions.assertNotNull(propertyNames);
        boolean found2 = false;
        for (final String propertyName : propertyNames) {
            Assertions.assertNotNull(propertyName);
            found2 = true;
        }
        Assertions.assertTrue(found2);

        final String a1 = microprofileConfigurationService.getValue("a1", String.class);
        Assertions.assertNotNull(a1);

        final DefaultConfigurationServiceTest.SampleAppbaseServiceConfiguration4 value =
                microprofileConfigurationService.getValue(
                        DefaultConfigurationServiceTest.SampleAppbaseServiceConfiguration4.class.getName(),
                        DefaultConfigurationServiceTest.SampleAppbaseServiceConfiguration4.class);
        Assertions.assertNotNull(value);

        final Optional<String> optionalValue = microprofileConfigurationService.getOptionalValue("a1", String.class);
        Assertions.assertNotNull(optionalValue);
        Assertions.assertTrue(optionalValue.isPresent());
        final String value1 = optionalValue.get();
        Assertions.assertNotNull(value1);

        final Optional<String> optionalValue1 =
                microprofileConfigurationService.getOptionalValue("UNKNOWN_KEY_123_789", String.class);
        Assertions.assertNotNull(optionalValue1);
        Assertions.assertTrue(optionalValue1.isEmpty());
    }

    @Test
    @Order(2)
    void testOtherMethods() throws Exception {
        microprofileConfigurationService.putBoolean("t-bo1", true);
        Assertions.assertEquals(
                true, microprofileConfigurationService.getBoolean("t-bo1"));
        Assertions.assertEquals(
                false, microprofileConfigurationService.getBoolean("t-bo11", false));
        microprofileConfigurationService.removeBoolean("t-bo1");
        Assertions.assertFalse(microprofileConfigurationService.containsProperty("t-bo1"));

        microprofileConfigurationService.putByte("t-b1", (byte) 1);
        Assertions.assertEquals(
                (byte) 1, microprofileConfigurationService.getByte("t-b1"));
        Assertions.assertEquals(
                (byte) 0, microprofileConfigurationService.getByte("t-b11", (byte) 0));
        microprofileConfigurationService.removeByte("t-b1");
        Assertions.assertFalse(microprofileConfigurationService.containsProperty("t-b1"));

        final byte[] bytes = "value".getBytes();
        final byte[] bytes1 = "value11".getBytes();
        microprofileConfigurationService.putBytes("t-bs1", bytes);
        Assertions.assertEquals(
                bytes, microprofileConfigurationService.getBytes("t-bs1"));
        Assertions.assertEquals(
                bytes1, microprofileConfigurationService.getBytes("t-bs11", bytes1));
        microprofileConfigurationService.removeBytes("t-bs1");
        Assertions.assertFalse(microprofileConfigurationService.containsProperty("t-bs1"));

        microprofileConfigurationService.putCharacter("t-c1", 'a');
        Assertions.assertEquals(
                'a', microprofileConfigurationService.getCharacter("t-c1"));
        Assertions.assertEquals(
                'b', microprofileConfigurationService.getCharacter("t-c11", 'b'));
        microprofileConfigurationService.removeCharacter("t-c1");
        Assertions.assertFalse(microprofileConfigurationService.containsProperty("t-c1"));

        microprofileConfigurationService.putDouble("t-d1", 24.09);
        Assertions.assertEquals(
                24.09, microprofileConfigurationService.getDouble("t-d1"));
        Assertions.assertEquals(
                2424.0909, microprofileConfigurationService.getDouble("t-d11", 2424.0909));
        microprofileConfigurationService.removeDouble("t-d1");
        Assertions.assertFalse(microprofileConfigurationService.containsProperty("t-d1"));

        microprofileConfigurationService.putFloat("t-f1", 9.24F);
        Assertions.assertEquals(
                9.24F, microprofileConfigurationService.getFloat("t-f1"));
        Assertions.assertEquals(
                99.2424F, microprofileConfigurationService.getFloat("t-f11", 99.2424F));
        microprofileConfigurationService.removeFloat("t-f1");
        Assertions.assertFalse(microprofileConfigurationService.containsProperty("t-f1"));

        microprofileConfigurationService.putInteger("t-i1", 2409);
        Assertions.assertEquals(
                2409, microprofileConfigurationService.getInteger("t-i1"));
        Assertions.assertEquals(
                240924, microprofileConfigurationService.getInteger("t-i11", 240924));
        microprofileConfigurationService.removeInteger("t-i1");
        Assertions.assertFalse(microprofileConfigurationService.containsProperty("t-i1"));

        microprofileConfigurationService.putLong("t-l1", 24092409L);
        Assertions.assertEquals(
                24092409L, microprofileConfigurationService.getLong("t-l1"));
        Assertions.assertEquals(
                2409240L, microprofileConfigurationService.getLong("t-l11", 2409240L));
        microprofileConfigurationService.removeLong("t-l1");
        Assertions.assertFalse(microprofileConfigurationService.containsProperty("t-l1"));

        microprofileConfigurationService.putObject(ConfigurationKey.fromPropertyKey("t-o1"), "v1");
        Assertions.assertEquals(
                "v1", microprofileConfigurationService.getObject(ConfigurationKey.fromPropertyKey("t-o1")));
        Assertions.assertEquals(
                "v11", microprofileConfigurationService.getObject(ConfigurationKey.fromPropertyKey("t-o11"), "v11"));
        microprofileConfigurationService.removeObject(ConfigurationKey.fromPropertyKey("t-o1"));
        Assertions.assertFalse(microprofileConfigurationService.containsProperty("t-o1"));

        microprofileConfigurationService.putShort("t-s1", (short) 12);
        Assertions.assertEquals(
                (short) 12, microprofileConfigurationService.getShort("t-s1"));
        Assertions.assertEquals(
                (short) 122, microprofileConfigurationService.getShort("t-s11", (short) 122));
        microprofileConfigurationService.removeShort("t-s1");
        Assertions.assertFalse(microprofileConfigurationService.containsProperty("t-s1"));

        microprofileConfigurationService.putString("t-st1", "val1");
        Assertions.assertEquals(
                "val1", microprofileConfigurationService.getString("t-st1"));
        Assertions.assertEquals(
                "val11", microprofileConfigurationService.getString("t-st11", "val11"));
        microprofileConfigurationService.removeString("t-st1");
        Assertions.assertFalse(microprofileConfigurationService.containsProperty("t-st1"));

        final DefaultMetricServiceConfiguration defaultMetricServiceConfiguration =
                DefaultMetricServiceConfiguration.builder().build();
        microprofileConfigurationService.putTypedConfiguration(
                DefaultMetricServiceConfiguration.class, defaultMetricServiceConfiguration);
        Assertions.assertEquals(
                defaultMetricServiceConfiguration,
                microprofileConfigurationService.getTypedConfiguration(DefaultMetricServiceConfiguration.class));
        Assertions.assertEquals(
                defaultMetricServiceConfiguration,
                microprofileConfigurationService.getTypedConfiguration(
                        DefaultMetricServiceConfiguration.class, defaultMetricServiceConfiguration));
        Assertions.assertTrue(
                microprofileConfigurationService.containsTypedConfiguration(DefaultMetricServiceConfiguration.class));
        microprofileConfigurationService.removeTypedConfiguration(DefaultMetricServiceConfiguration.class);
        Assertions.assertFalse(
                microprofileConfigurationService.containsTypedConfiguration(DefaultMetricServiceConfiguration.class));


        Assertions.assertDoesNotThrow(() ->
                microprofileConfigurationService.load());
        Assertions.assertDoesNotThrow(() ->
                microprofileConfigurationService.getConfigurationAccessor());
        Assertions.assertDoesNotThrow(() ->
                microprofileConfigurationService.getConfiguration());
        Assertions.assertDoesNotThrow(() ->
                microprofileConfigurationService.getPropertyNames());
        Assertions.assertDoesNotThrow(() ->
                microprofileConfigurationService.getConfigurationByPropertyNameMap());
        Assertions.assertDoesNotThrow(() ->
                microprofileConfigurationService.getConfigurationByTypeNameMap());
        Assertions.assertDoesNotThrow(() ->
                microprofileConfigurationService.getConfigurationListenerByKeyMap());
        Assertions.assertDoesNotThrow(() ->
                microprofileConfigurationService.getGlobalConfigurationListeners());
        Assertions.assertDoesNotThrow(() ->
                microprofileConfigurationService.getLoadedConfigurationValueByPropertyNameMap());
        Assertions.assertDoesNotThrow(() ->
                microprofileConfigurationService.getLoadedConfigurationValueByTypeNameMap());
        Assertions.assertDoesNotThrow(() ->
                microprofileConfigurationService.getManagedServiceByConfigurationKeyMap());
        Assertions.assertDoesNotThrow(() ->
                microprofileConfigurationService.getNonValidatingTypeNames());
        Assertions.assertDoesNotThrow(() ->
                microprofileConfigurationService.getPropertyDeserializerAndConfigurationTypesByPropertyKeyMap());
        Assertions.assertDoesNotThrow(() ->
                microprofileConfigurationService.getPropertyDeserializers());
        Assertions.assertDoesNotThrow(() ->
                microprofileConfigurationService.getSecureFieldNamesByConfigurationTypeNameMap());
        Assertions.assertDoesNotThrow(() ->
                microprofileConfigurationService.getSecurePropertyKeys());
        Assertions.assertDoesNotThrow(() ->
                microprofileConfigurationService.getSecureTypeNames());
        Assertions.assertDoesNotThrow(() ->
                microprofileConfigurationService.getStatus());
        Assertions.assertNull(
                microprofileConfigurationService.getConfigurationContext(ConfigurationKey.fromPropertyKey("propKey")));


        Assertions.assertDoesNotThrow(() -> {
            final ConfigurationAddedEvent configurationAddedEvent = new ConfigurationAddedEvent(
                    ConfigurationKey.fromPropertyKey("pk1"),
                    ConfigurationValue.fromValue("value1"),
                    System.currentTimeMillis());
            microprofileConfigurationService.notifyConfigurationAddedEvent(configurationAddedEvent);
        });
        Assertions.assertDoesNotThrow(() -> {
            final ConfigurationChangedEvent configurationChangedEvent = new ConfigurationChangedEvent(
                    ConfigurationKey.fromPropertyKey("pk1"),
                    ConfigurationValue.fromValue("value1"),
                    ConfigurationValue.fromValue("value2"),
                    System.currentTimeMillis());
            microprofileConfigurationService.notifyConfigurationChangedEvent(configurationChangedEvent);
        });
        Assertions.assertDoesNotThrow(() -> {
            final ConfigurationRemovedEvent configurationRemovedEvent = new ConfigurationRemovedEvent(
                    ConfigurationKey.fromPropertyKey("pk1"),
                    System.currentTimeMillis());
            microprofileConfigurationService.notifyConfigurationRemovedEvent(configurationRemovedEvent);
        });


        Assertions.assertThrows(AppbaseRuntimeException.class,
                () -> microprofileConfigurationService.registerConfigurationListenerByKey(null, null));
        Assertions.assertThrows(AppbaseRuntimeException.class,
                () -> microprofileConfigurationService.registerGlobalConfigurationListener(null));
        Assertions.assertThrows(AppbaseRuntimeException.class,
                () -> microprofileConfigurationService.registerManagedServiceByConfigurationKey(null, null));
        Assertions.assertThrows(AppbaseRuntimeException.class,
                () -> microprofileConfigurationService.registerNonValidatingType(null));
        Assertions.assertThrows(AppbaseRuntimeException.class,
                () -> microprofileConfigurationService.registerPropertyDeserializer(null));
        Assertions.assertThrows(AppbaseRuntimeException.class,
                () -> microprofileConfigurationService
                        .registerPropertyDeserializerAndConfigurationTypesByPropertyKey(null, null));
        Assertions.assertThrows(AppbaseRuntimeException.class,
                () -> microprofileConfigurationService.registerSecureFieldByConfigurationType(null, null));
        Assertions.assertThrows(AppbaseRuntimeException.class,
                () -> microprofileConfigurationService.registerSecurePropertyKey(null));
        Assertions.assertThrows(AppbaseRuntimeException.class,
                () -> microprofileConfigurationService.registerSecureType(null));


        Assertions.assertThrows(AppbaseRuntimeException.class,
                () -> microprofileConfigurationService.unregisterConfigurationListenerByKey(null, null));
        Assertions.assertThrows(AppbaseRuntimeException.class,
                () -> microprofileConfigurationService.unregisterGlobalConfigurationListener(null));
        Assertions.assertThrows(AppbaseRuntimeException.class,
                () -> microprofileConfigurationService.unregisterManagedServiceByConfigurationKey(null));
        Assertions.assertThrows(AppbaseRuntimeException.class,
                () -> microprofileConfigurationService.unregisterManagedServiceByConfigurationKey(null, null));
        Assertions.assertThrows(AppbaseRuntimeException.class,
                () -> microprofileConfigurationService.unregisterNonValidatingType(null));
        Assertions.assertThrows(AppbaseRuntimeException.class,
                () -> microprofileConfigurationService.unregisterPropertyDeserializer(null));
        Assertions.assertThrows(AppbaseRuntimeException.class,
                () -> microprofileConfigurationService
                        .unregisterPropertyDeserializerAndConfigurationTypesByPropertyKey(null));
        Assertions.assertThrows(AppbaseRuntimeException.class,
                () -> microprofileConfigurationService.unregisterSecureFieldByConfigurationType(null, null));
        Assertions.assertThrows(AppbaseRuntimeException.class,
                () -> microprofileConfigurationService.unregisterSecurePropertyKey(null));
        Assertions.assertThrows(AppbaseRuntimeException.class,
                () -> microprofileConfigurationService.unregisterSecureType(null));


        Assertions.assertDoesNotThrow(() ->
                microprofileConfigurationService.clearConfigurationListenerByKeyMap());
        Assertions.assertDoesNotThrow(() ->
                microprofileConfigurationService.clearGlobalConfigurationListeners());
        Assertions.assertDoesNotThrow(() ->
                microprofileConfigurationService.clearManagedServiceByConfigurationKeyMap());
        Assertions.assertDoesNotThrow(() ->
                microprofileConfigurationService.clearNonValidatingTypes());
        Assertions.assertDoesNotThrow(() ->
                microprofileConfigurationService.clearSecurePropertyKeys());
        Assertions.assertDoesNotThrow(() ->
                microprofileConfigurationService.clearPropertyDeserializerAndConfigurationTypesByPropertyKeyMap());
        Assertions.assertDoesNotThrow(() ->
                microprofileConfigurationService.clearPropertyDeserializers());
        Assertions.assertDoesNotThrow(() ->
                microprofileConfigurationService.clearSecureFieldByConfigurationTypeMap());
        Assertions.assertDoesNotThrow(() ->
                microprofileConfigurationService.clearSecureTypes());


        Assertions.assertThrows(AppbaseRuntimeException.class, () -> microprofileConfigurationService.reload(null));
    }
}
