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

package com.viveworks.appbase.core.util.net;

import com.viveworks.appbase.core.util.AppbaseUtil;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Represents popular {@link Charset}s.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public enum AppbaseCharset {
    US_ASCII(StandardCharsets.US_ASCII),

    ISO_8859_1(StandardCharsets.ISO_8859_1),

    UTF_8(StandardCharsets.UTF_8),

    UTF_16BE(StandardCharsets.UTF_16BE),

    UTF_16LE(StandardCharsets.UTF_16LE),

    UTF_16(StandardCharsets.UTF_16);

    private final Charset charset;

    AppbaseCharset(final Charset charset) {
        AppbaseUtil.notNull(charset, "charset");

        this.charset = charset;
    }

    public Charset getCharset() {
        return charset;
    }
}
