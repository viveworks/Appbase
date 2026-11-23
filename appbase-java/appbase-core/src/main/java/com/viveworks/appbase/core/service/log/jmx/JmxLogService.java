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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.jmx.AbstractAppbaseServiceMXBean;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.log.LogServiceConfiguration;
import com.viveworks.appbase.core.service.log.accessor.LogAccessor;
import com.viveworks.appbase.core.service.log.accessor.LogAccessorException;
import com.viveworks.appbase.core.service.log.accessor.LogRecord;
import com.viveworks.appbase.core.service.log.store.LogStore;
import com.viveworks.appbase.core.service.log.store.LogStoreConfiguration;
import com.viveworks.appbase.core.service.log.store.LogStoreFactory;
import com.viveworks.appbase.core.service.log.store.criteria.LogRecordCriteria;
import com.viveworks.appbase.core.util.AppbaseConstants;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.jackson.JsonUtil;

import javax.management.MBeanException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Default implementation of {@link JmxLogServiceMXBean}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class JmxLogService
        extends AbstractAppbaseServiceMXBean<LogService, LogServiceConfiguration>
        implements JmxLogServiceMXBean
{
    private static final String ENV_PREFIX = AppbaseConstants.PERMISSION_PREFIX_ENV +
            JmxLogService.class.getSimpleName().toUpperCase() +
            AppbaseConstants.PERMISSION_SUFFIX_ENV;
    public static final String ALL_OPERATIONS_PERMISSION_ENV =
            ENV_PREFIX + "ALL_OPERATIONS";
    public static final String REMOVE_OPERATIONS_PERMISSION_ENV =
            ENV_PREFIX + "REMOVE_OPERATIONS";
    public static final String UNREGISTER_OPERATIONS_PERMISSION_ENV =
            ENV_PREFIX + "UNREGISTER_OPERATIONS";
    public static final String CLEAR_OPERATIONS_PERMISSION_ENV =
            ENV_PREFIX + "CLEAR_OPERATIONS";
    public static final String SYNC_OPERATION_PERMISSION_ENV =
            ENV_PREFIX + "SYNC_OPERATION";

    private static final String PROPERTY_PREFIX = AppbaseConstants.PERMISSION_PREFIX_PROPERTY +
            JmxLogService.class.getSimpleName().toLowerCase() +
            AppbaseConstants.PERMISSION_SUFFIX_PROPERTY;
    public static final String ALL_OPERATIONS_PERMISSION_PROPERTY =
            PROPERTY_PREFIX + "allOperations";
    public static final String REMOVE_OPERATIONS_PERMISSION_PROPERTY =
            PROPERTY_PREFIX + "removeOperations";
    public static final String UNREGISTER_OPERATIONS_PERMISSION_PROPERTY =
            PROPERTY_PREFIX + "unregisterOperations";
    public static final String CLEAR_OPERATIONS_PERMISSION_PROPERTY =
            PROPERTY_PREFIX + "clearOperations";
    public static final String SYNC_OPERATION_PERMISSION_PROPERTY =
            PROPERTY_PREFIX + "syncOperation";

    public static final boolean DEFAULT_REMOVE_OPERATIONS_PERMISSION = true;
    public static final boolean DEFAULT_UNREGISTER_OPERATIONS_PERMISSION = true;
    public static final boolean DEFAULT_CLEAR_OPERATIONS_PERMISSION = true;
    public static final boolean DEFAULT_SYNC_OPERATION_PERMISSION = true;

    private final LogAccessor logAccessor;

    public JmxLogService(final LogService logService,
                         final LogService appbaseService,
                         final Class<? extends LogServiceConfiguration> appbaseServiceConfigurationType,
                         final ConfigurationService configurationService)
    {
        super(logService, appbaseService, appbaseServiceConfigurationType, configurationService);

        final LogAccessor localLogAccessor = logService.getLogAccessor();
        AppbaseUtil.notNull(localLogAccessor, "localLogAccessor");
        this.logAccessor = localLogAccessor;
    }

    @Override
    public Map<String, List<String>> readLogRecordsBetween(final long inclusiveStartTimestamp,
                                                           final long exclusiveEndTimestamp,
                                                           final int pageSize) throws MBeanException
    {
        try {
            final Map<String, Collection<LogRecord>> readLogRecordsBetween =
                    logAccessor.readLogRecordsBetween(inclusiveStartTimestamp, exclusiveEndTimestamp, pageSize);

            final Map<String, List<String>> resultMap = new HashMap<>();
            if (readLogRecordsBetween != null) {
                for (final Map.Entry<String, Collection<LogRecord>> entry : readLogRecordsBetween.entrySet()) {
                    final String logStoreName = entry.getKey();
                    final Collection<LogRecord> logRecords = entry.getValue();
                    final List<String> jsonList = jsonOf(logRecords);
                    resultMap.put(logStoreName, jsonList);
                }
            }

            return Collections.unmodifiableMap(resultMap);
        } catch (Exception ex) {
            throw new MBeanException(ex, "Error in readLogRecordsBetween");
        }
    }

    @Override
    public Map<String, List<String>> readLogRecordsAt(final long timestamp, final int pageSize) throws MBeanException {
        try {
            final Map<String, Collection<LogRecord>> readLogRecordsAt =
                    logAccessor.readLogRecordsAt(timestamp, pageSize);

            final Map<String, List<String>> resultMap = new HashMap<>();
            if (readLogRecordsAt != null) {
                for (final Map.Entry<String, Collection<LogRecord>> entry : readLogRecordsAt.entrySet()) {
                    final String logStoreName = entry.getKey();
                    final Collection<LogRecord> logRecords = entry.getValue();
                    final List<String> jsonList = jsonOf(logRecords);
                    resultMap.put(logStoreName, jsonList);
                }
            }

            return Collections.unmodifiableMap(resultMap);
        } catch (Exception ex) {
            throw new MBeanException(ex, "Error in readLogRecordsAt");
        }
    }

    @Override
    public Map<String, List<String>> readLogRecordsByCriteria(final String logRecordCriteriaJson,
                                                              final int pageSize) throws MBeanException
    {
        AppbaseUtil.notBlank(logRecordCriteriaJson, "logRecordCriteriaJson");

        try {
            final LogRecordCriteria logRecordCriteria = logRecordCriteriaFromJson(logRecordCriteriaJson);
            final Map<String, Collection<LogRecord>> readLogRecordsByCriteria =
                    logAccessor.readLogRecordsByCriteria(logRecordCriteria, pageSize);

            final Map<String, List<String>> resultMap = new HashMap<>();
            if (readLogRecordsByCriteria != null) {
                for (final Map.Entry<String, Collection<LogRecord>> entry : readLogRecordsByCriteria.entrySet()) {
                    final String logStoreName = entry.getKey();
                    final Collection<LogRecord> logRecords = entry.getValue();
                    final List<String> jsonList = jsonOf(logRecords);
                    resultMap.put(logStoreName, jsonList);
                }
            }

            return Collections.unmodifiableMap(resultMap);
        } catch (Exception ex) {
            throw new MBeanException(ex, "Error in readLogRecordsByCriteria");
        }
    }

    @Override
    public Map<String, Boolean> removeLogRecordsBetween(final long inclusiveStartTimestamp,
                                                        final long exclusiveEndTimestamp) throws MBeanException
    {
        checkRemovePermission("removeLogRecordsBetween");

        try {
            final Collection<String> removeLogRecordsBetween =
                    logAccessor.removeLogRecordsBetween(inclusiveStartTimestamp, exclusiveEndTimestamp);

            final Map<String, Boolean> resultMap = new HashMap<>();
            if (removeLogRecordsBetween != null) {
                for (final String logStoreName : removeLogRecordsBetween) {
                    resultMap.put(logStoreName, true);
                }
            }

            return Collections.unmodifiableMap(resultMap);
        } catch (Exception ex) {
            throw new MBeanException(ex, "Error in removeLogRecordsBetween");
        }
    }

    @Override
    public Map<String, Boolean> removeLogRecordsAt(final long timestamp) throws MBeanException {
        checkRemovePermission("removeLogRecordsAt");

        try {
            final Collection<String> removeLogRecordsAt = logAccessor.removeLogRecordsAt(timestamp);

            final Map<String, Boolean> resultMap = new HashMap<>();
            if (removeLogRecordsAt != null) {
                for (final String logStoreName : removeLogRecordsAt) {
                    resultMap.put(logStoreName, true);
                }
            }

            return Collections.unmodifiableMap(resultMap);
        } catch (Exception ex) {
            throw new MBeanException(ex, "Error in removeLogRecordsAt");
        }
    }

    @Override
    public Map<String, Boolean> removeLogRecordsByCriteria(final String logRecordCriteriaJson) throws MBeanException {
        checkRemovePermission("removeLogRecordsByCriteria");

        AppbaseUtil.notBlank(logRecordCriteriaJson, "logRecordCriteriaJson");

        final LogRecordCriteria logRecordCriteria = logRecordCriteriaFromJson(logRecordCriteriaJson);

        try {
            final Collection<String> removeLogRecordsByCriteria =
                    logAccessor.removeLogRecordsByCriteria(logRecordCriteria);

            final Map<String, Boolean> resultMap = new HashMap<>();
            if (removeLogRecordsByCriteria != null) {
                for (final String logStoreName : removeLogRecordsByCriteria) {
                    resultMap.put(logStoreName, true);
                }
            }

            return Collections.unmodifiableMap(resultMap);
        } catch (Exception ex) {
            throw new MBeanException(ex, "Error in removeLogRecordsByCriteria");
        }
    }

    @Override
    public Map<String, String> getLogStoreFactoryByConfigurationTypeMap() {
        final Map<String, LogStoreFactory<LogStore, LogStoreConfiguration>> accessorResult =
                logAccessor.getLogStoreFactoryByConfigurationTypeMap();

        final Map<String, String> resultMap = new HashMap<>();
        if (accessorResult != null) {
            accessorResult.forEach((configurationTypeName, logStoreFactory) -> {
                resultMap.put(configurationTypeName, logStoreFactory.getClass().getName());
            });
        }

        return Collections.unmodifiableMap(resultMap);
    }

    @Override
    public void unregisterLogStoreFactoryByConfigurationType(final String logStoreConfigurationTypeName,
                                                             final String logStoreFactoryTypeName) throws MBeanException
    {
        checkUnregisterPermission("unregisterLogStoreFactoryByConfigurationType");

        AppbaseUtil.notBlank(logStoreConfigurationTypeName, "logStoreConfigurationTypeName");
        AppbaseUtil.notBlank(logStoreFactoryTypeName, "logStoreFactoryTypeName");

        final Class<? extends LogStoreConfiguration> logStoreConfigurationType =
                logStoreConfigurationType(logStoreConfigurationTypeName);
        final Class<? extends LogStoreFactory> logStoreFactoryType =
                logStoreFactoryType(logStoreFactoryTypeName);

        logAccessor.unregisterLogStoreFactoryByConfigurationType(logStoreConfigurationType, logStoreFactoryType);
    }

    @Override
    public void clearLogStoreFactoryByConfigurationTypeMap() {
        checkClearPermission("clearLogStoreFactoryByConfigurationTypeMap");

        logAccessor.clearLogStoreFactoryByConfigurationTypeMap();
    }

    @Override
    public List<String> getLogStores() {
        final Collection<LogStore> logStores = logAccessor.getLogStores();

        final List<String> result = new ArrayList<>();
        logStores.forEach(logStore -> result.add(logStore.getClass().getName()));

        return Collections.unmodifiableList(result);
    }

    @Override
    public void unregisterLogStore(final String logStoreTypeName) throws MBeanException {
        checkUnregisterPermission("unregisterLogStore");

        AppbaseUtil.notBlank(logStoreTypeName, "logStoreTypeName");

        final Class<? extends LogStore> logStoreType = logStoreType(logStoreTypeName);
        logAccessor.unregisterLogStore(logStoreType);
    }

    @Override
    public void clearLogStores() {
        checkClearPermission("clearLogStores");

        logAccessor.clearLogStores();
    }

    @Override
    public void synchronizeLogStores() throws MBeanException {
        checkSyncPermission("synchronizeLogStores");

        try {
            logAccessor.synchronizeLogStores();
        } catch (LogAccessorException lae) {
            throw new MBeanException(lae, "Error synchronizing log stores");
        }
    }

    private List<String> jsonOf(final Collection<LogRecord> logRecords) throws MBeanException {
        AppbaseUtil.notNull(logRecords, "logRecords");

        final List<String> result = new ArrayList<>();
        for (final LogRecord logRecord : logRecords) {
            try {
                final String json = JsonUtil
                        .getJsonMapper()
                        .writerFor(LogRecord.class)
                        .writeValueAsString(logRecord);
                result.add(json);
            } catch (JsonProcessingException jpe) {
                throw new MBeanException(jpe, "Error serializing " + logRecord);
            }
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private Class<? extends LogStoreConfiguration> logStoreConfigurationType(
            final String logStoreConfigurationTypeName) throws MBeanException
    {
        try {
            return (Class<? extends LogStoreConfiguration>) Class.forName(logStoreConfigurationTypeName);
        } catch (ClassNotFoundException cnfe) {
            throw new MBeanException(cnfe, "Error with type name: " + logStoreConfigurationTypeName);
        }
    }

    @SuppressWarnings("unchecked")
    private Class<? extends LogStoreFactory> logStoreFactoryType(
            final String logStoreFactoryTypeName) throws MBeanException
    {
        try {
            return (Class<? extends LogStoreFactory>) Class.forName(logStoreFactoryTypeName);
        } catch (ClassNotFoundException cnfe) {
            throw new MBeanException(cnfe, "Error with type name: " + logStoreFactoryTypeName);
        }
    }

    @SuppressWarnings("unchecked")
    private Class<? extends LogStore> logStoreType(final String logStoreTypeName) throws MBeanException {
        try {
            return (Class<? extends LogStore>) Class.forName(logStoreTypeName);
        } catch (ClassNotFoundException cnfe) {
            throw new MBeanException(cnfe, "Error with type name: " + logStoreTypeName);
        }
    }

    private LogRecordCriteria logRecordCriteriaFromJson(final String json) throws MBeanException {
        AppbaseUtil.notNull(json, "json");

        try {
            return JsonUtil
                    .getJsonMapper()
                    .readerFor(LogRecordCriteria.class)
                    .readValue(json);
        } catch (JsonProcessingException jpe) {
            throw new MBeanException(jpe, "Error serializing JSON: " + json);
        }
    }

    private Boolean getResult(final CompletableFuture<Void> resultFuture,
                              final String methodName) throws MBeanException
    {
        try {
            resultFuture.get();
            return true;
        } catch (InterruptedException | ExecutionException iee) {
            throw new MBeanException(iee, "Error waiting on future in " + methodName);
        }
    }

    private Collection<LogRecord> getLogRecords(
            final CompletableFuture<Collection<LogRecord>> resultFuture,
            final String methodName) throws MBeanException
    {
        try {
            return resultFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            final String message = "Error waiting on future in";
            logger.error(message + " {}", methodName, e);
            throw new MBeanException(e, message + " " + methodName);
        }
    }

    private void checkRemovePermission(final String operationName) {
        checkPermission(
                REMOVE_OPERATIONS_PERMISSION_ENV,
                REMOVE_OPERATIONS_PERMISSION_PROPERTY,
                DEFAULT_REMOVE_OPERATIONS_PERMISSION,
                operationName);
    }

    private void checkUnregisterPermission(final String operationName) {
        checkPermission(
                UNREGISTER_OPERATIONS_PERMISSION_ENV,
                UNREGISTER_OPERATIONS_PERMISSION_PROPERTY,
                DEFAULT_UNREGISTER_OPERATIONS_PERMISSION,
                operationName);
    }

    private void checkClearPermission(final String operationName) {
        checkPermission(
                CLEAR_OPERATIONS_PERMISSION_ENV,
                CLEAR_OPERATIONS_PERMISSION_PROPERTY,
                DEFAULT_CLEAR_OPERATIONS_PERMISSION,
                operationName);
    }

    private void checkSyncPermission(final String operationName) {
        checkPermission(
                SYNC_OPERATION_PERMISSION_ENV,
                SYNC_OPERATION_PERMISSION_PROPERTY,
                DEFAULT_SYNC_OPERATION_PERMISSION,
                operationName);
    }
}
