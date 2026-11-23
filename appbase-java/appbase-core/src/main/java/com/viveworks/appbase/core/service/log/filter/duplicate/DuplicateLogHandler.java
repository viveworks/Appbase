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

package com.viveworks.appbase.core.service.log.filter.duplicate;

import com.viveworks.appbase.core.service.log.filter.burst.LogEntry;

import java.util.Map;

/**
 * Handler for duplicate logs. An example use case is the {@link DefaultDuplicateLogHandler}, which is used to
 * cleanup the duplicate logs cached by {@link DefaultLogDeduplicator}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public interface DuplicateLogHandler {
    /**
     * Handle logs with duplicate formatted message.
     *
     * @param loggerName Logger name
     * @param formattedMessage Duplicate formatted message
     * @param logEntryByTimestampMap Log entry for each duplicate log by occurrence timestamp
     */
    void duplicateFormattedMessage(String loggerName,
                                   String formattedMessage,
                                   Map<Long, LogEntry> logEntryByTimestampMap);


    /**
     * Handle logs with duplicate throwable type / class name.
     *
     * @param loggerName Logger name
     * @param throwableTypeName Duplicate throwable type / class name
     * @param logEntryByTimestampMap Log entry for each duplicate log by occurrence timestamp
     */
    void duplicateThrowableTypeName(String loggerName,
                                    String throwableTypeName,
                                    Map<Long, LogEntry> logEntryByTimestampMap);


    /**
     * Handle logs with duplicate throwable type / class name and throwable message.
     *
     * @param loggerName Logger name
     * @param throwableTypeName Duplicate throwable type / class name
     * @param throwableMessage Duplicate throwable message
     * @param logEntryByTimestampMap Log entry for each duplicate log by occurrence timestamp
     */
    void duplicateThrowableMessage(String loggerName,
                                   String throwableTypeName,
                                   String throwableMessage,
                                   Map<Long, LogEntry> logEntryByTimestampMap);
}
