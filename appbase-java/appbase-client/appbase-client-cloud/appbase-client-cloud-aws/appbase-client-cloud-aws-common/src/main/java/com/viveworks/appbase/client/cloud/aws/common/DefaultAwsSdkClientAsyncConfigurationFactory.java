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

/**
 * This package contains client services for {@link software.amazon.awssdk.http.async.SdkAsyncHttpClient}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */

package com.viveworks.appbase.client.cloud.aws.common;

import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.cloud.aws.common.configuration.AwsSdkClientAsyncConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.di.InjectedTypeConfiguration;
import com.viveworks.appbase.core.util.di.guice.GuiceInjectorService;
import software.amazon.awssdk.core.client.config.ClientAsyncConfiguration;
import software.amazon.awssdk.core.client.config.SdkAdvancedAsyncClientOption;

import java.util.Map;

/**
 * Default implementation of {@link AwsSdkClientAsyncConfigurationFactory}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultAwsSdkClientAsyncConfigurationFactory implements AwsSdkClientAsyncConfigurationFactory {
    private final AppbaseClientCore appbaseClientCore;
    private final AwsSdkClientAsyncConfiguration awsSdkClientAsyncConfiguration;

    /**
     * Creates a DefaultAwsSdkClientAsyncConfigurationFactory using the supplied configuration and collaborators.
     */
    public DefaultAwsSdkClientAsyncConfigurationFactory(
            final AppbaseClientCore appbaseClientCore,
            final AwsSdkClientAsyncConfiguration awsSdkClientAsyncConfiguration)
    {
        AppbaseUtil.notNull(appbaseClientCore, "appbaseClientCore");
        AppbaseUtil.notNull(awsSdkClientAsyncConfiguration, "awsSdkClientAsyncConfiguration");

        this.appbaseClientCore = appbaseClientCore;
        this.awsSdkClientAsyncConfiguration = awsSdkClientAsyncConfiguration;
    }

    @Override
    public ClientAsyncConfiguration getInstance() {
        final ClientAsyncConfiguration.Builder builder = ClientAsyncConfiguration.builder();

        final InjectedTypeConfiguration<? extends AwsSdkAdvancedAsyncClientOptionsFactory>
                awsSdkAdvancedAsyncClientOptionsFactoryInjectedTypeConfiguration =
                awsSdkClientAsyncConfiguration.getAwsSdkAdvancedAsyncClientOptionsFactoryInjectedTypeConfiguration();
        if (awsSdkAdvancedAsyncClientOptionsFactoryInjectedTypeConfiguration != null) {
            final GuiceInjectorService guiceInjectorService = appbaseClientCore.getAppbaseCore().getGuiceInjectorService();
            final AwsSdkAdvancedAsyncClientOptionsFactory awsSdkAdvancedAsyncClientOptionsFactory =
                    AppbaseUtil.injectedInstance(
                            guiceInjectorService, awsSdkAdvancedAsyncClientOptionsFactoryInjectedTypeConfiguration);
            if (awsSdkAdvancedAsyncClientOptionsFactory != null) {
                final Map<SdkAdvancedAsyncClientOption<?>, ?> sdkAdvancedAsyncClientOptionMap =
                        awsSdkAdvancedAsyncClientOptionsFactory.getInstance();
                if (sdkAdvancedAsyncClientOptionMap != null) {
                    builder.advancedOptions(sdkAdvancedAsyncClientOptionMap);
                }
            }
        }

        return builder.build();
    }
}
