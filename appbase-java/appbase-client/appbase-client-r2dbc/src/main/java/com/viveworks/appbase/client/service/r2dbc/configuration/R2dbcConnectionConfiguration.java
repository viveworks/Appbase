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

package com.viveworks.appbase.client.service.r2dbc.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.r2dbc.spi.ConnectionFactoryOptions;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Configuration used to build {@link ConnectionFactoryOptions} for a R2DBC connection.
 *
 * <p>Two, mutually-exclusive ways to describe the connection are supported:
 * <ul>
 *     <li>a raw {@link #getUrl()} (e.g. {@code r2dbc:h2:mem:///t1;DB_CLOSE_DELAY=-1}), parsed
 *     via {@link ConnectionFactoryOptions#parse(CharSequence)}; or</li>
 *     <li>the discrete {@link #getDriver()}/{@link #getHost()}/{@link #getPort()}/
 *     {@link #getDatabase()}/{@link #getUser()}/{@link #getPassword()}/{@link #getSsl()} options,
 *     assembled into {@link ConnectionFactoryOptions} field-by-field.</li>
 * </ul>
 * When {@link #getUrl()} is present it always wins; see
 * {@code com.viveworks.appbase.client.service.r2dbc.R2dbcClientUtil#options(R2dbcConnectionConfiguration)}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = R2dbcConnectionConfiguration.Builder.class)
public final class R2dbcConnectionConfiguration {
    @Nullable
    private final String url;

    @Nullable
    private final String driver;

    @Nullable
    private final String host;

    @Nullable
    private final Integer port;

    @Nullable
    private final String database;

    @Nullable
    private final String user;

    @Nullable
    private final String password;

    @Nullable
    private final Boolean ssl;


    private R2dbcConnectionConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");

        if (builder.url == null) {
            AppbaseUtil.notBlank(builder.driver, "builder.driver");
            AppbaseUtil.notBlank(builder.host, "builder.host");
        }

        url = builder.url;
        driver = builder.driver;
        host = builder.host;
        port = builder.port;
        database = builder.database;
        user = builder.user;
        password = builder.password;
        ssl = builder.ssl;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    @Nullable
    public String getUrl() {
        return url;
    }

    @Nullable
    public String getDriver() {
        return driver;
    }

    @Nullable
    public String getHost() {
        return host;
    }

    @Nullable
    public Integer getPort() {
        return port;
    }

    @Nullable
    public String getDatabase() {
        return database;
    }

    @Nullable
    public String getUser() {
        return user;
    }

    @Nullable
    public String getPassword() {
        return password;
    }

    @Nullable
    public Boolean getSsl() {
        return ssl;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final R2dbcConnectionConfiguration that = (R2dbcConnectionConfiguration) o;
        return Objects.equals(url, that.url) &&
                Objects.equals(driver, that.driver) &&
                Objects.equals(host, that.host) &&
                Objects.equals(port, that.port) &&
                Objects.equals(database, that.database) &&
                Objects.equals(user, that.user) &&
                Objects.equals(password, that.password) &&
                Objects.equals(ssl, that.ssl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, driver, host, port, database, user, password, ssl);
    }

    @Override
    public String toString() {
        return "R2dbcConnectionConfiguration{" +
                "url='" + AppbaseUtil.OBFUSCATE + '\'' +
                ", driver='" + driver + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", database='" + database + '\'' +
                ", user='" + user + '\'' +
                ", password='" + AppbaseUtil.OBFUSCATE + '\'' +
                ", ssl=" + ssl +
                '}';
    }


    /**
     * Builder for {@link R2dbcConnectionConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @author Claude
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final String DEFAULT_DRIVER = "mariadb";
        private static final String DEFAULT_HOST = "mariadb.local";
        private static final int DEFAULT_PORT = 3306;
        private static final String DEFAULT_DATABASE = "appbase-test-mariadb-db";
        private static final String DEFAULT_USER = "root";
        private static final String DEFAULT_PASSWORD = "password";
        private static final boolean DEFAULT_SSL = false;


        @Nullable
        private String url;

        private String driver = DEFAULT_DRIVER;

        private String host = DEFAULT_HOST;

        private Integer port = DEFAULT_PORT;

        private String database = DEFAULT_DATABASE;

        private String user = DEFAULT_USER;

        private String password = DEFAULT_PASSWORD;

        private Boolean ssl = DEFAULT_SSL;


        private Builder() {
        }

        public Builder url(@Nullable final String url) {
            this.url = url;
            return this;
        }

        public Builder driver(final String driver) {
            this.driver = driver;
            return this;
        }

        public Builder host(final String host) {
            this.host = host;
            return this;
        }

        public Builder port(final Integer port) {
            this.port = port;
            return this;
        }

        public Builder database(final String database) {
            this.database = database;
            return this;
        }

        public Builder user(final String user) {
            this.user = user;
            return this;
        }

        public Builder password(final String password) {
            this.password = password;
            return this;
        }

        public Builder ssl(final Boolean ssl) {
            this.ssl = ssl;
            return this;
        }

        public R2dbcConnectionConfiguration build() {
            return new R2dbcConnectionConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
