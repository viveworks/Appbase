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

package com.viveworks.appbase.core.service.exception.store.inmemory;

import com.viveworks.appbase.core.service.exception.store.ExceptionMessageContextCodec;
import com.viveworks.appbase.core.service.exception.store.ExceptionStoreFactory;
import com.viveworks.appbase.core.service.exception.store.ExceptionStoreFactoryException;
import com.viveworks.appbase.core.service.exception.store.StackTraceHashGenerator;
import com.viveworks.appbase.core.util.AppbaseUtil;

import javax.annotation.concurrent.ThreadSafe;

/**
 * Factory for {@link InMemoryExceptionStore}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@ThreadSafe
public class InMemoryExceptionStoreFactory
        implements ExceptionStoreFactory<InMemoryExceptionStore, InMemoryExceptionStoreConfiguration>
{
    private final StackTraceHashGenerator stackTraceHashGenerator;
    private final ExceptionMessageContextCodec exceptionMessageContextCodec;

    public InMemoryExceptionStoreFactory(final StackTraceHashGenerator stackTraceHashGenerator,
                                         final ExceptionMessageContextCodec exceptionMessageContextCodec)
    {
        AppbaseUtil.notNull(stackTraceHashGenerator, "stackTraceHashGenerator");
        AppbaseUtil.notNull(exceptionMessageContextCodec, "exceptionMessageContextCodec");

        this.stackTraceHashGenerator = stackTraceHashGenerator;
        this.exceptionMessageContextCodec = exceptionMessageContextCodec;
    }

    @Override
    public InMemoryExceptionStore newExceptionStore(
            final InMemoryExceptionStoreConfiguration exceptionStoreConfiguration) throws ExceptionStoreFactoryException
    {
        AppbaseUtil.notNull(exceptionStoreConfiguration, "exceptionStoreConfiguration");

        if (!exceptionStoreConfiguration.isEnabled()) {
            throw new ExceptionStoreFactoryException("The store " +
                    InMemoryExceptionStore.class.getName() + " is disabled: " + exceptionStoreConfiguration);
        }

        InMemoryExceptionStoreConfigurationValidator.validate(
                exceptionStoreConfiguration.getStackTraceSerializationMaxLength());

        return new InMemoryExceptionStore(
                stackTraceHashGenerator, exceptionMessageContextCodec, exceptionStoreConfiguration);
    }
}
