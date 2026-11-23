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

package com.viveworks.appbase.core.util.lucene;

import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.search.Query;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Timer;
import org.slf4j.Logger;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Default implementation of {@link IndexCleaner}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@ThreadSafe
public class TimestampBasedIndexCleaner implements IndexCleaner {
    private final Logger logger;
    private final LuceneService luceneService;
    private final String creationTimestampFieldName;
    private final long removeBeforeTimePeriod;
    private final TimeUnit removeBeforeTimePeriodUnit;
    private final Timer runTimer;
    private final Counter errorCounter;
    private final AtomicLong deletedSequenceNumber;
    private final ReentrantLock runLock;

    public TimestampBasedIndexCleaner(
            final LogService logService,
            final MetricService metricService,
            final LuceneService luceneService,
            final TimestampBasedIndexCleanerConfiguration timestampBasedIndexCleanerConfiguration)
    {
        AppbaseUtil.notNull(logService, "logService");
        AppbaseUtil.notNull(metricService, "metricService");
        AppbaseUtil.notNull(luceneService, "luceneService");
        AppbaseUtil.notNull(timestampBasedIndexCleanerConfiguration, "timestampBasedIndexCleanerConfiguration");

        this.logger = logService.getLogger(getClass());
        this.creationTimestampFieldName = timestampBasedIndexCleanerConfiguration.getCreationTimestampFieldName();
        this.removeBeforeTimePeriod = timestampBasedIndexCleanerConfiguration.getRemoveBeforeTimePeriod();
        this.removeBeforeTimePeriodUnit = timestampBasedIndexCleanerConfiguration.getRemoveBeforeTimePeriodUnit();
        this.luceneService = luceneService;

        runTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "run", "timer"));
        errorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "error", "counter"));

        deletedSequenceNumber = new AtomicLong(0);
        final Gauge<Long> deletedSequenceNumberGauge = deletedSequenceNumber::get;
        metricService.getMetricRegistry()
                .register(metricService.name(getClass(), "deletedSequenceNumber", "gauge"), deletedSequenceNumberGauge);

        runLock = new ReentrantLock();
    }

    @Override
    @SuppressFBWarnings({"UAC_UNNECESSARY_API_CONVERSION_DATE_TO_INSTANT"})
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void clean() {
        if (!runLock.tryLock()) {
            return;
        }
        try {
            final Timer.Context context = runTimer.time();
            try {
                luceneService.synchronizeIndex();
                final Date nowDate = new Date(System.currentTimeMillis());

                final long removeBeforeMillis = removeBeforeTimePeriodUnit.toMillis(removeBeforeTimePeriod);
                final long removeBeforeTimestamp = nowDate
                        .toInstant()
                        .minusMillis(removeBeforeMillis)
                        .toEpochMilli();

                final Query rangeQuery = LongPoint.newRangeQuery(
                        creationTimestampFieldName, Long.MIN_VALUE, Math.addExact(removeBeforeTimestamp, -1));
                final long sequenceNumber = luceneService.getIndexWriter().deleteDocuments(rangeQuery);
                deletedSequenceNumber.set(sequenceNumber);
                luceneService.getIndexWriter().commit();
            } finally {
                context.stop();
            }
        } catch (Exception ex) {
            logger.error("Error while cleaning the index", ex);
            errorCounter.inc();
        } finally {
            runLock.unlock();
        }
    }

    @Override
    public long getDeletedSequenceNumber() {
        return deletedSequenceNumber.get();
    }
}
