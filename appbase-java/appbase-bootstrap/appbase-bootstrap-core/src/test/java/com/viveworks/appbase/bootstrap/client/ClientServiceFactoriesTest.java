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

package com.viveworks.appbase.bootstrap.client;

import com.viveworks.appbase.bootstrap.client.factory.AerospikeClientServiceFactory;
import com.viveworks.appbase.bootstrap.client.factory.AwsSdkAsyncHttpClientServiceFactory;
import com.viveworks.appbase.bootstrap.client.factory.AwsSdkHttpClientServiceFactory;
import com.viveworks.appbase.bootstrap.client.factory.CassandraClientServiceFactory;
import com.viveworks.appbase.bootstrap.client.factory.ClamAVClientServiceFactory;
import com.viveworks.appbase.bootstrap.client.factory.DruidSqlClientServiceFactory;
import com.viveworks.appbase.bootstrap.client.factory.ElasticsearchClientServiceFactory;
import com.viveworks.appbase.bootstrap.client.factory.FirebaseAppClientServiceFactory;
import com.viveworks.appbase.bootstrap.client.factory.FirebaseAuthClientServiceFactory;
import com.viveworks.appbase.bootstrap.client.factory.JasyncClientServiceFactory;
import com.viveworks.appbase.bootstrap.client.factory.JpaClientServiceFactory;
import com.viveworks.appbase.bootstrap.client.factory.KafkaConsumerClientServiceFactory;
import com.viveworks.appbase.bootstrap.client.factory.KafkaProducerClientServiceFactory;
import com.viveworks.appbase.bootstrap.client.factory.RedisClientServiceFactory;
import com.viveworks.appbase.bootstrap.client.factory.S3ClientsServiceFactory;
import com.viveworks.appbase.bootstrap.client.factory.SmtpClientServiceFactory;
import com.viveworks.appbase.bootstrap.client.factory.SparkClientServiceFactory;
import com.viveworks.appbase.bootstrap.client.factory.VitessGrpcClientServiceFactory;
import com.viveworks.appbase.bootstrap.client.factory.VitessJdbiClientServiceFactory;
import com.viveworks.appbase.bootstrap.client.factory.VitessJdbcClientServiceFactory;
import com.viveworks.appbase.bootstrap.client.factory.ZooKeeperClientServiceFactory;
import com.viveworks.appbase.bootstrap.core.AppbaseCoreServices;
import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.service.aerospike.AerospikeClientService;
import com.viveworks.appbase.client.cloud.aws.httpclient.AwsSdkAsyncHttpClientService;
import com.viveworks.appbase.client.cloud.aws.httpclient.AwsSdkHttpClientService;
import com.viveworks.appbase.client.cloud.aws.s3.S3ClientsService;
import com.viveworks.appbase.client.service.cassandra.CassandraClientService;
import com.viveworks.appbase.client.service.clamav.ClamAVClientService;
import com.viveworks.appbase.client.service.druid.sql.DruidSqlClientService;
import com.viveworks.appbase.client.service.elasticsearch.restclient.lowlevel.ElasticsearchLowLevelRestClientService;
import com.viveworks.appbase.client.service.firebase.app.FirebaseAppClientService;
import com.viveworks.appbase.client.service.firebase.auth.FirebaseAuthClientService;
import com.viveworks.appbase.client.service.jasync.JasyncClientService;
import com.viveworks.appbase.client.service.jdbc.JdbcClientService;
import com.viveworks.appbase.client.service.jdbi.JdbiClientService;
import com.viveworks.appbase.client.service.jpa.JpaClientService;
import com.viveworks.appbase.client.service.kafka.consumer.KafkaConsumerClientService;
import com.viveworks.appbase.client.service.kafka.producer.KafkaProducerClientService;
import com.viveworks.appbase.client.service.redis.RedisClientService;
import com.viveworks.appbase.client.service.redis.configuration.DefaultRedisClientServiceConfiguration;
import com.viveworks.appbase.client.service.smtp.SmtpClientService;
import com.viveworks.appbase.client.service.spark.SparkClientService;
import com.viveworks.appbase.client.service.vitess.VitessGrpcClientService;
import com.viveworks.appbase.client.service.zookeeper.ZooKeeperClientService;
import com.viveworks.appbase.core.AppbaseException;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.configuration.accessor.ConfigurationAccessor;
import com.viveworks.appbase.core.service.configuration.accessor.store.filesystem.ClasspathConfigurationStore;
import com.viveworks.appbase.core.service.registry.AppbaseServiceEntry;
import com.viveworks.appbase.core.service.registry.ServiceRegistry;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import com.viveworks.appbase.client.service.aerospike.configuration.DefaultAerospikeClientServiceConfiguration;
import com.viveworks.appbase.client.cloud.aws.httpclient.configuration.DefaultAwsSdkHttpClientServiceConfiguration;
import com.viveworks.appbase.client.cloud.aws.httpclient.configuration.DefaultAwsSdkAsyncHttpClientServiceConfiguration;
import com.viveworks.appbase.client.cloud.aws.s3.configuration.DefaultS3ClientsServiceConfiguration;
import com.viveworks.appbase.client.service.druid.sql.client.configuration.DefaultDruidSqlClientSharedResourcesConfiguration;
import com.viveworks.appbase.client.service.druid.sql.configuration.DefaultDruidSqlClientServiceConfiguration;
import com.viveworks.appbase.client.service.elasticsearch.restclient.lowlevel.configuration.DefaultElasticsearchLowLevelRestClientServiceConfiguration;
import com.viveworks.appbase.client.service.firebase.app.configuration.DefaultFirebaseAppClientServiceConfiguration;
import com.viveworks.appbase.client.service.firebase.auth.configuration.DefaultFirebaseAuthClientServiceConfiguration;
import com.viveworks.appbase.client.service.vitess.configuration.DefaultVitessGrpcClientServiceConfiguration;
import com.viveworks.appbase.client.service.jdbc.configuration.DefaultJdbcClientServiceConfiguration;
import com.viveworks.appbase.client.service.jdbi.configuration.DefaultJdbiClientServiceConfiguration;
import com.viveworks.appbase.client.service.zookeeper.configuration.DefaultZooKeeperClientServiceConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for all 21 built-in {@link AppbaseClientServiceFactory} implementations.
 * Tests cover: pre-built path (same instance returned), serviceType(), initializationOrder().
 *
 * @author Navaneeth Mysore G
 * @author Claude Sonnet 4.6
 */
class ClientServiceFactoriesTest {

    // ── AwsSdkHttpClientServiceFactory ────────────────────────────────────────

    @Test
    void awsSdkHttpFactory_predefined_returnsSameInstance() throws AppbaseException {
        AwsSdkHttpClientService svc = mock(AwsSdkHttpClientService.class);
        assertSame(svc, new AwsSdkHttpClientServiceFactory(svc).create(mock(AppbaseCoreServices.class), mock(AppbaseClientCore.class)));
    }

    @Test
    void awsSdkHttpFactory_serviceType_isAwsSdkHttpClientService() {
        assertEquals(AwsSdkHttpClientService.class,
            new AwsSdkHttpClientServiceFactory(mock(AwsSdkHttpClientService.class)).serviceType());
    }

    @Test
    void awsSdkHttpFactory_order_matchesClientServiceOrder() {
        assertEquals(ClientServiceOrder.AWS_SDK_HTTP.order(),
            new AwsSdkHttpClientServiceFactory(mock(AwsSdkHttpClientService.class)).initializationOrder());
    }

    // ── AwsSdkAsyncHttpClientServiceFactory ───────────────────────────────────

    @Test
    void awsSdkAsyncHttpFactory_predefined_returnsSameInstance() throws AppbaseException {
        AwsSdkAsyncHttpClientService svc = mock(AwsSdkAsyncHttpClientService.class);
        assertSame(svc, new AwsSdkAsyncHttpClientServiceFactory(svc).create(mock(AppbaseCoreServices.class), mock(AppbaseClientCore.class)));
    }

    @Test
    void awsSdkAsyncHttpFactory_serviceType_isAwsSdkAsyncHttpClientService() {
        assertEquals(AwsSdkAsyncHttpClientService.class,
            new AwsSdkAsyncHttpClientServiceFactory(mock(AwsSdkAsyncHttpClientService.class)).serviceType());
    }

    @Test
    void awsSdkAsyncHttpFactory_order_matchesClientServiceOrder() {
        assertEquals(ClientServiceOrder.AWS_SDK_ASYNC_HTTP.order(),
            new AwsSdkAsyncHttpClientServiceFactory(mock(AwsSdkAsyncHttpClientService.class)).initializationOrder());
    }

    // ── S3ClientsServiceFactory ────────────────────────────────────────────────

    @Test
    void s3Factory_predefined_returnsSameInstance() throws AppbaseException {
        S3ClientsService svc = mock(S3ClientsService.class);
        assertSame(svc, new S3ClientsServiceFactory(svc).create(mock(AppbaseCoreServices.class), mock(AppbaseClientCore.class)));
    }

    @Test
    void s3Factory_serviceType_isS3ClientsService() {
        assertEquals(S3ClientsService.class,
            new S3ClientsServiceFactory(mock(S3ClientsService.class)).serviceType());
    }

    @Test
    void s3Factory_order_matchesClientServiceOrder() {
        assertEquals(ClientServiceOrder.S3.order(),
            new S3ClientsServiceFactory(mock(S3ClientsService.class)).initializationOrder());
    }

    // ── FirebaseAppClientServiceFactory ───────────────────────────────────────

    @Test
    void firebaseAppFactory_predefined_returnsSameInstance() throws AppbaseException {
        FirebaseAppClientService svc = mock(FirebaseAppClientService.class);
        assertSame(svc, new FirebaseAppClientServiceFactory(svc).create(mock(AppbaseCoreServices.class), mock(AppbaseClientCore.class)));
    }

    @Test
    void firebaseAppFactory_serviceType_isFirebaseAppClientService() {
        assertEquals(FirebaseAppClientService.class,
            new FirebaseAppClientServiceFactory(mock(FirebaseAppClientService.class)).serviceType());
    }

    @Test
    void firebaseAppFactory_order_matchesClientServiceOrder() {
        assertEquals(ClientServiceOrder.FIREBASE_APP.order(),
            new FirebaseAppClientServiceFactory(mock(FirebaseAppClientService.class)).initializationOrder());
    }

    // ── FirebaseAuthClientServiceFactory ──────────────────────────────────────

    @Test
    void firebaseAuthFactory_predefined_returnsSameInstance() throws AppbaseException {
        FirebaseAuthClientService svc = mock(FirebaseAuthClientService.class);
        assertSame(svc, new FirebaseAuthClientServiceFactory(svc).create(mock(AppbaseCoreServices.class), mock(AppbaseClientCore.class)));
    }

    @Test
    void firebaseAuthFactory_serviceType_isFirebaseAuthClientService() {
        assertEquals(FirebaseAuthClientService.class,
            new FirebaseAuthClientServiceFactory(mock(FirebaseAuthClientService.class)).serviceType());
    }

    @Test
    void firebaseAuthFactory_order_matchesClientServiceOrder() {
        assertEquals(ClientServiceOrder.FIREBASE_AUTH.order(),
            new FirebaseAuthClientServiceFactory(mock(FirebaseAuthClientService.class)).initializationOrder());
    }

    @Test
    void firebaseAuthFactory_classpath_reusesRegisteredFirebaseApp() throws AppbaseException {
        final AppbaseCoreServices coreServices = mock(AppbaseCoreServices.class);
        final AppbaseCore appbaseCore = mock(AppbaseCore.class);
        final ServiceRegistry serviceRegistry = mock(ServiceRegistry.class);
        final FirebaseAppClientService firebaseAppService = mock(FirebaseAppClientService.class);
        final AppbaseClientCore clientCore = mock(AppbaseClientCore.class);
        when(coreServices.appbaseCore()).thenReturn(appbaseCore);
        when(appbaseCore.getServiceRegistry()).thenReturn(serviceRegistry);
        when(serviceRegistry.getAppbaseService(FirebaseAppClientService.class))
            .thenReturn(new AppbaseServiceEntry(firebaseAppService, ServiceStatus.STARTED));
        final Object[][] constructorArguments = new Object[1][];

        try (MockedConstruction<com.viveworks.appbase.client.service.firebase.auth.DefaultFirebaseAuthClientService>
                 construction = Mockito.mockConstruction(
                     com.viveworks.appbase.client.service.firebase.auth.DefaultFirebaseAuthClientService.class,
                     (mockService, context) -> constructorArguments[0] = context.arguments().toArray())) {
            new FirebaseAuthClientServiceFactory("classpath:///firebase-auth.yaml")
                .create(coreServices, clientCore);

            assertEquals(1, construction.constructed().size());
            assertSame(clientCore, constructorArguments[0][0]);
            assertSame(firebaseAppService, constructorArguments[0][1]);
        }
    }

    // ── AerospikeClientServiceFactory ─────────────────────────────────────────

    @Test
    void aerospikeFactory_predefined_returnsSameInstance() throws AppbaseException {
        AerospikeClientService svc = mock(AerospikeClientService.class);
        assertSame(svc, new AerospikeClientServiceFactory(svc).create(mock(AppbaseCoreServices.class), mock(AppbaseClientCore.class)));
    }

    @Test
    void aerospikeFactory_serviceType_isAerospikeClientService() {
        assertEquals(AerospikeClientService.class,
            new AerospikeClientServiceFactory(mock(AerospikeClientService.class)).serviceType());
    }

    @Test
    void aerospikeFactory_order_matchesClientServiceOrder() {
        assertEquals(ClientServiceOrder.AEROSPIKE.order(),
            new AerospikeClientServiceFactory(mock(AerospikeClientService.class)).initializationOrder());
    }

    // ── CassandraClientServiceFactory ─────────────────────────────────────────

    @Test
    void cassandraFactory_predefined_returnsSameInstance() throws AppbaseException {
        CassandraClientService svc = mock(CassandraClientService.class);
        assertSame(svc, new CassandraClientServiceFactory(svc).create(mock(AppbaseCoreServices.class), mock(AppbaseClientCore.class)));
    }

    @Test
    void cassandraFactory_serviceType_isCassandraClientService() {
        assertEquals(CassandraClientService.class,
            new CassandraClientServiceFactory(mock(CassandraClientService.class)).serviceType());
    }

    @Test
    void cassandraFactory_order_matchesClientServiceOrder() {
        assertEquals(ClientServiceOrder.CASSANDRA.order(),
            new CassandraClientServiceFactory(mock(CassandraClientService.class)).initializationOrder());
    }

    // ── ClamAVClientServiceFactory ─────────────────────────────────────────────

    @Test
    void clamAVFactory_predefined_returnsSameInstance() throws AppbaseException {
        ClamAVClientService svc = mock(ClamAVClientService.class);
        assertSame(svc, new ClamAVClientServiceFactory(svc).create(mock(AppbaseCoreServices.class), mock(AppbaseClientCore.class)));
    }

    @Test
    void clamAVFactory_serviceType_isClamAVClientService() {
        assertEquals(ClamAVClientService.class,
            new ClamAVClientServiceFactory(mock(ClamAVClientService.class)).serviceType());
    }

    @Test
    void clamAVFactory_order_matchesClientServiceOrder() {
        assertEquals(ClientServiceOrder.CLAMAV.order(),
            new ClamAVClientServiceFactory(mock(ClamAVClientService.class)).initializationOrder());
    }

    // ── JasyncClientServiceFactory ─────────────────────────────────────────────

    @Test
    void jasyncFactory_predefined_returnsSameInstance() throws AppbaseException {
        JasyncClientService svc = mock(JasyncClientService.class);
        assertSame(svc, new JasyncClientServiceFactory(svc).create(mock(AppbaseCoreServices.class), mock(AppbaseClientCore.class)));
    }

    @Test
    void jasyncFactory_serviceType_isJasyncClientService() {
        assertEquals(JasyncClientService.class,
            new JasyncClientServiceFactory(mock(JasyncClientService.class)).serviceType());
    }

    @Test
    void jasyncFactory_order_matchesClientServiceOrder() {
        assertEquals(ClientServiceOrder.JASYNC.order(),
            new JasyncClientServiceFactory(mock(JasyncClientService.class)).initializationOrder());
    }

    // ── ElasticsearchClientServiceFactory ─────────────────────────────────────

    @Test
    void elasticsearchFactory_predefined_returnsSameInstance() throws AppbaseException {
        ElasticsearchLowLevelRestClientService svc = mock(ElasticsearchLowLevelRestClientService.class);
        assertSame(svc, new ElasticsearchClientServiceFactory(svc).create(mock(AppbaseCoreServices.class), mock(AppbaseClientCore.class)));
    }

    @Test
    void elasticsearchFactory_serviceType_isElasticsearchLowLevelRestClientService() {
        assertEquals(ElasticsearchLowLevelRestClientService.class,
            new ElasticsearchClientServiceFactory(mock(ElasticsearchLowLevelRestClientService.class)).serviceType());
    }

    @Test
    void elasticsearchFactory_order_matchesClientServiceOrder() {
        assertEquals(ClientServiceOrder.ELASTICSEARCH.order(),
            new ElasticsearchClientServiceFactory(mock(ElasticsearchLowLevelRestClientService.class)).initializationOrder());
    }

    // ── VitessJdbcClientServiceFactory ────────────────────────────────────────

    @Test
    void vitessJdbcFactory_predefined_returnsSameInstance() throws AppbaseException {
        JdbcClientService svc = mock(JdbcClientService.class);
        assertSame(svc, new VitessJdbcClientServiceFactory(svc).create(mock(AppbaseCoreServices.class), mock(AppbaseClientCore.class)));
    }

    @Test
    void vitessJdbcFactory_serviceType_isJdbcClientService() {
        assertEquals(JdbcClientService.class,
            new VitessJdbcClientServiceFactory(mock(JdbcClientService.class)).serviceType());
    }

    @Test
    void vitessJdbcFactory_order_matchesClientServiceOrder() {
        assertEquals(ClientServiceOrder.VITESS_JDBC.order(),
            new VitessJdbcClientServiceFactory(mock(JdbcClientService.class)).initializationOrder());
    }

    // ── VitessJdbiClientServiceFactory ────────────────────────────────────────

    @Test
    void vitessJdbiFactory_predefined_returnsSameInstance() throws AppbaseException {
        JdbiClientService svc = mock(JdbiClientService.class);
        assertSame(svc, new VitessJdbiClientServiceFactory(svc).create(mock(AppbaseCoreServices.class), mock(AppbaseClientCore.class)));
    }

    @Test
    void vitessJdbiFactory_serviceType_isJdbiClientService() {
        assertEquals(JdbiClientService.class,
            new VitessJdbiClientServiceFactory(mock(JdbiClientService.class)).serviceType());
    }

    @Test
    void vitessJdbiFactory_order_matchesClientServiceOrder() {
        assertEquals(ClientServiceOrder.VITESS_JDBI.order(),
            new VitessJdbiClientServiceFactory(mock(JdbiClientService.class)).initializationOrder());
    }

    // ── VitessGrpcClientServiceFactory ────────────────────────────────────────

    @Test
    void vitessGrpcFactory_predefined_returnsSameInstance() throws AppbaseException {
        VitessGrpcClientService svc = mock(VitessGrpcClientService.class);
        assertSame(svc, new VitessGrpcClientServiceFactory(svc).create(mock(AppbaseCoreServices.class), mock(AppbaseClientCore.class)));
    }

    @Test
    void vitessGrpcFactory_serviceType_isVitessGrpcClientService() {
        assertEquals(VitessGrpcClientService.class,
            new VitessGrpcClientServiceFactory(mock(VitessGrpcClientService.class)).serviceType());
    }

    @Test
    void vitessGrpcFactory_order_matchesClientServiceOrder() {
        assertEquals(ClientServiceOrder.VITESS_GRPC.order(),
            new VitessGrpcClientServiceFactory(mock(VitessGrpcClientService.class)).initializationOrder());
    }

    // ── JpaClientServiceFactory ────────────────────────────────────────────────

    @Test
    void jpaFactory_predefined_returnsSameInstance() throws AppbaseException {
        JpaClientService svc = mock(JpaClientService.class);
        assertSame(svc, new JpaClientServiceFactory(svc).create(mock(AppbaseCoreServices.class), mock(AppbaseClientCore.class)));
    }

    @Test
    void jpaFactory_serviceType_isJpaClientService() {
        assertEquals(JpaClientService.class,
            new JpaClientServiceFactory(mock(JpaClientService.class)).serviceType());
    }

    @Test
    void jpaFactory_order_matchesClientServiceOrder() {
        assertEquals(ClientServiceOrder.JPA.order(),
            new JpaClientServiceFactory(mock(JpaClientService.class)).initializationOrder());
    }

    // ── DruidSqlClientServiceFactory ──────────────────────────────────────────

    @Test
    void druidSqlFactory_predefined_returnsSameInstance() throws AppbaseException {
        DruidSqlClientService svc = mock(DruidSqlClientService.class);
        assertSame(svc, new DruidSqlClientServiceFactory(svc).create(mock(AppbaseCoreServices.class), mock(AppbaseClientCore.class)));
    }

    @Test
    void druidSqlFactory_serviceType_isDruidSqlClientService() {
        assertEquals(DruidSqlClientService.class,
            new DruidSqlClientServiceFactory(mock(DruidSqlClientService.class)).serviceType());
    }

    @Test
    void druidSqlFactory_order_matchesClientServiceOrder() {
        assertEquals(ClientServiceOrder.DRUID.order(),
            new DruidSqlClientServiceFactory(mock(DruidSqlClientService.class)).initializationOrder());
    }

    // ── KafkaProducerClientServiceFactory ─────────────────────────────────────

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void kafkaProducerFactory_predefined_returnsSameInstance() throws AppbaseException {
        KafkaProducerClientService svc = mock(KafkaProducerClientService.class);
        assertSame(svc, new KafkaProducerClientServiceFactory(svc).create(mock(AppbaseCoreServices.class), mock(AppbaseClientCore.class)));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void kafkaProducerFactory_serviceType_isKafkaProducerClientService() {
        assertEquals(KafkaProducerClientService.class,
            new KafkaProducerClientServiceFactory(mock(KafkaProducerClientService.class)).serviceType());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void kafkaProducerFactory_order_matchesClientServiceOrder() {
        assertEquals(ClientServiceOrder.KAFKA_PRODUCER.order(),
            new KafkaProducerClientServiceFactory(mock(KafkaProducerClientService.class)).initializationOrder());
    }

    // ── KafkaConsumerClientServiceFactory ─────────────────────────────────────

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void kafkaConsumerFactory_predefined_returnsSameInstance() throws AppbaseException {
        KafkaConsumerClientService svc = mock(KafkaConsumerClientService.class);
        assertSame(svc, new KafkaConsumerClientServiceFactory(svc).create(mock(AppbaseCoreServices.class), mock(AppbaseClientCore.class)));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void kafkaConsumerFactory_serviceType_isKafkaConsumerClientService() {
        assertEquals(KafkaConsumerClientService.class,
            new KafkaConsumerClientServiceFactory(mock(KafkaConsumerClientService.class)).serviceType());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void kafkaConsumerFactory_order_matchesClientServiceOrder() {
        assertEquals(ClientServiceOrder.KAFKA_CONSUMER.order(),
            new KafkaConsumerClientServiceFactory(mock(KafkaConsumerClientService.class)).initializationOrder());
    }

    // ── SmtpClientServiceFactory ───────────────────────────────────────────────

    @Test
    void smtpFactory_predefined_returnsSameInstance() throws AppbaseException {
        SmtpClientService svc = mock(SmtpClientService.class);
        assertSame(svc, new SmtpClientServiceFactory(svc).create(mock(AppbaseCoreServices.class), mock(AppbaseClientCore.class)));
    }

    @Test
    void smtpFactory_serviceType_isSmtpClientService() {
        assertEquals(SmtpClientService.class,
            new SmtpClientServiceFactory(mock(SmtpClientService.class)).serviceType());
    }

    @Test
    void smtpFactory_order_matchesClientServiceOrder() {
        assertEquals(ClientServiceOrder.SMTP.order(),
            new SmtpClientServiceFactory(mock(SmtpClientService.class)).initializationOrder());
    }

    // ── RedisClientServiceFactory ──────────────────────────────────────────────

    @Test
    void redisFactory_predefined_returnsSameInstance() throws AppbaseException {
        RedisClientService svc = mock(RedisClientService.class);
        assertSame(svc, new RedisClientServiceFactory(svc).create(mock(AppbaseCoreServices.class), mock(AppbaseClientCore.class)));
    }

    @Test
    void redisFactory_serviceType_isRedisClientService() {
        assertEquals(RedisClientService.class,
            new RedisClientServiceFactory(mock(RedisClientService.class)).serviceType());
    }

    @Test
    void redisFactory_order_matchesClientServiceOrder() {
        assertEquals(ClientServiceOrder.REDIS.order(),
            new RedisClientServiceFactory(mock(RedisClientService.class)).initializationOrder());
    }

    // ── SparkClientServiceFactory ──────────────────────────────────────────────

    @Test
    void sparkFactory_predefined_returnsSameInstance() throws AppbaseException {
        SparkClientService svc = mock(SparkClientService.class);
        assertSame(svc, new SparkClientServiceFactory(svc).create(mock(AppbaseCoreServices.class), mock(AppbaseClientCore.class)));
    }

    @Test
    void sparkFactory_serviceType_isSparkClientService() {
        assertEquals(SparkClientService.class,
            new SparkClientServiceFactory(mock(SparkClientService.class)).serviceType());
    }

    @Test
    void sparkFactory_order_matchesClientServiceOrder() {
        assertEquals(ClientServiceOrder.SPARK.order(),
            new SparkClientServiceFactory(mock(SparkClientService.class)).initializationOrder());
    }

    // ── ZooKeeperClientServiceFactory ─────────────────────────────────────────

    @Test
    void zooKeeperFactory_predefined_returnsSameInstance() throws AppbaseException {
        ZooKeeperClientService svc = mock(ZooKeeperClientService.class);
        assertSame(svc, new ZooKeeperClientServiceFactory(svc).create(mock(AppbaseCoreServices.class), mock(AppbaseClientCore.class)));
    }

    @Test
    void zooKeeperFactory_serviceType_isZooKeeperClientService() {
        assertEquals(ZooKeeperClientService.class,
            new ZooKeeperClientServiceFactory(mock(ZooKeeperClientService.class)).serviceType());
    }

    @Test
    void zooKeeperFactory_order_matchesClientServiceOrder() {
        assertEquals(ClientServiceOrder.ZOOKEEPER.order(),
            new ZooKeeperClientServiceFactory(mock(ZooKeeperClientService.class)).initializationOrder());
    }

    // ── registerConfiguration — classpath-file constructors ───────────────────

    /** Helper: returns a mock AppbaseCoreServices wired to a mock ConfigurationAccessor. */
    private static ConfigurationAccessor mockCoreWithAccessor(final AppbaseCoreServices core) {
        ConfigurationAccessor accessor = mock(ConfigurationAccessor.class);
        ConfigurationService configService = mock(ConfigurationService.class);
        when(configService.getConfigurationAccessor()).thenReturn(accessor);
        when(core.getConfigurationService()).thenReturn(configService);
        return accessor;
    }

    @Test
    void aerospikeFactory_classpath_registersConfigTypeAndStore() {
        AppbaseCoreServices core = mock(AppbaseCoreServices.class);
        ConfigurationAccessor accessor = mockCoreWithAccessor(core);
        new AerospikeClientServiceFactory("classpath:///aerospike.yaml").registerConfiguration(core);
        verify(accessor).registerConfigurationTypeByRawConfigurationName(
            DefaultAerospikeClientServiceConfiguration.class, "classpath:///aerospike.yaml");
        verify(accessor).registerConfigurationStore(any(ClasspathConfigurationStore.class));
    }

    @Test
    void awsSdkHttpFactory_classpath_registersConfigTypeAndStore() {
        AppbaseCoreServices core = mock(AppbaseCoreServices.class);
        ConfigurationAccessor accessor = mockCoreWithAccessor(core);
        new AwsSdkHttpClientServiceFactory("classpath:///aws-http.yaml").registerConfiguration(core);
        verify(accessor).registerConfigurationTypeByRawConfigurationName(
            DefaultAwsSdkHttpClientServiceConfiguration.class, "classpath:///aws-http.yaml");
        verify(accessor).registerConfigurationStore(any(ClasspathConfigurationStore.class));
    }

    @Test
    void awsSdkAsyncHttpFactory_classpath_registersConfigTypeAndStore() {
        AppbaseCoreServices core = mock(AppbaseCoreServices.class);
        ConfigurationAccessor accessor = mockCoreWithAccessor(core);
        new AwsSdkAsyncHttpClientServiceFactory("classpath:///aws-async-http.yaml").registerConfiguration(core);
        verify(accessor).registerConfigurationTypeByRawConfigurationName(
            DefaultAwsSdkAsyncHttpClientServiceConfiguration.class, "classpath:///aws-async-http.yaml");
        verify(accessor).registerConfigurationStore(any(ClasspathConfigurationStore.class));
    }

    @Test
    void s3Factory_classpath_registersConfigTypeAndStore() {
        AppbaseCoreServices core = mock(AppbaseCoreServices.class);
        ConfigurationAccessor accessor = mockCoreWithAccessor(core);
        new S3ClientsServiceFactory("classpath:///s3.yaml").registerConfiguration(core);
        verify(accessor).registerConfigurationTypeByRawConfigurationName(
            DefaultS3ClientsServiceConfiguration.class, "classpath:///s3.yaml");
        verify(accessor).registerConfigurationStore(any(ClasspathConfigurationStore.class));
    }

    @Test
    void druidSqlFactory_classpath_registersBothConfigTypesAndStores() {
        AppbaseCoreServices core = mock(AppbaseCoreServices.class);
        ConfigurationAccessor accessor = mockCoreWithAccessor(core);
        new DruidSqlClientServiceFactory("classpath:///druid-shared.yaml", "classpath:///druid.yaml")
            .registerConfiguration(core);
        verify(accessor).registerConfigurationTypeByRawConfigurationName(
            DefaultDruidSqlClientSharedResourcesConfiguration.class, "classpath:///druid-shared.yaml");
        verify(accessor).registerConfigurationTypeByRawConfigurationName(
            DefaultDruidSqlClientServiceConfiguration.class, "classpath:///druid.yaml");
        verify(accessor, times(2)).registerConfigurationStore(any(ClasspathConfigurationStore.class));
    }

    @Test
    void elasticsearchFactory_classpath_registersConfigTypeAndStore() {
        AppbaseCoreServices core = mock(AppbaseCoreServices.class);
        ConfigurationAccessor accessor = mockCoreWithAccessor(core);
        new ElasticsearchClientServiceFactory("classpath:///es.yaml").registerConfiguration(core);
        verify(accessor).registerConfigurationTypeByRawConfigurationName(
            DefaultElasticsearchLowLevelRestClientServiceConfiguration.class, "classpath:///es.yaml");
        verify(accessor).registerConfigurationStore(any(ClasspathConfigurationStore.class));
    }

    @Test
    void firebaseAppFactory_classpath_registersConfigTypeAndStore() {
        AppbaseCoreServices core = mock(AppbaseCoreServices.class);
        ConfigurationAccessor accessor = mockCoreWithAccessor(core);
        new FirebaseAppClientServiceFactory("classpath:///firebase-app.yaml").registerConfiguration(core);
        verify(accessor).registerConfigurationTypeByRawConfigurationName(
            DefaultFirebaseAppClientServiceConfiguration.class, "classpath:///firebase-app.yaml");
        verify(accessor).registerConfigurationStore(any(ClasspathConfigurationStore.class));
    }

    @Test
    void firebaseAuthFactory_classpath_registersConfigTypeAndStore() {
        AppbaseCoreServices core = mock(AppbaseCoreServices.class);
        ConfigurationAccessor accessor = mockCoreWithAccessor(core);
        new FirebaseAuthClientServiceFactory("classpath:///firebase-auth.yaml").registerConfiguration(core);
        verify(accessor).registerConfigurationTypeByRawConfigurationName(
            DefaultFirebaseAuthClientServiceConfiguration.class, "classpath:///firebase-auth.yaml");
        verify(accessor).registerConfigurationStore(any(ClasspathConfigurationStore.class));
    }

    @Test
    void vitessGrpcFactory_classpath_registersConfigTypeAndStore() {
        AppbaseCoreServices core = mock(AppbaseCoreServices.class);
        ConfigurationAccessor accessor = mockCoreWithAccessor(core);
        new VitessGrpcClientServiceFactory("classpath:///vitess-grpc.yaml").registerConfiguration(core);
        verify(accessor).registerConfigurationTypeByRawConfigurationName(
            DefaultVitessGrpcClientServiceConfiguration.class, "classpath:///vitess-grpc.yaml");
        verify(accessor).registerConfigurationStore(any(ClasspathConfigurationStore.class));
    }

    @Test
    void vitessJdbcFactory_classpath_registersConfigTypeAndStore() {
        AppbaseCoreServices core = mock(AppbaseCoreServices.class);
        ConfigurationAccessor accessor = mockCoreWithAccessor(core);
        new VitessJdbcClientServiceFactory("classpath:///vitess-jdbc.yaml").registerConfiguration(core);
        verify(accessor).registerConfigurationTypeByRawConfigurationName(
            DefaultJdbcClientServiceConfiguration.class, "classpath:///vitess-jdbc.yaml");
        verify(accessor).registerConfigurationStore(any(ClasspathConfigurationStore.class));
    }

    @Test
    void vitessJdbiFactory_classpath_registersConfigTypeAndStore() {
        AppbaseCoreServices core = mock(AppbaseCoreServices.class);
        ConfigurationAccessor accessor = mockCoreWithAccessor(core);
        new VitessJdbiClientServiceFactory("classpath:///vitess-jdbi.yaml").registerConfiguration(core);
        verify(accessor).registerConfigurationTypeByRawConfigurationName(
            DefaultJdbiClientServiceConfiguration.class, "classpath:///vitess-jdbi.yaml");
        verify(accessor).registerConfigurationStore(any(ClasspathConfigurationStore.class));
    }

    @Test
    void zooKeeperFactory_classpath_registersConfigTypeAndStore() {
        AppbaseCoreServices core = mock(AppbaseCoreServices.class);
        ConfigurationAccessor accessor = mockCoreWithAccessor(core);
        new ZooKeeperClientServiceFactory("classpath:///zookeeper.yaml").registerConfiguration(core);
        verify(accessor).registerConfigurationTypeByRawConfigurationName(
            DefaultZooKeeperClientServiceConfiguration.class, "classpath:///zookeeper.yaml");
        verify(accessor).registerConfigurationStore(any(ClasspathConfigurationStore.class));
    }

    @Test
    void redisFactory_classpath_registersConfigTypeAndStore() {
        AppbaseCoreServices core = mock(AppbaseCoreServices.class);
        ConfigurationAccessor accessor = mockCoreWithAccessor(core);
        new RedisClientServiceFactory("classpath:///redis.yaml").registerConfiguration(core);
        verify(accessor).registerConfigurationTypeByRawConfigurationName(
            DefaultRedisClientServiceConfiguration.class, "classpath:///redis.yaml");
        verify(accessor).registerConfigurationStore(any(ClasspathConfigurationStore.class));
    }
}
