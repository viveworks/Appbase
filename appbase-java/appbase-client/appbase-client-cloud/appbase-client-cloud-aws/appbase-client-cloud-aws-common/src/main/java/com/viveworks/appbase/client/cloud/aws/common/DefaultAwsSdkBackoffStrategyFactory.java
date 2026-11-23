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

import com.viveworks.appbase.client.core.AppbaseClientIntegrationRuntimeException;
import com.viveworks.appbase.client.cloud.aws.common.configuration.backoff.AwsSdkBackoffStrategyConfiguration;
import com.viveworks.appbase.client.cloud.aws.common.configuration.backoff.EqualJitterBackoffStrategyConfiguration;
import com.viveworks.appbase.client.cloud.aws.common.configuration.backoff.FixedDelayBackoffStrategyConfiguration;
import com.viveworks.appbase.client.cloud.aws.common.configuration.backoff.FullJitterBackoffStrategyConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import software.amazon.awssdk.core.retry.backoff.BackoffStrategy;
import software.amazon.awssdk.core.retry.backoff.EqualJitterBackoffStrategy;
import software.amazon.awssdk.core.retry.backoff.FixedDelayBackoffStrategy;
import software.amazon.awssdk.core.retry.backoff.FullJitterBackoffStrategy;

/**
 * Default implementation of {@link AwsSdkBackoffStrategyFactory}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultAwsSdkBackoffStrategyFactory implements AwsSdkBackoffStrategyFactory {
    private final AwsSdkBackoffStrategyConfiguration awsSdkBackoffStrategyConfiguration;

    /**
     * Creates a DefaultAwsSdkBackoffStrategyFactory using the supplied configuration and collaborators.
     */
    public DefaultAwsSdkBackoffStrategyFactory(
            final AwsSdkBackoffStrategyConfiguration awsSdkBackoffStrategyConfiguration)
    {
        AppbaseUtil.notNull(awsSdkBackoffStrategyConfiguration, "awsSdkBackoffStrategyConfiguration");

        this.awsSdkBackoffStrategyConfiguration = awsSdkBackoffStrategyConfiguration;
    }

    @Override
    public BackoffStrategy getInstance() {
        final Class<? extends AwsSdkBackoffStrategyConfiguration> awsSdkBackoffStrategyConfigurationType =
                awsSdkBackoffStrategyConfiguration.getClass();
        if (EqualJitterBackoffStrategyConfiguration.class.isAssignableFrom(awsSdkBackoffStrategyConfigurationType)) {
            final EqualJitterBackoffStrategyConfiguration equalJitterBackoffStrategyConfiguration =
                    (EqualJitterBackoffStrategyConfiguration) awsSdkBackoffStrategyConfiguration;

            return EqualJitterBackoffStrategy
                    .builder()
                    .baseDelay(equalJitterBackoffStrategyConfiguration.getBaseDelay())
                    .maxBackoffTime(equalJitterBackoffStrategyConfiguration.getMaxBackoffTime())
                    .build();
        } else if (FixedDelayBackoffStrategyConfiguration.class.isAssignableFrom(awsSdkBackoffStrategyConfigurationType)) {
            final FixedDelayBackoffStrategyConfiguration fixedDelayBackoffStrategyConfiguration =
                    (FixedDelayBackoffStrategyConfiguration) awsSdkBackoffStrategyConfiguration;

            return FixedDelayBackoffStrategy
                    .create(fixedDelayBackoffStrategyConfiguration.getFixedBackoff());
        } else if (FullJitterBackoffStrategyConfiguration.class.isAssignableFrom(awsSdkBackoffStrategyConfigurationType)) {
            final FullJitterBackoffStrategyConfiguration fullJitterBackoffStrategyConfiguration =
                    (FullJitterBackoffStrategyConfiguration) awsSdkBackoffStrategyConfiguration;

            return FullJitterBackoffStrategy
                    .builder()
                    .baseDelay(fullJitterBackoffStrategyConfiguration.getBaseDelay())
                    .maxBackoffTime(fullJitterBackoffStrategyConfiguration.getMaxBackoffTime())
                    .build();
        }

        throw new AppbaseClientIntegrationRuntimeException("Unsupported subtype of " +
                AwsSdkBackoffStrategyConfiguration.class.getName() + ": " + awsSdkBackoffStrategyConfigurationType);
    }
}
