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

package com.viveworks.appbase.client.service.druid.sql.client.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.viveworks.appbase.client.service.druid.sql.client.DruidResultSet;
import com.viveworks.appbase.client.service.druid.sql.client.DruidTransactionIsolation;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.net.TimeoutConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.sql.Connection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for {@link Connection}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = DruidConnectionConfiguration.Builder.class)
public final class DruidConnectionConfiguration {
    private final String url;

    private final String user;

    private final String password;

    private final Boolean sqlStringifyArrays;

    private final Boolean useApproximateCountDistinct;

    private final Boolean useGroupingSetForExactDistinct;

    private final Boolean useApproximateTopN;

    private final Boolean autoCommit;

    private final Boolean readOnly;

    private final String catalog;

    private final String schema;

    private final TimeoutConfiguration networkTimeoutConfiguration;

    private final DruidTransactionIsolation druidTransactionIsolation;

    @JsonSerialize(keyAs = String.class, contentAs = String.class)
    @JsonDeserialize(keyAs = String.class, contentAs = String.class)
    private final Map<String, String> clientInfo;

    private final DruidResultSet.ResultSetType defaultResultSetType;

    private final DruidResultSet.CursorHoldability defaultCursorHoldability;

    private final DruidResultSet.ConcurrencyType defaultConcurrencyType;

    private final DruidResultSet.FetchDirection defaultFetchDirection;

    private final String sqlQueryId;

    private final String sqlTimeZone;

    private final String authentication;

    private final String trustStore;

    private final String trustStorePassword;

    private final String keystore;

    private final String keystorePassword;

    private final String keyPassword;

    @JsonSerialize(keyAs = String.class, contentAs = String.class)
    @JsonDeserialize(keyAs = String.class, contentAs = String.class)
    private final Map<String, String> additionalProperties;



    private DruidConnectionConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notBlank(builder.url, "builder.url");

        url = builder.url;
        user = builder.user;
        password = builder.password;
        sqlStringifyArrays = builder.sqlStringifyArrays;
        useApproximateCountDistinct = builder.useApproximateCountDistinct;
        useGroupingSetForExactDistinct = builder.useGroupingSetForExactDistinct;
        useApproximateTopN = builder.useApproximateTopN;
        autoCommit = builder.autoCommit;
        readOnly = builder.readOnly;
        catalog = builder.catalog;
        schema = builder.schema;
        networkTimeoutConfiguration = builder.networkTimeoutConfiguration;
        druidTransactionIsolation = builder.druidTransactionIsolation;
        clientInfo = builder.clientInfo;
        defaultResultSetType = builder.defaultResultSetType;
        defaultCursorHoldability = builder.defaultCursorHoldability;
        defaultConcurrencyType = builder.defaultConcurrencyType;
        defaultFetchDirection = builder.defaultFetchDirection;
        sqlQueryId = builder.sqlQueryId;
        sqlTimeZone = builder.sqlTimeZone;
        authentication = builder.authentication;
        trustStore = builder.trustStore;
        trustStorePassword = builder.trustStorePassword;
        keystore = builder.keystore;
        keystorePassword = builder.keystorePassword;
        keyPassword = builder.keyPassword;
        additionalProperties = builder.additionalProperties;
    }


    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public String getUrl() {
        return url;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public Boolean getSqlStringifyArrays() {
        return sqlStringifyArrays;
    }

    public Boolean getUseApproximateCountDistinct() {
        return useApproximateCountDistinct;
    }

    public Boolean getUseGroupingSetForExactDistinct() {
        return useGroupingSetForExactDistinct;
    }

    public Boolean getUseApproximateTopN() {
        return useApproximateTopN;
    }

    public Boolean getAutoCommit() {
        return autoCommit;
    }

    public Boolean getReadOnly() {
        return readOnly;
    }

    public String getCatalog() {
        return catalog;
    }

    public String getSchema() {
        return schema;
    }

    public TimeoutConfiguration getNetworkTimeoutConfiguration() {
        return networkTimeoutConfiguration;
    }

    public DruidTransactionIsolation getDruidTransactionIsolation() {
        return druidTransactionIsolation;
    }

    public Map<String, String> getClientInfo() {
        return clientInfo;
    }

    public DruidResultSet.ResultSetType getDefaultResultSetType() {
        return defaultResultSetType;
    }

    public DruidResultSet.CursorHoldability getDefaultCursorHoldability() {
        return defaultCursorHoldability;
    }

    public DruidResultSet.ConcurrencyType getDefaultConcurrencyType() {
        return defaultConcurrencyType;
    }

    public DruidResultSet.FetchDirection getDefaultFetchDirection() {
        return defaultFetchDirection;
    }

    public String getSqlQueryId() {
        return sqlQueryId;
    }

    public String getSqlTimeZone() {
        return sqlTimeZone;
    }

    public String getAuthentication() {
        return authentication;
    }

    public String getTrustStore() {
        return trustStore;
    }

    public String getTrustStorePassword() {
        return trustStorePassword;
    }

    public String getKeystore() {
        return keystore;
    }

    public String getKeystorePassword() {
        return keystorePassword;
    }

    public String getKeyPassword() {
        return keyPassword;
    }

    public Map<String, String> getAdditionalProperties() {
        return additionalProperties;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final DruidConnectionConfiguration that = (DruidConnectionConfiguration) o;
        return Objects.equals(url, that.url) &&
                Objects.equals(user, that.user) &&
                Objects.equals(password, that.password) &&
                Objects.equals(sqlStringifyArrays, that.sqlStringifyArrays) &&
                Objects.equals(useApproximateCountDistinct, that.useApproximateCountDistinct) &&
                Objects.equals(useGroupingSetForExactDistinct, that.useGroupingSetForExactDistinct) &&
                Objects.equals(useApproximateTopN, that.useApproximateTopN) &&
                Objects.equals(autoCommit, that.autoCommit) &&
                Objects.equals(readOnly, that.readOnly) &&
                Objects.equals(catalog, that.catalog) &&
                Objects.equals(schema, that.schema) &&
                Objects.equals(networkTimeoutConfiguration, that.networkTimeoutConfiguration) &&
                druidTransactionIsolation == that.druidTransactionIsolation &&
                Objects.equals(clientInfo, that.clientInfo) &&
                defaultResultSetType == that.defaultResultSetType &&
                defaultCursorHoldability == that.defaultCursorHoldability &&
                defaultConcurrencyType == that.defaultConcurrencyType &&
                defaultFetchDirection == that.defaultFetchDirection &&
                Objects.equals(sqlQueryId, that.sqlQueryId) &&
                Objects.equals(sqlTimeZone, that.sqlTimeZone) &&
                Objects.equals(authentication, that.authentication) &&
                Objects.equals(trustStore, that.trustStore) &&
                Objects.equals(trustStorePassword, that.trustStorePassword) &&
                Objects.equals(keystore, that.keystore) &&
                Objects.equals(keystorePassword, that.keystorePassword) &&
                Objects.equals(keyPassword, that.keyPassword) &&
                Objects.equals(additionalProperties, that.additionalProperties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                url,
                user,
                password,
                sqlStringifyArrays,
                useApproximateCountDistinct,
                useGroupingSetForExactDistinct,
                useApproximateTopN,
                autoCommit,
                readOnly,
                catalog,
                schema,
                networkTimeoutConfiguration,
                druidTransactionIsolation,
                clientInfo,
                defaultResultSetType,
                defaultCursorHoldability,
                defaultConcurrencyType,
                defaultFetchDirection,
                sqlQueryId,
                sqlTimeZone,
                authentication,
                trustStore,
                trustStorePassword,
                keystore,
                keystorePassword,
                keyPassword,
                additionalProperties);
    }

    @Override
    public String toString() {
        // AUD-34/L4-06: url and authentication (may embed credentials; e.g. JDBC userinfo) and
        // clientInfo/additionalProperties (open-ended maps) are redacted alongside password,
        // matching the broader redaction PR internal work item 2558 established for this same class after review
        // found a single-field redaction insufficient.
        return "DruidConnectionConfiguration{" +
                "url='" + AppbaseUtil.OBFUSCATE + '\'' +
                ", user='" + user + '\'' +
                ", password='" + AppbaseUtil.OBFUSCATE + '\'' +
                ", sqlStringifyArrays=" + sqlStringifyArrays +
                ", useApproximateCountDistinct=" + useApproximateCountDistinct +
                ", useGroupingSetForExactDistinct=" + useGroupingSetForExactDistinct +
                ", useApproximateTopN=" + useApproximateTopN +
                ", autoCommit=" + autoCommit +
                ", readOnly=" + readOnly +
                ", catalog='" + catalog + '\'' +
                ", schema='" + schema + '\'' +
                ", networkTimeoutConfiguration=" + networkTimeoutConfiguration +
                ", druidTransactionIsolation=" + druidTransactionIsolation +
                ", clientInfo=" + AppbaseUtil.OBFUSCATE +
                ", defaultResultSetType=" + defaultResultSetType +
                ", defaultCursorHoldability=" + defaultCursorHoldability +
                ", defaultConcurrencyType=" + defaultConcurrencyType +
                ", defaultFetchDirection=" + defaultFetchDirection +
                ", sqlQueryId='" + sqlQueryId + '\'' +
                ", sqlTimeZone='" + sqlTimeZone + '\'' +
                ", account='" + AppbaseUtil.OBFUSCATE + '\'' +
                ", trustStore='" + trustStore + '\'' +
                ", trustStorePassword='" + AppbaseUtil.OBFUSCATE + '\'' +
                ", keystore='" + keystore + '\'' +
                ", keystorePassword='" + AppbaseUtil.OBFUSCATE + '\'' +
                ", keyPassword='" + AppbaseUtil.OBFUSCATE + '\'' +
                ", additionalProperties=" + AppbaseUtil.OBFUSCATE +
                '}';
    }


    /**
     * Builder for {@link DruidConnectionConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final String DEFAULT_URL =
                "jdbc:avatica:remote:url=http://localhost:8082/druid/v2/sql/avatica-protobuf/;serialization=protobuf";

        private static final Boolean DEFAULT_SQL_STRINGIFY_ARRAYS = true;

        private static final Boolean DEFAULT_USE_APPROXIMATE_COUNT_DISTINCT = true;

        private static final Boolean DEFAULT_USE_GROUPING_SET_FOR_EXACT_DISTINCT = false;

        private static final Boolean DEFAULT_USE_APPROXIMATE_TOP_N = true;

        private static final Boolean DEFAULT_AUTO_COMMIT = true;

        private static final Boolean DEFAULT_READONLY = true;

        private static final TimeoutConfiguration DEFAULT_NETWORK_TIMEOUT_CONFIGURATION = TimeoutConfiguration
                .builder()
                .enabled(true)
                .timeout(10)
                .timeoutTimeUnit(TimeUnit.SECONDS)
                .build();

        private static final DruidTransactionIsolation DEFAULT_DRUID_TRANSACTION_ISOLATION =
                DruidTransactionIsolation.TRANSACTION_REPEATABLE_READ;

        private static final DruidResultSet.ResultSetType DEFAULT_DEFAULT_RESULT_SET_TYPE =
                DruidResultSet.ResultSetType.FORWARD_ONLY;

        private static final DruidResultSet.CursorHoldability DEFAULT_DEFAULT_CURSOR_HOLDABILITY =
                DruidResultSet.CursorHoldability.CLOSE_CURSORS_AT_COMMIT;

        private static final DruidResultSet.ConcurrencyType DEFAULT_DEFAULT_CONCURRENCY_TYPE =
                DruidResultSet.ConcurrencyType.READ_ONLY;

        private static final DruidResultSet.FetchDirection DEFAULT_DEFAULT_FETCH_DIRECTION =
                DruidResultSet.FetchDirection.FETCH_FORWARD;

        private static final String DEFAULT_SQL_TIME_ZONE = "UTC";



        private String url = DEFAULT_URL;

        private String user;

        private String password;

        private Boolean sqlStringifyArrays = DEFAULT_SQL_STRINGIFY_ARRAYS;

        private Boolean useApproximateCountDistinct = DEFAULT_USE_APPROXIMATE_COUNT_DISTINCT;

        private Boolean useGroupingSetForExactDistinct = DEFAULT_USE_GROUPING_SET_FOR_EXACT_DISTINCT;

        private Boolean useApproximateTopN = DEFAULT_USE_APPROXIMATE_TOP_N;

        private Boolean autoCommit = DEFAULT_AUTO_COMMIT;

        private Boolean readOnly = DEFAULT_READONLY;

        private String catalog;

        private String schema;

        private TimeoutConfiguration networkTimeoutConfiguration = DEFAULT_NETWORK_TIMEOUT_CONFIGURATION;

        private DruidTransactionIsolation druidTransactionIsolation = DEFAULT_DRUID_TRANSACTION_ISOLATION;

        @JsonSerialize(keyAs = String.class, contentAs = String.class)
        @JsonDeserialize(keyAs = String.class, contentAs = String.class)
        private Map<String, String> clientInfo;

        private DruidResultSet.ResultSetType defaultResultSetType = DEFAULT_DEFAULT_RESULT_SET_TYPE;

        private DruidResultSet.CursorHoldability defaultCursorHoldability = DEFAULT_DEFAULT_CURSOR_HOLDABILITY;

        private DruidResultSet.ConcurrencyType defaultConcurrencyType = DEFAULT_DEFAULT_CONCURRENCY_TYPE;

        private DruidResultSet.FetchDirection defaultFetchDirection = DEFAULT_DEFAULT_FETCH_DIRECTION;

        private String sqlQueryId;

        private String sqlTimeZone = DEFAULT_SQL_TIME_ZONE;

        private String authentication;

        private String trustStore;

        private String trustStorePassword;

        private String keystore;

        private String keystorePassword;

        private String keyPassword;

        @JsonSerialize(keyAs = String.class, contentAs = String.class)
        @JsonDeserialize(keyAs = String.class, contentAs = String.class)
        private Map<String, String> additionalProperties;



        private Builder() {
        }

        public Builder url(final String url) {
            this.url = url;
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

        public Builder sqlTimeZone(final String sqlTimeZone) {
            this.sqlTimeZone = sqlTimeZone;
            return this;
        }

        public Builder sqlStringifyArrays(final Boolean sqlStringifyArrays) {
            this.sqlStringifyArrays = sqlStringifyArrays;
            return this;
        }

        public Builder useApproximateCountDistinct(final Boolean useApproximateCountDistinct) {
            this.useApproximateCountDistinct = useApproximateCountDistinct;
            return this;
        }

        public Builder useGroupingSetForExactDistinct(final Boolean useGroupingSetForExactDistinct) {
            this.useGroupingSetForExactDistinct = useGroupingSetForExactDistinct;
            return this;
        }

        public Builder useApproximateTopN(final Boolean useApproximateTopN) {
            this.useApproximateTopN = useApproximateTopN;
            return this;
        }

        public Builder autoCommit(final Boolean autoCommit) {
            this.autoCommit = autoCommit;
            return this;
        }

        public Builder readOnly(final Boolean readOnly) {
            this.readOnly = readOnly;
            return this;
        }

        public Builder catalog(final String catalog) {
            this.catalog = catalog;
            return this;
        }

        public Builder schema(final String schema) {
            this.schema = schema;
            return this;
        }

        public Builder networkTimeoutConfiguration(final TimeoutConfiguration networkTimeoutConfiguration) {
            this.networkTimeoutConfiguration = networkTimeoutConfiguration;
            return this;
        }

        public Builder druidTransactionIsolation(final DruidTransactionIsolation druidTransactionIsolation) {
            this.druidTransactionIsolation = druidTransactionIsolation;
            return this;
        }

        public Builder clientInfo(
                @JsonSerialize(keyAs = String.class, contentAs = String.class)
                @JsonDeserialize(keyAs = String.class, contentAs = String.class)
                final Map<String, String> clientInfo)
        {
            this.clientInfo = clientInfo;
            return this;
        }

        public Builder defaultResultSetType(final DruidResultSet.ResultSetType defaultResultSetType) {
            this.defaultResultSetType = defaultResultSetType;
            return this;
        }

        public Builder defaultCursorHoldability(final DruidResultSet.CursorHoldability defaultCursorHoldability) {
            this.defaultCursorHoldability = defaultCursorHoldability;
            return this;
        }

        public Builder defaultConcurrencyType(final DruidResultSet.ConcurrencyType defaultConcurrencyType) {
            this.defaultConcurrencyType = defaultConcurrencyType;
            return this;
        }

        public Builder defaultFetchDirection(final DruidResultSet.FetchDirection defaultFetchDirection) {
            this.defaultFetchDirection = defaultFetchDirection;
            return this;
        }

        public Builder sqlQueryId(final String sqlQueryId) {
            this.sqlQueryId = sqlQueryId;
            return this;
        }

        public Builder authentication(final String authentication) {
            this.authentication = authentication;
            return this;
        }

        public Builder trustStore(final String trustStore) {
            this.trustStore = trustStore;
            return this;
        }

        public Builder trustStorePassword(final String trustStorePassword) {
            this.trustStorePassword = trustStorePassword;
            return this;
        }

        public Builder keystore(final String keystore) {
            this.keystore = keystore;
            return this;
        }

        public Builder keystorePassword(final String keystorePassword) {
            this.keystorePassword = keystorePassword;
            return this;
        }

        public Builder keyPassword(final String keyPassword) {
            this.keyPassword = keyPassword;
            return this;
        }

        public Builder additionalProperties(
                @JsonSerialize(keyAs = String.class, contentAs = String.class)
                @JsonDeserialize(keyAs = String.class, contentAs = String.class)
                final Map<String, String> additionalProperties)
        {
            this.additionalProperties = additionalProperties;
            return this;
        }

        public DruidConnectionConfiguration build() {
            return new DruidConnectionConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
