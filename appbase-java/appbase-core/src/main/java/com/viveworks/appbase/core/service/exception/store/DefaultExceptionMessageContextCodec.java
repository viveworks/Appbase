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

import javax.annotation.concurrent.ThreadSafe;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation for {@link ExceptionMessageContextCodec}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@ThreadSafe
public class DefaultExceptionMessageContextCodec implements ExceptionMessageContextCodec {
    @Override
    public String encode(final Map<String, String> context) {
        final StringBuilder stringBuilder = new StringBuilder();

        if (context != null && !context.isEmpty()) {
            context.forEach((key, value) -> {
                if (key != null && value != null) {
                    stringBuilder
                            .append(key)
                            .append('=')
                            .append(value)
                            .append(',');
                }
            });

            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        }

        return stringBuilder.toString();
    }

    @Override
    public Map<String, String> decode(final String serializedContext) {
        final Map<String, String> result = new ConcurrentHashMap<>();

        if (serializedContext != null) {
            final String[] commaSplit = serializedContext.split(",");
            for (final String keyValue : commaSplit) {
                final String[] keyValueSplit = keyValue.split("=");
                result.put(keyValueSplit[0], keyValueSplit[1]);
            }
        }

        return result;
    }
}
