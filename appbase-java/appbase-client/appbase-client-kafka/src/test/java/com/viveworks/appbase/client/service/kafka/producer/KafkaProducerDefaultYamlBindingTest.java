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

package com.viveworks.appbase.client.service.kafka.producer;

import com.viveworks.appbase.client.service.kafka.producer.configuration.DefaultKafkaProducerClientIntegrationServiceConfiguration;
import com.viveworks.appbase.client.service.kafka.producer.configuration.DefaultKafkaProducerClientServiceConfiguration;
import com.viveworks.appbase.client.util.AppbaseClientObjectMapperSubTypes;
import com.viveworks.appbase.core.util.AppbaseConstants;
import com.viveworks.appbase.core.util.jackson.AppbaseCoreObjectMapperSubTypes;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

/**
 * Pins that the Kafka <b>producer</b> default YAML actually <em>binds</em> the serializers it
 * declares -- the configuration {@code ReactiveKafkaAccessorIT} boots from.
 *
 * <p>🔴 <b>Nothing failed, and that is the whole problem.</b> Both producer YAMLs declared their
 * serdes as {@code keyDeserializerType}/{@code valueDeserializerType} -- the <b>consumer</b>
 * spelling. {@link DefaultKafkaProducerClientIntegrationServiceConfiguration} has no deserializer
 * concept at all; it reads {@code keySerializerType}/{@code valueSerializerType}. The object mapper
 * runs with {@code FAIL_ON_UNKNOWN_PROPERTIES = false}, so both keys were silently discarded and the
 * builder fell back to {@code DEFAULT_KEY_SERIALIZER_TYPE = StringSerializer}. The discarded value
 * and the silent default were the same serializer, which is exactly why it survived: <b>configured
 * -under-the-wrong-name and defaulted-under-the-right-name produced the same object.</b> The declared
 * class was wrong too -- {@code StringDeserializer} is not a {@code Serializer} -- so the block was
 * copied from the consumer and had never bound.
 *
 * <p>The masking is what matters: anyone who later set a binary serializer would edit the key that is
 * thrown away, see no error, and watch the producer keep using {@code StringSerializer}.
 *
 * <p>The default-YAML checks verify declared names and selected configuration values. Because
 * those serializers equal the builder defaults, they cannot alone distinguish successful binding
 * from silent fallback. A separate non-default YAML fixture makes each getter observable: ignoring
 * either serializer property must fail its own test. No assertion caps the configuration field count.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class KafkaProducerDefaultYamlBindingTest {

    private static final String PRODUCER_YAML =
            AppbaseConstants.CLASSPATH_FILE_PREFIX + "/appbase-client-kafka-producer.yaml";
    private static final String PRODUCER_CONTAINER_YAML =
            AppbaseConstants.CLASSPATH_FILE_PREFIX + "/appbase-client-kafka-producer-container.yaml";

    private static final String DEFAULT_CONFIG_NODE =
            "defaultDefaultKafkaProducerClientIntegrationServiceConfiguration";
    private static final String KEY_SERIALIZER_KEY = "keySerializerType";
    private static final String VALUE_SERIALIZER_KEY = "valueSerializerType";

    /** The consumer-side spelling the producer configuration silently discards. */
    private static final String DISCARDED_KEY_SPELLING = "keyDeserializerType";
    private static final String DISCARDED_VALUE_SPELLING = "valueDeserializerType";

    @BeforeAll
    static void setUp() {
        AppbaseCoreObjectMapperSubTypes.register();
        AppbaseClientObjectMapperSubTypes.register();
        DefaultKafkaProducerClientService.registerObjectMapperSubtypes();
    }

    private static DefaultKafkaProducerClientServiceConfiguration load(final String yaml) throws IOException {
        return YamlUtil.readConfigurationFromFile(
                yaml, null, null, null, DefaultKafkaProducerClientServiceConfiguration.class);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> declaredDefaultConfiguration(final String yaml) throws IOException {
        final Map<String, Object> nodes = YamlUtil.readConfigurationNodes(yaml);
        return (Map<String, Object>) nodes.get(DEFAULT_CONFIG_NODE);
    }

    /**
     * The control. Without it, a YAML that failed to load at all would satisfy the binding
     * assertions below for the wrong reason -- an empty configuration object would also carry the
     * builder's default serializers.
     */
    @Test
    void theProducerYamlItselfLoads() throws IOException {
        Assertions.assertNotNull(
                load(PRODUCER_YAML).getDefaultDefaultKafkaProducerClientIntegrationServiceConfiguration(),
                "control: the producer YAML really deserialises, so the assertions below mean what they say");
    }

    @Test
    void theDefaultProducerYamlBindsTheSerializersItDeclares() throws IOException {
        assertDeclaredSerializersBind(PRODUCER_YAML);
    }

    @Test
    void theContainerProducerYamlBindsTheSerializersItDeclares() throws IOException {
        assertDeclaredSerializersBind(PRODUCER_CONTAINER_YAML);
    }

    @Test
    void theNonDefaultKeySerializerReachesTheGetter() throws IOException {
        final DefaultKafkaProducerClientIntegrationServiceConfiguration defaults =
                load(PRODUCER_YAML).getDefaultDefaultKafkaProducerClientIntegrationServiceConfiguration();
        Assertions.assertNotEquals(ByteArraySerializer.class, defaults.getKeySerializerType(),
                "control: the fixture must differ from the default producer key serializer");
        final DefaultKafkaProducerClientIntegrationServiceConfiguration bound =
                load(AppbaseConstants.CLASSPATH_FILE_PREFIX + "/kafka-producer-non-default-serde.yaml")
                        .getDefaultDefaultKafkaProducerClientIntegrationServiceConfiguration();
        Assertions.assertEquals(ByteArraySerializer.class, bound.getKeySerializerType(),
                "the non-default key serializer must reach the getter instead of silently defaulting");
    }

    @Test
    void theNonDefaultValueSerializerReachesTheGetter() throws IOException {
        final DefaultKafkaProducerClientIntegrationServiceConfiguration defaults =
                load(PRODUCER_YAML).getDefaultDefaultKafkaProducerClientIntegrationServiceConfiguration();
        Assertions.assertNotEquals(ByteArraySerializer.class, defaults.getValueSerializerType(),
                "control: the fixture must differ from the default producer value serializer");
        final DefaultKafkaProducerClientIntegrationServiceConfiguration bound =
                load(AppbaseConstants.CLASSPATH_FILE_PREFIX + "/kafka-producer-non-default-serde.yaml")
                        .getDefaultDefaultKafkaProducerClientIntegrationServiceConfiguration();
        Assertions.assertEquals(ByteArraySerializer.class, bound.getValueSerializerType(),
                "the non-default value serializer must reach the getter instead of silently defaulting");
    }

    /** Checks the shipped YAML declarations; non-default tests separately discriminate binding. */
    private static void assertDeclaredSerializersBind(final String yaml) throws IOException {
        final Map<String, Object> declared = declaredDefaultConfiguration(yaml);
        Assertions.assertNotNull(declared, "the YAML must carry a default producer configuration node");

        Assertions.assertNull(
                declared.get(DISCARDED_KEY_SPELLING),
                DISCARDED_KEY_SPELLING
                        + " is the consumer spelling; the producer configuration discards it silently");
        Assertions.assertNull(
                declared.get(DISCARDED_VALUE_SPELLING),
                DISCARDED_VALUE_SPELLING
                        + " is the consumer spelling; the producer configuration discards it silently");

        final Object declaredKeySerializer = declared.get(KEY_SERIALIZER_KEY);
        final Object declaredValueSerializer = declared.get(VALUE_SERIALIZER_KEY);
        Assertions.assertNotNull(
                declaredKeySerializer,
                "the producer must declare its key serializer under the key the producer configuration reads");
        Assertions.assertNotNull(
                declaredValueSerializer,
                "the producer must declare its value serializer under the key the producer configuration reads");

        final DefaultKafkaProducerClientIntegrationServiceConfiguration bound =
                load(yaml).getDefaultDefaultKafkaProducerClientIntegrationServiceConfiguration();

        Assertions.assertEquals(
                declaredKeySerializer,
                bound.getKeySerializerType().getName(),
                "the declared key serializer must reach getKeySerializerType(), not the builder default");
        Assertions.assertEquals(
                declaredValueSerializer,
                bound.getValueSerializerType().getName(),
                "the declared value serializer must reach getValueSerializerType(), not the builder default");
    }

    /** Pins the stopped-broker IT's Kafka deadlines ahead of its reactive deadline. */
    @Test
    void theLiveProducerYamlLetsKafkaReportItsOwnOutage() throws IOException {
        final DefaultKafkaProducerClientServiceConfiguration service = load(PRODUCER_YAML);
        final DefaultKafkaProducerClientIntegrationServiceConfiguration producer =
                service.getDefaultDefaultKafkaProducerClientIntegrationServiceConfiguration();
        final long deliveryMillis = producer.getDeliveryTimeoutConfiguration().getTimeoutTimeUnit()
                .toMillis(producer.getDeliveryTimeoutConfiguration().getTimeout());
        final long maxBlockMillis = producer.getMaxBlockTimeConfiguration().getTimeUnit()
                .toMillis(producer.getMaxBlockTimeConfiguration().getTime());
        final long requestMillis = producer.getRequestTimeoutConfiguration().getTimeoutTimeUnit()
                .toMillis(producer.getRequestTimeoutConfiguration().getTimeout());
        final long lingerMillis = producer.getLingerTimeConfiguration().getTimeUnit()
                .toMillis(producer.getLingerTimeConfiguration().getTime());
        final long reactiveTimeoutMillis = service.getReactiveOperationConfiguration().getTimeout().toMillis();

        Assertions.assertEquals(10_000L, deliveryMillis, "delivery limit must bind, not default to 2m");
        Assertions.assertEquals(5_000L, maxBlockMillis, "metadata wait must bind, not default to 1m");
        Assertions.assertEquals(5_000L, requestMillis, "request limit must bind, not default to 30s");
        Assertions.assertTrue(deliveryMillis >= requestMillis + lingerMillis,
                "Kafka requires delivery timeout >= request timeout + linger");
        Assertions.assertTrue(maxBlockMillis + deliveryMillis < reactiveTimeoutMillis,
                "even metadata wait followed by delivery must beat the reactive per-attempt timeout");
    }
}
