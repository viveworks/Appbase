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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.curator.SessionFailedRetryPolicy;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Represents {@link SessionFailedRetryPolicy} configuration.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = SessionFailedRetryConfiguration.Builder.class)
public final class SessionFailedRetryConfiguration implements RetryPolicyConfiguration {
    private final RetryPolicyConfiguration delegateRetryPolicyConfiguration;

    private SessionFailedRetryConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");

        delegateRetryPolicyConfiguration = builder.delegateRetryPolicyConfiguration;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public RetryPolicyConfiguration getDelegateRetryPolicyConfiguration() {
        return delegateRetryPolicyConfiguration;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final SessionFailedRetryConfiguration that = (SessionFailedRetryConfiguration) o;
        return Objects.equals(delegateRetryPolicyConfiguration, that.delegateRetryPolicyConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(delegateRetryPolicyConfiguration);
    }

    /**
     * Builder for {@link SessionFailedRetryConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings({"FCBL_FIELD_COULD_BE_LOCAL"})
    public static final class Builder {
        private RetryPolicyConfiguration DEFAULT_DELEGATE_RETRY_POLICY_CONFIGURATION = RetryForeverConfiguration
                .builder()
                .build();

        private RetryPolicyConfiguration delegateRetryPolicyConfiguration = DEFAULT_DELEGATE_RETRY_POLICY_CONFIGURATION;

        private Builder() {
        }

        public Builder delegateRetryPolicyConfiguration(
                final RetryPolicyConfiguration delegateRetryPolicyConfiguration)
        {
            this.delegateRetryPolicyConfiguration = delegateRetryPolicyConfiguration;
            return this;
        }

        public SessionFailedRetryConfiguration build() {
            return new SessionFailedRetryConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
