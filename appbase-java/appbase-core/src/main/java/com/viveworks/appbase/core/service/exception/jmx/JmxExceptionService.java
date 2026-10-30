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

package com.viveworks.appbase.core.service.exception.jmx;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.exception.ExceptionService;
import com.viveworks.appbase.core.service.exception.ExceptionServiceConfiguration;
import com.viveworks.appbase.core.service.exception.accessor.ExceptionAccessor;
import com.viveworks.appbase.core.service.exception.accessor.ExceptionAccessorException;
import com.viveworks.appbase.core.service.exception.listener.ExceptionListener;
import com.viveworks.appbase.core.service.exception.store.ExceptionMessageDetails;
import com.viveworks.appbase.core.service.exception.store.ExceptionStore;
import com.viveworks.appbase.core.service.exception.store.ExceptionStoreConfiguration;
import com.viveworks.appbase.core.service.exception.store.ExceptionStoreFactory;
import com.viveworks.appbase.core.service.exception.store.RedactedExceptionMessageDetails;
import com.viveworks.appbase.core.service.jmx.AbstractAppbaseServiceMXBean;
import com.viveworks.appbase.core.service.log.LogService;
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
 * Default implementation of {@link JmxExceptionServiceMXBean}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class JmxExceptionService
        extends AbstractAppbaseServiceMXBean<ExceptionService, ExceptionServiceConfiguration>
        implements JmxExceptionServiceMXBean
{
    private static final String ENV_PREFIX = AppbaseConstants.PERMISSION_PREFIX_ENV +
            JmxExceptionService.class.getSimpleName().toUpperCase() +
            AppbaseConstants.PERMISSION_SUFFIX_ENV;
    public static final String ALL_OPERATIONS_PERMISSION_ENV =
            ENV_PREFIX + "ALL_OPERATIONS";
    public static final String REMOVE_OPERATIONS_PERMISSION_ENV =
            ENV_PREFIX + "REMOVE_OPERATIONS";
    public static final String UNREGISTER_OPERATIONS_PERMISSION_ENV =
            ENV_PREFIX + "UNREGISTER_OPERATIONS";
    public static final String CLEAR_OPERATIONS_PERMISSION_ENV =
            ENV_PREFIX + "CLEAR_OPERATIONS";

    private static final String PROPERTY_PREFIX = AppbaseConstants.PERMISSION_PREFIX_PROPERTY +
            JmxExceptionService.class.getSimpleName().toLowerCase() +
            AppbaseConstants.PERMISSION_SUFFIX_PROPERTY;
    public static final String ALL_OPERATIONS_PERMISSION_PROPERTY =
            PROPERTY_PREFIX + "allOperations";
    public static final String REMOVE_OPERATIONS_PERMISSION_PROPERTY =
            PROPERTY_PREFIX + "removeOperations";
    public static final String UNREGISTER_OPERATIONS_PERMISSION_PROPERTY =
            PROPERTY_PREFIX + "unregisterOperations";
    public static final String CLEAR_OPERATIONS_PERMISSION_PROPERTY =
            PROPERTY_PREFIX + "clearOperations";

    public static final boolean DEFAULT_REMOVE_OPERATIONS_PERMISSION = true;
    public static final boolean DEFAULT_UNREGISTER_OPERATIONS_PERMISSION = true;
    public static final boolean DEFAULT_CLEAR_OPERATIONS_PERMISSION = true;

    private final ExceptionAccessor exceptionAccessor;

    public JmxExceptionService(final LogService logService,
                               final ExceptionService appbaseService,
                               final Class<? extends ExceptionServiceConfiguration> appbaseServiceConfigurationType,
                               final ConfigurationService configurationService)
    {
        super(logService, appbaseService, appbaseServiceConfigurationType, configurationService);

        final ExceptionAccessor localExceptionAccessor = appbaseService.getExceptionAccessor();
        AppbaseUtil.notNull(localExceptionAccessor, "exceptionAccessor");
        this.exceptionAccessor = localExceptionAccessor;
    }

    @Override
    public Map<String, String> readExceptionMessageDetailsById(final Integer id) throws MBeanException {
        try {
            final Map<String, ExceptionMessageDetails> result =
                    exceptionAccessor.readExceptionMessageDetailsById(id);

            final Map<String, String> resultMap = new HashMap<>();
            if (result != null) {
                for (final Map.Entry<String, ExceptionMessageDetails> entry : result.entrySet()) {
                    final String exceptionStoreName = entry.getKey();
                    final ExceptionMessageDetails exceptionMessageDetails = entry.getValue();
                    if (exceptionMessageDetails != null) {
                        final String json = jsonOf(exceptionMessageDetails);
                        resultMap.put(exceptionStoreName, json);
                    }
                }
            }

            return Collections.unmodifiableMap(resultMap);
        } catch (ExceptionAccessorException e) {
            throw new MBeanException(e, "Error in readExceptionMessageDetailsById");
        }
    }

    @Override
    public Map<String, List<String>> readAllExceptionMessageDetails() throws MBeanException {
        try {
            final Map<String, Collection<ExceptionMessageDetails>> result =
                    exceptionAccessor.readAllExceptionMessageDetails();

            final Map<String, List<String>> resultMap = new HashMap<>();
            if (result != null) {
                for (final Map.Entry<String, Collection<ExceptionMessageDetails>> entry :
                        result.entrySet()) {
                    final String exceptionStoreName = entry.getKey();
                    final Collection<ExceptionMessageDetails> exceptionMessageDetailsCollection = entry.getValue();
                    if (exceptionMessageDetailsCollection != null) {
                        final List<String> jsonList = jsonListOf(exceptionMessageDetailsCollection);
                        resultMap.put(exceptionStoreName, jsonList);
                    }
                }
            }

            return Collections.unmodifiableMap(resultMap);
        } catch (ExceptionAccessorException e) {
            throw new MBeanException(e, "Error in readAllExceptionMessageDetails");
        }
    }

    @Override
    public Map<String, List<String>> readAllExceptionMessageDetailsByTimestamps(
            final long inclusiveMinTimestamp,
            final long exclusiveMaxTimestamp) throws MBeanException
    {
        try {
            final Map<String, Collection<ExceptionMessageDetails>> result =
                    exceptionAccessor.readAllExceptionMessageDetailsByTimestamps(
                            inclusiveMinTimestamp, exclusiveMaxTimestamp);

            final Map<String, List<String>> resultMap = new HashMap<>();
            if (result != null) {
                for (final Map.Entry<String, Collection<ExceptionMessageDetails>> entry :
                        result.entrySet()) {
                    final String exceptionStoreName = entry.getKey();
                    final Collection<ExceptionMessageDetails> exceptionMessageDetailsCollection =
                            entry.getValue();
                    if (exceptionMessageDetailsCollection != null) {
                        final List<String> jsonList = jsonListOf(exceptionMessageDetailsCollection);
                        resultMap.put(exceptionStoreName, jsonList);
                    }
                }
            }

            return Collections.unmodifiableMap(resultMap);
        } catch (ExceptionAccessorException e) {
            throw new MBeanException(e, "Error in readAllExceptionMessageDetailsByTimestamps");
        }
    }

    @Override
    public Map<String, List<String>> readUniqueExceptionTypeNames() throws MBeanException {
        try {
            final Map<String, Collection<String>> result =
                    exceptionAccessor.readUniqueExceptionTypeNames();

            final Map<String, List<String>> resultMap = new HashMap<>();
            if (result != null) {
                for (final Map.Entry<String, Collection<String>> entry : result.entrySet()) {
                    final String exceptionStoreName = entry.getKey();
                    final Collection<String> exceptionTypeNamesCollection = entry.getValue();
                    if (exceptionTypeNamesCollection != null) {
                        final List<String> exceptionTypeNameList = new ArrayList<>(exceptionTypeNamesCollection);
                        resultMap.put(exceptionStoreName, exceptionTypeNameList);
                    }
                }
            }

            return Collections.unmodifiableMap(resultMap);
        } catch (ExceptionAccessorException e) {
            throw new MBeanException(e, "Error in readUniqueExceptionTypeNames");
        }
    }

    @Override
    public Map<String, List<String>> readExceptionMessageDetailsByThrowableTypeName(
            final String throwableTypeName) throws MBeanException
    {
        try {
            final Map<String, Collection<ExceptionMessageDetails>> result =
                    exceptionAccessor.readExceptionMessageDetailsByThrowableTypeName(throwableTypeName);

            final Map<String, List<String>> resultMap = new HashMap<>();
            if (result != null) {
                for (final Map.Entry<String, Collection<ExceptionMessageDetails>> entry : result.entrySet()) {
                    final String exceptionStoreName = entry.getKey();
                    final Collection<ExceptionMessageDetails> exceptionMessageDetailsCollection = entry.getValue();
                    if (exceptionMessageDetailsCollection != null) {
                        final List<String> jsonList = jsonListOf(exceptionMessageDetailsCollection);
                        resultMap.put(exceptionStoreName, jsonList);
                    }
                }
            }

            return Collections.unmodifiableMap(resultMap);
        } catch (ExceptionAccessorException e) {
            throw new MBeanException(e, "Error in readExceptionMessageDetailsByThrowableTypeName");
        }
    }

    @Override
    public Map<String, Boolean> removeExceptionMessageDetailsById(final int id) throws MBeanException {
        checkRemovePermission("removeExceptionMessageDetailsById");

        try {
            final Collection<String> result = exceptionAccessor.removeExceptionMessageDetailsById(id);

            final Map<String, Boolean> resultMap = new HashMap<>();
            if (result != null) {
                for (String exceptionStoreName : result) {
                    resultMap.put(exceptionStoreName, true);
                }
            }

            return Collections.unmodifiableMap(resultMap);
        } catch (ExceptionAccessorException e) {
            throw new MBeanException(e, "Error in removeExceptionMessageDetailsById");
        }
    }

    @Override
    public Map<String, Boolean> removeExceptionMessageDetailsExclusiveBefore(
            final long timestamp) throws MBeanException
    {
        checkRemovePermission("removeExceptionMessageDetailsExclusiveBefore");

        try {
            final Collection<String> result =
                    exceptionAccessor.removeExceptionMessageDetailsExclusiveBefore(timestamp);

            final Map<String, Boolean> resultMap = new HashMap<>();
            if (result != null) {
                for (String exceptionStoreName : result) {
                    resultMap.put(exceptionStoreName, true);
                }
            }

            return Collections.unmodifiableMap(resultMap);
        } catch (ExceptionAccessorException e) {
            throw new MBeanException(e, "Error in removeExceptionMessageDetailsExclusiveBefore");
        }
    }

    @Override
    public Map<String, List<String>> getExceptionListenersByExceptionTypeMap() {
        final Map<String, Collection<ExceptionListener>> serviceResult =
                appbaseService.getExceptionListenersByExceptionTypeMap();

        final Map<String, List<String>> resultMap = new HashMap<>();
        if (serviceResult != null) {
            for (final Map.Entry<String, Collection<ExceptionListener>> entry : serviceResult.entrySet()) {
                final String exceptionStoreName = entry.getKey();
                final Collection<ExceptionListener> exceptionListeners = entry.getValue();
                if (exceptionListeners != null) {
                    final List<String> exceptionListenerNames = new ArrayList<>(exceptionListeners.size());
                    exceptionListeners.forEach(exceptionListener -> {
                        exceptionListenerNames.add(exceptionListener.getClass().getName());
                    });
                    resultMap.put(exceptionStoreName, exceptionListenerNames);
                }
            }
        }

        return Collections.unmodifiableMap(resultMap);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void unregisterExceptionListenerByExceptionType(
            final String exceptionTypeName,
            final String exceptionListenerTypeName) throws MBeanException
    {
        checkUnregisterPermission("unregisterExceptionListenerByExceptionType");

        AppbaseUtil.notBlank(exceptionTypeName, "exceptionTypeName");
        AppbaseUtil.notBlank(exceptionListenerTypeName, "exceptionListenerTypeName");

        final Class<? extends Throwable> exceptionType =
                (Class<? extends Throwable>) classOf(exceptionTypeName);
        final Class<? extends ExceptionListener> exceptionListenerType =
                (Class<? extends ExceptionListener>) classOf(exceptionListenerTypeName);

        appbaseService.unregisterExceptionListenerByExceptionType(exceptionType, exceptionListenerType);
    }

    @Override
    public void clearExceptionListenersByExceptionTypeMap() {
        checkClearPermission("clearExceptionListenersByExceptionTypeMap");

        appbaseService.clearExceptionListenersByExceptionTypeMap();
    }

    @Override
    public List<String> getGlobalExceptionListeners() {
        final List<String> result = new ArrayList<>();

        final Collection<ExceptionListener> globalExceptionListeners = appbaseService.getGlobalExceptionListeners();
        if (globalExceptionListeners != null) {
            globalExceptionListeners.forEach(exceptionListener -> {
                result.add(exceptionListener.getClass().getName());
            });
        }

        return Collections.unmodifiableList(result);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void unregisterGlobalExceptionListener(
            final String exceptionListenerTypeName) throws MBeanException
    {
        checkUnregisterPermission("unregisterGlobalExceptionListener");

        AppbaseUtil.notBlank(exceptionListenerTypeName, "exceptionListenerTypeName");

        final Class<? extends ExceptionListener> exceptionListenerType =
                (Class<? extends ExceptionListener>) classOf(exceptionListenerTypeName);
        appbaseService.unregisterGlobalExceptionListener(exceptionListenerType);
    }

    @Override
    public void clearGlobalExceptionListeners() {
        checkClearPermission("clearGlobalExceptionListeners");

        appbaseService.clearGlobalExceptionListeners();
    }

    @Override
    public Map<String, String> getExceptionStoreFactoryByConfigurationTypeNameMap() {
        final Map<String, ExceptionStoreFactory> serviceResult =
                exceptionAccessor.getExceptionStoreFactoryByConfigurationTypeNameMap();

        final Map<String, String> resultMap = new HashMap<>();
        if (serviceResult != null) {
            serviceResult.forEach((exceptionStoreConfigurationType, exceptionListenerFactory) -> {
                resultMap.put(exceptionStoreConfigurationType, exceptionListenerFactory.getClass().getName());
            });
        }

        return Collections.unmodifiableMap(resultMap);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void unregisterExceptionStoreFactoryByConfigurationType(
            final String exceptionStoreConfigurationTypeName,
            final String exceptionStoreFactoryTypeName) throws MBeanException
    {
        checkUnregisterPermission("unregisterExceptionStoreFactoryByConfigurationType");

        AppbaseUtil.notBlank(exceptionStoreConfigurationTypeName, "exceptionStoreConfigurationTypeName");
        AppbaseUtil.notBlank(exceptionStoreFactoryTypeName, "exceptionStoreFactoryTypeName");

        final Class<? extends ExceptionStoreConfiguration> exceptionStoreConfigurationType =
                (Class<? extends ExceptionStoreConfiguration>) classOf(exceptionStoreConfigurationTypeName);
        final Class<? extends ExceptionStoreFactory<ExceptionStore, ExceptionStoreConfiguration>>
                exceptionStoreFactoryType =
                (Class<? extends ExceptionStoreFactory<ExceptionStore, ExceptionStoreConfiguration>>)
                        classOf(exceptionStoreFactoryTypeName);

        exceptionAccessor.unregisterExceptionStoreFactoryByConfigurationType(
                exceptionStoreConfigurationType, exceptionStoreFactoryType);
    }

    @Override
    public void clearExceptionStoreFactoryByConfigurationTypeMap() {
        checkClearPermission("clearExceptionStoreFactoryByConfigurationTypeMap");

        exceptionAccessor.clearExceptionStoreFactoryByConfigurationTypeMap();
    }

    @Override
    public List<String> getExceptionStores() {
        final Collection<ExceptionStore> exceptionStores = exceptionAccessor.getExceptionStores();

        final List<String> result = new ArrayList<>();
        if (exceptionStores != null) {
            exceptionStores.forEach(exceptionStore -> {
                result.add(exceptionStore.getClass().getName());
            });
        }

        return Collections.unmodifiableList(result);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void unregisterExceptionStore(final String exceptionStoreTypeName) throws MBeanException {
        checkUnregisterPermission("unregisterExceptionStore");

        AppbaseUtil.notBlank(exceptionStoreTypeName, "exceptionStoreTypeName");

        final Class<? extends ExceptionStore> exceptionStoreType =
                (Class<? extends ExceptionStore>) classOf(exceptionStoreTypeName);
        exceptionAccessor.unregisterExceptionStore(exceptionStoreType);
    }

    @Override
    public void clearExceptionStores() {
        checkClearPermission("clearExceptionStores");

        exceptionAccessor.clearExceptionStores();
    }

    private Class<?> classOf(final String typeName) throws MBeanException {
        try {
            return Class.forName(typeName);
        } catch (ClassNotFoundException cnfe) {
            throw new MBeanException(cnfe, "Error finding class " + typeName);
        }
    }

    private ExceptionMessageDetails getExceptionMessageDetails(
            final CompletableFuture<ExceptionMessageDetails> exceptionMessageDetailsFuture,
            final String methodName) throws MBeanException
    {
        try {
            return exceptionMessageDetailsFuture.get();
        } catch (InterruptedException | ExecutionException iee) {
            throw new MBeanException(iee, "Error in " + methodName);
        }
    }

    private void getVoid(final CompletableFuture<Void> voidFuture, final String methodName) throws MBeanException {
        try {
            voidFuture.get();
        } catch (InterruptedException | ExecutionException iee) {
            throw new MBeanException(iee, "Error in " + methodName);
        }
    }

    private Collection<String> getStringCollection(final CompletableFuture<Collection<String>> stringCollectionFuture,
                                                   final String methodName) throws MBeanException
    {
        try {
            return stringCollectionFuture.get();
        } catch (InterruptedException | ExecutionException iee) {
            throw new MBeanException(iee, "Error in " + methodName);
        }
    }

    private Collection<ExceptionMessageDetails> getExceptionMessageDetailsCollection(
            final CompletableFuture<Collection<ExceptionMessageDetails>> exceptionMessageDetailsFuture,
            final String methodName) throws MBeanException
    {
        try {
            return exceptionMessageDetailsFuture.get();
        } catch (InterruptedException | ExecutionException iee) {
            throw new MBeanException(iee, "Error in " + methodName);
        }
    }

    private List<String> jsonListOf(
            final Collection<ExceptionMessageDetails> exceptionMessageDetailsCollection) throws MBeanException
    {
        final List<String> resultList = new ArrayList<>();

        for (final ExceptionMessageDetails exceptionMessageDetails : exceptionMessageDetailsCollection) {
            final String json = jsonOf(exceptionMessageDetails);
            resultList.add(json);
        }

        return Collections.unmodifiableList(resultList);
    }

    /**
     * Serialises {@code exceptionMessageDetails} as its {@link RedactedExceptionMessageDetails}
     * view rather than the entity itself (Lens 4 / L4-04, CWE-200, CWE-209): the entity's
     * {@code throwableMessage}/{@code stackTrace}/{@code context} fields can embed whatever a
     * caller or a downstream library put into an exception, and this value is exposed through the
     * (not-necessarily-authenticated) exception-reporting MXBean operations consumed by the
     * servlet, offloaded, and native {@code /exception} faces alike.
     *
     * @param exceptionMessageDetails the entity to serialise
     * @return the redacted JSON view
     * @throws MBeanException wrapping a {@link JsonProcessingException}, if serialisation fails
     */
    private String jsonOf(final ExceptionMessageDetails exceptionMessageDetails) throws MBeanException {
        try {
            return JsonUtil
                    .getJsonMapper()
                    .writerFor(RedactedExceptionMessageDetails.class)
                    .writeValueAsString(RedactedExceptionMessageDetails.from(exceptionMessageDetails));
        } catch (JsonProcessingException jpe) {
            throw new MBeanException(jpe, "Error in JSON serialization of exception message details");
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
}
