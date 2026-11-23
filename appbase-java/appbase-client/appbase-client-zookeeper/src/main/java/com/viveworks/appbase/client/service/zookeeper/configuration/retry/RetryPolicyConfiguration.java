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

package com.viveworks.appbase.client.service.zookeeper.configuration.retry;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Represents a {@link org.apache.curator.RetryPolicy} configuration.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = BoundedExponentialBackoffRetryConfiguration.class,
                name = "BoundedExponentialBackoffRetryConfiguration"),
        @JsonSubTypes.Type(value = ExponentialBackoffRetryConfiguration.class,
                name = "ExponentialBackoffRetryConfiguration"),
        @JsonSubTypes.Type(value = RetryForeverConfiguration.class,
                name = "RetryForeverConfiguration"),
        @JsonSubTypes.Type(value = RetryNTimesConfiguration.class,
                name = "RetryNTimesConfiguration"),
        @JsonSubTypes.Type(value = RetryOneTimeConfiguration.class,
                name = "RetryOneTimeConfiguration"),
        @JsonSubTypes.Type(value = RetryUntilElapsedConfiguration.class,
                name = "RetryUntilElapsedConfiguration"),
        @JsonSubTypes.Type(value = SessionFailedRetryConfiguration.class,
                name = "SessionFailedRetryConfiguration")
})
public interface RetryPolicyConfiguration {
}
