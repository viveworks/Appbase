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
package com.viveworks.appbase.service.supportmessage.store;

import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.registry.ServiceRegistry;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.AbstractStatelessAppbaseService;
import com.viveworks.appbase.service.AppbaseServiceUtil;
import com.viveworks.appbase.service.AppbaseStoreException;
import com.viveworks.appbase.service.supportmessage.SupportMessage;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Timer;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.mapper.reflect.BeanMapper;

import java.util.Collection;
import java.util.Optional;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class JdbiSupportMessageStore
        extends AbstractStatelessAppbaseService<JdbiSupportMessageStoreConfiguration>
        implements SupportMessageStore
{
    private final Handle jdbiHandle;
    private final Timer createSupportMessageTimer;
    private final Timer readPageTimer;
    private final Timer readByMessageIdTimer;
    private final Timer existsByMessageIdTimer;
    private final Timer updateMessageTimer;
    private final Timer deleteByMessageIdTimer;

    public JdbiSupportMessageStore(
            final LogService logService,
            final MetricService metricService,
            final MediatorService mediatorService,
            final ServiceRegistry serviceRegistry,
            final Handle jdbiHandle,
            final JdbiSupportMessageStoreConfiguration serviceConfiguration)
    {
        super(logService, metricService, mediatorService, serviceRegistry, serviceConfiguration);

        AppbaseUtil.notNull(jdbiHandle, "jdbiHandle");
        this.jdbiHandle = jdbiHandle;

        final MetricRegistry metricRegistry = metricService.getMetricRegistry();
        this.createSupportMessageTimer =
                metricRegistry.timer(metricService.name(getClass(), "createUserAccountTimer"));
        this.readPageTimer =
                metricRegistry.timer(metricService.name(getClass(), "readPageTimer"));
        this.readByMessageIdTimer =
                metricRegistry.timer(metricService.name(getClass(), "readByMessageIdTimer"));
        this.existsByMessageIdTimer =
                metricRegistry.timer(metricService.name(getClass(), "existsByMessageIdTimer"));
        this.updateMessageTimer =
                metricRegistry.timer(metricService.name(getClass(), "updateMessageTimer"));
        this.deleteByMessageIdTimer =
                metricRegistry.timer(metricService.name(getClass(), "deleteByMessageIdTimer"));
    }

    @Override
    protected void registerService() {
        serviceRegistry.registerAppbaseService(SupportMessageStore.class, this);
        serviceRegistry.registerAppbaseService(JdbiSupportMessageStore.class, this);
    }

    @Override
    protected void shutdownImpl() {
        jdbiHandle.close();
    }

    @Override
    public int createSupportMessage(final SupportMessage supportMessage) throws AppbaseStoreException {
        AppbaseUtil.notNull(supportMessage, "supportMessage");

        final Timer.Context timer = createSupportMessageTimer.time();
        try {
            if (getConfiguration().isManualValidation()) {
                AppbaseUtil.validate(new SupportMessage.Validator(), supportMessage);
            }


            return jdbiHandle
                    .createUpdate("INSERT INTO " + getConfiguration().getSupportMessageTableName() +
                            "(messageId, email, message, appbaseUserId, status, " +
                            "deleted, creationTimestamp, lastAccessTimestamp) " +
                            "VALUES (" +
                            ":messageId, :email, :message, :appbaseUserId, :status, " +
                            ":deleted, :creationTimestamp, :lastAccessTimestamp" +
                            ")")
                    .bind("messageId", supportMessage.getMessageId())
                    .bind("email", supportMessage.getEmail())
                    .bind("message", supportMessage.getMessage())
                    .bind("appbaseUserId", supportMessage.getAppbaseUserId())
                    .bind("status", supportMessage.getStatus())
                    .bind("deleted", AppbaseServiceUtil.sqlTinyInt(supportMessage.getDeleted()))
                    .bind("creationTimestamp", supportMessage.getCreationTimestamp())
                    .bind("lastAccessTimestamp", supportMessage.getLastAccessTimestamp())
                    .execute();
        } catch (Exception e) {
            throw new AppbaseStoreException(e);
        } finally {
            timer.stop();
        }
    }

    @Override
    public Collection<SupportMessage> readPage(final int offset, final int limit) throws AppbaseStoreException {
        AppbaseUtil.positiveOrZero(offset, "offset");
        AppbaseUtil.positiveOrZero(limit, "limit");

        final Timer.Context timer = readPageTimer.time();
        try {
            return jdbiHandle
                    .select("SELECT * FROM " + getConfiguration().getSupportMessageTableName() +
                            " LIMIT :limit OFFSET :offset")
                    .bind("offset", offset)
                    .bind("limit", limit)
                    .map(BeanMapper.of(SupportMessage.class))
                    .list();
        } catch (Exception e) {
            throw new AppbaseStoreException(e);
        } finally {
            timer.stop();
        }
    }

    @Override
    public Optional<SupportMessage> readByMessageId(final String messageId) throws AppbaseStoreException {
        AppbaseUtil.notBlank(messageId, "messageId");

        final Timer.Context timer = readByMessageIdTimer.time();
        try {
            return jdbiHandle
                    .select("SELECT * FROM " + getConfiguration().getSupportMessageTableName() +
                            " WHERE messageId=:messageId")
                    .bind("messageId", messageId)
                    .map(BeanMapper.of(SupportMessage.class))
                    .findOne();
        } catch (Exception e) {
            throw new AppbaseStoreException(e);
        } finally {
            timer.stop();
        }
    }

    @Override
    public boolean existsByMessageId(final String messageId) throws AppbaseStoreException {
        AppbaseUtil.notBlank(messageId, "messageId");

        final Timer.Context timer = existsByMessageIdTimer.time();
        try {
            return jdbiHandle
                    .select("SELECT messageId FROM " + getConfiguration().getSupportMessageTableName() +
                            " WHERE messageId=:messageId")
                    .bind("messageId", messageId)
                    .mapTo(String.class)
                    .findOne()
                    .isPresent();
        } catch (Exception e) {
            throw new AppbaseStoreException(e);
        } finally {
            timer.stop();
        }
    }

    @Override
    public int updateMessage(final String messageId, final String message) throws AppbaseStoreException {
        AppbaseUtil.notBlank(messageId, "messageId");
        AppbaseUtil.notNull(message, "message");

        final Timer.Context timer = updateMessageTimer.time();
        try {
            return jdbiHandle
                    .createUpdate("UPDATE " + getConfiguration().getSupportMessageTableName() + " SET " +
                            " message=:message WHERE messageId=:messageId")
                    .bind("messageId", messageId)
                    .bind("message", message)
                    .execute();
        } catch (Exception e) {
            throw new AppbaseStoreException(e);
        } finally {
            timer.stop();
        }
    }

    @Override
    public int updateStatus(final String messageId, final String status) throws AppbaseStoreException {
        AppbaseUtil.notBlank(messageId, "messageId");

        final Timer.Context timer = updateMessageTimer.time();
        try {
            return jdbiHandle
                    .createUpdate("UPDATE " + getConfiguration().getSupportMessageTableName() + " SET " +
                            " status=:status WHERE messageId=:messageId")
                    .bind("messageId", messageId)
                    .bind("status", status)
                    .execute();
        } catch (Exception e) {
            throw new AppbaseStoreException(e);
        } finally {
            timer.stop();
        }
    }

    @Override
    public boolean deleteByMessageId(final String messageId) throws AppbaseStoreException {
        AppbaseUtil.notBlank(messageId, "messageId");

        final Timer.Context timer = deleteByMessageIdTimer.time();
        try {
            final int deleted = jdbiHandle
                    .createUpdate("DELETE FROM " + getConfiguration().getSupportMessageTableName() +
                            " WHERE messageId=:messageId")
                    .bind("messageId", messageId)
                    .execute();
            return deleted > 0;
        } catch (Exception e) {
            throw new AppbaseStoreException(e);
        } finally {
            timer.stop();
        }
    }
}
