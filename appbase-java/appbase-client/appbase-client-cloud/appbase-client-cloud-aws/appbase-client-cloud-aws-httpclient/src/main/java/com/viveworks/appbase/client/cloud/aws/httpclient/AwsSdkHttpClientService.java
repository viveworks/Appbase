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

package com.viveworks.appbase.client.cloud.aws.httpclient;

import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.AppbaseClientService;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValue;
import software.amazon.awssdk.http.SdkHttpClient;

/**
 * Represents {@link AppbaseClientService} for {@link }.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public interface AwsSdkHttpClientService extends AppbaseClientService<SdkHttpClient, TextValue> {
    @Override
    AwsSdkHttpClientIntegrationService getClientIntegrationService()
            throws AppbaseClientIntegrationService.NotFoundException;

    @Override
    AwsSdkHttpClientIntegrationService getClientIntegrationService(AppbaseClientKey<TextValue> appbaseClientKey)
            throws AppbaseClientIntegrationService.NotFoundException;
}
