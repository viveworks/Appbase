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

package com.viveworks.appbase.client.service.r2dbc;

import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValue;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValueConfiguration;
import com.viveworks.appbase.client.service.r2dbc.configuration.DefaultR2dbcClientIntegrationServiceConfiguration;
import com.viveworks.appbase.client.service.r2dbc.configuration.R2dbcConnectionConfiguration;
import com.viveworks.appbase.client.service.r2dbc.configuration.R2dbcPoolConfiguration;
import com.viveworks.appbase.client.util.AppbaseClientUtil;
import com.viveworks.appbase.core.util.AppbaseUtil;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;

import java.time.Duration;

/**
 * Utilities for this module.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public final class R2dbcClientUtil {
    private R2dbcClientUtil() {
    }

    /**
     * Build {@link ConnectionFactoryOptions} from a {@link R2dbcConnectionConfiguration}.
     *
     * <p>If {@link R2dbcConnectionConfiguration#getUrl()} is present it is parsed directly via
     * {@link ConnectionFactoryOptions#parse(CharSequence)} and always wins; otherwise the discrete
     * driver/host/port/database/user/password/ssl options are assembled field-by-field.
     *
     * @param r2dbcConnectionConfiguration {@link R2dbcConnectionConfiguration}
     * @return {@link ConnectionFactoryOptions}
     */
    public static ConnectionFactoryOptions options(final R2dbcConnectionConfiguration r2dbcConnectionConfiguration) {
        AppbaseUtil.notNull(r2dbcConnectionConfiguration, "r2dbcConnectionConfiguration");

        final String url = r2dbcConnectionConfiguration.getUrl();
        if (url != null) {
            return ConnectionFactoryOptions.parse(url);
        }

        final ConnectionFactoryOptions.Builder builder = ConnectionFactoryOptions.builder()
                .option(ConnectionFactoryOptions.DRIVER, r2dbcConnectionConfiguration.getDriver())
                .option(ConnectionFactoryOptions.HOST, r2dbcConnectionConfiguration.getHost());

        final Integer port = r2dbcConnectionConfiguration.getPort();
        if (port != null) {
            builder.option(ConnectionFactoryOptions.PORT, port);
        }

        final String database = r2dbcConnectionConfiguration.getDatabase();
        if (database != null) {
            builder.option(ConnectionFactoryOptions.DATABASE, database);
        }

        final String user = r2dbcConnectionConfiguration.getUser();
        if (user != null) {
            builder.option(ConnectionFactoryOptions.USER, user);
        }

        final String password = r2dbcConnectionConfiguration.getPassword();
        if (password != null) {
            builder.option(ConnectionFactoryOptions.PASSWORD, password);
        }

        final Boolean ssl = r2dbcConnectionConfiguration.getSsl();
        if (ssl != null) {
            builder.option(ConnectionFactoryOptions.SSL, ssl);
        }

        return builder.build();
    }

    /**
     * Build a native {@link ConnectionPool} wrapping a {@link ConnectionFactory} resolved from
     * {@link #options(R2dbcConnectionConfiguration)} via the R2DBC SPI's
     * {@link ConnectionFactories#get(ConnectionFactoryOptions)} service-loader lookup.
     *
     * @param r2dbcConnectionConfiguration {@link R2dbcConnectionConfiguration}
     * @param r2dbcPoolConfiguration {@link R2dbcPoolConfiguration}
     * @return {@link ConnectionPool}
     */
    public static ConnectionPool connectionPool(
            final R2dbcConnectionConfiguration r2dbcConnectionConfiguration,
            final R2dbcPoolConfiguration r2dbcPoolConfiguration)
    {
        AppbaseUtil.notNull(r2dbcConnectionConfiguration, "r2dbcConnectionConfiguration");
        AppbaseUtil.notNull(r2dbcPoolConfiguration, "r2dbcPoolConfiguration");

        final ConnectionFactoryOptions options = options(r2dbcConnectionConfiguration);
        final ConnectionFactory connectionFactory = ConnectionFactories.get(options);

        final ConnectionPoolConfiguration.Builder poolConfigurationBuilder =
                ConnectionPoolConfiguration.builder(connectionFactory);

        final Integer maxSize = r2dbcPoolConfiguration.getMaxSize();
        if (maxSize != null) {
            poolConfigurationBuilder.maxSize(maxSize);
        }

        final Integer initialSize = r2dbcPoolConfiguration.getInitialSize();
        if (initialSize != null) {
            poolConfigurationBuilder.initialSize(initialSize);
        }

        final Long maxIdleTimeSeconds = r2dbcPoolConfiguration.getMaxIdleTimeSeconds();
        if (maxIdleTimeSeconds != null) {
            poolConfigurationBuilder.maxIdleTime(Duration.ofSeconds(maxIdleTimeSeconds));
        }

        final Long maxAcquireTimeSeconds = r2dbcPoolConfiguration.getMaxAcquireTimeSeconds();
        if (maxAcquireTimeSeconds != null) {
            poolConfigurationBuilder.maxAcquireTime(Duration.ofSeconds(maxAcquireTimeSeconds));
        }

        final String validationQuery = r2dbcPoolConfiguration.getValidationQuery();
        if (validationQuery != null) {
            poolConfigurationBuilder.validationQuery(validationQuery);
        }

        return new ConnectionPool(poolConfigurationBuilder.build());
    }

    /**
     * Generate {@link AppbaseClientKey} used by {@link R2dbcClientService}.
     *
     * @param defaultR2dbcClientIntegrationServiceConfiguration
     * {@link DefaultR2dbcClientIntegrationServiceConfiguration}
     * @return {@link AppbaseClientKey}
     */
    public static AppbaseClientKey<TextValue> appbaseClientKey(
            final DefaultR2dbcClientIntegrationServiceConfiguration defaultR2dbcClientIntegrationServiceConfiguration)
    {
        AppbaseUtil.notNull(
                defaultR2dbcClientIntegrationServiceConfiguration, "defaultR2dbcClientIntegrationServiceConfiguration");

        final TextValueConfiguration textValueKeyConfiguration =
                defaultR2dbcClientIntegrationServiceConfiguration.getTextValueKeyConfiguration();
        if (textValueKeyConfiguration != null) {
            return AppbaseClientUtil.textValueKey(textValueKeyConfiguration);
        }

        final R2dbcConnectionConfiguration r2dbcConnectionConfiguration =
                defaultR2dbcClientIntegrationServiceConfiguration.getR2dbcConnectionConfiguration();
        final String url = r2dbcConnectionConfiguration.getUrl();
        final String key;
        if (url != null) {
            key = url;
        } else {
            key = r2dbcConnectionConfiguration.getDriver() + AppbaseUtil.AT +
                    r2dbcConnectionConfiguration.getHost() + AppbaseUtil.COLON +
                    r2dbcConnectionConfiguration.getPort() + AppbaseUtil.FORWARD_SLASH +
                    r2dbcConnectionConfiguration.getDatabase();
        }

        final TextValue textValue = new TextValue();
        textValue.init(key);

        return AppbaseClientUtil.textValueKey(textValue);
    }
}
