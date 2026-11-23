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

import com.viveworks.appbase.core.util.AppbaseUtil;

/**
 * Represents an {@code item} in the {@link AppbaseHandlerChain} and delegates to configured {@link AppbaseHandler}.
 *
 * @author Navaneeth Mysore G
 * @param <R> {@link AppbaseHandlerRequest}
 * @since 1.0.0
 */
public class AppbaseHandlerChainItem<R extends AppbaseHandlerRequest> implements AppbaseHandler<R> {
    private final AppbaseHandler<R> appbaseHandler;
    private AppbaseHandlerChainItem<R> nextHandlerChainItem;

    public AppbaseHandlerChainItem(final AppbaseHandler<R> appbaseHandler) {
        AppbaseUtil.notNull(appbaseHandler, "appbaseHandler");

        this.appbaseHandler = appbaseHandler;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void handle(final R message) {
        final AbstractMutableAppbaseHandlerRequest appbaseHandlerRequest = (AbstractMutableAppbaseHandlerRequest) message;
        appbaseHandlerRequest.setCurrentHandlerChainItem(
                (AppbaseHandlerChainItem<AppbaseHandlerRequest>) nextHandlerChainItem);
        appbaseHandler.handle((R) appbaseHandlerRequest);
    }

    public AppbaseHandler<R> getAppbaseHandler() {
        return appbaseHandler;
    }

    public boolean hasNextHandlerChainItem() {
        return nextHandlerChainItem != null;
    }

    public AppbaseHandlerChainItem<R> getNextHandlerChainItem() {
        return nextHandlerChainItem;
    }

    public void setNextHandlerChainItem(final AppbaseHandlerChainItem<R> nextHandlerChainItem) {
        AppbaseUtil.notNull(nextHandlerChainItem, "nextHandlerChainItem");

        this.nextHandlerChainItem = nextHandlerChainItem;
    }
}
