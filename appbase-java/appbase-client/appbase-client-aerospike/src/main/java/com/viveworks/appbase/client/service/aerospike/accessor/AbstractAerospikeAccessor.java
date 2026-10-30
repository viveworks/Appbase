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

package com.viveworks.appbase.client.service.aerospike.accessor;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.Bin;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.RecordExistsAction;
import com.aerospike.client.policy.WritePolicy;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.viveworks.appbase.client.service.aerospike.AerospikeClientService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.jackson.JsonUtil;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Timer;

import javax.annotation.Nullable;

/**
 * Provides basic Aerospike operations.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public abstract class AbstractAerospikeAccessor {
    private final AerospikeClientService aerospikeClientService;
    private final String namespace;
    private final String setName;

    private final Timer deleteTimer;
    private final Timer readTimer;
    private final Timer createOnlyTimer;
    private final Timer createOrUpdateTimer;
    private final Timer replaceOnlyTimer;
    private final Timer createOrReplaceTimer;
    private final Timer updateOnlyTimer;

    public AbstractAerospikeAccessor(
            final AerospikeClientService aerospikeClientService,
            final MetricService metricService,
            final String namespace,
            final String setName)
    {
        AppbaseUtil.notNull(aerospikeClientService, "aerospikeClientService");
        AppbaseUtil.notBlank(namespace, "namespace");
        AppbaseUtil.notBlank(setName, "setName");

        this.aerospikeClientService = aerospikeClientService;
        this.namespace = namespace;
        this.setName = setName;

        final MetricRegistry metricRegistry = metricService.getMetricRegistry();
        this.deleteTimer = metricRegistry.timer(metricService.name(getClass(),  "deleteTimer"));
        this.readTimer = metricRegistry.timer(metricService.name(getClass(),  "readTimer"));
        this.createOnlyTimer = metricRegistry.timer(metricService.name(getClass(), "createOnlyTimer"));
        this.createOrUpdateTimer = metricRegistry.timer(metricService.name(getClass(), "createOrUpdateTimer"));
        this.replaceOnlyTimer = metricRegistry.timer(metricService.name(getClass(), "replaceOnlyTimer"));
        this.createOrReplaceTimer = metricRegistry.timer(metricService.name(getClass(), "replaceOnlyTimer"));
        this.updateOnlyTimer = metricRegistry.timer(metricService.name(getClass(), "updateOnlyTimer"));
    }

    protected boolean delete(final String key) throws AerospikeAccessorException {
        AppbaseUtil.notBlank(key, "key");

        final Timer.Context timer = deleteTimer.time();
        try {
            return aerospikeClient().delete(writePolicy(), aerospikeKey(key));
        } catch (AerospikeException ae) {
            throw new AerospikeAccessorException(ae);
        } finally {
            timer.stop();
        }
    }

    @Nullable
    protected <T> T read(
            final String key,
            final String bin,
            final Class<T> type) throws AerospikeAccessorException
    {
        AppbaseUtil.notBlank(key, "key");
        AppbaseUtil.notBlank(bin, "bin");
        AppbaseUtil.notNull(type, "type");

        final Timer.Context timer = readTimer.time();
        final Record record;
        try {
            record = aerospikeClient().get(policy(), aerospikeKey(key), bin);

            if (record != null) {
                final String json = record.getString(bin);
                if (!AppbaseUtil.isBlank(json)) {
                    return deserialize(json, type);
                }
            }
        }  catch (AerospikeException ae) {
            throw new AerospikeAccessorException(ae);
        } finally {
            timer.stop();
        }

        return null;
    }

    protected <T> void createOnly(
            final String key,
            final String bin,
            final T object,
            final Class<T> type) throws AerospikeAccessorException
    {
        AppbaseUtil.notBlank(key, "key");
        AppbaseUtil.notBlank(bin, "bin");
        AppbaseUtil.notNull(object, "object");
        AppbaseUtil.notNull(type, "type");

        final Timer.Context timer = createOnlyTimer.time();
        try {
            put(aerospikeKey(key), bin, writePolicy(RecordExistsAction.CREATE_ONLY), object, type);
        } finally {
            timer.stop();
        }
    }

    protected <T> void createOrUpdate(
            final String key,
            final String bin,
            final T object,
            final Class<T> type) throws AerospikeAccessorException
    {
        AppbaseUtil.notBlank(key, "key");
        AppbaseUtil.notBlank(bin, "bin");
        AppbaseUtil.notNull(object, "object");
        AppbaseUtil.notNull(type, "type");

        final Timer.Context timer = createOrUpdateTimer.time();
        try {
            put(aerospikeKey(key), bin, writePolicy(RecordExistsAction.UPDATE), object, type);
        } finally {
            timer.stop();
        }
    }

    protected <T> void replaceOnly(
            final String key,
            final String bin,
            final T object,
            final Class<T> type) throws AerospikeAccessorException
    {
        AppbaseUtil.notBlank(key, "key");
        AppbaseUtil.notBlank(bin, "bin");
        AppbaseUtil.notNull(object, "object");
        AppbaseUtil.notNull(type, "type");

        final Timer.Context timer = replaceOnlyTimer.time();
        try {
            put(aerospikeKey(key), bin, writePolicy(RecordExistsAction.REPLACE_ONLY), object, type);
        } finally {
            timer.stop();
        }
    }

    protected <T> void createOrReplace(
            final String key,
            final String bin,
            final T object,
            final Class<T> type) throws AerospikeAccessorException
    {
        AppbaseUtil.notBlank(key, "key");
        AppbaseUtil.notBlank(bin, "bin");
        AppbaseUtil.notNull(object, "object");
        AppbaseUtil.notNull(type, "type");

        final Timer.Context timer = createOrReplaceTimer.time();
        try {
            put(aerospikeKey(key), bin, writePolicy(RecordExistsAction.REPLACE), object, type);
        } finally {
            timer.stop();
        }
    }

    protected <T> void updateOnly(
            final String key,
            final String bin,
            final T object,
            final Class<T> type) throws AerospikeAccessorException
    {
        AppbaseUtil.notBlank(key, "key");
        AppbaseUtil.notBlank(bin, "bin");
        AppbaseUtil.notNull(object, "object");
        AppbaseUtil.notNull(type, "type");

        final Timer.Context timer = updateOnlyTimer.time();
        try {
            put(aerospikeKey(key), bin, writePolicy(RecordExistsAction.UPDATE_ONLY), object, type);
        } finally {
            timer.stop();
        }
    }

    private  <T> void put(
            final Key key,
            final String bin,
            final WritePolicy writePolicy,
            final T object,
            final Class<T> type) throws AerospikeAccessorException
    {
        AppbaseUtil.notNull(key, "key");
        AppbaseUtil.notBlank(bin, "bin");
        AppbaseUtil.notNull(writePolicy, "writePolicy");
        AppbaseUtil.notNull(object, "object");
        AppbaseUtil.notNull(type, "type");

        final String json = json(object, type);
        final Bin aerospikeBin = new Bin(bin, json);

        try {
            aerospikeClient().put(writePolicy, key, aerospikeBin);
        } catch (AerospikeException ae) {
            throw new AerospikeAccessorException(ae);
        }
    }

    private WritePolicy writePolicy(final RecordExistsAction recordExistsAction) {
        final WritePolicy writePolicy = writePolicy();
        writePolicy.recordExistsAction = recordExistsAction;

        return writePolicy;
    }

    private Key aerospikeKey(final String key) {
        return new Key(namespace, setName, key);
    }

    private WritePolicy writePolicy() {
        return aerospikeClient().getWritePolicyDefault();
    }

    private Policy policy() {
        return aerospikeClient().getReadPolicyDefault();
    }

    private <T> String json(final T object, final Class<T> type) throws AerospikeAccessorException {
        AppbaseUtil.notNull(object, "object");
        AppbaseUtil.notNull(type, "type");

        try {
            return JsonUtil
                    .getJsonMapper()
                    .writerFor(type)
                    .writeValueAsString(object);
        } catch (JsonProcessingException jpe) {
            throw new AerospikeAccessorException(jpe);
        }
    }

    private <T> T deserialize(final String json, final Class<T> type) throws AerospikeAccessorException {
        AppbaseUtil.notBlank(json, "json");
        AppbaseUtil.notNull(type, "type");

        try {
            return JsonUtil
                    .getJsonMapper()
                    .readerFor(type)
                    .readValue(json);
        } catch (JsonProcessingException jpe) {
            throw new AerospikeAccessorException(jpe);
        }
    }

    private IAerospikeClient aerospikeClient() {
        return aerospikeClientService
                .getClientIntegrationService()
                .getClient();
    }
}
