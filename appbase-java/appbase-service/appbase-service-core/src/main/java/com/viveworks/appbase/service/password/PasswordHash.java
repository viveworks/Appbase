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

package com.viveworks.appbase.service.password;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
@PasswordHash.Valid
@JsonDeserialize(builder = PasswordHash.Builder.class)
public class PasswordHash {
    @NotNull
    private final HashAlgorithm hashAlgorithm;

    @NotBlank
    private final String hash;

    @NotBlank
    private final String salt;

    @Positive
    private final int shaRounds;

    private PasswordHash(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");

        hashAlgorithm = builder.hashAlgorithm;
        hash = builder.hash;
        salt = builder.salt;
        shaRounds = builder.shaRounds;
    }

    public static Builder builder() {
        return new Builder();
    }

    public HashAlgorithm getHashAlgorithm() {
        return hashAlgorithm;
    }

    public String getHash() {
        return hash;
    }

    public String getSalt() {
        return salt;
    }

    public int getShaRounds() {
        return shaRounds;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final PasswordHash that = (PasswordHash) o;
        return shaRounds == that.shaRounds &&
                hashAlgorithm == that.hashAlgorithm &&
                Objects.equals(hash, that.hash) &&
                Objects.equals(salt, that.salt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hashAlgorithm, hash, salt, shaRounds);
    }

    @Override
    public String toString() {
        return "PasswordHash{" +
                "hashAlgorithm=" + hashAlgorithm +
                ", hash='" + AppbaseUtil.OBFUSCATE + '\'' +
                ", salt='" + AppbaseUtil.OBFUSCATE + '\'' +
                ", shaRounds=" + shaRounds +
                '}';
    }

    /**
     * Builder for {@link PasswordHash}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private HashAlgorithm hashAlgorithm;
        private String hash;
        private String salt;
        private int shaRounds;

        private Builder() {
        }

        public Builder hashAlgorithm(final HashAlgorithm hashAlgorithm) {
            this.hashAlgorithm = hashAlgorithm;
            return this;
        }

        public Builder hash(final String hash) {
            this.hash = hash;
            return this;
        }

        public Builder salt(final String salt) {
            this.salt = salt;
            return this;
        }

        public Builder shaRounds(final int shaRounds) {
            this.shaRounds = shaRounds;
            return this;
        }

        public PasswordHash build() {
            return new PasswordHash(this);
        }
    }
    // CheckStyle: Builder end

    @Target({ TYPE, METHOD, FIELD, PARAMETER })
    @Retention(RUNTIME)
    @Constraint(validatedBy = PasswordHash.Validator.class)
    public @interface Valid {
        String message() default "com.viveworks.appbase.service.user.account.data.PasswordHash";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    public static class Validator implements ConstraintValidator<PasswordHash.Valid, PasswordHash> {
        @Override
        public boolean isValid(final PasswordHash value, final ConstraintValidatorContext context) {
            return AppbaseUtil.isNotNull(value) &&
                    AppbaseUtil.isNotBlank(value.getHash()) &&
                    AppbaseUtil.isNotBlank(value.getSalt()) &&
                    AppbaseUtil.isNotNull(value.getHashAlgorithm()) &&
                    AppbaseUtil.isPositive(value.getShaRounds());
        }
    }

    public static class PasswordHashColumnMapper implements ColumnMapper<PasswordHash> {
        @Override
        public PasswordHash map(
                final ResultSet rs,
                final int columnNumber,
                final StatementContext ctx) throws SQLException
        {
            final String string = rs.getString(columnNumber);
            return passwordHash(string);
        }

        @Override
        public PasswordHash map(
                final ResultSet rs,
                final String columnLabel,
                final StatementContext ctx) throws SQLException
        {
            final String string = rs.getString(columnLabel);
            return passwordHash(string);
        }

        @Nullable
        private PasswordHash passwordHash(final String str) {
            if (str == null) {
                return null;
            }

            return PasswordService.deserializePasswordHashImpl(str);
        }
    }
}
