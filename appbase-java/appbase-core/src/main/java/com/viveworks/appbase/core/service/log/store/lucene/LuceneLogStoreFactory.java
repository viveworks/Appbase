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

package com.viveworks.appbase.core.service.log.store.lucene;

import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.log.common.lucene.LuceneLogIndex;
import com.viveworks.appbase.core.service.log.store.LogStoreFactory;
import com.viveworks.appbase.core.service.log.store.LogStoreFactoryException;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.util.AppbaseUtil;

import javax.annotation.concurrent.ThreadSafe;
import java.util.concurrent.ExecutorService;

/**
 * {@link LogStoreFactory} for {@link LuceneLogStore}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@ThreadSafe
public class LuceneLogStoreFactory implements LogStoreFactory<LuceneLogStore, LuceneLogStoreConfiguration> {
    private final LogService logService;
    private final MetricService metricService;
    private final LuceneLogIndex luceneLogIndex;
    private final ExecutorService executorService;

    public LuceneLogStoreFactory(final LogService logService,
                                 final MetricService metricService,
                                 final LuceneLogIndex luceneLogIndex,
                                 final ExecutorService executorService)
    {
        AppbaseUtil.notNull(logService, "logService");
        AppbaseUtil.notNull(metricService, "metricService");
        AppbaseUtil.notNull(luceneLogIndex, "luceneLogIndex");
        AppbaseUtil.notNull(executorService, "executorService");

        this.logService = logService;
        this.metricService = metricService;
        this.luceneLogIndex = luceneLogIndex;
        this.executorService = executorService;
    }

    @Override
    public LuceneLogStore newLogStore(
            final LuceneLogStoreConfiguration logStoreConfiguration) throws LogStoreFactoryException
    {
        AppbaseUtil.notNull(logStoreConfiguration, "logStoreConfiguration");

        return new LuceneLogStore(
                logService,
                metricService,
                luceneLogIndex,
                executorService,
                logStoreConfiguration);
    }
}
