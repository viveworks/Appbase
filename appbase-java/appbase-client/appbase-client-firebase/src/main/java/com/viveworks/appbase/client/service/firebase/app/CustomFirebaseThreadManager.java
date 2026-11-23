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

package com.viveworks.appbase.client.service.firebase.app;

import com.google.firebase.FirebaseApp;
import com.google.firebase.ThreadManager;
import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.service.firebase.app.configuration.CustomFirebaseThreadManagerConfiguration;
import com.viveworks.appbase.core.service.mediator.executor.ExecutorServiceConfiguration;
import com.viveworks.appbase.core.service.mediator.executor.ExecutorServiceFactory;
import com.viveworks.appbase.core.util.AppbaseUtil;
import org.slf4j.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

/**
 * Custom implementation of {@link ThreadManager}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class CustomFirebaseThreadManager extends ThreadManager {
    private final ExecutorService executorService;
    private final ThreadFactory threadFactory;

    public CustomFirebaseThreadManager(
            final AppbaseClientCore appbaseClientCore,
            final CustomFirebaseThreadManagerConfiguration customFirebaseThreadManagerConfiguration)
    {
        AppbaseUtil.notNull(appbaseClientCore, "appbaseClientCore");
        AppbaseUtil.notNull(customFirebaseThreadManagerConfiguration, "customFirebaseThreadManagerConfiguration");

        if (customFirebaseThreadManagerConfiguration.isUseSharedExecutorService()) {
            this.executorService = appbaseClientCore
                    .getAppbaseCore()
                    .getMediatorService()
                    .getMediator()
                    .getSharedExecutorService();
        } else {
            final ExecutorServiceConfiguration executorServiceConfiguration =
                    customFirebaseThreadManagerConfiguration.getExecutorServiceConfiguration();
            this.executorService = ExecutorServiceFactory.newExecutorService(
                    executorServiceConfiguration,
                    AppbaseUtil.defaultUncaughtExceptionHandler(
                            appbaseClientCore
                                    .getAppbaseCore()
                                    .getLogService()
                                    .getLogger(getClass())));
        }

        final Logger logger = appbaseClientCore
                .getAppbaseCore()
                .getLogService()
                .getLogger(getClass());
        this.threadFactory = AppbaseUtil.newThreadFactory(
                customFirebaseThreadManagerConfiguration.getThreadFactoryConfiguration(),
                AppbaseUtil.defaultUncaughtExceptionHandler(logger));
    }

    @Override
    protected ExecutorService getExecutor(final FirebaseApp app) {
        return executorService;
    }

    @Override
    protected void releaseExecutor(final FirebaseApp app, final ExecutorService executor) {
        // no-op
    }

    @Override
    protected ThreadFactory getThreadFactory() {
        return threadFactory;
    }
}
