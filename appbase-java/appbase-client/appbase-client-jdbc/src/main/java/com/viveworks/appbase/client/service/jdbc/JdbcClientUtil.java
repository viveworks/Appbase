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

package com.viveworks.appbase.client.service.jdbc;

import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValue;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValueConfiguration;
import com.viveworks.appbase.client.service.jdbc.configuration.DefaultJdbcClientIntegrationServiceConfiguration;
import com.viveworks.appbase.client.service.jdbc.configuration.HikariDataSourceConfiguration;
import com.viveworks.appbase.client.service.jdbc.configuration.JdbcUrlConfiguration;
import com.viveworks.appbase.client.util.AppbaseClientUtil;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.net.HostPortConfiguration;

import javax.sql.DataSource;
import java.util.Map;

/**
 * Utilities for this module.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public final class JdbcClientUtil {
    public static final String USER = "user";
    public static final String PASSWORD = "password";

    private static final String JDBC_URL_PREFIX = "jdbc:";


    private JdbcClientUtil() {
    }


    /**
     * Convert {@link JdbcUrlConfiguration} to {@code JDBC URL} of the form:
     * {@code jdbc://host:port/databaseName?user=username&password=secret&propertyKey1=propertyValue1}.
     *
     * @param jdbcUrlConfiguration {@link JdbcUrlConfiguration}
     * @param includeProperties If properties must be included in the {@code JDBC URL}
     * @return {@code JDBC URL}
     */
    public static String jdbcUrl(
            final JdbcUrlConfiguration jdbcUrlConfiguration,
            final boolean includeProperties)
    {
        AppbaseUtil.notNull(jdbcUrlConfiguration, "jdbcUrlConfiguration");

        final StringBuilder jdbcUrl = new StringBuilder();


        final String rdbmsName = jdbcUrlConfiguration.getRdbmsName();
        jdbcUrl.append(JDBC_URL_PREFIX)
                .append(rdbmsName)
                .append(AppbaseUtil.COLON)
                .append(AppbaseUtil.FORWARD_SLASH)
                .append(AppbaseUtil.FORWARD_SLASH);

        final HostPortConfiguration hostPortConfiguration = jdbcUrlConfiguration.getHostPortConfiguration();
        final String host = hostPortConfiguration.getHost();
        final int port = hostPortConfiguration.getPort();
        jdbcUrl.append(host)
                .append(AppbaseUtil.COLON)
                .append(port);

        boolean hasUser = false;
        boolean hasPassword = false;

        final String databaseName = jdbcUrlConfiguration.getDatabaseName();
        if (databaseName != null) {
            jdbcUrl.append(AppbaseUtil.FORWARD_SLASH)
                    .append(databaseName);
        }

        if (includeProperties) {
            final String user = jdbcUrlConfiguration.getUser();
            if (user != null) {
                hasUser = true;
                jdbcUrl.append(AppbaseUtil.QUESTION_MARK)
                        .append(USER)
                        .append(AppbaseUtil.EQUALS)
                        .append(user);
            }

            final String password = jdbcUrlConfiguration.getPassword();
            if (password != null) {
                if (hasUser) {
                    jdbcUrl.append(AppbaseUtil.AMPERSAND);
                } else {
                    jdbcUrl.append(AppbaseUtil.QUESTION_MARK);
                }

                hasPassword = true;
                jdbcUrl.append(PASSWORD)
                        .append(AppbaseUtil.EQUALS)
                        .append(password);
            }

            final Map<String, String> additionalProperties = jdbcUrlConfiguration.getAdditionalProperties();
            if (additionalProperties != null && !additionalProperties.isEmpty()) {
                if (hasUser || hasPassword) {
                    jdbcUrl.append(AppbaseUtil.AMPERSAND);
                } else {
                    jdbcUrl.append(AppbaseUtil.QUESTION_MARK);
                }

                boolean first = true;
                for (final Map.Entry<String, String> entry : additionalProperties.entrySet()) {
                    final String key = entry.getKey();
                    AppbaseUtil.notNull(key, "key");

                    final String value = entry.getValue();
                    AppbaseUtil.notNull(value, "value");

                    if (!first) {
                        jdbcUrl.append(AppbaseUtil.AMPERSAND);
                    }

                    jdbcUrl.append(key)
                            .append(AppbaseUtil.EQUALS)
                            .append(value);
                    first = false;
                }
            }
        }

        return jdbcUrl.toString();
    }

    /**
     * Generate {@link AppbaseClientKey} used by {@link JdbcClientService}.
     *
     * @param defaultJdbcClientIntegrationServiceConfiguration
     * {@link DefaultJdbcClientIntegrationServiceConfiguration}
     * @return {@link AppbaseClientKey}
     */
    public static AppbaseClientKey<TextValue> appbaseClientKey(
            final DefaultJdbcClientIntegrationServiceConfiguration defaultJdbcClientIntegrationServiceConfiguration)
    {
        AppbaseUtil.notNull(
                defaultJdbcClientIntegrationServiceConfiguration, "defaultJdbcClientIntegrationServiceConfiguration");

        final TextValueConfiguration textValueKeyConfiguration =
                defaultJdbcClientIntegrationServiceConfiguration.getTextValueKeyConfiguration();
        if (textValueKeyConfiguration != null) {
            return AppbaseClientUtil.textValueKey(textValueKeyConfiguration);
        }

        final HikariDataSourceConfiguration hikariDataSourceConfiguration =
                defaultJdbcClientIntegrationServiceConfiguration.getHikariDataSourceConfiguration();
        final Class<? extends DataSource> dataSourceType =
                hikariDataSourceConfiguration.getDataSourceType();
        final HostPortConfiguration serverHostPortConfiguration =
                hikariDataSourceConfiguration.getServerHostPortConfiguration();
        final String databaseName = hikariDataSourceConfiguration.getDatabaseName();
        final String key = dataSourceType + AppbaseUtil.AT +
                serverHostPortConfiguration.getHost() + AppbaseUtil.COLON +
                serverHostPortConfiguration.getPort() + AppbaseUtil.FORWARD_SLASH +
                databaseName;
        final TextValue textValue = new TextValue();
        textValue.init(key);

        return AppbaseClientUtil.textValueKey(textValue);
    }
}
