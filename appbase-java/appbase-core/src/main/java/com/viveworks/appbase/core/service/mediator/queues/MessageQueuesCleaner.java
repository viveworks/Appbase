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

package com.viveworks.appbase.core.service.mediator.queues;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.mediator.Schedulable;
import com.viveworks.appbase.core.service.mediator.message.Message;

import java.util.Collection;

/**
 * A cleaner task for the message queues.
 *
 * <b>Expected contract</b>
 *
 * <p>
 *     An implementation of this interface is meant to remove expired message from the mediator message queues. If a
 *     message has not expired, it calls the message handler if one exists, else re-queues it back.
 * </p>
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public interface MessageQueuesCleaner extends Schedulable<Void> {
    /**
     * Remove expired message.
     */
    void clean();


    /**
     * Get a read only view of removed message.
     *
     * @return Read only copy of all removed message
     */
    Collection<Message> getRemovedMessages();


    /**
     * Clear the removed message collection. This can be used by a manager task that can handle removed message
     * using {@link #getRemovedMessages()} and then clear this collection.
     */
    void resetRemovedMessages();


    /**
     * Get a read only view of exceptions encountered during an operation. Since, there is no logger used in this
     * module, exceptions are collected. This method can be used to deal with the exceptions that occurred as necessary.
     *
     * @return Read only copy of all exceptions
     */
    Collection<AppbaseRuntimeException> getAppbaseRuntimeExceptions();


    /**
     * Many metric systems clean counters (to 0) after reporting. This method can be used for that purpose, to clean
     * <code>appbaseRuntimeExceptions</code>
     */
    void resetAppbaseRuntimeExceptions();
}
