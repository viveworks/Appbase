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

import com.viveworks.appbase.bootstrap.client.AppbaseClientServices;
import com.viveworks.appbase.bootstrap.client.factory.AerospikeClientServiceFactory;
import com.viveworks.appbase.bootstrap.client.factory.AwsSdkAsyncHttpClientServiceFactory;
import com.viveworks.appbase.bootstrap.client.factory.AwsSdkHttpClientServiceFactory;
import com.viveworks.appbase.bootstrap.client.factory.DruidSqlClientServiceFactory;
import com.viveworks.appbase.bootstrap.client.factory.ElasticsearchRestClientFactory;
import com.viveworks.appbase.bootstrap.client.factory.FirebaseAppClientServiceFactory;
import com.viveworks.appbase.bootstrap.client.factory.FirebaseAuthClientServiceFactory;
import com.viveworks.appbase.bootstrap.client.factory.RedisClientServiceFactory;
import com.viveworks.appbase.bootstrap.client.factory.S3ClientsServiceFactory;
import com.viveworks.appbase.bootstrap.client.factory.VitessGrpcClientServiceFactory;
import com.viveworks.appbase.bootstrap.client.factory.VitessJdbcClientServiceFactory;
import com.viveworks.appbase.bootstrap.client.factory.VitessJdbiClientServiceFactory;
import com.viveworks.appbase.bootstrap.client.factory.ZooKeeperClientServiceFactory;
import com.viveworks.appbase.bootstrap.core.AppbaseCoreServices;
import com.viveworks.appbase.bootstrap.jetty.server.JettyServletContainer;
import com.viveworks.appbase.bootstrap.server.AppbaseWebServer;
import com.viveworks.appbase.bootstrap.service.AppbaseServices;
import com.viveworks.appbase.bootstrap.tomcat.server.TomcatServletContainer;
import com.viveworks.appbase.bootstrap.undertow.server.UndertowServletContainer;
import com.viveworks.appbase.bootstrap.service.factory.AerospikeUserSessionStoreFactory;
import com.viveworks.appbase.bootstrap.service.factory.DefaultFirebaseAuthServiceFactory;
import com.viveworks.appbase.bootstrap.service.factory.DefaultHealthCheckServiceFactory;
import com.viveworks.appbase.bootstrap.service.factory.DefaultIdServiceFactory;
import com.viveworks.appbase.bootstrap.service.factory.DefaultPasswordServiceFactory;
import com.viveworks.appbase.bootstrap.service.factory.DefaultSupportMessageServiceFactory;
import com.viveworks.appbase.bootstrap.service.factory.DefaultUserAccountServiceFactory;
import com.viveworks.appbase.bootstrap.service.factory.JdbiSupportMessageStoreFactory;
import com.viveworks.appbase.bootstrap.service.factory.JdbiUserAccountStoreFactory;
import com.viveworks.appbase.bootstrap.service.factory.StaticClusterServiceFactory;
import com.viveworks.appbase.client.service.jdbi.JdbiClientService;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.util.AppbaseConstants;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.cluster.StaticClusterServiceConfiguration;
import com.viveworks.appbase.service.health.DefaultHealthCheckServiceConfiguration;
import com.viveworks.appbase.service.id.DefaultIdServiceConfiguration;
import com.viveworks.appbase.service.password.DefaultPasswordServiceConfiguration;
import com.viveworks.appbase.service.supportmessage.DefaultSupportMessageServiceConfiguration;
import com.viveworks.appbase.service.supportmessage.endpoint.jaxrs.SupportMessageJaxrsResource;
import com.viveworks.appbase.service.supportmessage.store.JdbiSupportMessageStoreConfiguration;
import com.viveworks.appbase.service.user.account.DefaultUserAccountServiceConfiguration;
import com.viveworks.appbase.service.user.account.firebaseauth.DefaultFirebaseAuthServiceConfiguration;
import com.viveworks.appbase.service.user.account.store.JdbiUserAccountStoreConfiguration;
import com.viveworks.appbase.service.user.account.web.endpoint.jaxrs.UserAccountJaxrsResource;
import com.viveworks.appbase.service.user.account.web.filter.jaxrs.IdpUserTokenCheckJaxrsRequestFilter;
import com.viveworks.appbase.service.user.session.store.DefaultUserSessionStoreConfiguration;
import com.viveworks.appbase.service.user.session.web.filter.jaxrs.UserSessionJaxrsRequestFilter;
import com.viveworks.appbase.service.user.session.web.filter.jaxrs.UserSessionJaxrsResponseFilter;
import com.viveworks.appbase.service.web.authorization.filter.jaxrs.AuthorizationJaxrsRequestFilter;
import com.viveworks.appbase.service.web.endpoint.jaxrs.appbase.StatusJaxrsResource;
import com.viveworks.appbase.service.web.exceptionhandler.jaxrs.JaxrsExceptionHandlerMapper;
import com.viveworks.appbase.service.web.health.endpoint.jaxrs.HealthCheckJaxrsResource;
import com.viveworks.appbase.service.web.metric.filter.jaxrs.MetricJaxrsRequestFilter;
import com.viveworks.appbase.service.web.trace.filter.jaxrs.TraceIdJaxrsRequestFilter;
import com.viveworks.appbase.stacktest.service.StackTestServices;
import com.viveworks.appbase.stacktest.http.jaxrs.CustomerJaxrsResource;
import com.viveworks.appbase.stacktest.http.jaxrs.provider.JaxrsAppbaseCoreProvider;
import com.viveworks.appbase.stacktest.test.app.CustomerApiTest;
import com.viveworks.appbase.stacktest.test.app.CustomerPhotoApiTest;
import com.viveworks.appbase.stacktest.test.app.CustomerStatisticsApiTest;
import com.viveworks.appbase.stacktest.test.app.OrderApiTest;
import com.viveworks.appbase.stacktest.test.app.ProductApiTest;
import com.viveworks.appbase.stacktest.test.app.SearchApiTest;
import com.viveworks.appbase.stacktest.test.core.ConfigurationApiTest;
import com.viveworks.appbase.stacktest.test.core.container.ContainerApiTest;
import com.viveworks.appbase.stacktest.test.core.CoreApiTest;
import com.viveworks.appbase.stacktest.test.core.ExceptionApiTest;
import com.viveworks.appbase.stacktest.test.core.JmxApiTest;
import com.viveworks.appbase.stacktest.test.core.LogApiTest;
import com.viveworks.appbase.stacktest.test.core.MediatorApiTest;
import com.viveworks.appbase.stacktest.test.core.MetricApiTest;
import com.viveworks.appbase.stacktest.test.core.ServiceRegistryApiTest;
import com.viveworks.appbase.stacktest.test.core.StatusApiTest;
import com.viveworks.appbase.stacktest.test.service.authorization.AuthorizationFilterTest;
import com.viveworks.appbase.stacktest.test.service.authorization.testimpl.AuthorizationFilterTestImpl;
import com.viveworks.appbase.stacktest.test.service.exceptionhandler.ExceptionHandlerTest;
import com.viveworks.appbase.stacktest.test.service.exceptionhandler.testimpl.ExceptionHandlerTestImpl;
import com.viveworks.appbase.stacktest.test.service.healthcheck.HealthCheckApiTest;
import com.viveworks.appbase.stacktest.test.service.metric.MetricFilterTest;
import com.viveworks.appbase.stacktest.test.service.metric.testimpl.MetricFilterTestImpl;
import com.viveworks.appbase.stacktest.test.service.origin.CorsFilterTest;
import com.viveworks.appbase.stacktest.test.service.origin.testimpl.CorsFilterTestImpl;
import com.viveworks.appbase.stacktest.test.service.redis.RedisApiTest;
import com.viveworks.appbase.stacktest.test.service.supportmessage.SupportMessageApiTest;
import com.viveworks.appbase.stacktest.test.service.syncfilter.AppbaseSyncFilterTest;
import com.viveworks.appbase.stacktest.test.service.traceid.TraceIdFilterTest;
import com.viveworks.appbase.stacktest.test.service.traceid.testimpl.TraceIdFilterTestImpl;
import com.viveworks.appbase.stacktest.test.service.user.account.UserAccountApiTest;
import com.viveworks.appbase.stacktest.test.service.user.session.IdpUserTokenCheckApiTest;
import com.viveworks.appbase.stacktest.test.service.user.session.UserSessionApiTest;
import com.viveworks.appbase.stacktest.test.service.user.session.testImpl.IdpUserTokenCheckFilterTestImpl;
import com.viveworks.appbase.stacktest.test.service.user.session.testImpl.UserSessionFilterTestImpl;
import org.apache.http.HttpHost;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.slf4j.Logger;

import java.io.IOException;

/**
 * Entry point for the Appbase stack test suite and a reference example for bootstrapping
 * an Appbase application.
 *
 * <h2>Bootstrap sequence</h2>
 * <ol>
 *   <li><b>Core services</b> — build {@link com.viveworks.appbase.bootstrap.core.AppbaseCoreServices}
 *       from a YAML file (or programmatically via
 *       {@code AppbaseCoreServicesConfiguration.builder().build()}). This initialises the
 *       configuration service, log service, JMX, metrics, mediator, and all other
 *       cross-cutting concerns.</li>
 *   <li><b>Client services</b> — build
 *       {@link com.viveworks.appbase.bootstrap.client.AppbaseClientServices} on top of core.
 *       Each external system (Aerospike, Elasticsearch, ZooKeeper, S3, Vitess, etc.) is
 *       registered via a factory. Factories accept either a classpath YAML path or a
 *       programmatic config object — see the inline comments in {@link #main} for examples
 *       of both forms.</li>
 *   <li><b>Application services</b> — build
 *       {@link com.viveworks.appbase.bootstrap.service.AppbaseServices} on top of core and
 *       client services. User-account, session, health-check, password, and other high-level
 *       services are registered here via service factories. A
 *       {@link com.viveworks.appbase.service.authorization.ContextAuthorizer} can be wired in
 *       to enforce request-level access control.</li>
 *   <li><b>HTTP server</b> — build a
 *       {@link com.viveworks.appbase.bootstrap.server.AppbaseWebServer}. Standard Appbase
 *       core servlets, service filters, service servlets, and Swagger UI are added via
 *       dedicated builder methods. Application-specific JAX-RS resources and servlets are
 *       registered through {@code resourceConfig()} and {@code withServletContext()}.</li>
 * </ol>
 *
 * <h2>Customisation points shown here</h2>
 * <ul>
 *   <li>Programmatic vs. YAML factory creation (see comment block above
 *       {@code AppbaseClientServices.builder()}).</li>
 *   <li>Excluding or re-pathing standard Appbase servlets/filters via
 *       {@code withAppbaseCoreServlets(excludes, pathOverrides)} overloads.</li>
 *   <li>Wiring a custom {@link com.viveworks.appbase.service.authorization.ContextAuthorizer}
 *       via {@code AppbaseServices.Builder.withContextAuthorizer()}.</li>
 * </ul>
 *
 * <p>The infrastructure check ({@link StackTestInfra#checkAndCreateRequiredInfra()}) and
 * the test runner ({@link #runTests()}) are stack-test-specific and are not required in
 * a production Appbase deployment.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class StackTestMain {

    private static final int HTTP_CONNECTION_MAX_TOTAL    = 4000;
    private static final int HTTP_CONNECTION_MAX_PER_ROUTE = 2000;

    private static AppbaseServices appbaseServices;
    private static StackTestServices stackTestServices;
    private static Logger logger;
    private static AppbaseWebServer stackTestServer;
    private static String baseUrl;


    @SuppressWarnings("checkstyle:IllegalCatch")
    public static void main(final String[] args) throws Exception {
        final String container = parseArg(args, "--container", "jetty");
        final boolean pathProbeOnly = hasFlag(args, "--path-probe-only");
        try {
            System.setProperty(
                    AppbaseConstants.APPBASE_CONFIGURATION_FILE_PATH_PROPERTY_KEY,
                    "classpath:///appbase-stacktest-core.yaml");
            final int jettyPort = Integer.parseInt(System.getProperty(
                    StackTestConstants.JETTY_PORT_PROPERTY,
                    StackTestConstants.DEFAULT_JETTY_PORT));


            // Bootstrap core services from YAML. Alternatively, build fully programmatically
            // without YAML — override only what differs from defaults:
            final AppbaseCoreServices coreServices = AppbaseCoreServices.builder()
                    .fromYaml("classpath:///appbase-stacktest-bootstrap.yaml")
                    .build();
            logger = coreServices.appbaseCore().getLogService().getLogger(StackTestMain.class);

            if (pathProbeOnly) {
                startPathProbeServer(coreServices, jettyPort);
                return;
            }


            // Each factory accepts either a classpath YAML path or a programmatic config object. For example:
            //   .with(new ZooKeeperClientServiceFactory(
            //       DefaultZooKeeperClientServiceConfiguration.builder()
            //           // configure host/port, retry policy, timeouts, etc.
            //           .build()))
            final AppbaseClientServices clientServices = AppbaseClientServices.builder()
                    .from(coreServices)
                    .with(new AerospikeClientServiceFactory("classpath:///appbase-stacktest-aerospike.yaml"))
                    .with(new DruidSqlClientServiceFactory(
                            "classpath:///appbase-stacktest-druid-sql-shared-resources.yaml",
                            "classpath:///appbase-stacktest-druid-sql.yaml"))
                    .with(new ElasticsearchRestClientFactory("classpath:///appbase-stacktest-elasticsearch-low-level.yaml"))
                    .with(new FirebaseAppClientServiceFactory("classpath:///appbase-stacktest-firebase-app.yaml"))
                    .with(new FirebaseAuthClientServiceFactory("classpath:///appbase-stacktest-firebase-auth.yaml"))
                    .with(new VitessJdbcClientServiceFactory("classpath:///appbase-stacktest-vitess-jdbc.yaml"))
                    .with(new VitessJdbiClientServiceFactory("classpath:///appbase-stacktest-vitess-jdbi.yaml"))
                    .with(new VitessGrpcClientServiceFactory("classpath:///appbase-stacktest-vitess-grpc.yaml"))
                    .with(new AwsSdkHttpClientServiceFactory("classpath:///appbase-stacktest-aws-http-client.yaml"))
                    .with(new AwsSdkAsyncHttpClientServiceFactory(
                            "classpath:///appbase-stacktest-aws-async-http-client.yaml"))
                    .with(new S3ClientsServiceFactory("classpath:///appbase-stacktest-aws-s3.yaml"))
                    .with(new ZooKeeperClientServiceFactory("classpath:///appbase-stacktest-zookeeper.yaml"))
                    .with(new RedisClientServiceFactory("classpath:///appbase-stacktest-redis.yaml"))
                    .build();


            appbaseServices = AppbaseServices.builder()
                    .from(coreServices)
                    .withClientServices(clientServices)
                    .withContextAuthorizer(new StackTestContextAuthorizer())
                    .with(new StaticClusterServiceFactory(
                            StaticClusterServiceConfiguration.builder()
                                    .homeDataCenter("dev")
                                    .build()))
                    .with(new DefaultHealthCheckServiceFactory(
                            DefaultHealthCheckServiceConfiguration.builder().build()))
                    .with(new DefaultIdServiceFactory(DefaultIdServiceConfiguration.builder().build()))
                    .with(new DefaultPasswordServiceFactory(new DefaultPasswordServiceConfiguration()))
                    .with(new AerospikeUserSessionStoreFactory(
                            DefaultUserSessionStoreConfiguration.builder().build()))
                    .with(new JdbiUserAccountStoreFactory(
                            JdbiUserAccountStoreConfiguration.builder().build(),
                            clientServices.get(JdbiClientService.class)))
                    .with(new DefaultFirebaseAuthServiceFactory(
                            DefaultFirebaseAuthServiceConfiguration.builder()
                                    .userTokenVerificationEnabled(true)
                                    .build()))
                    .with(new DefaultUserAccountServiceFactory(new DefaultUserAccountServiceConfiguration()))
                    .with(new JdbiSupportMessageStoreFactory(
                            JdbiSupportMessageStoreConfiguration.builder().build(),
                            clientServices.get(JdbiClientService.class)))
                    .with(new DefaultSupportMessageServiceFactory(
                            new DefaultSupportMessageServiceConfiguration()))
                    .withAllJmxOperationsEnabled()
                    .build();


            stackTestServices = new StackTestServices(clientServices, appbaseServices);


            // Stack-test only: ensures the required Docker-backed infrastructure
            // (Aerospike, Elasticsearch, S3/MinIO, ZooKeeper, etc.) is running before
            // tests execute. Not part of a production bootstrap.
            final StackTestInfra stackTestInfra = new StackTestInfra(stackTestServices);
            stackTestInfra.checkAndCreateRequiredInfra();


            // Non-Jetty containers support core Appbase services only (no StackTestServletConfig
            // wiring). App-level servlet tests may fail when --container undertow|tomcat is used.
            // Full parity across containers is tracked as P-008.
            if (!"jetty".equals(container) && !"undertow".equals(container) && !"tomcat".equals(container)) {
                logger.warn("Unknown --container value '{}'; falling back to jetty", container);
            }
            switch (container) {
                case "undertow":
                    stackTestServer = UndertowServletContainer.builder()
                            .from(appbaseServices)
                            .port(jettyPort)
                            .withAppbaseCoreServlets()
                            .withAppbaseServiceFilters()
                            .withAppbaseServiceServlets()
                            .resourceConfig(buildResourceConfig(stackTestServices))
                            .build();
                    break;
                case "tomcat":
                    stackTestServer = TomcatServletContainer.builder()
                            .from(appbaseServices)
                            .port(jettyPort)
                            .withAppbaseCoreServlets()
                            .withAppbaseServiceFilters()
                            .withAppbaseServiceServlets()
                            .resourceConfig(buildResourceConfig(stackTestServices))
                            .build();
                    break;
                // "jetty"
                default:
                    stackTestServer = JettyServletContainer.builder()
                            .from(appbaseServices)
                            .port(jettyPort)
                            .withDefaultMXBeans()
                            .withAppbaseCoreServlets()
                            .withAppbaseServiceFilters()
                            .withAppbaseServiceServlets()
                            .withSwaggerUI("/restapi", "/swagger-ui/dist", "index.html")
                            .resourceConfig(buildResourceConfig(stackTestServices))
                            .withServletContext(ctx -> {
                                try {
                                    StackTestServletConfig.configure(ctx, stackTestServices, appbaseServices);
                                } catch (IOException exception) {
                                    throw new StackTestRuntimeException(exception);
                                }
                            })
                            .build();
                    break;
            }

            baseUrl = System.getProperty("stacktest.server.protocol", "http") +
                    "://" + System.getProperty("stacktest.server.host", "localhost") +
                    ":" + jettyPort;

            stackTestServer.start();
            runTests();

            printServerReady(baseUrl);
        } catch (Exception ex) {
            if (logger != null) {
                logger.error("Error starting stack test server", ex);
            } else {
                ex.printStackTrace();
            }
            Thread.sleep(1000);

            if (stackTestServer != null) {
                stackTestServer.stop();
            }
            System.exit(-1);
        }
    }

    /**
     * Starts the real Jetty service-filter chain for transport-level path authorization probes.
     * External client factories and the stack-test suite are deliberately omitted: NM-5 measures
     * {@code AuthorizationServletFilter}'s canonical-path behavior and does not exercise a data
     * backend. Secure-by-default makes every canonical {@code /admin} spelling terminate at that
     * real filter with the same forbidden response instead of relying on a downstream 404.
     */
    private static void startPathProbeServer(
            final AppbaseCoreServices coreServices,
            final int jettyPort) throws Exception
    {
        appbaseServices = AppbaseServices.builder()
                .from(coreServices)
                .secureByDefault(true)
                .build();
        stackTestServer = JettyServletContainer.builder()
                .from(appbaseServices)
                .port(jettyPort)
                .withAppbaseServiceFilters()
                .withServletContext(context -> context.setAttribute(
                        AppbaseServiceConstants.APPBASE_CORE_ATTRIBUTE,
                        coreServices.appbaseCore()))
                .build();
        stackTestServer.start();
        baseUrl = "http://127.0.0.1:" + jettyPort;
        logger.info("NM-5 path-probe-only server ready at {}", baseUrl);
    }

    private static ResourceConfig buildResourceConfig(final StackTestServices stackTestServices) {
        return new ResourceConfig()
                .register(new AbstractBinder() {
                    @Override
                    protected void configure() {
                        bind(stackTestServices).to(StackTestServices.class);
                    }
                })
                .packages(
                        CustomerJaxrsResource.class.getPackageName(),
                        JaxrsAppbaseCoreProvider.class.getPackageName(),
                        JaxrsExceptionHandlerMapper.class.getPackageName(),
                        StatusJaxrsResource.class.getPackageName(),
                        ExceptionHandlerTestImpl.ExceptionHandlerTestResource.class.getPackageName(),
                        MetricJaxrsRequestFilter.class.getPackageName(),
                        MetricFilterTestImpl.MetricTestResource1.class.getPackageName(),
                        TraceIdJaxrsRequestFilter.class.getPackageName(),
                        TraceIdFilterTestImpl.TraceIdTestResource.class.getPackageName(),
                        CorsFilterTestImpl.CorsTestResource.class.getPackageName(),
                        HealthCheckJaxrsResource.class.getPackageName(),
                        UserAccountJaxrsResource.class.getPackageName(),
                        UserSessionJaxrsRequestFilter.class.getPackageName(),
                        UserSessionFilterTestImpl.UserSessionTestResource.class.getPackageName(),
                        IdpUserTokenCheckJaxrsRequestFilter.class.getPackageName(),
                        IdpUserTokenCheckFilterTestImpl.IdpUserTokenCheckTestResource.class.getPackageName(),
                        AuthorizationJaxrsRequestFilter.class.getPackageName(),
                        AuthorizationFilterTestImpl.AuthorizedResourceTest.class.getPackageName(),
                        UserSessionJaxrsResponseFilter.class.getPackageName(),
                        SupportMessageJaxrsResource.class.getPackageName(),
                        "io.swagger.jaxrs.json",
                        "io.swagger.jaxrs.listing",
                        "io.swagger.v3.jaxrs2.integration.resources")
                .register(MultiPartFeature.class)
                .property(ServerProperties.WADL_FEATURE_DISABLE, true)
                .property(JettyServletContainer.OPENAPI_PRETTY_PRINT, true)
                .property(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true)
                .property(ServerProperties.BV_DISABLE_VALIDATE_ON_EXECUTABLE_OVERRIDE_CHECK, true);
    }

    private static void runTestsImpl() throws Exception {
        final PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(HTTP_CONNECTION_MAX_TOTAL);
        final int jettyPort = Integer.parseInt(System.getProperty(
                StackTestConstants.JETTY_PORT_PROPERTY,
                StackTestConstants.DEFAULT_JETTY_PORT));
        final String serverHost = System.getProperty("stacktest.server.host", "localhost");
        final HttpRoute httpRoute = new HttpRoute(new HttpHost(serverHost, jettyPort));
        connectionManager.setMaxPerRoute(httpRoute, HTTP_CONNECTION_MAX_PER_ROUTE);
        final CookieStore cookieStore = new BasicCookieStore();
        final HttpClient httpClient = HttpClients
                .custom()
                .setConnectionManager(connectionManager)
                .setDefaultCookieStore(cookieStore)
                .build();

        Thread.sleep(2000);
        ContainerApiTest.runTests(appbaseServices, httpClient, cookieStore, baseUrl);

        Thread.sleep(2000);
        ConfigurationApiTest.runTests(appbaseServices, httpClient, cookieStore, baseUrl);
        CoreApiTest.runTests(appbaseServices, httpClient, cookieStore, baseUrl);
        ExceptionApiTest.runTests(appbaseServices, httpClient, cookieStore, baseUrl);
        JmxApiTest.runTests(appbaseServices, httpClient, cookieStore, baseUrl);
        LogApiTest.runTests(appbaseServices, httpClient, cookieStore, baseUrl);
        MetricApiTest.runTests(appbaseServices, httpClient, cookieStore, baseUrl);
        MediatorApiTest.runTests(appbaseServices, httpClient, cookieStore, baseUrl);
        ServiceRegistryApiTest.runTests(appbaseServices, httpClient, cookieStore, baseUrl);
        StatusApiTest.runTests(appbaseServices, httpClient, cookieStore, baseUrl);

        Thread.sleep(2000);
        MetricFilterTest.runTests(appbaseServices, httpClient, cookieStore, baseUrl);
        TraceIdFilterTest.runTests(appbaseServices, httpClient, cookieStore, baseUrl);
        CorsFilterTest.runTests(appbaseServices, httpClient, cookieStore, baseUrl);
        Thread.sleep(2000);
        HealthCheckApiTest.runTests(appbaseServices, httpClient, cookieStore, baseUrl);
        ExceptionHandlerTest.runTests(appbaseServices, httpClient, cookieStore, baseUrl);
        AppbaseSyncFilterTest.runTests(appbaseServices, httpClient, cookieStore, baseUrl);

        Thread.sleep(2000);
        UserAccountApiTest.runTests(appbaseServices, httpClient, cookieStore, baseUrl);
        UserSessionApiTest.runTests(appbaseServices, httpClient, cookieStore, baseUrl);
        IdpUserTokenCheckApiTest.runTests(appbaseServices, httpClient, cookieStore, baseUrl);
        AuthorizationFilterTest.runTests(appbaseServices, httpClient, cookieStore, baseUrl);

        Thread.sleep(2000);
        SupportMessageApiTest.runTests(appbaseServices, httpClient, cookieStore, baseUrl);

        Thread.sleep(2000);
        RedisApiTest.runTests(appbaseServices);

        Thread.sleep(2000);
        CustomerApiTest.runTests(stackTestServices, httpClient, cookieStore, baseUrl);
        CustomerStatisticsApiTest.runTests(stackTestServices, httpClient, cookieStore, baseUrl);
        CustomerPhotoApiTest.runTests(stackTestServices, httpClient, cookieStore, baseUrl);
        OrderApiTest.runTests(stackTestServices, httpClient, cookieStore, baseUrl);
        ProductApiTest.runTests(stackTestServices, httpClient, cookieStore, baseUrl);
        SearchApiTest.runTests(stackTestServices, httpClient, cookieStore, baseUrl);
    }

    private static void runTests() throws Exception {
        logger.info("Running tests in 5 seconds...");
        Thread.sleep(5 * 1000);

        try {
            runTestsImpl();
        } catch (HttpHostConnectException hce) {
            Thread.sleep(10 * 1000);
            runTestsImpl();
        }
    }

    private static String parseArg(final String[] args, final String flag, final String defaultValue) {
        for (int i = 0; i < args.length - 1; i++) {
            if (flag.equals(args[i])) {
                return args[i + 1];
            }
        }
        return defaultValue;
    }

    private static boolean hasFlag(final String[] args, final String flag) {
        for (final String arg : args) {
            if (flag.equals(arg)) {
                return true;
            }
        }
        return false;
    }

    private static void printServerReady(final String baseUrl) throws Exception {
        logger.info("Testing done, server will be ready in 5 seconds...");
        Thread.sleep(5 * 1000);

        logger.info("Swagger UI: {}",
                baseUrl + "/restapi");
        logger.info("OpenAPI JSON: {}",
                baseUrl + "/openapi.json");
        logger.info("stacktest REST API: {}",
                baseUrl + StackTestConstants.STACK_TEST_RESTAPI_PREFIX);
        logger.info("appbase-core REST API: {}",
                baseUrl + AppbaseServiceConstants.DEFAULT_APPBASE_CORE_RESTAPI_PREFIX);
        logger.info("Health check: {}", baseUrl +
                StackTestConstants.STACK_TEST_HEALTH_CHECK_CONTEXT_PATH + StackTestConstants.HEALTH_CHECK_PATH);
        logger.info("Servlets context path: {}", baseUrl);
        logger.info("Server ready");
    }
}
