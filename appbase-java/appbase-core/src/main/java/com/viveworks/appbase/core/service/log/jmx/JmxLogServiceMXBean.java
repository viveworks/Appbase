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

package com.viveworks.appbase.core.service.log.jmx;

import com.viveworks.appbase.core.service.jmx.AppbaseServiceMXBean;
import com.viveworks.appbase.core.service.log.accessor.LogRecord;
import com.viveworks.appbase.core.service.log.store.LogStore;
import com.viveworks.appbase.core.service.log.store.LogStoreConfiguration;
import com.viveworks.appbase.core.service.log.store.LogStoreFactory;
import com.viveworks.appbase.core.service.log.store.criteria.LogRecordCriteria;
import com.viveworks.appbase.core.util.Unmodifiable;

import javax.management.MBeanException;
import javax.management.MXBean;
import java.util.List;
import java.util.Map;

/**
 * Represents a {@link MXBean} for {@link com.viveworks.appbase.core.service.log.LogService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@MXBean
public interface JmxLogServiceMXBean extends AppbaseServiceMXBean {
    /**
     * Read {@link LogRecord}s JSON between start timestamp (inclusive) and end timestamp (exclusive).
     *
     * @param inclusiveStartTimestamp Start timestamp (inclusive)
     * @param exclusiveEndTimestamp End timestamp (exclusive)
     * @param pageSize Page size of the result
     * @return {@link LogRecord}s JSON by {@link LogStore} type name
     * @throws MBeanException if there were any errors
     */
    @Unmodifiable
    Map<String, List<String>> readLogRecordsBetween(
            long inclusiveStartTimestamp, long exclusiveEndTimestamp, int pageSize) throws MBeanException;


    /**
     * Read {@link LogRecord} at a specific timestamp.
     *
     * @param timestamp Specific log timestamp
     * @param pageSize Page size of the result
     * @return {@link LogRecord}s JSON by {@link LogStore} type name
     * @throws MBeanException if there were any errors
     */
    @Unmodifiable
    Map<String, List<String>> readLogRecordsAt(long timestamp, int pageSize) throws MBeanException;


    /**
     * Read by matching {@link LogRecordCriteria} JSON.
     *
     * @param logRecordCriteriaJson Matching {@link LogRecordCriteria} JSON
     * @param pageSize Page size of the result
     * @return {@link LogRecord}s JSON by {@link LogStore} type name
     * @throws MBeanException if there were any errors
     */
    @Unmodifiable
    Map<String, List<String>> readLogRecordsByCriteria(
            String logRecordCriteriaJson, int pageSize) throws MBeanException;


    /**
     * Remove {@link LogRecord}s between start timestamp (inclusive) and end timestamp (exclusive).
     *
     * @param inclusiveStartTimestamp Start timestamp (inclusive)
     * @param exclusiveEndTimestamp End timestamp (exclusive)
     * @return Results by {@link LogStore} type name
     * @throws MBeanException if there were any errors
     */
    @Unmodifiable
    Map<String, Boolean> removeLogRecordsBetween(
            long inclusiveStartTimestamp, long exclusiveEndTimestamp) throws MBeanException;


    /**
     * Remove {@link LogRecord}s at a given timestamp.
     *
     * @param timestamp Log timestamp
     * @return Results by {@link LogStore} type name
     * @throws MBeanException if there were any errors
     */
    @Unmodifiable
    Map<String, Boolean> removeLogRecordsAt(long timestamp) throws MBeanException;


    /**
     * Remove {@link LogRecord}s by matching {@link LogRecordCriteria}.
     *
     * @param logRecordCriteriaJson Matching {@link LogRecordCriteria} JSON
     * @return Results by {@link LogStore} type name
     * @throws MBeanException if there were any errors
     */
    @Unmodifiable
    Map<String, Boolean> removeLogRecordsByCriteria(String logRecordCriteriaJson) throws MBeanException;


    /**
     * Get {@link LogStoreFactory} type name by {@link LogStoreConfiguration} type name map.
     *
     * @return Log store factory type name by configuration type name map
     */
    @Unmodifiable
    Map<String, String> getLogStoreFactoryByConfigurationTypeMap();


    /**
     * Unregister {@link LogStoreFactory} by {@link LogStoreConfiguration} by type names.
     *
     * @param logStoreConfigurationTypeName Log store configuration type name
     * @param logStoreFactoryTypeName Log store factory type name
     * @throws MBeanException if there were any errors
     */
    void unregisterLogStoreFactoryByConfigurationType(
            String logStoreConfigurationTypeName, String logStoreFactoryTypeName) throws MBeanException;


    /**
     * Clear {@link LogStoreFactory} by {@link LogStoreConfiguration} type name map.
     */
    void clearLogStoreFactoryByConfigurationTypeMap();


    /**
     * Get {@link LogStore} type names.
     *
     * @return Log store type names
     */
    @Unmodifiable
    List<String> getLogStores();


    /**
     * Unregister {@link LogStore} by type name.
     *
     * @param logStoreTypeName Log store type name
     * @throws MBeanException if there were any errors
     */
    void unregisterLogStore(String logStoreTypeName) throws MBeanException;


    /**
     * Clear {@link LogStore}s.
     */
    void clearLogStores();


    /**
     * Used to synchronize stores.
     *
     * @throws MBeanException if there were any errors
     */
    void synchronizeLogStores() throws MBeanException;
}
