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

package com.viveworks.appbase.client.cloud.aws.common.configuration;

import com.viveworks.appbase.core.util.AppbaseUtil;
import software.amazon.awssdk.core.retry.RetryMode;

/**
 * Wrapper for {@link software.amazon.awssdk.core.retry.RetryMode}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public enum AwsSdkRetryMode {
    LEGACY(RetryMode.LEGACY),

    STANDARD(RetryMode.STANDARD);


    private final RetryMode retryMode;

    AwsSdkRetryMode(final RetryMode retryMode) {
        AppbaseUtil.notNull(retryMode, "retryMode");

        this.retryMode = retryMode;
    }

    /**
     * Returns the configured retry mode.
     *
     * @return the configured retry mode
     */
    public RetryMode getRetryMode() {
        return retryMode;
    }

    /**
     * Wrapper for {@link RetryMode#defaultRetryMode()}.
     *
     * @return Default {@link AwsSdkRetryMode}
     */
    public static AwsSdkRetryMode getDefaultRetryMode() {
        final RetryMode retryMode = RetryMode.defaultRetryMode();

        switch (retryMode) {
            case LEGACY:
                return LEGACY;
            case STANDARD:
            default:
                return STANDARD;
        }
    }
}
