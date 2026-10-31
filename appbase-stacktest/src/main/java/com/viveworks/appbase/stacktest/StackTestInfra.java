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
package com.viveworks.appbase.stacktest;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.Key;
import com.viveworks.appbase.client.core.clientkey.DefaultMutableAppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValue;
import com.viveworks.appbase.client.service.aerospike.AerospikeClientService;
import com.viveworks.appbase.client.cloud.aws.s3.S3ClientsService;
import com.viveworks.appbase.client.service.druid.sql.DruidSqlClientService;
import com.viveworks.appbase.client.service.druid.sql.client.DruidResultSet;
import com.viveworks.appbase.client.service.druid.sql.client.DruidSqlClientException;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.ElasticsearchRestClient;
import com.viveworks.appbase.client.service.jdbi.JdbiClientService;
import com.viveworks.appbase.client.service.zookeeper.ZooKeeperClientService;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.index.CreateIndexRequest;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.index.CreateIndexRequestBody;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.index.CreateIndexResponse;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.index.ExistsIndexRequest;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.index.ExistsIndexResponse;
import com.viveworks.appbase.client.service.zookeeper.accessor.DefaultDistributedCounterZKA;
import com.viveworks.appbase.client.service.zookeeper.accessor.DistributedCounterZKA;
import com.viveworks.appbase.stacktest.accessor.vitess.CustomerJDBA;
import com.viveworks.appbase.stacktest.accessor.vitess.OrderJDBA;
import com.viveworks.appbase.stacktest.accessor.vitess.ProductJDBA;
import com.viveworks.appbase.stacktest.data.model.Customer;
import com.viveworks.appbase.stacktest.data.model.Order;
import com.viveworks.appbase.stacktest.data.model.Product;
import com.viveworks.appbase.stacktest.service.StackTestServiceException;
import com.viveworks.appbase.stacktest.service.StackTestServices;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.data.Stat;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.JdbiException;
import org.slf4j.Logger;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.CreateBucketResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public final class StackTestInfra {
    private static final String DRUID_QUERY = "SELECT * FROM appbase_stacktest_healthcheck_data";

    private final Logger logger;
    private final StackTestServices stackTestServices;

    public StackTestInfra(final StackTestServices stackTestServices) {
        this.stackTestServices = stackTestServices;
        this.logger = stackTestServices
                .getAppbaseServices()
                .getCoreServices()
                .appbaseCore()
                .getLogService()
                .getLogger(getClass());
    }

    public void checkAndCreateRequiredInfra() throws StackTestServiceException {
        try {
            checkAerospike();
            checkDruidDataSources();
            createElasticSearchIndicesIfNotExists();
            createS3BucketsIfNotExists();
            checkVitessTables();
            checkAndCreateZooKeeperPaths();
        } catch (Exception ex) {
            throw new StackTestServiceException(ex);
        }
    }



    // ..............................
    // Aerospike ....................
    // ..............................
    private void checkAerospike() throws StackTestServiceException {
        logger.info("Aerospike check...");

        final IAerospikeClient aerospikeClient =
                stackTestServices
                        .getClientServices()
                        .get(AerospikeClientService.class)
                        .getClient();

        aerospikeCheckStackTestCustomerSet(aerospikeClient);
        aerospikeCheckStackTestOrderSet(aerospikeClient);
        aerospikeCheckStackTestProductSet(aerospikeClient);
        logger.info("Aerospike check - DONE");
    }

    private void aerospikeCheckStackTestCustomerSet(
            final IAerospikeClient aerospikeClient) throws StackTestServiceException
    {
        final Key key = new Key(
                StackTestConstants.AEROSPIKE_NAMESPACE,
                StackTestConstants.AEROSPIKE_CUSTOMER_SET,
                1111L);
        aerospikeCheckKeyExists(aerospikeClient, key);
    }

    private void aerospikeCheckStackTestOrderSet(
            final IAerospikeClient aerospikeClient) throws StackTestServiceException
    {
        final Key key = new Key(
                StackTestConstants.AEROSPIKE_NAMESPACE,
                StackTestConstants.AEROSPIKE_ORDER_SET,
                123L);
        aerospikeCheckKeyExists(aerospikeClient, key);
    }

    private void aerospikeCheckStackTestProductSet(
            final IAerospikeClient aerospikeClient) throws StackTestServiceException
    {
        final Key key = new Key(
                StackTestConstants.AEROSPIKE_NAMESPACE,
                StackTestConstants.AEROSPIKE_PRODUCT_SET,
                "sku2");
        aerospikeCheckKeyExists(aerospikeClient, key);
    }

    private void aerospikeCheckKeyExists(
            final IAerospikeClient aerospikeClient,
            final Key key)
            throws StackTestServiceException
    {
        logger.debug("Checking if key exists in Aerospike: {}", key);

        try {
            aerospikeClient.exists(aerospikeClient.getReadPolicyDefault(), key);
        } catch (AerospikeException.InvalidNamespace ae) {
            throw new StackTestServiceException("Namespace does not exist for key: " + key, ae);
        }
    }



    // ..............................
    // Druid ........................
    // ..............................
    private void checkDruidDataSources() throws StackTestServiceException {
        logger.info("Druid check...");

        checkDruidHealthCheckDataSource();
        logger.info("Druid check - DONE");
    }

    private void checkDruidHealthCheckDataSource() throws StackTestServiceException {
        try {
            final DruidResultSet druidResultSet = stackTestServices
                    .getClientServices()
                    .get(DruidSqlClientService.class)
                    .getClient()
                    .executeQuery(DRUID_QUERY);
            if (druidResultSet == null) {
                throw new StackTestServiceException("No druid result set returned");
            }

            boolean found = false;
            while (druidResultSet.next()) {
                final String time = druidResultSet.getVarchar("__time");
                final String userId = druidResultSet.getVarchar("userId");
                final String name = druidResultSet.getVarchar("name");
                final String email = druidResultSet.getVarchar("email");
                final String country = druidResultSet.getVarchar("country");

                if (time == null || userId == null ||
                        name == null || email == null || country == null)
                {
                    throw new StackTestServiceException("Druid row with all null values found");
                }

                if (time.isBlank() || userId.isBlank() ||
                        name.isBlank() || email.isBlank() || country.isBlank())
                {
                    throw new StackTestServiceException("Druid row with all blank values found");
                }

                logger.info("Druid record found [time={}, userId={}, name={}, email={}, country={}]",
                        time, userId, name, email, country);

                found = true;
            }

            if (!found) {
                throw new StackTestServiceException("No results found in druid");
            }
        } catch (DruidSqlClientException dsce) {
            throw new StackTestServiceException(dsce);
        }
    }



    // ..............................
    // Elasticsearch ................
    // ..............................
    private void createElasticSearchIndicesIfNotExists() throws StackTestServiceException {
        logger.info("Elasticsearch check...");

        final ElasticsearchRestClient elasticsearchRestClient = stackTestServices
                .getClientServices()
                .get(ElasticsearchRestClient.class);
        createStackTestCustomerIndexIfNotExists(elasticsearchRestClient);
        createStackTestOrderIndexIfNotExists(elasticsearchRestClient);
        createStackTestProductIndexIfNotExists(elasticsearchRestClient);
        logger.info("Elasticsearch check - DONE");
    }

    private void createStackTestCustomerIndexIfNotExists(
            final ElasticsearchRestClient elasticsearchRestClient)
            throws StackTestServiceException
    {
        final Map<String, CreateIndexRequestBody.FieldProperties> properties = new HashMap<>();

        final CreateIndexRequestBody.FieldProperties customerId =
                new CreateIndexRequestBody.FieldProperties();
        customerId.setIndex(true);
        customerId.setType("integer");
        properties.put(Customer.CUSTOMER_ID_INDEX_FIELD, customerId);

        final CreateIndexRequestBody.FieldProperties email =
                new CreateIndexRequestBody.FieldProperties();
        email.setIndex(true);
        email.setType("text");
        properties.put(Customer.EMAIL_INDEX_FIELD, email);

        final CreateIndexRequestBody.FieldProperties photoId =
                new CreateIndexRequestBody.FieldProperties();
        photoId.setIndex(true);
        photoId.setType("text");
        properties.put(Customer.PHOTO_ID_INDEX_FIELD, photoId);

        createElasticSearchIndexIfNotExists(
                elasticsearchRestClient,
                StackTestConstants.ELASTICSEARCH_CUSTOMER_INDEX_NAME,
                properties);
    }

    private void createStackTestOrderIndexIfNotExists(
            final ElasticsearchRestClient elasticsearchRestClient)
            throws StackTestServiceException
    {
        final Map<String, CreateIndexRequestBody.FieldProperties> properties = new HashMap<>();

        final CreateIndexRequestBody.FieldProperties orderId =
                new CreateIndexRequestBody.FieldProperties();
        orderId.setIndex(true);
        orderId.setType("integer");
        properties.put(Order.ORDER_ID_INDEX_FIELD, orderId);

        final CreateIndexRequestBody.FieldProperties customerId =
                new CreateIndexRequestBody.FieldProperties();
        customerId.setIndex(true);
        customerId.setType("integer");
        properties.put(Order.CUSTOMER_ID_INDEX_FIELD, customerId);

        final CreateIndexRequestBody.FieldProperties price =
                new CreateIndexRequestBody.FieldProperties();
        price.setIndex(true);
        price.setType("double");
        properties.put(Order.PRICE_INDEX_FIELD, price);

        final CreateIndexRequestBody.FieldProperties sku =
                new CreateIndexRequestBody.FieldProperties();
        sku.setIndex(true);
        sku.setType("text");
        properties.put(Order.SKU_INDEX_FIELD, sku);

        createElasticSearchIndexIfNotExists(
                elasticsearchRestClient,
                StackTestConstants.ELASTICSEARCH_ORDER_INDEX_NAME,
                properties);
    }

    private void createStackTestProductIndexIfNotExists(
            final ElasticsearchRestClient elasticsearchRestClient)
            throws StackTestServiceException
    {
        final Map<String, CreateIndexRequestBody.FieldProperties> properties = new HashMap<>();

        final CreateIndexRequestBody.FieldProperties sku =
                new CreateIndexRequestBody.FieldProperties();
        sku.setIndex(true);
        sku.setType("keyword");
        properties.put(Product.SKU_INDEX_FIELD, sku);

        final CreateIndexRequestBody.FieldProperties price =
                new CreateIndexRequestBody.FieldProperties();
        price.setIndex(true);
        price.setType("double");
        properties.put(Product.PRICE_INDEX_FIELD, price);

        final CreateIndexRequestBody.FieldProperties description =
                new CreateIndexRequestBody.FieldProperties();
        description.setIndex(true);
        description.setType("text");
        properties.put(Product.DESCRIPTION_INDEX_FIELD, description);

        createElasticSearchIndexIfNotExists(
                elasticsearchRestClient,
                StackTestConstants.ELASTICSEARCH_PRODUCT_INDEX_NAME + "-product",
                properties);
    }

    private void createElasticSearchIndexIfNotExists(
            final ElasticsearchRestClient elasticsearchRestClient,
            final String indexName,
            final Map<String, CreateIndexRequestBody.FieldProperties> properties)
            throws StackTestServiceException
    {
        logger.debug("Elasticsearch index creation if not exist: [name={}, properties={}]",
                indexName, properties);

        final ExistsIndexRequest existsIndexRequest = new ExistsIndexRequest(indexName);
        try {
            final ExistsIndexResponse existsIndexResponse =
                    elasticsearchRestClient.existsIndex(existsIndexRequest);
            logger.debug("Elasticsearch index exists response: {}", existsIndexResponse);

            if (existsIndexResponse == null) {
                throw new StackTestServiceException("Exists index response was null for " + indexName);
            }

            if (!existsIndexResponse.isExists()) {
                logger.debug("Elasticsearch index creation: {}", indexName);

                final CreateIndexRequestBody.Mappings mappings = new CreateIndexRequestBody.Mappings();
                mappings.setProperties(properties);
                final CreateIndexRequestBody createIndexRequestBody = new CreateIndexRequestBody();
                createIndexRequestBody.setMappings(mappings);
                final CreateIndexRequest createIndexRequest =
                        new CreateIndexRequest(indexName, createIndexRequestBody);

                final CreateIndexResponse createIndexResponse =
                        elasticsearchRestClient.createIndex(createIndexRequest);
                logger.debug("Elasticsearch create index response: {}", createIndexResponse);

                if (createIndexResponse == null) {
                    throw new StackTestServiceException("Create index response was null for " + indexName);
                }

                if (!createIndexResponse.isSuccess()) {
                    throw new StackTestServiceException("Create index response failed for " + indexName);
                }
            }
        } catch (IOException e) {
            throw new StackTestServiceException(e);
        }
    }



    // ..............................
    // S3 ...........................
    // ..............................
    private void createS3BucketsIfNotExists() throws StackTestServiceException {
        logger.info("S3 / MinIO check...");

        final S3Client s3Client = stackTestServices
                .getClientServices()
                .get(S3ClientsService.class)
                .getClient()
                .getS3Client();
        createS3BucketIfNotExists(s3Client, StackTestConstants.S3_BUCKET_NAME);
        logger.info("S3 / MinIO check - DONE");
    }

    private void createS3BucketIfNotExists(
            final S3Client s3Client,
            final String bucketName)
            throws StackTestServiceException
    {
        logger.debug("S3 bucket exists check for: {}", bucketName);

        final HeadBucketRequest headBucketRequest = HeadBucketRequest
                .builder()
                .bucket(bucketName)
                .build();

        boolean exists = false;
        try {
            final HeadBucketResponse headBucketResponse = s3Client.headBucket(headBucketRequest);
            logger.debug("S3 bucket exists response: {}", headBucketResponse);

            if (headBucketResponse.sdkHttpResponse().isSuccessful()) {
                exists = true;
            }
        } catch (NoSuchBucketException nsbe) {
            // ignore, create the bucket
        }

        if (!exists) {
            logger.debug("S3 bucket create...");
            final CreateBucketRequest createBucketRequest = CreateBucketRequest
                    .builder()
                    .bucket(bucketName)
                    .objectLockEnabledForBucket(false)
                    .build();
            final CreateBucketResponse createBucketResponse =
                    s3Client.createBucket(createBucketRequest);
            logger.debug("S3 bucket create response={}", createBucketResponse);

            if (createBucketResponse == null) {
                throw new StackTestServiceException("Null response for create bucket: " + bucketName);
            }

            final String location = createBucketResponse.location();
            if (location == null || location.isBlank()) {
                throw new StackTestServiceException("Blank location response for bucket: " + bucketName);
            }
        }
    }



    // ..............................
    // Vitess .......................
    // ..............................
    private void checkVitessTables() throws StackTestServiceException {
        logger.info("Vitess check...");

        final Jdbi shardedJdbi = stackTestServices
                .getClientServices()
                .get(JdbiClientService.class)
                .getClientIntegrationService(
                        new DefaultMutableAppbaseClientKey<>(new TextValue(StackTestConstants.SHARDED)))
                .getClient();
        final Handle shardedHandle = shardedJdbi.open();
        try {
            final Jdbi unshardedJdbi = stackTestServices
                    .getClientServices()
                    .get(JdbiClientService.class)
                    .getClientIntegrationService(
                            new DefaultMutableAppbaseClientKey<>(new TextValue(StackTestConstants.UNSHARDED)))
                    .getClient();
            final Handle unshardedHandle = unshardedJdbi.open();
            try {
                checkStackTestCustomerVitessTable(shardedHandle);
                checkStackTestOrderVitessTable(shardedHandle);
                checkStackTestProductVitessTable(unshardedHandle);
                logger.info("Vitess check - DONE");
            } finally {
                try { unshardedHandle.close(); } catch (Exception ignored) {}
            }
        } finally {
            try { shardedHandle.close(); } catch (Exception ignored) {}
        }
    }

    private void checkStackTestCustomerVitessTable(
            final Handle handle) throws StackTestServiceException
    {
        logger.debug("Vitess table {} check...", StackTestConstants.STACK_TEST_CUSTOMER_TABLE_NAME);

        try {
            final CustomerJDBA customerJDBA = handle.attach(CustomerJDBA.class);
            customerJDBA.readById(1L);
        } catch (JdbiException je) {
            throw new StackTestServiceException("Exception querying vitess table: " +
                    StackTestConstants.STACK_TEST_CUSTOMER_TABLE_NAME, je);
        }
    }

    private void checkStackTestOrderVitessTable(
            final Handle handle) throws StackTestServiceException
    {
        logger.debug("Vitess table {} check...", StackTestConstants.STACK_TEST_ORDER_TABLE_NAME);

        try {
            final OrderJDBA orderJDBA = handle.attach(OrderJDBA.class);
            orderJDBA.readById(1L);
        } catch (JdbiException je) {
            throw new StackTestServiceException("Exception querying vitess table: " +
                    StackTestConstants.STACK_TEST_ORDER_TABLE_NAME, je);
        }
    }

    private void checkStackTestProductVitessTable(
            final Handle handle) throws StackTestServiceException
    {
        logger.debug("Vitess table {} check...", StackTestConstants.STACK_TEST_PRODUCT_TABLE_NAME);

        try {
            final ProductJDBA productJDBA = handle.attach(ProductJDBA.class);
            productJDBA.readById("sku1");
        } catch (JdbiException je) {
            throw new StackTestServiceException("Exception querying vitess table: " +
                    StackTestConstants.STACK_TEST_PRODUCT_TABLE_NAME, je);
        }
    }



    // ..............................
    // ZooKeeper ....................
    // ..............................
    private void checkAndCreateZooKeeperPaths() throws StackTestServiceException {
        logger.info("ZooKeeper check...");

        final CuratorFramework curatorFramework = stackTestServices
                .getClientServices()
                .get(ZooKeeperClientService.class)
                .getClient();

        checkAndCreateStackTestSharedCounterPath(curatorFramework);

        logger.info("ZooKeeper check - DONE");
    }

    private void checkAndCreateStackTestSharedCounterPath(
            final CuratorFramework curatorFramework) throws StackTestServiceException
    {
        logger.debug("ZooKeeper create if not exist path: {}", StackTestConstants.ZOOKEEPER_SHARED_COUNTER_PATH);

        try {
            if (!checkZooKeeperPathExists(curatorFramework, StackTestConstants.ZOOKEEPER_SHARED_COUNTER_PATH)) {
                createZooKeeperPath(curatorFramework, StackTestConstants.ZOOKEEPER_SHARED_COUNTER_PATH);
                final DistributedCounterZKA distributedCounterZKA =
                        new DefaultDistributedCounterZKA(
                                stackTestServices.getAppbaseServices().getCoreServices().appbaseCore(),
                                stackTestServices.getClientServices().get(ZooKeeperClientService.class),
                                StackTestConstants.ZOOKEEPER_SHARED_COUNTER_PATH);
                distributedCounterZKA.set(0);
            }
        } catch (Exception e) {
            throw new StackTestServiceException(e);
        }
    }

    private boolean checkZooKeeperPathExists(
            final CuratorFramework curatorFramework,
            final String path)
            throws Exception
    {
        logger.debug("ZooKeeper path exists check for: {}", path);

        final Stat stat = curatorFramework
                .checkExists()
                .forPath(StackTestConstants.ZOOKEEPER_SHARED_COUNTER_PATH);
        final boolean exists = stat != null;
        logger.debug("ZooKeeper check path exists response: {}", exists);

        return exists;
    }

    private void createZooKeeperPath(
            final CuratorFramework curatorFramework,
            final String path)
            throws Exception
    {
        logger.debug("ZooKeeper path create for: {}", path);

        final String responsePath = curatorFramework
                .create()
                .creatingParentsIfNeeded()
                .forPath(StackTestConstants.ZOOKEEPER_SHARED_COUNTER_PATH);
        logger.debug("ZooKeeper path create response: {}", responsePath);

        if (responsePath == null) {
            throw new StackTestServiceException("Shared counter path creation returned null");
        }
    }
}
