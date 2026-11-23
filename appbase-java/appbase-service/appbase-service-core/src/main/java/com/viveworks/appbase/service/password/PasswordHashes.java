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

package com.viveworks.appbase.service.password;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@PasswordHashes.Valid
@JsonDeserialize(builder = PasswordHashes.Builder.class)
public class PasswordHashes {
    private final PasswordHash clientPasswordHash;

    /**
     * Server password hash is not sent to the client or cached in session store.
     * If it is needed, read it directly from the database to compare.
     */
    @JsonIgnore
    private final PasswordHash serverPasswordHash;

    private PasswordHashes(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");

        if (AppbaseUtil.isNull(builder.clientPasswordHash) && AppbaseUtil.isNull(builder.serverPasswordHash)) {
            throw new AppbaseRuntimeException("Either client or server password hashes must not be null");
        }

        clientPasswordHash = builder.clientPasswordHash;
        serverPasswordHash = builder.serverPasswordHash;
    }

    public static Builder builder() {
        return new Builder();
    }

    public PasswordHash getClientPasswordHash() {
        return clientPasswordHash;
    }

    @JsonIgnore
    public PasswordHash getServerPasswordHash() {
        return serverPasswordHash;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final PasswordHashes that = (PasswordHashes) o;
        return Objects.equals(clientPasswordHash, that.clientPasswordHash) &&
                Objects.equals(serverPasswordHash, that.serverPasswordHash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clientPasswordHash, serverPasswordHash);
    }

    @Override
    public String toString() {
        return "PasswordHashes{" +
                "clientPasswordHash=" + AppbaseUtil.OBFUSCATE +
                ", serverPasswordHash=" + AppbaseUtil.OBFUSCATE +
                '}';
    }

    /**
     * Builder for {@link PasswordHashes}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private PasswordHash clientPasswordHash;

        @JsonIgnore
        private PasswordHash serverPasswordHash;

        private Builder() {
        }

        public Builder clientPasswordHash(final PasswordHash clientPasswordHash) {
            this.clientPasswordHash = clientPasswordHash;
            return this;
        }

        @JsonIgnore
        public Builder serverPasswordHash(final PasswordHash serverPasswordHash) {
            this.serverPasswordHash = serverPasswordHash;
            return this;
        }

        public PasswordHashes build() {
            return new PasswordHashes(this);
        }
    }
    // CheckStyle: Builder end

    @Target({ TYPE, METHOD, FIELD, PARAMETER })
    @Retention(RUNTIME)
    @Constraint(validatedBy = PasswordHashes.Validator.class)
    public @interface Valid {
        String message() default "com.viveworks.appbase.service.user.account.data.PasswordHashes";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    public static class Validator implements ConstraintValidator<PasswordHashes.Valid, PasswordHashes> {
        @Override
        public boolean isValid(final PasswordHashes value, final ConstraintValidatorContext context) {
            return AppbaseUtil.isNotNull(value) &&
                    (AppbaseUtil.isValid(value.getClientPasswordHash(), new PasswordHash.Validator(), context) ||
                            AppbaseUtil.isValid(value.getServerPasswordHash(), new PasswordHash.Validator(), context));
        }
    }

    public static class PasswordHashesColumnMapper implements ColumnMapper<PasswordHashes> {
        @Override
        public PasswordHashes map(
                final ResultSet rs,
                final int columnNumber,
                final StatementContext ctx) throws SQLException
        {
            final String string = rs.getString(columnNumber);
            return passwordHashes(string);
        }

        @Override
        public PasswordHashes map(
                final ResultSet rs,
                final String columnLabel,
                final StatementContext ctx) throws SQLException
        {
            final String string = rs.getString(columnLabel);
            return passwordHashes(string);
        }

        @Nullable
        private PasswordHashes passwordHashes(final String str) {
            if (str == null) {
                return null;
            }

            return PasswordService.deserializePasswordHashesImpl(str);
        }
    }
}
