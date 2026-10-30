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

package com.viveworks.appbase.client.cloud.aws.s3;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.core.AppbaseClientCoreGuiceModule;
import com.viveworks.appbase.client.core.ClientCoreTestHelper;
import com.viveworks.appbase.client.cloud.aws.httpclient.AwsSdkAsyncHttpClientServiceGuiceModule;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.di.guice.AppbaseCoreGuiceModule;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@EnabledIfSystemProperty(named = "S3ClientServiceTests.enabled", matches = "true")
public class S3ClientServiceGuiceModuleTest {
    private static AppbaseCore appbaseCore;
    private static AppbaseClientCore appbaseClientCore;
    private static S3ClientsService s3ClientsService;

    @AfterAll
    static void tearDown() {
        if (s3ClientsService != null) {
            s3ClientsService.shutdown();
        }

        if (appbaseClientCore != null) {
            appbaseClientCore.shutdown();
        }

        if (appbaseCore != null) {
            appbaseCore.shutdown();
        }
    }

    @Test
    void testS3ClientServiceGuiceModule() throws Exception {
        appbaseCore = S3ClientServiceTestHelper.appbaseCore();
        appbaseClientCore = ClientCoreTestHelper.defaultAppbaseClientCore(appbaseCore);

        final AppbaseCoreGuiceModule appbaseCoreGuiceModule = new AppbaseCoreGuiceModule(appbaseCore);
        final AppbaseClientCoreGuiceModule appbaseClientCoreGuiceModule =
                new AppbaseClientCoreGuiceModule(appbaseClientCore);
        final AwsSdkAsyncHttpClientServiceGuiceModule awsSdkAsyncHttpClientServiceGuiceModule =
                new AwsSdkAsyncHttpClientServiceGuiceModule();
        final S3ClientsServiceGuiceModule s3ClientsServiceGuiceModule = new S3ClientsServiceGuiceModule();

        final Injector injector = Guice.createInjector(
                appbaseCoreGuiceModule,
                appbaseClientCoreGuiceModule,
                awsSdkAsyncHttpClientServiceGuiceModule,
                s3ClientsServiceGuiceModule);
        Assertions.assertNotNull(injector);


        s3ClientsService = injector.getInstance(S3ClientsService.class);
        Assertions.assertNotNull(s3ClientsService);

        final S3ClientsIntegrationService s3ClientsIntegrationService = s3ClientsService.getClientIntegrationService();
        S3ClientServiceTestUtil.s3ClientServiceTest(s3ClientsService, s3ClientsIntegrationService);
    }
}
