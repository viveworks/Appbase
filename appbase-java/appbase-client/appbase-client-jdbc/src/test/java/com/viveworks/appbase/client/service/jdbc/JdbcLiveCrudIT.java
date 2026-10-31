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
package com.viveworks.appbase.client.service.jdbc;

import com.viveworks.appbase.client.util.AppbaseClientObjectMapperSubTypes;
import com.viveworks.appbase.core.util.jackson.AppbaseCoreObjectMapperSubTypes;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/** Live MariaDB CRUD contract for the JDBC adapter. */
@EnabledIfSystemProperty(named = "JdbcClientServiceTests.enabled", matches = "true")
class JdbcLiveCrudIT {
    private static final String TABLE = "appbase_jdbc_live_it";
    private static DefaultJdbcClientService service;

    @BeforeAll
    static void setUp() {
        AppbaseCoreObjectMapperSubTypes.register();
        AppbaseClientObjectMapperSubTypes.register();
        DefaultJdbcClientService.registerObjectMapperSubtypes();
        service = JdbcClientServiceTestHelper.defaultJdbcClientService();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (service != null) {
            try (Connection connection = service.getClientIntegrationService().getClient().getConnection();
                 Statement statement = connection.createStatement()) {
                statement.executeUpdate("DROP TABLE IF EXISTS " + TABLE);
            } finally {
                service.shutdown();
            }
        }
    }

    @Test
    void createReadUpdateDeleteAndNegativeQuery() throws Exception {
        try (Connection connection = service.getClientIntegrationService().getClient().getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("DROP TABLE IF EXISTS " + TABLE);
            statement.executeUpdate("CREATE TABLE " + TABLE + " (id INT PRIMARY KEY, value VARCHAR(64))");
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO " + TABLE + " (id, value) VALUES (?, ?)")) {
                insert.setInt(1, 1);
                insert.setString(2, "before");
                Assertions.assertEquals(1, insert.executeUpdate());
            }
            try (ResultSet rows = statement.executeQuery("SELECT value FROM " + TABLE + " WHERE id=1")) {
                Assertions.assertTrue(rows.next());
                Assertions.assertEquals("before", rows.getString(1));
            }
            Assertions.assertEquals(1, statement.executeUpdate("UPDATE " + TABLE + " SET value='after' WHERE id=1"));
            Assertions.assertEquals(1, statement.executeUpdate("DELETE FROM " + TABLE + " WHERE id=1"));
            Assertions.assertThrows(Exception.class,
                    () -> statement.executeQuery("SELECT * FROM appbase_jdbc_missing_table"));
            statement.executeUpdate("DROP TABLE " + TABLE);
        }
    }
}
