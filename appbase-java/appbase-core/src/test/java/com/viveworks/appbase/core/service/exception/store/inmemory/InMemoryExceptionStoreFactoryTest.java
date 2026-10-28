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

package com.viveworks.appbase.core.service.exception.store.inmemory;

import com.viveworks.appbase.core.service.exception.store.DefaultExceptionMessageContextCodec;
import com.viveworks.appbase.core.service.exception.store.DefaultStackTraceHashGenerator;
import com.viveworks.appbase.core.service.exception.store.ExceptionStore;
import com.viveworks.appbase.core.service.exception.store.ExceptionStoreFactoryException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class InMemoryExceptionStoreFactoryTest {
    @Test
    void testNewExceptionStore() throws Exception {
        final InMemoryExceptionStoreFactory inMemoryExceptionStoreFactory = new InMemoryExceptionStoreFactory(
                new DefaultStackTraceHashGenerator(), new DefaultExceptionMessageContextCodec());

        final InMemoryExceptionStoreConfiguration inMemoryExceptionStoreConfiguration1 =
                InMemoryExceptionStoreConfiguration
                .builder()
                .stackTraceSerializationMaxLength(2048)
                .discriminator("DISCRIMINATOR")
                .enabled(true)
                .build();
        final ExceptionStore exceptionStore =
                inMemoryExceptionStoreFactory.newExceptionStore(inMemoryExceptionStoreConfiguration1);
        Assertions.assertNotNull(exceptionStore);

        final InMemoryExceptionStoreConfiguration inMemoryExceptionStoreConfiguration2 =
                InMemoryExceptionStoreConfiguration
                        .builder()
                        .stackTraceSerializationMaxLength(2048)
                        .discriminator("DISCRIMINATOR")
                        .enabled(false)
                        .build();
        Assertions.assertThrows(ExceptionStoreFactoryException.class, () -> {
            inMemoryExceptionStoreFactory.newExceptionStore(inMemoryExceptionStoreConfiguration2);
        });
    }
}
