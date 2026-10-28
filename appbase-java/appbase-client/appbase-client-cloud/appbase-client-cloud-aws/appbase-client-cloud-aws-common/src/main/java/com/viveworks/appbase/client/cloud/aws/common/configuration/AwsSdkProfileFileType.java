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

/**
 * This package contains client services for {@link software.amazon.awssdk.http.async.SdkAsyncHttpClient}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */

package com.viveworks.appbase.client.cloud.aws.common.configuration;

import com.viveworks.appbase.core.util.AppbaseUtil;
import software.amazon.awssdk.profiles.ProfileFile;

/**
 * Wrapper for {@link software.amazon.awssdk.profiles.ProfileFile.Type}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public enum AwsSdkProfileFileType {
    CONFIGURATION(ProfileFile.Type.CONFIGURATION),

    CREDENTIALS(ProfileFile.Type.CREDENTIALS);


    private final ProfileFile.Type type;

    AwsSdkProfileFileType(final ProfileFile.Type type) {
        AppbaseUtil.notNull(type, "type");

        this.type = type;
    }

    /**
     * Returns the configured type.
     *
     * @return the configured type
     */
    public ProfileFile.Type getType() {
        return type;
    }
}
