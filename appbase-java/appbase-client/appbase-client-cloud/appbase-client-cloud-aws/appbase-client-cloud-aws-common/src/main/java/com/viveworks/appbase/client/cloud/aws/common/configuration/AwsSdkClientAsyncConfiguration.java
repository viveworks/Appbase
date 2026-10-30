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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.client.cloud.aws.common.AwsSdkAdvancedAsyncClientOptionsFactory;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.di.InjectedTypeConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import software.amazon.awssdk.core.client.config.ClientAsyncConfiguration;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Configuration used to generate {@link ClientAsyncConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = AwsSdkClientAsyncConfiguration.Builder.class)
public final class AwsSdkClientAsyncConfiguration {
    private final InjectedTypeConfiguration<? extends AwsSdkAdvancedAsyncClientOptionsFactory>
            awsSdkAdvancedAsyncClientOptionsFactoryInjectedTypeConfiguration;

    private AwsSdkClientAsyncConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");

        awsSdkAdvancedAsyncClientOptionsFactoryInjectedTypeConfiguration =
                builder.awsSdkAdvancedAsyncClientOptionsFactoryInjectedTypeConfiguration;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public InjectedTypeConfiguration<? extends AwsSdkAdvancedAsyncClientOptionsFactory>
    getAwsSdkAdvancedAsyncClientOptionsFactoryInjectedTypeConfiguration()
    {
        return awsSdkAdvancedAsyncClientOptionsFactoryInjectedTypeConfiguration;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final AwsSdkClientAsyncConfiguration that = (AwsSdkClientAsyncConfiguration) o;
        return Objects.equals(
                awsSdkAdvancedAsyncClientOptionsFactoryInjectedTypeConfiguration,
                that.awsSdkAdvancedAsyncClientOptionsFactoryInjectedTypeConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(awsSdkAdvancedAsyncClientOptionsFactoryInjectedTypeConfiguration);
    }

    @Override
    public String toString() {
        return "AwsSdkClientAsyncConfiguration{" +
                "awsSdkAdvancedAsyncClientOptionsFactoryInjectedTypeConfiguration=" +
                awsSdkAdvancedAsyncClientOptionsFactoryInjectedTypeConfiguration +
                '}';
    }


    /**
     * Builder for {@link AwsSdkClientAsyncConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private InjectedTypeConfiguration<? extends AwsSdkAdvancedAsyncClientOptionsFactory>
                awsSdkAdvancedAsyncClientOptionsFactoryInjectedTypeConfiguration;

        private Builder() {
        }

        public Builder awsSdkAdvancedAsyncClientOptionsFactoryInjectedTypeConfiguration(
                final InjectedTypeConfiguration<? extends AwsSdkAdvancedAsyncClientOptionsFactory>
                        awsSdkAdvancedAsyncClientOptionsFactoryInjectedTypeConfiguration)
        {
            this.awsSdkAdvancedAsyncClientOptionsFactoryInjectedTypeConfiguration =
                    awsSdkAdvancedAsyncClientOptionsFactoryInjectedTypeConfiguration;
            return this;
        }

        public AwsSdkClientAsyncConfiguration build() {
            return new AwsSdkClientAsyncConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
