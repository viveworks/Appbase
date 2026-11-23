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

package com.viveworks.appbase.core.util.handler;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.di.guice.GuiceInjectorService;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Timer;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Default implementation of {@link AppbaseHandlerChain}.
 *
 * @author Navaneeth Mysore G
 * @param <R> {@link AppbaseHandlerRequest}
 * @since 1.0.0
 */
public class DefaultAppbaseHandlerChain<R extends AppbaseHandlerRequest> implements AppbaseHandlerChain<R> {
    private final Logger logger;
    private final MetricService metricService;
    private final List<AppbaseHandler<R>> appbaseHandlers;
    private final Timer executionTimer;
    private final Counter errorCounter;
    private final Map<String, Timer> handlerExecutionTimers;
    private AppbaseHandlerChainItem<R> rootAppbaseHandlerChainItem;

    @SuppressWarnings("unchecked")
    public DefaultAppbaseHandlerChain(
            final LogService logService,
            final MetricService metricService,
            final GuiceInjectorService guiceInjectorService,
            final DefaultAppbaseHandlerChainConfiguration defaultAppbaseHandlerChainConfiguration)
    {
        AppbaseUtil.notNull(logService, "logService");
        AppbaseUtil.notNull(metricService, "metricService");
        AppbaseUtil.notNull(guiceInjectorService, "guiceInjectorService");

        this.logger = logService.getLogger(getClass());
        this.metricService = metricService;
        this.appbaseHandlers = new CopyOnWriteArrayList<>();

        this.executionTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "execution", "timer"));
        this.errorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "error", "counter"));
        this.handlerExecutionTimers = new ConcurrentHashMap<>();

        defaultAppbaseHandlerChainConfiguration
                .getAppbaseHandlerInjectedTypeConfigurations()
                .forEach(configuration -> {
                    final AppbaseHandler<R> appbaseHandler =
                            (AppbaseHandler<R>) AppbaseUtil.injectedInstance(guiceInjectorService, configuration);
                    appendHandler(appbaseHandler);
                });
    }

    public DefaultAppbaseHandlerChain(final LogService logService,
                                      final MetricService metricService,
                                      final List<AppbaseHandler<R>> appbaseHandlers)
    {
        AppbaseUtil.notNull(logService, "logService");
        AppbaseUtil.notNull(metricService, "metricService");
        AppbaseUtil.notEmpty(appbaseHandlers, "appbaseHandlers");

        this.logger = logService.getLogger(getClass());
        this.metricService = metricService;
        this.appbaseHandlers = new CopyOnWriteArrayList<>();

        this.executionTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "execution", "timer"));
        this.errorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "error", "counter"));
        this.handlerExecutionTimers = new ConcurrentHashMap<>();

        appbaseHandlers.forEach(this::appendHandler);
    }

    private void appendHandler(final AppbaseHandler<R> appbaseHandler) {
        AppbaseUtil.notNull(appbaseHandler, "appbaseHandler");

        logger.info("Appending handler: {}", appbaseHandler.getClass().getName());
        final AppbaseHandlerChainItem<R> appbaseHandlerChainItem = new AppbaseHandlerChainItem<>(appbaseHandler);

        if (rootAppbaseHandlerChainItem == null) {
            rootAppbaseHandlerChainItem = appbaseHandlerChainItem;
        } else {
            AppbaseHandlerChainItem<R> nextAppbaseHandlerChainItem =
                    rootAppbaseHandlerChainItem.getNextHandlerChainItem();
            while (nextAppbaseHandlerChainItem.hasNextHandlerChainItem()) {
                nextAppbaseHandlerChainItem = nextAppbaseHandlerChainItem.getNextHandlerChainItem();
            }
            nextAppbaseHandlerChainItem.setNextHandlerChainItem(appbaseHandlerChainItem);
        }

        this.appbaseHandlers.add(appbaseHandler);
        this.handlerExecutionTimers.computeIfAbsent(
                appbaseHandler.getClass().getName(),
                f -> metricService.getMetricRegistry().timer(metricService.name(getClass(), "execution", "timer")));
    }

    @Override
    public Collection<AppbaseHandler<R>> getHandlers() {
        return Collections.unmodifiableList(appbaseHandlers);
    }

    @Override
    public boolean hasHandler(final Class<AppbaseHandler<R>> appbaseHandlerType) {
        AppbaseUtil.notNull(appbaseHandlerType, "appbaseHandlerType");

        for (final AppbaseHandler<R> appbaseHandler : appbaseHandlers) {
            if (appbaseHandler.getClass().equals(appbaseHandlerType)) {
                return true;
            }
        }

        return false;
    }

    @Override
    @SuppressWarnings({"unchecked", "checkstyle:IllegalCatch"})
    public void execute(final R request, final AppbaseHandlerChainHandler<R> appbaseHandlerChainHandler) {
        AppbaseUtil.notNull(request, "request");
        AppbaseUtil.notNull(appbaseHandlerChainHandler, "appbaseHandlerChainHandler");

        if (rootAppbaseHandlerChainItem == null) {
            throw new IllegalStateException("Handlers must not be empty");
        }

        if (AbstractMutableAppbaseHandlerRequest.class.isAssignableFrom(request.getClass())) {
            final AbstractMutableAppbaseHandlerRequest abstractMutableAppbaseHandlerRequest =
                    (AbstractMutableAppbaseHandlerRequest) request;
            abstractMutableAppbaseHandlerRequest.setAppbaseHandlerChainHandler(
                    (AppbaseHandlerChainHandler<AppbaseHandlerRequest>) appbaseHandlerChainHandler);
            abstractMutableAppbaseHandlerRequest.setCurrentHandlerChainItem(
                    (AppbaseHandlerChainItem<AppbaseHandlerRequest>) rootAppbaseHandlerChainItem);

            if (!AbstractAppbaseHandler.class
                    .isAssignableFrom(rootAppbaseHandlerChainItem.getAppbaseHandler().getClass()) &&
                    !abstractMutableAppbaseHandlerRequest.isValid())
            {
                errorCounter.inc();

                final String message = "The request " + abstractMutableAppbaseHandlerRequest + " is not in a valid state";
                logger.error(message);
                appbaseHandlerChainHandler.error(request, new AppbaseRuntimeException(message));
                return;
            }

            rootAppbaseHandlerChainItem.handle(request);
        } else {
            final Timer.Context timerContext = executionTimer.time();
            try {
                for (final AppbaseHandler<R> appbaseHandler : appbaseHandlers) {
                    try {
                        if (!request.isValid()) {
                            errorCounter.inc();

                            final String message = "The request " + request + " is not in a valid state";
                            logger.error(message);
                            appbaseHandlerChainHandler.error(request, new AppbaseRuntimeException(message));
                            break;
                        } else {
                            final Timer.Context handlerTimerContext = handlerExecutionTimers
                                    .get(appbaseHandler.getClass().getName())
                                    .time();
                            try {
                                appbaseHandler.handle(request);
                            } finally {
                                handlerTimerContext.stop();
                            }
                        }
                    } catch (Exception ex) {
                        errorCounter.inc();
                        appbaseHandlerChainHandler.error(request, ex);
                    }
                }
            } finally {
                timerContext.stop();
            }

            appbaseHandlerChainHandler.complete(request);
        }
    }

    @Override
    public String toString() {
        return "DefaultAppbaseHandlerChain{" +
                "appbaseHandlers=" + appbaseHandlers +
                '}';
    }
}
