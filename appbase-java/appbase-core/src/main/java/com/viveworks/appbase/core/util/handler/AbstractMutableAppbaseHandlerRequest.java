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

package com.viveworks.appbase.core.util.handler;

import com.viveworks.appbase.core.util.AppbaseUtil;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Timer;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract implementation of {@link AppbaseHandlerRequest}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public abstract class AbstractMutableAppbaseHandlerRequest implements MutableAppbaseHandlerRequest {
    private AppbaseHandlerChainHandler<AppbaseHandlerRequest> appbaseHandlerChainHandler;
    private AppbaseHandlerChainItem<AppbaseHandlerRequest> currentHandlerChainItem;
    private Map<Object, Object> handlerContext;
    private Throwable cause;
    private Timer.Context timerContext;
    private Counter errorCounter;

    public AbstractMutableAppbaseHandlerRequest() {
        handlerContext = new ConcurrentHashMap<>();
    }

    @Override
    public void complete() {
        if (timerContext != null) {
            timerContext.stop();
            timerContext = null;
        }

        final AppbaseHandlerChainItem<AppbaseHandlerRequest> nextHandlerChainItem;
        if (currentHandlerChainItem != null) {
            nextHandlerChainItem = this.currentHandlerChainItem.getNextHandlerChainItem();
        } else {
            nextHandlerChainItem = null;
        }

        if (nextHandlerChainItem != null) {
            nextHandlerChainItem.handle(this);
        } else if (appbaseHandlerChainHandler != null) {
            this.appbaseHandlerChainHandler.complete(this);
        }
    }

    @Override
    public void completeWithException(final Throwable cause) {
        this.cause = cause;

        if (errorCounter != null) {
            errorCounter.inc();
            errorCounter = null;
        }

        if (timerContext != null) {
            timerContext.stop();
            timerContext = null;
        }

        if (appbaseHandlerChainHandler != null) {
            this.appbaseHandlerChainHandler.error(this, cause);
        }
    }

    @Override
    public void putHandlerContext(final String key, final Object value) {
        if (handlerContext == null) {
            this.handlerContext = new ConcurrentHashMap<>();
        }

        this.handlerContext.put(key, value);
    }

    @Override
    public Map<Object, Object> getHandlerContext() {
        if (handlerContext == null) {
            this.handlerContext = new ConcurrentHashMap<>();
        }

        return this.handlerContext;
    }

    @Override
    public void setHandlerContext(final Map<Object, Object> handlerContext) {
        AppbaseUtil.notNull(handlerContext, "handlerContext");

        this.handlerContext = handlerContext;
    }

    @Override
    public Throwable getException() {
        return cause;
    }

    /**
     * Set all properties.
     *
     * @param appbaseHandlerChainHandler {@link AppbaseHandlerChainHandler}
     * @param timerContext Timer context
     * @param errorCounter Error counter
     * @param currentHandlerChainItem Current {@link AppbaseHandlerChainItem}
     */
    public void init(final AppbaseHandlerChainHandler<AppbaseHandlerRequest> appbaseHandlerChainHandler,
                     final Timer.Context timerContext,
                     final Counter errorCounter,
                     @Nullable
                     final AppbaseHandlerChainItem<AppbaseHandlerRequest> currentHandlerChainItem)
    {
        setAppbaseHandlerChainHandler(appbaseHandlerChainHandler);
        setTimerContext(timerContext);
        setErrorCounter(errorCounter);
        setCurrentHandlerChainItem(currentHandlerChainItem);
    }

    public AppbaseHandlerChainItem<AppbaseHandlerRequest> getCurrentHandlerChainItem() {
        return currentHandlerChainItem;
    }

    public void setCurrentHandlerChainItem(
            @Nullable
            final AppbaseHandlerChainItem<AppbaseHandlerRequest> currentHandlerChainItem)
    {
        this.currentHandlerChainItem = currentHandlerChainItem;
    }

    public AppbaseHandlerChainHandler<AppbaseHandlerRequest> getAppbaseHandlerChainHandler() {
        return appbaseHandlerChainHandler;
    }

    public void setAppbaseHandlerChainHandler(
            final AppbaseHandlerChainHandler<AppbaseHandlerRequest> appbaseHandlerChainHandler)
    {
        AppbaseUtil.notNull(appbaseHandlerChainHandler, "appbaseHandlerChainHandler");

        this.appbaseHandlerChainHandler = appbaseHandlerChainHandler;
    }

    public Timer.Context getTimerContext() {
        return timerContext;
    }

    public void setTimerContext(final Timer.Context timerContext) {
        AppbaseUtil.notNull(timerContext, "timerContext");

        this.timerContext = timerContext;
    }

    public Counter getErrorCounter() {
        return errorCounter;
    }

    public void setErrorCounter(final Counter errorCounter) {
        AppbaseUtil.notNull(errorCounter, "errorCounter");

        this.errorCounter = errorCounter;
    }

    @Override
    public boolean isValid() {
        return appbaseHandlerChainHandler != null &&
                currentHandlerChainItem != null &&
                handlerContext != null &&
                timerContext != null &&
                errorCounter != null;
    }

    @Override
    public boolean isClean() {
        return appbaseHandlerChainHandler == null &&
                currentHandlerChainItem == null &&
                handlerContext.isEmpty() &&
                timerContext == null &&
                errorCounter == null;
    }

    @Override
    public void clean() {
        appbaseHandlerChainHandler = null;
        currentHandlerChainItem = null;
        handlerContext.clear();
        timerContext = null;
        errorCounter = null;
    }

    @Override
    public String toString() {
        return "AbstractMutableAppbaseHandlerRequest{" +
                "appbaseHandlerChainHandler=" + appbaseHandlerChainHandler +
                ", currentHandlerChainItem=" + currentHandlerChainItem +
                ", handlerContext=" + handlerContext +
                ", cause=" + cause +
                ", timerContext=" + timerContext +
                ", errorCounter=" + errorCounter +
                '}';
    }
}
