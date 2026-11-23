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
package com.viveworks.appbase.stacktest;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.stacktest.data.model.Customer;
import com.viveworks.appbase.stacktest.data.model.Order;
import com.viveworks.appbase.stacktest.data.model.Product;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class StackTestConstants {
    public static final String STACK_TEST = "stacktest";
    public static final String AEROSPIKE_NAMESPACE = "dev";
    public static final String AEROSPIKE_CUSTOMER_SET = Customer.class.getSimpleName().toLowerCase();
    public static final String AEROSPIKE_ORDER_SET = Order.class.getSimpleName().toLowerCase();
    public static final String AEROSPIKE_PRODUCT_SET = Product.class.getSimpleName().toLowerCase();
    public static final String DRUID_DATASOURCE_NAME = "appbase_stacktest_healthcheck_data";
    public static final String ELASTICSEARCH_CUSTOMER_INDEX_NAME = STACK_TEST + "-customer";
    public static final String ELASTICSEARCH_ORDER_INDEX_NAME = STACK_TEST + "-order";
    public static final String ELASTICSEARCH_PRODUCT_INDEX_NAME = STACK_TEST + "-product";
    public static final String S3_BUCKET_NAME = STACK_TEST;
    public static final String ZOOKEEPER_SHARED_COUNTER_PATH = "/stacktest/shared/counter";

    public static final String SHARDED = "sharded";
    public static final String UNSHARDED = "unsharded";
    public static final String STACK_TEST_CUSTOMER_TABLE_NAME = "stacktest_customer";
    public static final String STACK_TEST_ORDER_TABLE_NAME = "stacktest_order";
    public static final String STACK_TEST_PRODUCT_TABLE_NAME = "stacktest_product";

    public static final String ID_PARAM = "id";
    public static final String LIMIT_PARAM = "limit";
    public static final String CUSTOMER_ID_PARAM = "customer_id";
    public static final String PHOTO_ID_PARAM = "photo_id";
    public static final String EMAIL_PARAM = "email";
    public static final String SEARCH_PARAM = "search";

    public static final String STACK_TEST_ALL_PATH = "/*";
    public static final String STACK_TEST_REST_ROOT_PATH = "/*";
    public static final String VERSION_PATH = "/v1";

    public static final String STACK_TEST_CUSTOMER_PATH = "/customer";
    public static final String STACK_TEST_CUSTOMER_ORDERS_PATH = "/customer_orders";
    public static final String STACK_TEST_CUSTOMER_PHOTO_PATH = "/customer_photo";
    public static final String STACK_TEST_CUSTOMER_SEARCH_PATH = "/customer_search";
    public static final String STACK_TEST_CUSTOMER_STATISTICS_PATH = "/customer_statistics";
    public static final String STACK_TEST_SEARCH_PATH = "/search";
    public static final String STACK_TEST_ORDER_PATH = "/order";
    public static final String STACK_TEST_ORDER_SEARCH_PATH = "/order_search";
    public static final String STACK_TEST_PRODUCT_PATH = "/product";
    public static final String STACK_TEST_PRODUCT_SEARCH_PATH = "/product_search";

    /**
     * Used to set a different servlet context for stacktest REST API. By doing this, we can keep the
     * stacktest REST API / Swagger UI separate from appbase-core's REST API / Swagger UI. We could
     * choose a different servlet context or the prefix, but we want `/stacktest` to appear in the Swagger UI,
     * so STACK_TEST_RESTAPI_PREFIX contains `/stacktest` and just to have no conflicts, the
     * context is `/stacktest-context`
     */
    public static final String STACK_TEST_RESTAPI_SERVLET_CONTEXT = "/stacktest-context";
    public static final String STACK_TEST_RESTAPI_PREFIX = "/stacktest/restapi" + VERSION_PATH;
    public static final String STACK_TEST_CUSTOMERS_RESOURCE =
            STACK_TEST_RESTAPI_PREFIX + "/customers";
    public static final String STACK_TEST_ORDERS_RESOURCE =
            STACK_TEST_RESTAPI_PREFIX + "/orders";
    public static final String STACK_TEST_PRODUCTS_RESOURCE =
            STACK_TEST_RESTAPI_PREFIX + "/products";
    public static final String STACK_TEST_CUSTOMER_PHOTOS_RESOURCE =
            STACK_TEST_RESTAPI_PREFIX + "/customer_photos";
    public static final String STACK_TEST_CUSTOMER_STATISTICS_RESOURCE =
            STACK_TEST_RESTAPI_PREFIX + "/customer_statistics";
    public static final String STACK_TEST_SEARCH_RESOURCE =
            STACK_TEST_RESTAPI_PREFIX + "/search";

    public static final String METRIC_TEST_RESOURCE1_PATH = STACK_TEST_RESTAPI_PREFIX + "/metric_test_resource1";
    public static final String METRIC_TEST_SERVLET1_PATH = "/metric_test_servlet1";
    public static final String METRIC_TEST_RESOURCE2_PATH = STACK_TEST_RESTAPI_PREFIX + "/metric_test_resource2";

    public static final String TRACE_ID_TEST_RESOURCE_PATH = STACK_TEST_RESTAPI_PREFIX + "/trace_id_test_resource";
    public static final String TRACE_ID_TEST_SERVLET_PATH = "/trace_id_test_servlet";

    public static final String EXCEPTION_HANDLER_TEST_RESOURCE_PATH =
            STACK_TEST_RESTAPI_PREFIX + "/exception_handler_test_resource";
    public static final String EXCEPTION_HANDLER_TEST_SERVLET_PATH = "/exception_handler_test_servlet";

    public static final String CORS_TEST_RESOURCE_PATH = STACK_TEST_RESTAPI_PREFIX + "/cors_test_resource";
    public static final String CORS_TEST_SERVLET_PATH = "/origin_test_servlet";

    public static final String APPBASE_SYNC_SERVLET_FILTER_TEST_SERVLET_PATH =
            "/appbase_sync_servlet_filter_test_servlet";

    public static final String USER_SESSION_FILTER_TEST_RESOURCE_PATH =
            STACK_TEST_RESTAPI_PREFIX + "/user_session_filter_test_resource";
    public static final String USER_SESSION_FILTER_TEST_SERVLET_PATH = "/user_session_filter_test_servlet";

    public static final String IDP_USER_TOKEN_CHECK_FILTER_TEST_RESOURCE_PATH =
            STACK_TEST_RESTAPI_PREFIX + "/idp_user_token_check_test_resource";
    public static final String IDP_USER_TOKEN_CHECK_FILTER_TEST_SERVLET_PATH = "/idp_user_token_check_test_servlet";

    public static final String AUTHORIZATION_FILTER_AUTHORIZED_RESOURCE_PATH =
            STACK_TEST_RESTAPI_PREFIX + "/authorization_filter_authorized_resource";
    public static final String AUTHORIZATION_FILTER_AUTHORIZED_SERVLET_PATH =
            "/authorization_filter_authorized_servlet";
    public static final String AUTHORIZATION_FILTER_UNAUTHORIZED_RESOURCE_PATH =
            STACK_TEST_RESTAPI_PREFIX + "/authorization_filter_unauthorized_resource";
    public static final String AUTHORIZATION_FILTER_UNAUTHORIZED_SERVLET_PATH =
            "/authorization_filter_unauthorized_servlet";
    public static final String AUTHORIZATION_FILTER_AUTHORIZED_RESOURCE_UNAUTHORIZED_METHOD_PATH =
            STACK_TEST_RESTAPI_PREFIX + "/authorization_filter_authorized_resource_unauthorized_method";
    public static final String AUTHORIZATION_FILTER_AUTHORIZED_SERVLET_UNAUTHORIZED_METHOD_PATH =
            "/authorization_filter_authorized_servlet_unauthorized_method";
    public static final String AUTHORIZATION_FILTER_AUTHORIZED_RESOURCE_METHOD_PATH =
            STACK_TEST_RESTAPI_PREFIX + "/authorization_filter_authorized_resource_method";
    public static final String AUTHORIZATION_FILTER_AUTHORIZED_SERVLET_METHOD_PATH =
            "/authorization_filter_authorized_servlet_method";

    public static final String STACK_TEST_HEALTH_CHECK_CONTEXT_PATH = "/health";
    public static final String SWAGGER_UI_PATH = "/restapi";
    public static final String OPENAPI_JSON_PATH = "/openapi.json";
    public static final String HEALTH_CHECK_PATH = "/check";
    public static final String SWAGGER_UI_INDEX_HTML_PATH = "/swagger-ui/dist";
    public static final String INDEX_HTML = "index.html";

    public static final String OPENAPI_PRETTY_PRINT = "openApi.configuration.prettyPrint";
    public static final String PROPERTY_PREFIX = "appbase.stacktest.";
    public static final String JETTY_PORT_PROPERTY = PROPERTY_PREFIX + "jetty.port";
    public static final String DEFAULT_JETTY_PORT = "2409";
    public static final long ELASTICSEARCH_INDEX_SYNC_WAIT_MS = 3000;
    public static final String LOCALHOST = "localhost";

    public static final String CUSTOMER_PHOTO_UPLOAD_PATHNAME;
    public static final String CUSTOMER_PHOTO_BY_CUSTOMER_ID_DOWNLOAD_PATHNAME;
    public static final String CUSTOMER_PHOTO_BY_PHOTO_ID_DOWNLOAD_PATHNAME;
    public static final String OBJECT_STORE_READ_DESTINATION_BASE_PATHNAME;
    static {
        try {
            final Path s3ReadDestinationBasePathname =
                    Files.createTempDirectory(STACK_TEST + "-objectStoreReadDestination");
            s3ReadDestinationBasePathname.toFile().deleteOnExit();
            OBJECT_STORE_READ_DESTINATION_BASE_PATHNAME = s3ReadDestinationBasePathname.toString();

            final Path customerPhotoUploadPath =
                    Files.createTempDirectory(STACK_TEST + "-customerPhotoUpload");
            customerPhotoUploadPath.toFile().deleteOnExit();
            CUSTOMER_PHOTO_UPLOAD_PATHNAME = customerPhotoUploadPath.toString();

            final Path customerPhotoByCustomerIdDownloadPath =
                    Files.createTempDirectory(STACK_TEST + "-customerPhotoByCustomerIdDownload");
            customerPhotoByCustomerIdDownloadPath.toFile().deleteOnExit();
            CUSTOMER_PHOTO_BY_CUSTOMER_ID_DOWNLOAD_PATHNAME = customerPhotoByCustomerIdDownloadPath.toString();

            final Path customerPhotoByPhotoIdDownloadPath =
                    Files.createTempDirectory(STACK_TEST + "-customerPhotoByPhotoIdDownload");
            customerPhotoByPhotoIdDownloadPath.toFile().deleteOnExit();
            CUSTOMER_PHOTO_BY_PHOTO_ID_DOWNLOAD_PATHNAME = customerPhotoByPhotoIdDownloadPath.toString();
        } catch (IOException e) {
            throw new AppbaseRuntimeException(e);
        }
    }
}
