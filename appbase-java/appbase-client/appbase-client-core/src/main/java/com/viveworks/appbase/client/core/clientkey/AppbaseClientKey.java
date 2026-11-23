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

package com.viveworks.appbase.client.core.clientkey;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.AppbaseClientService;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.ComparableValue;
import com.viveworks.appbase.core.util.objectpool.Poolable;

/**
 * Represents a key for {@link AppbaseClientService} and {@link AppbaseClientIntegrationService}.
 *
 * @author Navaneeth Mysore G
 * @param <V> {@link ComparableValue} type
 * @since 1.0.0
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
public interface AppbaseClientKey<V extends ComparableValue> extends ComparableValue, Poolable {
    /**
     * Get the {@link ComparableValue} that this key represents.
     *
     * @return Key value
     */
    V getValue();
}
