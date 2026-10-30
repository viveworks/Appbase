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

package com.viveworks.appbase.core.util.lucene;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class LuceneUtilTest {
    @Test
    void testAddTextField() {
        final String fieldName = "fieldName1";
        final String value = "text1";
        final Document document = new Document();
        LuceneUtil.addTextField(fieldName, value, document);

        final IndexableField field = document.getField(fieldName);
        Assertions.assertNotNull(field);
        Assertions.assertEquals(field.stringValue(), value);
    }

    @Test
    void testAddLongPoint() {
        final String longPointName = "longPointName1";
        final String storedFieldName = "storeFieldName1";
        final long value = 240923L;
        final Document document = new Document();
        LuceneUtil.addLongPoint(longPointName, storedFieldName, value, document);

        final IndexableField longPointField = document.getField(longPointName);
        Assertions.assertNotNull(longPointField);
        Assertions.assertEquals(value, longPointField.numericValue().longValue());

        final IndexableField storedField = document.getField(storedFieldName);
        Assertions.assertNotNull(storedField);
        Assertions.assertEquals(value, storedField.numericValue().longValue());
    }

    @Test
    void testAddIntPoint() {
        final String intPointName = "intPointName1";
        final String storedFieldName = "storeFieldName1";
        final int value = 2409;
        final Document document = new Document();
        LuceneUtil.addIntPoint(intPointName, storedFieldName, value, document);

        final IndexableField intPointField = document.getField(intPointName);
        Assertions.assertNotNull(intPointField);
        Assertions.assertEquals(value, intPointField.numericValue().intValue());

        final IndexableField storedField = document.getField(storedFieldName);
        Assertions.assertNotNull(storedField);
        Assertions.assertEquals(value, storedField.numericValue().intValue());
    }
}
