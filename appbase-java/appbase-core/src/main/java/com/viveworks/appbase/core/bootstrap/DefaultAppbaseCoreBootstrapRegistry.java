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

package com.viveworks.appbase.core.bootstrap;

import com.viveworks.appbase.core.service.configuration.accessor.AbstractConfigurationAccessorRegistry;
import com.viveworks.appbase.core.service.exception.store.DefaultExceptionMessageContextCodec;
import com.viveworks.appbase.core.service.exception.store.ExceptionMessageContextCodec;
import com.viveworks.appbase.core.util.AppbaseUtil;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Default implementation of {@link AppbaseCoreBootstrapRegistry}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultAppbaseCoreBootstrapRegistry
        extends AbstractConfigurationAccessorRegistry implements AppbaseCoreBootstrapRegistry
{
    private static final ExceptionMessageContextCodec DEFAULT_EXCEPTION_MESSAGE_CONTEXT_CODEC =
            new DefaultExceptionMessageContextCodec();

    private final AtomicReference<ExceptionMessageContextCodec> exceptionMessageContextCodecRef;

    /**
     * Initializes the registry with a {@link DefaultExceptionMessageContextCodec}; configuration
     * accessors remain empty until callers register them.
     */
    public DefaultAppbaseCoreBootstrapRegistry() {
        exceptionMessageContextCodecRef = new AtomicReference<>(DEFAULT_EXCEPTION_MESSAGE_CONTEXT_CODEC);
    }

    @Override
    public ExceptionMessageContextCodec getExceptionMessageContextCodec() {
        return exceptionMessageContextCodecRef.get();
    }

    @Override
    public void setExceptionMessageContextCodec(final ExceptionMessageContextCodec exceptionMessageContextCodec) {
        AppbaseUtil.notNull(exceptionMessageContextCodec, "exceptionMessageContextCodec");

        exceptionMessageContextCodecRef.set(exceptionMessageContextCodec);
    }

    @Override
    public void clear() {
        super.shutdown();
        exceptionMessageContextCodecRef.set(null);
    }
}
