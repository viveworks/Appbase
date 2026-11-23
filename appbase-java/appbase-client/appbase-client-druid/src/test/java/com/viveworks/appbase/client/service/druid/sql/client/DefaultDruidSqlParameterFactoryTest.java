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

package com.viveworks.appbase.client.service.druid.sql.client;

import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.service.druid.DruidSqlClientTestHelper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@EnabledIfSystemProperty(named = "DruidClientServiceTests.enabled", matches = "true")
class DefaultDruidSqlParameterFactoryTest {
    private static DruidSqlClientSharedResources druidSqlClientSharedResources;
    private static DruidSqlParameterFactory druidSqlParameterFactory;

    @BeforeAll
    static void setUp() {
        final AppbaseClientCore appbaseClientCore = DruidSqlClientTestHelper.appbaseClientCore();
        druidSqlClientSharedResources = new DefaultDruidSqlClientSharedResources(appbaseClientCore);
        druidSqlParameterFactory = new DefaultDruidSqlParameterFactory(druidSqlClientSharedResources);
    }

    @AfterAll
    static void tearDown() {
        druidSqlClientSharedResources.shutdown();
    }

    private void assertDruidSqlParameter(
            final DruidSqlParameter druidSqlParameter,
            final int expectedPosition,
            final Object expectedValue,
            final DruidSqlType expectedDruidSqlType)
    {
        Assertions.assertEquals(expectedPosition, druidSqlParameter.getPosition());
        Assertions.assertEquals(expectedValue, druidSqlParameter.getValue());
        Assertions.assertEquals(expectedDruidSqlType, druidSqlParameter.getDruidSqlType());
    }

    @Test
    void testNullParameter() {
        final DruidSqlParameter druidSqlParameter = druidSqlParameterFactory.nullParameter(2, DruidSqlType.FLOAT);
        assertDruidSqlParameter(druidSqlParameter, 2, null, DruidSqlType.FLOAT);
    }

    @Test
    void testCharParameter() {
        final DruidSqlParameter druidSqlParameter = druidSqlParameterFactory.charParameter(3, 'c');
        assertDruidSqlParameter(druidSqlParameter, 3, 'c', DruidSqlType.CHAR);
    }

    @Test
    void testVarcharParameter() {
        final DruidSqlParameter druidSqlParameter = druidSqlParameterFactory.varcharParameter(5, "randomly");
        assertDruidSqlParameter(druidSqlParameter, 5, "randomly", DruidSqlType.VARCHAR);
    }

    @Test
    void testDecimalParameter() {
        final DruidSqlParameter druidSqlParameter = druidSqlParameterFactory.decimalParameter(7, 3.14123);
        assertDruidSqlParameter(druidSqlParameter, 7, 3.14123, DruidSqlType.DECIMAL);
    }

    @Test
    void testFloatParameter() {
        final DruidSqlParameter druidSqlParameter = druidSqlParameterFactory.floatParameter(11, 3.14F);
        assertDruidSqlParameter(druidSqlParameter, 11, 3.14F, DruidSqlType.FLOAT);
    }

    @Test
    void testRealParameter() {
        final DruidSqlParameter druidSqlParameter = druidSqlParameterFactory.realParameter(11, 23.2409);
        assertDruidSqlParameter(druidSqlParameter, 11, 23.2409, DruidSqlType.REAL);
    }

    @Test
    void testDoubleParameter() {
        final DruidSqlParameter druidSqlParameter = druidSqlParameterFactory.doubleParameter(13, 2409.23);
        assertDruidSqlParameter(druidSqlParameter, 13, 2409.23, DruidSqlType.DOUBLE);
    }

    @Test
    void testBooleanParameter() {
        final DruidSqlParameter druidSqlParameter = druidSqlParameterFactory.booleanParameter(17, true);
        assertDruidSqlParameter(druidSqlParameter, 17, true, DruidSqlType.BOOLEAN);
    }

    @Test
    void testTinyIntParameter() {
        final DruidSqlParameter druidSqlParameter = druidSqlParameterFactory.tinyIntParameter(19, (byte) 232409);
        assertDruidSqlParameter(druidSqlParameter, 19, (byte) 232409, DruidSqlType.TINYINT);
    }

    @Test
    void testSmallIntParameter() {
        final DruidSqlParameter druidSqlParameter = druidSqlParameterFactory.smallIntParameter(23, (short) 2409);
        assertDruidSqlParameter(druidSqlParameter, 23, (short) 2409, DruidSqlType.SMALLINT);
    }

    @Test
    void testIntegerParameter() {
        final DruidSqlParameter druidSqlParameter = druidSqlParameterFactory.integerParameter(29, 240923);
        assertDruidSqlParameter(druidSqlParameter, 29, 240923, DruidSqlType.INTEGER);
    }

    @Test
    void testBigIntParameter() {
        final DruidSqlParameter druidSqlParameter = druidSqlParameterFactory.bigIntParameter(31, 240923240923L);
        assertDruidSqlParameter(druidSqlParameter, 31, 240923240923L, DruidSqlType.BIGINT);
    }

    @Test
    void testTimestampParameter() {
        final Timestamp timestamp = Timestamp.from(Instant.now());
        final DruidSqlParameter druidSqlParameter = druidSqlParameterFactory.timestampParameter(37, timestamp);
        assertDruidSqlParameter(druidSqlParameter, 37, timestamp, DruidSqlType.TIMESTAMP);
    }

    @Test
    void testDateParameter() {
        final java.util.Date date = Date.from(Instant.now());
        final DruidSqlParameter druidSqlParameter = druidSqlParameterFactory.dateParameter(41, date);
        assertDruidSqlParameter(druidSqlParameter, 41, date, DruidSqlType.DATE);
    }

    @Test
    void testOtherParameter() {
        final DruidSqlParameter druidSqlParameter = druidSqlParameterFactory.otherParameter(31, 240923240923L);
        assertDruidSqlParameter(druidSqlParameter, 31, 240923240923L, DruidSqlType.OTHER);
    }
}
