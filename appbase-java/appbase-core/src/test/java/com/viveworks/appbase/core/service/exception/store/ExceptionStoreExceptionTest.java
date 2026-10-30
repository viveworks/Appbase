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

package com.viveworks.appbase.core.service.exception.store;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class ExceptionStoreExceptionTest {
    @Test
    void testThrow() {
        Assertions.assertThrows(ExceptionStoreException.class, () -> {
            throw new ExceptionStoreException();
        });

        Assertions.assertThrows(ExceptionStoreException.class, () -> {
            throw new ExceptionStoreException("message");
        });

        Assertions.assertThrows(ExceptionStoreException.class, () -> {
            throw new ExceptionStoreException("message", new NullPointerException());
        });

        Assertions.assertThrows(ExceptionStoreException.class, () -> {
            throw new ExceptionStoreException(new NullPointerException());
        });

        Assertions.assertThrows(ExceptionStoreException.class, () -> {
            throw new ExceptionStoreException("message", new NullPointerException(), false, true);
        });

        try {
            throw new ExceptionStoreException();
        } catch (ExceptionStoreException ese) {
            Assertions.assertTrue(ese.getTimestamp() > 0);
        }
    }
}
