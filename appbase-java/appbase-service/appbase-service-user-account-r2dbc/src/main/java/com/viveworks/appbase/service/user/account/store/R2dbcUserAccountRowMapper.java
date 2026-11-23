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

package com.viveworks.appbase.service.user.account.store;

import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.password.PasswordService;
import com.viveworks.appbase.service.user.account.data.UserAccount;

import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;

import java.time.LocalDateTime;

/**
 * Hand-written {@code appbase_UserAccount} row mapper for the R2DBC reactive twin. R2DBC has no
 * {@code BeanMapper} equivalent to JDBI's -- {@link io.r2dbc.spi.Row} column access is only valid
 * inside {@link io.r2dbc.spi.Result#map(java.util.function.BiFunction)}, so mapping is done here by
 * hand, column by column, matching {@code JdbiUserAccountStore}'s {@code BeanMapper.of(UserAccount.
 * class)} field-for-field.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public final class R2dbcUserAccountRowMapper {
    private final PasswordService passwordService;

    public R2dbcUserAccountRowMapper(final PasswordService passwordService) {
        this.passwordService = AppbaseUtil.notNull(passwordService, "passwordService");
    }

    /**
     * Maps one {@code appbase_UserAccount} row to a {@link UserAccount}. Must run INSIDE {@link
     * io.r2dbc.spi.Result#map(java.util.function.BiFunction)} -- {@code row}'s column accessors are
     * not valid once that callback returns.
     *
     * @param row R2DBC row, valid only for the duration of this call
     * @param meta row metadata (unused; columns are addressed by name)
     * @return mapped {@link UserAccount}
     */
    public UserAccount map(final Row row, final RowMetadata meta) {
        final UserAccount userAccount = new UserAccount();
        userAccount.setAppbaseUserId(row.get("appbaseUserId", String.class));
        userAccount.setIdpUserId(row.get("idpUserId", String.class));
        userAccount.setEmail(row.get("email", String.class));
        userAccount.setName(row.get("name", String.class));
        final String passwordHashes = row.get("passwordHashes", String.class);
        userAccount.setPasswordHashes(
                passwordHashes == null ? null : passwordService.deserializePasswordHashes(passwordHashes));
        userAccount.setPasswordResetEmailSent(row.get("passwordResetEmailSent", Boolean.class));
        final String passwordResetCodeHash = row.get("passwordResetCodeHash", String.class);
        userAccount.setPasswordResetCodeHash(
                passwordResetCodeHash == null ? null : passwordService.deserializePasswordHash(passwordResetCodeHash));
        userAccount.setUserTopic(row.get("userTopic", String.class));
        userAccount.setUserHomeDataCenter(row.get("userHomeDataCenter", String.class));
        userAccount.setEmailVerified(row.get("emailVerified", Boolean.class));
        userAccount.setLanguageCode(row.get("languageCode", String.class));
        userAccount.setAddressLine1(row.get("addressLine1", String.class));
        userAccount.setAddressLine2(row.get("addressLine2", String.class));
        userAccount.setCity(row.get("city", String.class));
        userAccount.setStateProvince(row.get("stateProvince", String.class));
        userAccount.setPostalCode(row.get("postalCode", String.class));
        userAccount.setCountry(row.get("country", String.class));
        userAccount.setLatitude(row.get("latitude", String.class));
        userAccount.setLongitude(row.get("longitude", String.class));
        userAccount.setDeleted(row.get("deleted", Boolean.class));
        userAccount.setCreationTimestamp(row.get("creationTimestamp", LocalDateTime.class));
        userAccount.setLastAccessTimestamp(row.get("lastAccessTimestamp", LocalDateTime.class));
        return userAccount;
    }
}
