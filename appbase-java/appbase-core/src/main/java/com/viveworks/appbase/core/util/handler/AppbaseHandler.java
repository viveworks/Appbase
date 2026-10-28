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

/**
 * Represents a generic {@code handler}. When using {@link DefaultAppbaseHandlerChain} and
 * {@link AbstractMutableAppbaseHandlerRequest}, implementations of this interface must finish the handler execution either
 * using {@link AbstractMutableAppbaseHandlerRequest#complete()} or
 * {@link AbstractMutableAppbaseHandlerRequest#completeWithException(Throwable)} for the chain to be fully executed.
 *
 * @author Navaneeth Mysore G
 * @param <R> {@link AppbaseHandlerRequest}
 * @since 1.0.0
 */
@FunctionalInterface
public interface AppbaseHandler<R extends AppbaseHandlerRequest> {
    String APPBASE_HANDLER_SPAN_KEY_SUFFIX = "-Span";
    String APPBASE_HANDLER_REQUEST_SPAN_KEY = AppbaseHandler.class.getName() + "-RequestSpanKey";
    String APPBASE_HANDLER_PARENT_SPAN_KEY = AppbaseHandler.class.getName() + "-ParentSpanKey";
    String APPBASE_HANDLER_ENABLE_TRACE_KEY = AppbaseHandler.class.getName() + "-EnableTraceKey";
    String APPBASE_HANDLER_SPAN_CUSTOMIZER_KEY = AppbaseHandler.class.getName() + "-SpanCustomizerKey";

    void handle(R message);
}
