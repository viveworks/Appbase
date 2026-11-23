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

package com.viveworks.appbase.service.path;

import com.viveworks.appbase.core.AppbaseRuntimeException;

/**
 * Signals that a request path or a configured path key cannot be reduced to the canonical form.
 *
 * <p>Unchecked on purpose. The configuration classes canonicalize inside a Jackson
 * {@code @JsonPOJOBuilder}-driven constructor, which cannot declare a checked exception without
 * fighting Jackson's builder contract, and the reactive face constructs its exchange inside a
 * reactor-netty lambda. Both callers still catch it explicitly.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class AppbasePathException extends AppbaseRuntimeException {

    private static final long serialVersionUID = 1L;

    public AppbasePathException(final String message) {
        super(message);
    }
}
