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

package com.viveworks.appbase.core.service.jmx.mxbean.entry;

import com.viveworks.appbase.core.util.Unmodifiable;

import javax.management.Descriptor;
import javax.management.MBeanParameterInfo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utilities for this package.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public final class EntryUtil {
    private EntryUtil() {
    }

    /**
     * Converts {@link Descriptor} to a {@link Map} of key value pairs.
     *
     * @param descriptor Descriptor
     * @return Map of key value pairs
     */
    @Unmodifiable
    public static Map<String, String> descriptor2FieldMap(final Descriptor descriptor) {
        final Map<String, String> fieldMap = new HashMap<>();

        if (descriptor != null) {
            final String[] fields = descriptor.getFields();
            if (fields != null) {
                for (final String keyValue : fields) {
                    if (keyValue != null) {
                        final String[] splits = keyValue.split("=");
                        if (splits.length == 2) {
                            final String key = splits[0].trim();
                            final String value = splits[1].trim();
                            fieldMap.put(key, value);
                        }
                    }
                }
            }
        }

        return Collections.unmodifiableMap(fieldMap);
    }

    /**
     * Convert array of {@link MBeanParameterInfo}s to list of {@link MXBeanParameterEntry}.
     *
     * @param signature Array of MBeanParameterInfo
     * @return List of MXBeanParameterEntry
     */
    public static List<MXBeanParameterEntry> signature2MxBeanParameterEntries(final MBeanParameterInfo[] signature) {
        final List<MXBeanParameterEntry> mxBeanParameterEntries = new ArrayList<>();
        if (signature != null) {
            for (final MBeanParameterInfo mBeanParameterInfo : signature) {
                if (mBeanParameterInfo != null) {
                    final MXBeanParameterEntry mxBeanParameterEntry = MXBeanParameterEntry.from(mBeanParameterInfo);
                    mxBeanParameterEntries.add(mxBeanParameterEntry);
                }
            }
        }

        return Collections.unmodifiableList(mxBeanParameterEntries);
    }
}
