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

package com.viveworks.appbase.service.supportmessage.store;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import org.junit.jupiter.api.Test;

/**
 * AUD-001 regression: the configured table name is concatenated into SQL identifiers, so it must be a
 * valid SQL identifier. These assertions fail against the pre-fix config (which enforced only
 * {@code notBlank}) and pass once the allowlist is added.
 *
 * @author Navaneeth Mysore G
 */
class JdbiSupportMessageStoreConfigurationTest {

    private static JdbiSupportMessageStoreConfiguration build(final String tableName) {
        return JdbiSupportMessageStoreConfiguration.builder().supportMessageTableName(tableName).build();
    }

    @Test
    void defaultTableNameIsAccepted() {
        assertDoesNotThrow(() -> JdbiSupportMessageStoreConfiguration.builder().build());
    }

    @Test
    void validIdentifiersAreAccepted() {
        assertEquals("appbase_SupportMessage",
                build("appbase_SupportMessage").getSupportMessageTableName());
        assertDoesNotThrow(() -> build("my_schema.my_table")); // optional schema qualifier
    }

    @Test
    void sqlInjectingIdentifierIsRejected() {
        assertThrows(AppbaseRuntimeException.class,
                () -> build("msg; DROP TABLE appbase_SupportMessage --"));
    }

    @Test
    void malformedIdentifiersAreRejected() {
        assertThrows(AppbaseRuntimeException.class, () -> build("support-message")); // hyphen
        assertThrows(AppbaseRuntimeException.class, () -> build("9messages"));        // leading digit
        assertThrows(AppbaseRuntimeException.class, () -> build("support message")); // space
    }
}
