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

import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.handler.AbstractMutableAppbaseHandlerRequest;
import com.viveworks.appbase.core.util.objectpool.ObjectFactory;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Default implementation of {@link DruidSqlParameter}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultDruidSqlParameter extends AbstractMutableAppbaseHandlerRequest implements DruidSqlParameter {
    private final AtomicReference<Integer> positionRef;
    private final AtomicReference<Object> valueRef;
    private final AtomicReference<DruidSqlType> druidSqlTypeRef;

    public DefaultDruidSqlParameter() {
        this.positionRef = new AtomicReference<>();
        this.valueRef = new AtomicReference<>();
        this.druidSqlTypeRef = new AtomicReference<>();
    }

    public DefaultDruidSqlParameter(
            final int position,
            final Object value,
            final DruidSqlType druidSqlType)
    {
        this();
        init(position, value, druidSqlType);
    }

    /**
     * Initialize the object.
     *
     * @param position Position in the parameterized query (starts with 1)
     * @param value Parameter value
     * @param druidSqlType {@link DruidSqlType}
     */
    public void init(
            final int position,
            final Object value,
            final DruidSqlType druidSqlType)
    {
        AppbaseUtil.positive(position, "position");
        AppbaseUtil.notNull(value, "value");
        AppbaseUtil.notNull(druidSqlType, "druidSqlType");

        this.positionRef.set(position);
        this.valueRef.set(value);
        this.druidSqlTypeRef.set(druidSqlType);
    }

    @Override
    public void initNull(final int position, final DruidSqlType druidSqlType) {
        AppbaseUtil.positive(position, "position");
        AppbaseUtil.notNull(druidSqlType, "druidSqlType");

        this.positionRef.set(position);
        this.valueRef.set(null);
        this.druidSqlTypeRef.set(druidSqlType);
    }

    @Override
    public int getPosition() {
        return positionRef.get();
    }

    @Override
    public Object getValue() {
        return valueRef.get();
    }

    @Override
    public DruidSqlType getDruidSqlType() {
        return druidSqlTypeRef.get();
    }

    @Override
    public boolean isValid() {
        final Integer position = positionRef.get();
        final DruidSqlType druidSqlType = druidSqlTypeRef.get();

        if (position == null || druidSqlType == null) {
            return false;
        }

        final Object value = valueRef.get();
        switch (druidSqlType) {
            case CHAR:
            case VARCHAR:
                return String.class.isAssignableFrom(value.getClass());
            case BOOLEAN:
                return (boolean.class.isAssignableFrom(value.getClass()) ||
                        Boolean.class.isAssignableFrom(value.getClass()));
            case TINYINT:
                return (byte.class.isAssignableFrom(value.getClass()) ||
                        Byte.class.isAssignableFrom(value.getClass()));
            case SMALLINT:
                return (short.class.isAssignableFrom(value.getClass()) ||
                        Short.class.isAssignableFrom(value.getClass()));
            case INTEGER:
                return (int.class.isAssignableFrom(value.getClass()) ||
                        Integer.class.isAssignableFrom(value.getClass()));
            case BIGINT:
                return (long.class.isAssignableFrom(value.getClass()) ||
                        Long.class.isAssignableFrom(value.getClass()));
            case FLOAT:
                return (float.class.isAssignableFrom(value.getClass()) ||
                        Float.class.isAssignableFrom(value.getClass()));
            case DOUBLE:
            case REAL:
            case DECIMAL:
                return (double.class.isAssignableFrom(value.getClass()) ||
                        Double.class.isAssignableFrom(value.getClass()));
            case TIMESTAMP:
                return (Timestamp.class.isAssignableFrom(value.getClass()));
            case DATE:
                return (Date.class.isAssignableFrom(value.getClass()));
            case OTHER:
                return value != null;
            case NULL:
                return value == null;
            default:
                return true;
        }
    }

    @Override
    public boolean isClean() {
        return super.isClean() &&
                positionRef.get() == null &&
                valueRef.get() == null &&
                druidSqlTypeRef.get() == null;
    }

    @Override
    public void clean() {
        positionRef.set(null);
        valueRef.set(null);
        druidSqlTypeRef.set(null);

        super.clean();
    }

    @Override
    public String toString() {
        return "DefaultDruidSqlParameter{" +
                "position=" + positionRef.get() +
                ", value=" + valueRef.get() +
                ", druidSqlType=" + druidSqlTypeRef.get() +
                "} " + super.toString();
    }



    /**
     * {@link ObjectFactory} for {@link DruidSqlParameter}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    public static class Factory implements ObjectFactory<DruidSqlParameter> {
        @Override
        public DruidSqlParameter newObject() {
            return new DefaultDruidSqlParameter();
        }
    }
}
