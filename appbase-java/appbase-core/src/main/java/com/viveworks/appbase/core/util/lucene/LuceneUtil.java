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

package com.viveworks.appbase.core.util.lucene;

import com.viveworks.appbase.core.util.AppbaseUtil;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;

/**
 * <code>Lucene</code> utilities.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public final class LuceneUtil {
    private LuceneUtil() {
    }

    /**
     * Add {@link TextField} to {@link Document}.
     *
     * @param fieldName Field name
     * @param text Text
     * @param document Document
     */
    public static void addTextField(final String fieldName, final String text, final Document document) {
        if (!AppbaseUtil.isBlank(fieldName) && !AppbaseUtil.isBlank(text) && (document != null)) {
            final TextField textField = new TextField(fieldName, text, Field.Store.YES);
            document.add(textField);
        }
    }

    /**
     * Add {@link LongPoint} to {@link Document}.
     *
     * @param longPointName Long point name
     * @param storedFieldName Store field name
     * @param value Value
     * @param document Document
     */
    public static void addLongPoint(final String longPointName,
                                    final String storedFieldName,
                                    final long value,
                                    final Document document)
    {
        if (!AppbaseUtil.isBlank(longPointName) && !AppbaseUtil.isBlank(storedFieldName) && (document != null)) {
            final LongPoint longPoint = new LongPoint(longPointName, value);
            final StoredField storedField = new StoredField(storedFieldName, value);
            document.add(longPoint);
            document.add(storedField);
        }
    }

    /**
     * Add {@link IntPoint} to {@link Document}.
     *
     * @param intPointName Int point name
     * @param storedFieldName Stored field name
     * @param value Value
     * @param document Document
     */
    public static void addIntPoint(final String intPointName,
                                   final String storedFieldName,
                                   final int value,
                                   final Document document)
    {
        if (!AppbaseUtil.isBlank(intPointName) && !AppbaseUtil.isBlank(storedFieldName) && (document != null)) {
            final IntPoint intPoint = new IntPoint(intPointName, value);
            final StoredField storedField = new StoredField(storedFieldName, value);
            document.add(intPoint);
            document.add(storedField);
        }
    }
}
