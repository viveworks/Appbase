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

package com.viveworks.appbase.client.service.druid.sql.client;

import com.viveworks.appbase.core.util.AppbaseUtil;

import javax.inject.Inject;
import java.sql.Timestamp;
import java.util.Date;

/**
 * Default implementation of {@link DruidSqlParameterFactory}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultDruidSqlParameterFactory implements DruidSqlParameterFactory {
    private final DruidSqlClientSharedResources druidSqlClientSharedResources;

    @Inject
    public DefaultDruidSqlParameterFactory(final DruidSqlClientSharedResources druidSqlClientSharedResources) {
        AppbaseUtil.notNull(druidSqlClientSharedResources, "druidSqlClientSharedResources");

        this.druidSqlClientSharedResources = druidSqlClientSharedResources;
    }

    @Override
    public DruidSqlParameter nullParameter(final int position, final DruidSqlType druidSqlType) {
        AppbaseUtil.positive(position, "position");
        AppbaseUtil.notNull(druidSqlType, "druidSqlType");

        final DruidSqlParameter druidSqlParameter = druidSqlParameter();
        druidSqlParameter.initNull(position, druidSqlType);

        return druidSqlParameter;
    }

    @Override
    public DruidSqlParameter charParameter(final int position, final char value) {
        AppbaseUtil.positive(position, "position");

        final DruidSqlParameter druidSqlParameter = druidSqlParameter();
        druidSqlParameter.init(position, value, DruidSqlType.CHAR);

        return druidSqlParameter;
    }

    @Override
    public DruidSqlParameter varcharParameter(final int position, final String value) {
        AppbaseUtil.positive(position, "position");
        AppbaseUtil.notNull(value, "value");

        final DruidSqlParameter druidSqlParameter = druidSqlParameter();
        druidSqlParameter.init(position, value, DruidSqlType.VARCHAR);

        return druidSqlParameter;
    }

    @Override
    public DruidSqlParameter decimalParameter(final int position, final double value) {
        AppbaseUtil.positive(position, "position");

        final DruidSqlParameter druidSqlParameter = druidSqlParameter();
        druidSqlParameter.init(position, value, DruidSqlType.DECIMAL);

        return druidSqlParameter;
    }

    @Override
    public DruidSqlParameter floatParameter(final int position, final float value) {
        AppbaseUtil.positive(position, "position");

        final DruidSqlParameter druidSqlParameter = druidSqlParameter();
        druidSqlParameter.init(position, value, DruidSqlType.FLOAT);

        return druidSqlParameter;
    }

    @Override
    public DruidSqlParameter realParameter(final int position, final double value) {
        AppbaseUtil.positive(position, "position");

        final DruidSqlParameter druidSqlParameter = druidSqlParameter();
        druidSqlParameter.init(position, value, DruidSqlType.REAL);

        return druidSqlParameter;
    }

    @Override
    public DruidSqlParameter doubleParameter(final int position, final double value) {
        AppbaseUtil.positive(position, "position");

        final DruidSqlParameter druidSqlParameter = druidSqlParameter();
        druidSqlParameter.init(position, value, DruidSqlType.DOUBLE);

        return druidSqlParameter;
    }

    @Override
    public DruidSqlParameter booleanParameter(final int position, final boolean value) {
        AppbaseUtil.positive(position, "position");

        final DruidSqlParameter druidSqlParameter = druidSqlParameter();
        druidSqlParameter.init(position, value, DruidSqlType.BOOLEAN);

        return druidSqlParameter;
    }

    @Override
    public DruidSqlParameter tinyIntParameter(final int position, final byte value) {
        AppbaseUtil.positive(position, "position");

        final DruidSqlParameter druidSqlParameter = druidSqlParameter();
        druidSqlParameter.init(position, value, DruidSqlType.TINYINT);

        return druidSqlParameter;
    }

    @Override
    public DruidSqlParameter smallIntParameter(final int position, final short value) {
        AppbaseUtil.positive(position, "position");

        final DruidSqlParameter druidSqlParameter = druidSqlParameter();
        druidSqlParameter.init(position, value, DruidSqlType.SMALLINT);

        return druidSqlParameter;
    }

    @Override
    public DruidSqlParameter integerParameter(final int position, final int value) {
        AppbaseUtil.positive(position, "position");

        final DruidSqlParameter druidSqlParameter = druidSqlParameter();
        druidSqlParameter.init(position, value, DruidSqlType.INTEGER);

        return druidSqlParameter;
    }

    @Override
    public DruidSqlParameter bigIntParameter(final int position, final long value) {
        AppbaseUtil.positive(position, "position");

        final DruidSqlParameter druidSqlParameter = druidSqlParameter();
        druidSqlParameter.init(position, value, DruidSqlType.BIGINT);

        return druidSqlParameter;
    }

    @Override
    public DruidSqlParameter timestampParameter(final int position, final Timestamp value) {
        AppbaseUtil.positive(position, "position");
        AppbaseUtil.notNull(value, "value");

        final DruidSqlParameter druidSqlParameter = druidSqlParameter();
        druidSqlParameter.init(position, value, DruidSqlType.TIMESTAMP);

        return druidSqlParameter;
    }

    @Override
    public DruidSqlParameter dateParameter(final int position, final Date value) {
        AppbaseUtil.positive(position, "position");
        AppbaseUtil.notNull(value, "value");

        final DruidSqlParameter druidSqlParameter = druidSqlParameter();
        druidSqlParameter.init(position, value, DruidSqlType.DATE);

        return druidSqlParameter;
    }

    @Override
    public DruidSqlParameter otherParameter(final int position, final Object value) {
        AppbaseUtil.positive(position, "position");
        AppbaseUtil.notNull(value, "value");

        final DruidSqlParameter druidSqlParameter = druidSqlParameter();
        druidSqlParameter.init(position, value, DruidSqlType.OTHER);

        return druidSqlParameter;
    }

    private DruidSqlParameter druidSqlParameter() {
        return druidSqlClientSharedResources
                .getSharedDruidSqlParameterObjectPool()
                .pollCleanObject();
    }
}
