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

package com.viveworks.appbase.core.service.configuration.accessor.store.filesystem;

import com.viveworks.appbase.core.service.configuration.MimeType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class DefaultFileMimeTypeResolverTest {
    @Test
    void resolveMimeType() {
        final Map<String, MimeType> mimeTypeByExtensionMap = new HashMap<>();
        mimeTypeByExtensionMap.put("properties", MimeType.PROPERTIES);

        final DefaultFileMimeTypeResolver defaultFileMimeTypeResolver =
                new DefaultFileMimeTypeResolver(mimeTypeByExtensionMap);

        Assertions.assertEquals(
                MimeType.PROPERTIES,
                defaultFileMimeTypeResolver.resolveMimeType("/etc/dir1/file1.properties"));
        Assertions.assertEquals(
                MimeType.PROPERTIES,
                defaultFileMimeTypeResolver.resolveMimeType("file2.properties"));

        Assertions.assertNull(defaultFileMimeTypeResolver.resolveMimeType("/etc/dir1/file1.json"));
        Assertions.assertNull(defaultFileMimeTypeResolver.resolveMimeType("/etc/dir1/file1.yaml"));
        Assertions.assertNull(defaultFileMimeTypeResolver.resolveMimeType("/etc/dir1/file1"));
    }
}
