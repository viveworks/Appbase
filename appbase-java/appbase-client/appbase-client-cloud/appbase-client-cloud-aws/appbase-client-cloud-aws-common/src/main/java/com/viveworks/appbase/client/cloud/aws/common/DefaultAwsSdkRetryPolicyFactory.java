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

package com.viveworks.appbase.client.cloud.aws.common;

import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.cloud.aws.common.configuration.AwsSdkRetryMode;
import com.viveworks.appbase.client.cloud.aws.common.configuration.AwsSdkRetryPolicyConfiguration;
import com.viveworks.appbase.client.cloud.aws.common.configuration.backoff.AwsSdkBackoffStrategyConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.di.InjectedTypeConfiguration;
import com.viveworks.appbase.core.util.di.guice.GuiceInjectorService;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.retry.backoff.BackoffStrategy;
import software.amazon.awssdk.core.retry.conditions.RetryCondition;

/**
 * Default implementation of {@link AwsSdkRetryPolicyFactory}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultAwsSdkRetryPolicyFactory implements AwsSdkRetryPolicyFactory {
    private final AppbaseClientCore appbaseClientCore;
    private final AwsSdkRetryPolicyConfiguration awsSdkRetryPolicyConfiguration;

    /**
     * Creates a DefaultAwsSdkRetryPolicyFactory using the supplied configuration and collaborators.
     */
    public DefaultAwsSdkRetryPolicyFactory(
            final AppbaseClientCore appbaseClientCore,
            final AwsSdkRetryPolicyConfiguration awsSdkRetryPolicyConfiguration)
    {
        AppbaseUtil.notNull(appbaseClientCore, "appbaseClientCore");
        AppbaseUtil.notNull(awsSdkRetryPolicyConfiguration, "awsSdkRetryPolicyConfiguration");

        this.appbaseClientCore = appbaseClientCore;
        this.awsSdkRetryPolicyConfiguration = awsSdkRetryPolicyConfiguration;
    }

    @Override
    public RetryPolicy getInstance() {
        if (awsSdkRetryPolicyConfiguration.isUseDefaultRetryPolicy()) {
            return RetryPolicy.defaultRetryPolicy();
        }


        RetryPolicy.Builder retryPolicyBuilder = null;


        final AwsSdkRetryMode awsSdkRetryMode = awsSdkRetryPolicyConfiguration.getAwsSdkRetryMode();
        if (awsSdkRetryMode != null) {
            if (awsSdkRetryPolicyConfiguration.isUseBuilderWithRetryMode()) {
                retryPolicyBuilder = RetryPolicy.builder(awsSdkRetryMode.getRetryMode());
            } else {
                return RetryPolicy.forRetryMode(awsSdkRetryMode.getRetryMode());
            }
        }


        if (retryPolicyBuilder == null) {
            retryPolicyBuilder = RetryPolicy.builder();
        }


        final AwsSdkBackoffStrategyConfiguration awsSdkBackoffStrategyConfiguration =
                awsSdkRetryPolicyConfiguration.getAwsSdkBackoffStrategyConfiguration();
        if (awsSdkBackoffStrategyConfiguration != null) {
            final AwsSdkBackoffStrategyFactory awsSdkBackoffStrategyFactory =
                    new DefaultAwsSdkBackoffStrategyFactory(awsSdkBackoffStrategyConfiguration);
            final BackoffStrategy backoffStrategy = awsSdkBackoffStrategyFactory.getInstance();
            retryPolicyBuilder.backoffStrategy(backoffStrategy);
        }


        final Boolean additionalRetryConditionsAllowed =
                awsSdkRetryPolicyConfiguration.getAdditionalRetryConditionsAllowed();
        if (additionalRetryConditionsAllowed != null) {
            retryPolicyBuilder.additionalRetryConditionsAllowed(additionalRetryConditionsAllowed);
        }


        final Integer retryCount = awsSdkRetryPolicyConfiguration.getRetryCount();
        if (retryCount != null) {
            retryPolicyBuilder.numRetries(retryCount);
        }


        final GuiceInjectorService guiceInjectorService = appbaseClientCore.getAppbaseCore().getGuiceInjectorService();


        final InjectedTypeConfiguration<? extends RetryCondition> retryConditionInjectedTypeConfiguration =
                awsSdkRetryPolicyConfiguration.getRetryConditionInjectedTypeConfiguration();
        if (retryConditionInjectedTypeConfiguration != null) {
            final RetryCondition retryCondition =
                    AppbaseUtil.injectedInstance(guiceInjectorService, retryConditionInjectedTypeConfiguration);
            if (retryCondition != null) {
                retryPolicyBuilder.retryCondition(retryCondition);
            }
        }


        final InjectedTypeConfiguration<? extends RetryCondition> retryCapacityConditionInjectedTypeConfiguration =
                awsSdkRetryPolicyConfiguration.getRetryCapacityConditionInjectedTypeConfiguration();
        if (retryCapacityConditionInjectedTypeConfiguration != null) {
            final RetryCondition retryCapacityCondition =
                    AppbaseUtil.injectedInstance(guiceInjectorService, retryCapacityConditionInjectedTypeConfiguration);
            if (retryCapacityCondition != null) {
                retryPolicyBuilder.retryCapacityCondition(retryCapacityCondition);
            }
        }


        final AwsSdkBackoffStrategyConfiguration throttlingBackoffStrategyConfiguration =
                awsSdkRetryPolicyConfiguration.getThrottlingBackoffStrategyConfiguration();
        if (throttlingBackoffStrategyConfiguration != null) {
            final AwsSdkBackoffStrategyFactory throttlingAwsSdkBackoffStrategyFactory =
                    new DefaultAwsSdkBackoffStrategyFactory(throttlingBackoffStrategyConfiguration);
            final BackoffStrategy throttlingBackoffStrategy = throttlingAwsSdkBackoffStrategyFactory.getInstance();
            retryPolicyBuilder.throttlingBackoffStrategy(throttlingBackoffStrategy);
        }


        return retryPolicyBuilder.build();
    }
}
