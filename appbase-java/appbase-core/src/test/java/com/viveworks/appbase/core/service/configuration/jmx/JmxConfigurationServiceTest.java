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

package com.viveworks.appbase.core.service.configuration.jmx;

import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.configuration.DefaultConfigurationServiceConfiguration;
import com.viveworks.appbase.core.service.configuration.util.ConfigurationsJson;
import com.viveworks.appbase.core.service.configuration.property.deserializer.YamlPropertyDeserializer;
import com.viveworks.appbase.core.service.jmx.DefaultJmxService;
import com.viveworks.appbase.core.service.jmx.DefaultJmxServiceConfiguration;
import com.viveworks.appbase.core.service.jmx.mxbean.entry.MXBeanEntry;
import com.viveworks.appbase.core.service.log.DefaultLogService;
import com.viveworks.appbase.core.service.log.DefaultLogServiceConfiguration;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.DefaultMediatorService;
import com.viveworks.appbase.core.service.mediator.DefaultMediatorServiceConfiguration;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.metric.DefaultMetricService;
import com.viveworks.appbase.core.service.metric.DefaultMetricServiceConfiguration;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.registry.DefaultServiceRegistry;
import com.viveworks.appbase.core.service.validation.DefaultValidationService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.AdditionalAnswers;

import javax.management.MBeanException;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class JmxConfigurationServiceTest {
    private static LogService logService;
    private static MetricService metricService;
    private static ValidationService validationService;
    private static MediatorService mediatorService;
    private static ConfigurationService configurationService;
    private static DefaultJmxService jmxService;
    private static JmxConfigurationService jmxConfigurationService;

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
        configurationService = CoreTestFixtures.getEmptyDefaultConfigurationService(
                logService, metricService, mediatorService, validationService);
        configurationService.putTypedConfiguration(
                DefaultJmxServiceConfiguration.class,
                DefaultJmxServiceConfiguration.builder().build());
        jmxService = new DefaultJmxService(
                logService, metricService, mediatorService, validationService, configurationService);
        // AUD-08: reload()/shutdown() below are now denied by default (owner decision DQ-1).
        CoreTestFixtures.grantJmxOperationPermissions(configurationService, JmxConfigurationService.class);
        jmxConfigurationService = new JmxConfigurationService(
                logService, configurationService, DefaultConfigurationServiceConfiguration.class, configurationService);
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (jmxConfigurationService != null) {
            jmxConfigurationService.shutdown();
        }

        if (jmxService != null) {
            jmxService.shutdown();
        }

        if (configurationService != null) {
            configurationService.shutdown();
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
    void restrictedReadsDoNotExposeRegisteredSecrets() throws Exception {
        final String secretKey = "restricted-read-secret";
        final String secretValue = "credential-sentinel-2751";
        final String typeName = DefaultMediatorServiceConfiguration.class.getName();
        configurationService.putString(secretKey, secretValue);
        configurationService.registerSecurePropertyKey(secretKey);
        configurationService.putTypedConfiguration(
                DefaultMediatorServiceConfiguration.class,
                DefaultMediatorServiceConfiguration.builder().build());
        configurationService.registerSecureType(DefaultMediatorServiceConfiguration.class);
        Assertions.assertEquals(secretValue, jmxConfigurationService.getString(secretKey));
        Assertions.assertNotNull(jmxConfigurationService.getTypedConfigurationYaml(typeName));
        Assertions.assertTrue(configurationService.getSecurePropertyKeys().contains(secretKey));
        Assertions.assertTrue(configurationService.getSecureTypeNames().contains(typeName));
        final JmxConfigurationService snapshotBean = freshSnapshotBean();
        final ConfigurationsJson unrestrictedSnapshot = snapshotBean.getConfigurationsJson();
        Assertions.assertEquals(secretValue,
                unrestrictedSnapshot.getConfigurationByPropertyNameMap().get(secretKey));
        Assertions.assertTrue(unrestrictedSnapshot.getConfigurationByTypeNameMap().containsKey(typeName));
        configurationService.putBoolean(
                JmxConfigurationService.SECURE_CONFIG_OPERATIONS_PERMISSION_PROPERTY, false);
        try {
            final ConfigurationsJson restrictedSnapshot = snapshotBean.getConfigurationsJson();
            Assertions.assertFalse(restrictedSnapshot.getConfigurationByPropertyNameMap().containsKey(secretKey));
            Assertions.assertFalse(restrictedSnapshot.getConfigurationByTypeNameMap().containsKey(typeName));
            Assertions.assertNull(jmxConfigurationService.getString(secretKey));
            Assertions.assertEquals("safe default", jmxConfigurationService.getString(secretKey, "safe default"));
            Assertions.assertNull(jmxConfigurationService.getTypedConfigurationYaml(typeName));
        } finally {
            configurationService.removeBoolean(
                    JmxConfigurationService.SECURE_CONFIG_OPERATIONS_PERMISSION_PROPERTY);
            Assertions.assertEquals(secretValue, jmxConfigurationService.getString(secretKey));
            Assertions.assertNotNull(jmxConfigurationService.getTypedConfigurationYaml(typeName));
            configurationService.unregisterSecurePropertyKey(secretKey);
            configurationService.unregisterSecureType(DefaultMediatorServiceConfiguration.class);
            configurationService.removeString(secretKey);
            configurationService.removeTypedConfiguration(DefaultMediatorServiceConfiguration.class);
        }
    }

    @Test
    void restrictedTypeReadAlsoHidesConfigurationsWithSecureFields() throws Exception {
        final String typeName = DefaultMediatorServiceConfiguration.class.getName();
        configurationService.putTypedConfiguration(
                DefaultMediatorServiceConfiguration.class,
                DefaultMediatorServiceConfiguration.builder().build());
        configurationService.registerSecureFieldByConfigurationType(
                DefaultMediatorServiceConfiguration.class, "messageObjectPoolCapacity");
        final JmxConfigurationService snapshotBean = freshSnapshotBean();
        Assertions.assertTrue(snapshotBean.getConfigurationsJson()
                .getConfigurationByTypeNameMap().containsKey(typeName));
        configurationService.putBoolean(
                JmxConfigurationService.SECURE_CONFIG_OPERATIONS_PERMISSION_PROPERTY, false);
        try {
            Assertions.assertTrue(configurationService.getSecureFieldNamesByConfigurationTypeNameMap()
                    .containsKey(typeName));
            Assertions.assertFalse(snapshotBean.getConfigurationsJson()
                    .getConfigurationByTypeNameMap().containsKey(typeName));
            Assertions.assertNull(jmxConfigurationService.getTypedConfigurationYaml(typeName));
        } finally {
            configurationService.removeBoolean(
                    JmxConfigurationService.SECURE_CONFIG_OPERATIONS_PERMISSION_PROPERTY);
            configurationService.unregisterSecureFieldByConfigurationType(
                    DefaultMediatorServiceConfiguration.class, "messageObjectPoolCapacity");
            configurationService.removeTypedConfiguration(DefaultMediatorServiceConfiguration.class);
        }
    }

    private static JmxConfigurationService freshSnapshotBean() {
        // The shared fixture keeps snapshots from its last accessor load. Build both views from
        // its current values/registrations, then exercise the real JMX snapshot selection path.
        final ConfigurationService snapshotSource = mock(
                ConfigurationService.class, AdditionalAnswers.delegatesTo(configurationService));
        doReturn(ConfigurationsJson.build(configurationService, false))
                .when(snapshotSource).getConfigurationsJson();
        doReturn(ConfigurationsJson.build(configurationService, true))
                .when(snapshotSource).getSecureConfigurationsJson();
        return new JmxConfigurationService(
                logService, snapshotSource, DefaultConfigurationServiceConfiguration.class, snapshotSource);
    }

    @Test
    void testMXBean() throws Exception {
        final Class<? extends JmxConfigurationService> jmxConfigurationServiceType = jmxConfigurationService.getClass();
        if (jmxService.isMXBeanRegistered(jmxConfigurationServiceType)) {
            jmxService.unregisterMXBean(jmxConfigurationServiceType);
        }
        jmxService.registerMXBean(jmxConfigurationService);

        final JmxConfigurationService mxBean = jmxService.getMXBean(JmxConfigurationService.class);
        Assertions.assertNotNull(mxBean);

        final MXBeanEntry mxBeanEntry = jmxService.getMXBeanEntry(JmxConfigurationService.class);
        Assertions.assertNotNull(mxBeanEntry);
        Assertions.assertEquals(JmxConfigurationService.class.getName(), mxBeanEntry.getClassName());
    }

    @Test
    void testConfigurations() throws Exception {
        jmxConfigurationService.putBoolean("t-bo1", true);
        Assertions.assertEquals(
                true, jmxConfigurationService.getBoolean("t-bo1"));
        Assertions.assertEquals(
                false, jmxConfigurationService.getBoolean("t-bo11", false));
        jmxConfigurationService.removeBoolean("t-bo1");
        Assertions.assertFalse(jmxConfigurationService.containsProperty("t-bo1"));

        jmxConfigurationService.putByte("t-b1", (byte) 1);
        Assertions.assertEquals(
                (byte) 1, jmxConfigurationService.getByte("t-b1"));
        Assertions.assertEquals(
                (byte) 0, jmxConfigurationService.getByte("t-b11", (byte) 0));
        jmxConfigurationService.removeByte("t-b1");
        Assertions.assertFalse(jmxConfigurationService.containsProperty("t-b1"));

        final byte[] bytes = "value".getBytes();
        final byte[] bytes1 = "value11".getBytes();
        jmxConfigurationService.putBytes("t-bs1", bytes);
        Assertions.assertEquals(
                bytes, jmxConfigurationService.getBytes("t-bs1"));
        Assertions.assertEquals(
                bytes1, jmxConfigurationService.getBytes("t-bs11", bytes1));
        jmxConfigurationService.removeBytes("t-bs1");
        Assertions.assertFalse(jmxConfigurationService.containsProperty("t-bs1"));

        jmxConfigurationService.putCharacter("t-c1", 'a');
        Assertions.assertEquals(
                'a', jmxConfigurationService.getCharacter("t-c1"));
        Assertions.assertEquals(
                'b', jmxConfigurationService.getCharacter("t-c11", 'b'));
        jmxConfigurationService.removeCharacter("t-c1");
        Assertions.assertFalse(jmxConfigurationService.containsProperty("t-c1"));

        jmxConfigurationService.putDouble("t-d1", 24.09);
        Assertions.assertEquals(
                24.09, jmxConfigurationService.getDouble("t-d1"));
        Assertions.assertEquals(
                2424.0909, jmxConfigurationService.getDouble("t-d11", 2424.0909));
        jmxConfigurationService.removeDouble("t-d1");
        Assertions.assertFalse(jmxConfigurationService.containsProperty("t-d1"));

        jmxConfigurationService.putFloat("t-f1", 9.24F);
        Assertions.assertEquals(
                9.24F, jmxConfigurationService.getFloat("t-f1"));
        Assertions.assertEquals(
                99.2424F, jmxConfigurationService.getFloat("t-f11", 99.2424F));
        jmxConfigurationService.removeFloat("t-f1");
        Assertions.assertFalse(jmxConfigurationService.containsProperty("t-f1"));

        jmxConfigurationService.putInteger("t-i1", 2409);
        Assertions.assertEquals(
                2409, jmxConfigurationService.getInteger("t-i1"));
        Assertions.assertEquals(
                240924, jmxConfigurationService.getInteger("t-i11", 240924));
        jmxConfigurationService.removeInteger("t-i1");
        Assertions.assertFalse(jmxConfigurationService.containsProperty("t-i1"));

        jmxConfigurationService.putLong("t-l1", 24092409L);
        Assertions.assertEquals(
                24092409L, jmxConfigurationService.getLong("t-l1"));
        Assertions.assertEquals(
                2409240L, jmxConfigurationService.getLong("t-l11", 2409240L));
        jmxConfigurationService.removeLong("t-l1");
        Assertions.assertFalse(jmxConfigurationService.containsProperty("t-l1"));

        jmxConfigurationService.putShort("t-s1", (short) 12);
        Assertions.assertEquals(
                (short) 12, jmxConfigurationService.getShort("t-s1"));
        Assertions.assertEquals(
                (short) 122, jmxConfigurationService.getShort("t-s11", (short) 122));
        jmxConfigurationService.removeShort("t-s1");
        Assertions.assertFalse(jmxConfigurationService.containsProperty("t-s1"));

        jmxConfigurationService.putString("t-st1", "val1");
        Assertions.assertEquals(
                "val1", jmxConfigurationService.getString("t-st1"));
        Assertions.assertEquals(
                "val11", jmxConfigurationService.getString("t-st11", "val11"));
        jmxConfigurationService.removeString("t-st1");
        Assertions.assertFalse(jmxConfigurationService.containsProperty("t-st1"));

        final DefaultMediatorServiceConfiguration defaultMediatorServiceConfiguration =
                DefaultMediatorServiceConfiguration.builder().build();
        final String defaultMediatorServiceConfigurationYaml = YamlUtil
                .getYamlMapper()
                .writerFor(DefaultMediatorServiceConfiguration.class)
                .writeValueAsString(defaultMediatorServiceConfiguration);
        jmxConfigurationService.putTypedConfigurationYaml(
                defaultMediatorServiceConfigurationYaml,
                DefaultMediatorServiceConfiguration.class.getName());
        final String typedConfigurationYaml =
                jmxConfigurationService.getTypedConfigurationYaml(DefaultMediatorServiceConfiguration.class.getName());
        Assertions.assertNotNull(typedConfigurationYaml);
        Assertions.assertEquals(defaultMediatorServiceConfigurationYaml, typedConfigurationYaml);
        jmxConfigurationService.removeTypedConfigurationYaml(DefaultMediatorServiceConfiguration.class.getName());
        Assertions.assertFalse(jmxConfigurationService
                .containsTypedConfiguration(DefaultMediatorServiceConfiguration.class.getName()));

        Assertions.assertDoesNotThrow(
                () -> jmxConfigurationService.unregisterPropertyDeserializerAndConfigurationTypesByPropertyKey("k1"));
        Assertions.assertDoesNotThrow(() ->
                jmxConfigurationService.clearPropertyDeserializerAndConfigurationTypesByPropertyKeyMap());

        Assertions.assertDoesNotThrow(
                () -> jmxConfigurationService.unregisterPropertyDeserializer(YamlPropertyDeserializer.class.getName()));
        Assertions.assertDoesNotThrow(() -> jmxConfigurationService.clearPropertyDeserializers());

        Assertions.assertNotNull(jmxConfigurationService.getSecurePropertyKeys());
        Assertions.assertDoesNotThrow(() -> jmxConfigurationService.unregisterSecurePropertyKey("k1"));
        Assertions.assertDoesNotThrow(() -> jmxConfigurationService.clearSecurePropertyKeys());

        Assertions.assertNotNull(jmxConfigurationService.getNonValidatingTypeNames());
        Assertions.assertDoesNotThrow(() ->
                jmxConfigurationService.unregisterNonValidatingTypeName(
                        DefaultMediatorServiceConfiguration.class.getName()));
        Assertions.assertDoesNotThrow(() -> jmxConfigurationService.clearNonValidatingTypeNames());

        Assertions.assertNotNull(jmxConfigurationService.getSecureTypeNames());
        Assertions.assertDoesNotThrow(() ->
                jmxConfigurationService.unregisterSecureType(DefaultMediatorServiceConfiguration.class.getName()));
        Assertions.assertDoesNotThrow(() -> jmxConfigurationService.clearSecureTypes());

        Assertions.assertDoesNotThrow(() ->
                jmxConfigurationService.unregisterSecureFieldByConfigurationType(
                        DefaultMediatorServiceConfiguration.class.getName(), "messageObjectPoolCapacity"));
        Assertions.assertDoesNotThrow(() ->
                jmxConfigurationService.clearSecureFieldByConfigurationTypeMap());

        Assertions.assertDoesNotThrow(() ->
                jmxConfigurationService.unregisterGlobalConfigurationListener(DefaultServiceRegistry.class.getName()));
        Assertions.assertDoesNotThrow(() ->
                jmxConfigurationService.clearGlobalConfigurationListeners());

        Assertions.assertDoesNotThrow(() ->
                jmxConfigurationService.unregisterConfigurationListenerByKey(new JmxConfigurationKey("key1", null)));
        Assertions.assertDoesNotThrow(() ->
                jmxConfigurationService.unregisterConfigurationListenerByKey(
                        new JmxConfigurationKey(null, DefaultMediatorServiceConfiguration.class.getName())));
        Assertions.assertDoesNotThrow(() ->
                jmxConfigurationService.clearConfigurationListenerByKeyMap());

        Assertions.assertDoesNotThrow(() -> jmxConfigurationService.load());

        Assertions.assertEquals(ServiceStatus.STARTED, jmxConfigurationService.getStatus());
        Assertions.assertDoesNotThrow(() -> jmxConfigurationService.reload());

        final DefaultConfigurationServiceConfiguration defaultConfigurationServiceConfiguration =
                DefaultConfigurationServiceConfiguration.builder().build();
        final String yaml = YamlUtil
                .getYamlMapper()
                .writerFor(DefaultConfigurationServiceConfiguration.class)
                .writeValueAsString(defaultConfigurationServiceConfiguration);
        Assertions.assertDoesNotThrow(() -> jmxConfigurationService.reload(yaml));
    }

    /**
     * A separate side channel the fixture class's static initializer writes to. Reading this
     * directly, rather than a field declared ON the fixture class, matters: reading a non-constant
     * static field of a class is itself an "active use" under JLS 12.4.1 and would initialize that
     * class as a side effect of the ASSERTION, making the check assert nothing.
     */
    static final class InitializationTracker {
        static final java.util.concurrent.atomic.AtomicBoolean WAS_INITIALIZED =
                new java.util.concurrent.atomic.AtomicBoolean(false);
    }

    /**
     * Not an {@link com.viveworks.appbase.core.service.AppbaseConfiguration}, used only to detect
     * whether {@code putTypedConfigurationYaml} initializes an arbitrary named class as a side
     * effect of merely naming it, before any type check runs. {@code NotAnAppbaseConfiguration.class}
     * (a class literal) and {@code .getName()} do NOT trigger initialization, so referencing the
     * class that way in the test below is safe.
     */
    static final class NotAnAppbaseConfiguration {
        static {
            InitializationTracker.WAS_INITIALIZED.set(true);
        }
    }

    /**
     * Lens 4 F3 / Lens 5 (fourth pass) S1 regression: {@code putTypedConfigurationYaml} resolved
     * its type-name argument via {@code Class.forName(name)} -- which initializes the class -- and
     * only discovered it was not an {@code AppbaseConfiguration} later, via a {@code ClassCastException}
     * from the generic-cast checkcast on assignment, deep inside the try block. A caller naming an
     * arbitrary class triggers that class's static initializer merely by naming it to this
     * permission-gated MBean operation, before any constraint is applied. The fix must resolve
     * without initializing and reject before any side effect runs.
     */
    @Test
    void testPutTypedConfigurationYamlRejectsANonAppbaseConfigurationTypeWithoutInitializingIt() {
        Assertions.assertFalse(InitializationTracker.WAS_INITIALIZED.get(),
                "the fixture class must not already be initialized when this test starts");

        Assertions.assertThrows(MBeanException.class, () ->
                jmxConfigurationService.putTypedConfigurationYaml("{}", NotAnAppbaseConfiguration.class.getName()));

        Assertions.assertFalse(InitializationTracker.WAS_INITIALIZED.get(),
                "a type name that is not an AppbaseConfiguration must be rejected before Class.forName "
                        + "triggers its static initializer as a side effect");
    }
}
