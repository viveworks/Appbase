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

package com.viveworks.appbase.core.service.exception.store;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class DefaultExceptionMessageContextCodecTest {
    @Test
    void testCodec() {
        final Map<String, String> context = new HashMap<>();
        context.put("A", "B");
        context.put("C", "D");

        final DefaultExceptionMessageContextCodec defaultContextCodec = new DefaultExceptionMessageContextCodec();
        final String encode = defaultContextCodec.encode(context);
        Assertions.assertNotNull(encode);

        final Map<String, String> decode = defaultContextCodec.decode(encode);
        Assertions.assertNotNull(decode);
        Assertions.assertEquals(context, decode);
    }
}
