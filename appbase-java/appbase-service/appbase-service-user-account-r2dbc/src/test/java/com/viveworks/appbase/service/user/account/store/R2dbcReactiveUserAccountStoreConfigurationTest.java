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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import org.junit.jupiter.api.Test;

/**
 * AUD-001 regression for the R2DBC reactive twin's config: the configured table name is
 * concatenated into SQL identifiers (R2DBC cannot bind identifiers as parameters), so it must be
 * a valid SQL identifier. Mirrors {@code JdbiUserAccountStoreConfigurationTest} (blocking twin,
 * same {@code SQL_IDENTIFIER} pattern copied verbatim) so both twins carry the same proof.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class R2dbcReactiveUserAccountStoreConfigurationTest {

    private static R2dbcReactiveUserAccountStoreConfiguration build(final String tableName) {
        return R2dbcReactiveUserAccountStoreConfiguration.builder().userAccountTableName(tableName).build();
    }

    @Test
    void defaultTableNameIsAccepted() {
        assertDoesNotThrow(() -> R2dbcReactiveUserAccountStoreConfiguration.builder().build());
        assertEquals("appbase_UserAccount",
                R2dbcReactiveUserAccountStoreConfiguration.builder().build().getUserAccountTableName());
    }

    @Test
    void validIdentifiersAreAccepted() {
        assertEquals("appbase_UserAccount", build("appbase_UserAccount").getUserAccountTableName());
        assertDoesNotThrow(() -> build("_private"));
        assertEquals("my_schema.my_table", build("my_schema.my_table").getUserAccountTableName()); // optional schema qualifier
    }

    @Test
    void sqlInjectingIdentifierIsRejected() {
        assertThrows(AppbaseRuntimeException.class,
                () -> build("bad; DROP TABLE x--"));
    }

    @Test
    void malformedIdentifiersAreRejected() {
        assertThrows(AppbaseRuntimeException.class, () -> build("user-account")); // hyphen
        assertThrows(AppbaseRuntimeException.class, () -> build("1users"));        // leading digit
        assertThrows(AppbaseRuntimeException.class, () -> build("user account"));  // space
        assertThrows(AppbaseRuntimeException.class, () -> build("a.b.c"));          // too many parts
    }
}
