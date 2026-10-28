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

import com.viveworks.appbase.client.core.AppbaseClientRuntimeException;
import com.viveworks.appbase.client.cloud.aws.common.configuration.AwsSdkConfigurationFileConfiguration;
import com.viveworks.appbase.client.cloud.aws.common.configuration.AwsSdkCredentialsFileConfiguration;
import com.viveworks.appbase.client.cloud.aws.common.configuration.AwsSdkProfileFileConfiguration;
import com.viveworks.appbase.core.util.AppbaseConstants;
import com.viveworks.appbase.core.util.AppbaseUtil;
import software.amazon.awssdk.profiles.ProfileFile;

import java.io.InputStream;
import java.nio.file.Paths;

/**
 * Default implementation of {@link AwsSdkProfileFileFactory}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultAwsSdkProfileFileFactory implements AwsSdkProfileFileFactory {
    private final AwsSdkProfileFileConfiguration awsSdkProfileFileConfiguration;

    /**
     * Creates a DefaultAwsSdkProfileFileFactory using the supplied configuration and collaborators.
     */
    public DefaultAwsSdkProfileFileFactory(final AwsSdkProfileFileConfiguration awsSdkProfileFileConfiguration) {
        AppbaseUtil.notNull(awsSdkProfileFileConfiguration, "awsSdkProfileFileConfiguration");

        this.awsSdkProfileFileConfiguration = awsSdkProfileFileConfiguration;
    }

    @Override
    public ProfileFile getInstance() {
        if (awsSdkProfileFileConfiguration.isUseDefaultProfile()) {
            return ProfileFile.defaultProfileFile();
        }

        final AwsSdkConfigurationFileConfiguration awsSdkConfigurationFileConfiguration =
                awsSdkProfileFileConfiguration.getAwsSdkConfigurationFileConfiguration();
        final AwsSdkCredentialsFileConfiguration awsSdkCredentialsFileConfiguration =
                awsSdkProfileFileConfiguration.getAwsSdkCredentialsFileConfiguration();

        if (awsSdkConfigurationFileConfiguration != null && awsSdkCredentialsFileConfiguration != null) {
            final ProfileFile configurationFile =
                    profileFile(awsSdkConfigurationFileConfiguration.getLocation(), ProfileFile.Type.CONFIGURATION);
            final ProfileFile credentialsFile =
                    profileFile(awsSdkCredentialsFileConfiguration.getLocation(), ProfileFile.Type.CREDENTIALS);

            final ProfileFile profileFile = ProfileFile.aggregator()
                    .addFile(configurationFile)
                    .addFile(credentialsFile)
                    .build();
            return profileFile;
        }

        if (awsSdkConfigurationFileConfiguration != null) {
            return profileFile(awsSdkConfigurationFileConfiguration.getLocation(), ProfileFile.Type.CONFIGURATION);
        } else if (awsSdkCredentialsFileConfiguration != null) {
            return profileFile(awsSdkCredentialsFileConfiguration.getLocation(), ProfileFile.Type.CREDENTIALS);
        }

        return ProfileFile.defaultProfileFile();
    }

    private ProfileFile profileFile(final String location, final ProfileFile.Type type) {
        AppbaseUtil.notNull(location, "location");
        AppbaseUtil.notNull(type, "type");

        final ProfileFile.Builder profileFileBuilder = ProfileFile.builder();
        if (location.startsWith(AppbaseConstants.CLASSPATH_FILE_PREFIX)) {
            final String finalLocation = AppbaseUtil.stripClasspathPrefix(location);

            InputStream resourceAsStream = getClass().getResourceAsStream(finalLocation);
            if (resourceAsStream == null) {
                resourceAsStream = getClass().getClassLoader().getResourceAsStream(finalLocation);
            }

            if (resourceAsStream == null) {
                throw new AppbaseClientRuntimeException("Cannot read profile file from classpath: " + location);
            }

            profileFileBuilder.content(resourceAsStream);
        } else {
            profileFileBuilder.content(Paths.get(location));
        }

        return profileFileBuilder.type(type).build();
    }
}
