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

import com.viveworks.appbase.client.cloud.aws.common.configuration.AwsSdkCredentialsProviderConfiguration;
import com.viveworks.appbase.client.cloud.aws.common.configuration.AwsSdkProfileFileConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.profiles.ProfileFile;

/**
 * Default implementation of {@link AwsSdkCredentialsProviderFactory}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultAwsSdkCredentialsProviderFactory implements AwsSdkCredentialsProviderFactory {
    private final AwsSdkCredentialsProviderConfiguration awsSdkCredentialsProviderConfiguration;

    /**
     * Creates a DefaultAwsSdkCredentialsProviderFactory using the supplied configuration and collaborators.
     */
    public DefaultAwsSdkCredentialsProviderFactory(
            final AwsSdkCredentialsProviderConfiguration awsSdkCredentialsProviderConfiguration)
    {
        AppbaseUtil.notNull(awsSdkCredentialsProviderConfiguration, "awsSdkCredentialsProviderConfiguration");

        this.awsSdkCredentialsProviderConfiguration = awsSdkCredentialsProviderConfiguration;
    }

    @Override
    public AwsCredentialsProvider getInstance() {
        final DefaultCredentialsProvider.Builder defaultCredentialsProviderBuilder = DefaultCredentialsProvider.builder();

        final AwsSdkProfileFileConfiguration awsSdkProfileFileConfiguration =
                awsSdkCredentialsProviderConfiguration.getAwsSdkProfileFileConfiguration();
        if (awsSdkProfileFileConfiguration != null) {
            final DefaultAwsSdkProfileFileFactory defaultAwsSdkProfileFileFactory =
                    new DefaultAwsSdkProfileFileFactory(awsSdkProfileFileConfiguration);
            final ProfileFile profileFile = defaultAwsSdkProfileFileFactory.getInstance();
            defaultCredentialsProviderBuilder.profileFile(profileFile);
        }

        final String profileName = awsSdkCredentialsProviderConfiguration.getProfileName();
        if (profileName != null) {
            defaultCredentialsProviderBuilder.profileName(profileName);
        }

        return defaultCredentialsProviderBuilder
                .asyncCredentialUpdateEnabled(awsSdkCredentialsProviderConfiguration.isAsyncCredentialUpdateEnabled())
                .reuseLastProviderEnabled(awsSdkCredentialsProviderConfiguration.isReuseLastProviderEnabled())
                .build();
    }
}
