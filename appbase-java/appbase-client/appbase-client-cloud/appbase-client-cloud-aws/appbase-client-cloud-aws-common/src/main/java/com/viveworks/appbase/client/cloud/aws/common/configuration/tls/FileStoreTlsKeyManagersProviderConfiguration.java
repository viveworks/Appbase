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

package com.viveworks.appbase.client.cloud.aws.common.configuration.tls;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.io.File;
import java.util.Objects;

/**
 * Configuration for {@link software.amazon.awssdk.http.FileStoreTlsKeyManagersProvider}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = FileStoreTlsKeyManagersProviderConfiguration.Builder.class)
public final class FileStoreTlsKeyManagersProviderConfiguration implements AwsSdkTlsKeyManagersProviderConfiguration {
    private final String storeLocation;
    private final String storeType;
    private final String password;

    private FileStoreTlsKeyManagersProviderConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notBlank(builder.storeLocation, "builder.storeLocation");
        AppbaseUtil.notBlank(builder.storeType, "builder.storeType");

        storeLocation = builder.storeLocation;
        storeType = builder.storeType;
        password = builder.password;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the configured store location.
     *
     * @return the configured store location
     */
    public String getStoreLocation() {
        return storeLocation;
    }

    /**
     * Returns the configured store type.
     *
     * @return the configured store type
     */
    public String getStoreType() {
        return storeType;
    }

    /**
     * Returns the configured password.
     *
     * @return the configured password
     */
    public String getPassword() {
        return password;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final FileStoreTlsKeyManagersProviderConfiguration that = (FileStoreTlsKeyManagersProviderConfiguration) o;
        return Objects.equals(storeLocation, that.storeLocation) &&
                Objects.equals(storeType, that.storeType) &&
                Objects.equals(password, that.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(storeLocation, storeType, password);
    }

    @Override
    public String toString() {
        return "FileStoreTlsKeyManagersProviderConfiguration{" +
                "storeLocation=" + storeLocation +
                ", storeType='" + storeType + '\'' +
                ", password=" + AppbaseUtil.OBFUSCATE +
                '}';
    }


    /**
     * Builder for {@link FileStoreTlsKeyManagersProviderConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final String DEFAULT_STORE_LOCATION = System.getProperty("JAVA_HOME") + File.separator +
                "lib" + File.separator +
                "security" + File.separator +
                "cacerts";
        private static final String DEFAULT_STORE_TYPE = "PKCS12";


        private String storeLocation = DEFAULT_STORE_LOCATION;
        private String storeType = DEFAULT_STORE_TYPE;
        private String password;


        private Builder() {
        }

        public Builder storeLocation(final String storeLocation) {
            this.storeLocation = storeLocation;
            return this;
        }

        public Builder storeType(final String storeType) {
            this.storeType = storeType;
            return this;
        }

        public Builder password(final String password) {
            this.password = password;
            return this;
        }

        public FileStoreTlsKeyManagersProviderConfiguration build() {
            return new FileStoreTlsKeyManagersProviderConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
