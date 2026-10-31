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

package com.viveworks.appbase.service.user.account.store;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Configuration for {@link R2dbcReactiveUserAccountStore}. A separate type from the blocking
 * twin's {@code JdbiUserAccountStoreConfiguration} (different module) rather than a shared one --
 * {@code appbase-service-user-account-r2dbc} deliberately does not depend on {@code
 * appbase-service-user-account-jdbi} (module-per-backing, see the class Javadoc on {@link
 * R2dbcReactiveUserAccountStore}) -- but the table name, default, and {@link #SQL_IDENTIFIER} guard
 * are kept identical so the two twins stay pointed at the same table by construction.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = R2dbcReactiveUserAccountStoreConfiguration.Builder.class)
public class R2dbcReactiveUserAccountStoreConfiguration implements AppbaseConfiguration {
    private final String userAccountTableName;

    /**
     * A SQL identifier: a leading letter/underscore then letters/digits/underscores, with an optional
     * schema qualifier ({@code schema.table}). Guards the configured table name that is concatenated
     * into SQL (AUD-001) -- R2DBC cannot bind identifiers as parameters, so a malformed/templated
     * config value could alter the query. It is trusted operator config today (not a request-input
     * path); this allowlist makes that guarantee explicit and fails fast at startup.
     */
    private static final Pattern SQL_IDENTIFIER =
            Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)?$");

    private R2dbcReactiveUserAccountStoreConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notBlank(builder.userAccountTableName, "builder.userAccountTableName");
        if (!SQL_IDENTIFIER.matcher(builder.userAccountTableName).matches()) {
            throw new AppbaseRuntimeException(
                    "builder.userAccountTableName is not a valid SQL identifier: "
                            + builder.userAccountTableName);
        }

        userAccountTableName = builder.userAccountTableName;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getUserAccountTableName() {
        return userAccountTableName;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final R2dbcReactiveUserAccountStoreConfiguration that = (R2dbcReactiveUserAccountStoreConfiguration) o;
        return Objects.equals(userAccountTableName, that.userAccountTableName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userAccountTableName);
    }

    @Override
    public String toString() {
        return "R2dbcReactiveUserAccountStoreConfiguration{" +
                "userAccountTableName='" + userAccountTableName + '\'' +
                '}';
    }

    /**
     * Builder for {@link R2dbcReactiveUserAccountStoreConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @author Claude
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final String DEFAULT_TABLE_NAME = "appbase_UserAccount";

        private String userAccountTableName = DEFAULT_TABLE_NAME;

        private Builder() {
        }

        public Builder userAccountTableName(final String userAccountTableName) {
            this.userAccountTableName = userAccountTableName;
            return this;
        }

        public R2dbcReactiveUserAccountStoreConfiguration build() {
            return new R2dbcReactiveUserAccountStoreConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
