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

package com.viveworks.appbase.core.service.configuration.accessor.store.filesystem;

import com.viveworks.appbase.core.service.configuration.MimeType;
import com.viveworks.appbase.core.util.AppbaseUtil;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of {@link FileMimeTypeResolver}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@ThreadSafe
public class DefaultFileMimeTypeResolver implements FileMimeTypeResolver {
    private final Map<String, MimeType> mimeTypeByExtensionMap;

    public DefaultFileMimeTypeResolver() {
        this.mimeTypeByExtensionMap = new ConcurrentHashMap<>();

        this.mimeTypeByExtensionMap.put("properties", MimeType.PROPERTIES);
        this.mimeTypeByExtensionMap.put("yml", MimeType.YAML);
        this.mimeTypeByExtensionMap.put("yaml", MimeType.YAML);
        this.mimeTypeByExtensionMap.put("json", MimeType.JSON);
    }

    public DefaultFileMimeTypeResolver(final Map<String, MimeType> mimeTypeByExtensionMap) {
        AppbaseUtil.notNull(mimeTypeByExtensionMap, "mimeTypeByExtensionMap");

        this.mimeTypeByExtensionMap = new HashMap<>(mimeTypeByExtensionMap);
    }

    @Nullable
    @Override
    public MimeType resolveMimeType(final String filename) {
        AppbaseUtil.notBlank(filename, "filename");

        final int dotLastIndex = filename.lastIndexOf('.');
        if (dotLastIndex >= 0) {
            final String extension = filename.substring(dotLastIndex + 1);
            return mimeTypeByExtensionMap.get(extension);
        }

        return null;
    }

    @Override
    public String toString() {
        return "DefaultFileMimeTypeResolver{" +
                "mimeTypeByExtensionMap=" + mimeTypeByExtensionMap +
                '}';
    }
}
