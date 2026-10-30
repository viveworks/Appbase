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

package com.viveworks.appbase.client.cloud.aws.common;

import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.cloud.aws.common.configuration.AwsSdkClientOverrideConfiguration;
import com.viveworks.appbase.client.cloud.aws.common.configuration.AwsSdkProfileFileConfiguration;
import com.viveworks.appbase.client.cloud.aws.common.configuration.AwsSdkRetryPolicyConfiguration;
import com.viveworks.appbase.client.util.AppbaseClientUtil;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.StringList;
import com.viveworks.appbase.core.util.di.InjectedTypeConfiguration;
import com.viveworks.appbase.core.util.di.guice.GuiceInjectorService;
import com.viveworks.appbase.core.util.net.TimeoutConfiguration;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.profiles.ProfileFile;

import java.util.List;
import java.util.Map;

/**
 * Default implementation of {@link AwsSdkClientOverrideConfigurationFactory}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultAwsSdkClientOverrideConfigurationFactory implements AwsSdkClientOverrideConfigurationFactory {
    private final AppbaseClientCore appbaseClientCore;
    private final AwsSdkClientOverrideConfiguration awsSdkClientOverrideConfiguration;

    /**
     * Creates a DefaultAwsSdkClientOverrideConfigurationFactory using the supplied configuration and collaborators.
     */
    public DefaultAwsSdkClientOverrideConfigurationFactory(
            final AppbaseClientCore appbaseClientCore,
            final AwsSdkClientOverrideConfiguration awsSdkClientOverrideConfiguration)
    {
        AppbaseUtil.notNull(appbaseClientCore, "appbaseClientCore");
        AppbaseUtil.notNull(awsSdkClientOverrideConfiguration, "awsSdkClientOverrideConfiguration");

        this.appbaseClientCore = appbaseClientCore;
        this.awsSdkClientOverrideConfiguration = awsSdkClientOverrideConfiguration;
    }

    @Override
    public ClientOverrideConfiguration getInstance() {
        final ClientOverrideConfiguration.Builder builder = ClientOverrideConfiguration.builder();


        final AwsSdkProfileFileConfiguration defaultAwsSdkProfileFileConfiguration =
                awsSdkClientOverrideConfiguration.getDefaultAwsSdkProfileFileConfiguration();
        if (defaultAwsSdkProfileFileConfiguration != null) {
            final AwsSdkProfileFileFactory awsSdkProfileFileFactory =
                    new DefaultAwsSdkProfileFileFactory(defaultAwsSdkProfileFileConfiguration);
            final ProfileFile profileFile = awsSdkProfileFileFactory.getInstance();
            builder.defaultProfileFile(profileFile);
        }

        final String defaultProfileName = awsSdkClientOverrideConfiguration.getDefaultProfileName();
        if (defaultProfileName != null) {
            builder.defaultProfileName(defaultProfileName);
        }


        final TimeoutConfiguration apiCallAttemptTimeoutConfiguration =
                awsSdkClientOverrideConfiguration.getApiCallAttemptTimeoutConfiguration();
        if (apiCallAttemptTimeoutConfiguration != null) {
            builder.apiCallAttemptTimeout(AppbaseClientUtil.duration(apiCallAttemptTimeoutConfiguration));
        }


        final TimeoutConfiguration apiCallTimeoutConfiguration =
                awsSdkClientOverrideConfiguration.getApiCallTimeoutConfiguration();
        if (apiCallTimeoutConfiguration != null) {
            builder.apiCallTimeout(AppbaseClientUtil.duration(apiCallTimeoutConfiguration));
        }


        final AwsSdkRetryPolicyConfiguration awsSdkRetryPolicyConfiguration =
                awsSdkClientOverrideConfiguration.getAwsSdkRetryPolicyConfiguration();
        if (awsSdkRetryPolicyConfiguration != null) {
            final AwsSdkRetryPolicyFactory awsSdkRetryPolicyFactory =
                    new DefaultAwsSdkRetryPolicyFactory(appbaseClientCore, awsSdkRetryPolicyConfiguration);
            final RetryPolicy retryPolicy = awsSdkRetryPolicyFactory.getInstance();
            if (retryPolicy != null) {
                builder.retryPolicy(retryPolicy);
            }
        }


        final Map<String, StringList> headers = awsSdkClientOverrideConfiguration.getHeaders();
        if (headers != null) {
            headers.forEach((key, stringList) -> {
                if (stringList != null) {
                    final List<String> values = stringList.getStrings();
                    builder.putHeader(key, values);
                }
            });
        }


        final GuiceInjectorService guiceInjectorService = appbaseClientCore.getAppbaseCore().getGuiceInjectorService();

        final InjectedTypeConfiguration<? extends AwsSdkAdvancedClientOptionsFactory>
                awsSdkAdvancedClientOptionsFactoryInjectedTypeConfiguration =
                awsSdkClientOverrideConfiguration.getAwsSdkAdvancedClientOptionsFactoryInjectedTypeConfiguration();
        if (awsSdkAdvancedClientOptionsFactoryInjectedTypeConfiguration != null) {
            final AwsSdkAdvancedClientOptionsFactory awsSdkAdvancedClientOptionsFactory =
                    AppbaseUtil.injectedInstance(
                            guiceInjectorService, awsSdkAdvancedClientOptionsFactoryInjectedTypeConfiguration);
            final Map<SdkAdvancedClientOption<?>, ?> sdkAdvancedAsyncClientOptions =
                    awsSdkAdvancedClientOptionsFactory.getInstance();
            builder.advancedOptions(sdkAdvancedAsyncClientOptions);
        }


        final InjectedTypeConfiguration<AwsSdkExecutionInterceptorsFactory>
                awsSdkExecutionInterceptorsFactoryInjectedTypeConfiguration =
                awsSdkClientOverrideConfiguration.getAwsSdkExecutionInterceptorsFactoryInjectedTypeConfiguration();
        if (awsSdkExecutionInterceptorsFactoryInjectedTypeConfiguration != null) {
            final AwsSdkExecutionInterceptorsFactory awsSdkExecutionInterceptorsFactory =
                    AppbaseUtil.injectedInstance(
                            guiceInjectorService, awsSdkExecutionInterceptorsFactoryInjectedTypeConfiguration);
            final List<ExecutionInterceptor> executionInterceptors = awsSdkExecutionInterceptorsFactory.getInstance();
            if (executionInterceptors != null) {
                builder.executionInterceptors(executionInterceptors);
            }
        }


        return builder.build();
    }
}
