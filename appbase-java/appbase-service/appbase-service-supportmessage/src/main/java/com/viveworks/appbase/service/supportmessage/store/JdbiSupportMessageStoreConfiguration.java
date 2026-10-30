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
package com.viveworks.appbase.service.supportmessage.store;

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
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = JdbiSupportMessageStoreConfiguration.Builder.class)
public class JdbiSupportMessageStoreConfiguration implements AppbaseConfiguration {
    private final String supportMessageTableName;

    private final boolean manualValidation;

    /**
     * A SQL identifier: a leading letter/underscore then letters/digits/underscores, with an optional
     * schema qualifier ({@code schema.table}). Guards the configured table name that is concatenated
     * into SQL (AUD-001) — JDBI cannot bind identifiers as parameters, so a malformed/templated config
     * value could alter the query. It is trusted operator config today (not a request-input path); this
     * allowlist makes that guarantee explicit and fails fast at startup.
     */
    private static final Pattern SQL_IDENTIFIER =
            Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)?$");

    private JdbiSupportMessageStoreConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notBlank(builder.supportMessageTableName, "builder.supportMessageTableName");
        if (!SQL_IDENTIFIER.matcher(builder.supportMessageTableName).matches()) {
            throw new AppbaseRuntimeException(
                    "builder.supportMessageTableName is not a valid SQL identifier: "
                            + builder.supportMessageTableName);
        }

        supportMessageTableName = builder.supportMessageTableName;
        manualValidation = builder.manualValidation;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getSupportMessageTableName() {
        return supportMessageTableName;
    }

    public boolean isManualValidation() {
        return manualValidation;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final JdbiSupportMessageStoreConfiguration that = (JdbiSupportMessageStoreConfiguration) o;
        return manualValidation == that.manualValidation &&
                Objects.equals(supportMessageTableName, that.supportMessageTableName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(supportMessageTableName, manualValidation);
    }

    @Override
    public String toString() {
        return "JdbiSupportMessageStoreConfiguration{" +
                "supportMessageTableName='" + supportMessageTableName + '\'' +
                ", manualValidation=" + manualValidation +
                '}';
    }


    /**
     * Builder for {@link JdbiSupportMessageStoreConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        public static final String DEFAULT_SUPPORT_MESSAGE_TABLE_NAME = "appbase_SupportMessage";

        private String supportMessageTableName = DEFAULT_SUPPORT_MESSAGE_TABLE_NAME;
        private boolean manualValidation;

        private Builder() {
        }

        public Builder supportMessageTableName(final String supportMessageTableName) {
            this.supportMessageTableName = supportMessageTableName;
            return this;
        }

        public Builder manualValidation(final boolean manualValidation) {
            this.manualValidation = manualValidation;
            return this;
        }

        public JdbiSupportMessageStoreConfiguration build() {
            return new JdbiSupportMessageStoreConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
