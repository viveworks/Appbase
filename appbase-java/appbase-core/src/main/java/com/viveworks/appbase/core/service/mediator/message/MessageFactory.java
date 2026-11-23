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

package com.viveworks.appbase.core.service.mediator.message;

import com.viveworks.appbase.core.service.AppbaseService;
import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.util.AppbaseUtil;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Map;

/**
 * A factory for core message.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@ThreadSafe
public final class MessageFactory {
    private MessageFactory() {
    }

    /**
     * Retrieves a {@link Message} from the mediator's pool and populates an {@link ExceptionMessage}.
     *
     * @param mediatorService Mediator service
     * @param source Exception message's source
     * @param message Exception message's message
     * @param threadName Name of the thread where the throwable originated
     * @param throwable Exception message's throwable
     * @return Message with populated throwable message
     */
    public static Message exceptionMessage(final MediatorService mediatorService,
                                           final Class<?> source,
                                           final String message,
                                           final String threadName,
                                           final Throwable throwable)
    {
        return exceptionMessage(mediatorService, source, message, threadName, throwable, null);
    }

    /**
     * Retrieves a {@link Message} from the mediator's pool and populates an {@link ExceptionMessage}.
     *
     * @param mediatorService Mediator service
     * @param source Exception message's source
     * @param message Exception message's message
     * @param threadName Name of the thread where the throwable originated
     * @param throwable Exception message's throwable
     * @param context An optional context
     * @return Message with populated throwable message
     */
    public static Message exceptionMessage(final MediatorService mediatorService,
                                           final Class<?> source,
                                           final String message,
                                           final String threadName,
                                           final Throwable throwable,
                                           @Nullable final Map<String, String> context)
    {
        AppbaseUtil.notNull(mediatorService, "mediatorService");
        AppbaseUtil.notNull(source, "source");
        AppbaseUtil.notNull(message, "message");
        AppbaseUtil.notNull(threadName, "threadName");
        AppbaseUtil.notNull(throwable, "throwable");

        final Message messageFromPool = mediatorService.getMediator().getMessageFromPool();
        messageFromPool.setTimestamp(System.currentTimeMillis());
        messageFromPool.setMessageType(MessageType.EXCEPTION);

        final ExceptionMessage exceptionMessage = messageFromPool.getExceptionMessage();
        exceptionMessage.setSourceType(source);
        exceptionMessage.setMessage(message);
        exceptionMessage.setThreadName(threadName);
        exceptionMessage.setThrowable(throwable);
        exceptionMessage.setTimestamp(System.currentTimeMillis());

        if (context != null) {
            context.forEach((key, value) -> {
                if (key != null && value != null) {
                    exceptionMessage.addContext(key, value);
                }
            });
        }

        return messageFromPool;
    }


    /**
     * Retrieves a {@link Message} from the mediator's pool and populates an {@link ServiceStatusMessage}.
     *
     *
     * @param mediatorService Mediator service
     * @param serviceType Service type
     * @param serviceStatus Service status
     * @return Message with populated service status message
     */
    public static Message serviceStatusMessage(final MediatorService mediatorService,
                                               final Class<? extends AppbaseService> serviceType,
                                               final ServiceStatus serviceStatus)
    {
        return serviceStatusMessage(mediatorService, serviceType, serviceStatus, null);
    }


    /**
     * Retrieves a {@link Message} from the mediator's pool and populates an {@link ServiceStatusMessage}.
     *
     *
     * @param mediatorService Mediator service
     * @param serviceType Service type
     * @param serviceStatus Service status
     * @param context An optional context
     * @return Message with populated service status message
     */
    public static Message serviceStatusMessage(final MediatorService mediatorService,
                                               final Class<? extends AppbaseService> serviceType,
                                               final ServiceStatus serviceStatus,
                                               @Nullable final Map<String, String> context)
    {
        AppbaseUtil.notNull(mediatorService, "mediatorService");
        AppbaseUtil.notNull(serviceType, "serviceType");
        AppbaseUtil.notNull(serviceStatus, "serviceStatus");

        final Message messageFromPool = mediatorService.getMediator().getMessageFromPool();
        messageFromPool.setTimestamp(System.currentTimeMillis());
        messageFromPool.setMessageType(MessageType.SERVICE_STATUS);

        final ServiceStatusMessage serviceStatusMessage = messageFromPool.getServiceStatusMessage();
        serviceStatusMessage.setServiceType(serviceType);
        serviceStatusMessage.setServiceStatus(serviceStatus);
        serviceStatusMessage.setTimestamp(System.currentTimeMillis());

        if (context != null) {
            context.forEach((key, value) -> {
                if (key != null && value != null) {
                    serviceStatusMessage.addContext(key, value);
                }
            });
        }

        return messageFromPool;
    }
}
