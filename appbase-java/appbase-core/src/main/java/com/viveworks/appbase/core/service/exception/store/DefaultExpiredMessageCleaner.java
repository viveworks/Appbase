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

package com.viveworks.appbase.core.service.exception.store;

import com.viveworks.appbase.core.service.exception.accessor.ExceptionAccessor;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.mediator.message.Message;
import com.viveworks.appbase.core.service.mediator.message.MessageFactory;
import com.viveworks.appbase.core.service.mediator.queues.MediatorException;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Default implementation of {@link ExpiredMessageCleaner}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@ThreadSafe
public class DefaultExpiredMessageCleaner implements ExpiredMessageCleaner {
    private final Logger logger;
    private final long exclusiveRemoveBeforeMillis;
    private final ExceptionAccessor exceptionAccessor;
    private final MediatorService mediatorService;

    public DefaultExpiredMessageCleaner(
            final LogService logService,
            final Long removeBeforeTimePeriod,
            final TimeUnit removeBeforeTimePeriodUnit,
            final ExceptionAccessor exceptionAccessor,
            final MediatorService mediatorService)
    {
        AppbaseUtil.notNull(logService, "logService");
        this.logger = logService.getLogger(getClass());

        AppbaseUtil.notNull(removeBeforeTimePeriod, "removeBeforeTimePeriod");
        AppbaseUtil.notNull(removeBeforeTimePeriodUnit, "removeBeforeTimePeriodUnit");
        AppbaseUtil.notNull(exceptionAccessor, "exceptionAccessor");
        AppbaseUtil.notNull(mediatorService, "mediatorService");

        this.exclusiveRemoveBeforeMillis = removeBeforeTimePeriodUnit.toMillis(removeBeforeTimePeriod);
        this.exceptionAccessor = exceptionAccessor;
        this.mediatorService = mediatorService;
    }

    @Override
    @SuppressFBWarnings({"UAC_UNNECESSARY_API_CONVERSION_DATE_TO_INSTANT"})
    public void clean() {
        final Date nowDate = new Date(System.currentTimeMillis());
        final long removeBeforeTimestamp = nowDate
                .toInstant()
                .minusMillis(exclusiveRemoveBeforeMillis)
                .toEpochMilli();
        final Map<String, CompletableFuture<Void>> completableFutureMap =
                exceptionAccessor.asyncRemoveExceptionMessageDetailsExclusiveBefore(removeBeforeTimestamp);
        if (completableFutureMap != null) {
            completableFutureMap.forEach((exceptionStoreName, completableFuture) -> {
                completableFuture.whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        final String exceptionMessage = "Error cleaning expired message in " + exceptionStoreName;
                        final Message message = MessageFactory.exceptionMessage(
                                mediatorService,
                                getClass(),
                                exceptionMessage,
                                AppbaseUtil.threadName(),
                                throwable,
                                null);
                        try {
                            mediatorService.getMediator().asyncHandleMessage(message, true);
                        } catch (MediatorException me) {
                            logger.error("Error sending message to mediator: {}", message, me);
                        }
                    }
                });
            });
        }
    }
}
