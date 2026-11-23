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

package com.viveworks.appbase.core.service.log.filter.event.listener.lucene;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.viveworks.appbase.core.service.log.common.TurboLog;
import com.viveworks.appbase.core.service.log.common.lucene.LuceneLogIndex;
import com.viveworks.appbase.core.service.log.filter.event.LogEventFilterListener;
import com.viveworks.appbase.core.service.log.filter.event.TurboLogEventFilterListener;
import com.viveworks.appbase.core.util.AppbaseUtil;

import javax.annotation.concurrent.ThreadSafe;

/**
 * {@link LogEventFilterListener} used to index log message for full-text search using
 * <code>Apache Lucene</code>.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@ThreadSafe
public class LuceneLogAndTurboLogEventFilterListener implements LogEventFilterListener, TurboLogEventFilterListener {
    private final LuceneLogIndex luceneLogIndex;

    public LuceneLogAndTurboLogEventFilterListener(final LuceneLogIndex luceneLogIndex) {
        AppbaseUtil.notNull(luceneLogIndex, "luceneLogIndex");

        this.luceneLogIndex = luceneLogIndex;
    }

    @Override
    public void onTurboLog(final TurboLog turboLog) {
        luceneLogIndex.indexTurboLog(turboLog);
    }

    @Override
    public void onLog(final ILoggingEvent loggingEvent) {
        luceneLogIndex.indexLog(loggingEvent);
    }
}
