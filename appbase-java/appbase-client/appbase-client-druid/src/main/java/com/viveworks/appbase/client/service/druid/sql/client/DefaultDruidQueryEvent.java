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
import com.viveworks.appbase.core.util.objectpool.ObjectFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Default implementation of {@link DruidQueryEvent}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public final class DefaultDruidQueryEvent implements DruidQueryEvent {
    private final AtomicReference<String> queryRef;
    private final AtomicReference<List<DruidSqlParameter>> druidSqlParametersRef;
    private final AtomicReference<DruidResultSetHandler> druidResultSetHandlerRef;
    private final AtomicReference<DruidResultSet> druidResultSetRef;

    public DefaultDruidQueryEvent() {
        this.queryRef = new AtomicReference<>();
        this.druidSqlParametersRef = new AtomicReference<>();
        this.druidResultSetHandlerRef = new AtomicReference<>();
        this.druidResultSetRef = new AtomicReference<>();
    }

    public DefaultDruidQueryEvent(
            final String query,
            final DruidResultSetHandler druidResultSetHandler)
    {
        this();
        init(query, druidResultSetHandler);
    }

    public DefaultDruidQueryEvent(
            final String query,
            final List<DruidSqlParameter> druidSqlParameters,
            final DruidResultSetHandler druidResultSetHandler)
    {
        this();
        init(query, druidSqlParameters, druidResultSetHandler);
    }

    @Override
    public void init(
            final String query,
            final DruidResultSetHandler druidResultSetHandler)
    {
        AppbaseUtil.notNull(query, "query");
        AppbaseUtil.notNull(druidResultSetHandler, "druidResultSetHandler");

        this.queryRef.set(query);
        this.druidResultSetHandlerRef.set(druidResultSetHandler);
    }

    @Override
    public void init(
            final String parameterizedQuery,
            final List<DruidSqlParameter> druidSqlParameters,
            final DruidResultSetHandler druidResultSetHandler)
    {
        AppbaseUtil.notNull(druidSqlParameters, "druidSqlParameters");

        this.init(parameterizedQuery, druidResultSetHandler);
        this.druidSqlParametersRef.set(druidSqlParameters);
    }

    @Override
    public String getQuery() {
        return queryRef.get();
    }

    @Override
    public List<DruidSqlParameter> getDruidSqlParameters() {
        return druidSqlParametersRef.get();
    }

    @Override
    public DruidResultSetHandler getDruidResultSetHandler() {
        return druidResultSetHandlerRef.get();
    }

    @Override
    public boolean isValid() {
        return queryRef.get() != null && druidResultSetHandlerRef.get() != null;
    }

    @Override
    public boolean isClean() {
        return queryRef.get() == null &&
                druidSqlParametersRef.get() == null &&
                druidResultSetHandlerRef.get() == null &&
                druidResultSetRef.get() == null;
    }

    @Override
    public void clean() {
        queryRef.set(null);
        druidSqlParametersRef.set(null);
        druidResultSetHandlerRef.set(null);
        druidResultSetRef.set(null);
    }

    @Override
    public String toString() {
        return "DefaultDruidQueryEvent{" +
                "query=" + queryRef.get() +
                ", druidSqlParameters=" + druidSqlParametersRef.get() +
                ", druidResultSetHandler=" + druidResultSetHandlerRef.get() +
                ", druidResultSet=" + druidResultSetRef.get() +
                "} " + super.toString();
    }

    /**
     * {@link ObjectFactory} for {@link DruidQueryEvent}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    public static class Factory implements ObjectFactory<DruidQueryEvent> {
        @Override
        public DruidQueryEvent newObject() {
            return new DefaultDruidQueryEvent();
        }
    }
}
