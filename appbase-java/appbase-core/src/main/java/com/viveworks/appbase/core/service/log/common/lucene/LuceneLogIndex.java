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

package com.viveworks.appbase.core.service.log.common.lucene;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.viveworks.appbase.core.service.log.accessor.LogRecord;
import com.viveworks.appbase.core.service.log.common.TurboLog;
import com.viveworks.appbase.core.util.lucene.LuceneService;

/**
 * Index of logs in <code>Apache Lucene</code>.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public interface LuceneLogIndex {
    /**
     * Index a log event.
     *
     * @param loggingEvent Log event
     */
    void indexLog(ILoggingEvent loggingEvent);


    /**
     * Index a log event as seen by a {@link ch.qos.logback.classic.turbo.TurboFilter}.
     *
     * @param turboLog Turbo log
     */
    void indexTurboLog(TurboLog turboLog);


    /**
     * Index a {@link LogRecord}.
     *
     * @param logRecord {@link LogRecord} to index
     */
    void indexLogRecord(LogRecord logRecord);


    /**
     * Get the {@link LuceneService} that this index is using.
     *
     * @return Lucene service
     */
    LuceneService getLuceneService();


    /**
     * Get the {@link LuceneLogIndexConfiguration} used to configure the index.
     *
     * @return Configuration
     */
    LuceneLogIndexConfiguration getLuceneLogIndexConfiguration();
}
