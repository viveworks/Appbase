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

package com.viveworks.appbase.core.service.mediator.message;

import com.viveworks.appbase.core.AppbaseException;
import com.viveworks.appbase.core.util.AppbaseUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class ExceptionMessageTest {
    @Test
    void testExceptionMessage() {
        final ExceptionMessage exceptionMessage = new ExceptionMessage();
        exceptionMessage.setSourceType(getClass());
        exceptionMessage.setThreadName(AppbaseUtil.threadName());

        final AppbaseException appbaseException = new AppbaseException();
        exceptionMessage.setThrowable(appbaseException);

        final Map<String, String> context = new HashMap<>();
        context.put("a", "b");
        exceptionMessage.addAllContext(context);

        Assertions.assertEquals(getClass(), exceptionMessage.getSourceType());
        Assertions.assertEquals(appbaseException, exceptionMessage.getThrowable());
        Assertions.assertNotNull(exceptionMessage.getContext());
        Assertions.assertEquals("b", exceptionMessage.getContext().get("a"));

        final ExceptionMessage exceptionMessage1 = new ExceptionMessage(exceptionMessage);
        Assertions.assertEquals(exceptionMessage, exceptionMessage1);
        Assertions.assertEquals(exceptionMessage.hashCode(), exceptionMessage1.hashCode());
        Assertions.assertEquals(exceptionMessage.toString(), exceptionMessage1.toString());

        exceptionMessage.clean();
        Assertions.assertTrue(exceptionMessage.getContext().isEmpty());
        Assertions.assertNull(exceptionMessage.getThrowable());
        Assertions.assertNull(exceptionMessage.getSourceType());
        Assertions.assertNull(exceptionMessage.getThreadName());
    }
}
