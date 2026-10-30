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

package com.viveworks.appbase.core.service.jmx.mxbean.entry;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.management.Descriptor;
import javax.management.MBeanAttributeInfo;
import javax.management.modelmbean.DescriptorSupport;
import java.util.Map;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class MXBeanAttributeEntryTest {
    @Test
    void testSerde() throws Exception {
        final MXBeanAttributeEntry mxBeanAttributeEntry = MXBeanAttributeEntry
                .builder()
                .name("username")
                .description("Represents user name")
                .fieldMap(Map.of("k1", "v1"))
                .attributeType(String.class.getName())
                .is(false)
                .readable(true)
                .writable(false)
                .build();

        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();
        final String yaml = yamlMapper
                .writerFor(MXBeanAttributeEntry.class)
                .writeValueAsString(mxBeanAttributeEntry);
        final MXBeanAttributeEntry mxBeanAttributeEntry1 = yamlMapper
                .readerFor(MXBeanAttributeEntry.class)
                .readValue(yaml);

        Assertions.assertEquals(mxBeanAttributeEntry.getName(), mxBeanAttributeEntry1.getName());
        Assertions.assertEquals(mxBeanAttributeEntry.getDescription(), mxBeanAttributeEntry1.getDescription());
        Assertions.assertEquals(mxBeanAttributeEntry.getFieldMap(), mxBeanAttributeEntry1.getFieldMap());
        Assertions.assertEquals(mxBeanAttributeEntry.getAttributeType(), mxBeanAttributeEntry1.getAttributeType());
        Assertions.assertEquals(mxBeanAttributeEntry.isIs(), mxBeanAttributeEntry1.isIs());
        Assertions.assertEquals(mxBeanAttributeEntry.isReadable(), mxBeanAttributeEntry1.isReadable());
        Assertions.assertEquals(mxBeanAttributeEntry.isWritable(), mxBeanAttributeEntry1.isWritable());
        Assertions.assertEquals(mxBeanAttributeEntry, mxBeanAttributeEntry1);
        Assertions.assertEquals(mxBeanAttributeEntry.hashCode(), mxBeanAttributeEntry1.hashCode());
        Assertions.assertEquals(mxBeanAttributeEntry.toString(), mxBeanAttributeEntry1.toString());
    }

    @Test
    void testFrom() throws Exception {
        final Descriptor descriptor = new DescriptorSupport();
        descriptor.setField("fk1", "fv1");
        descriptor.setField("fk2", "fv2");
        final Map<String, String> fieldMap = EntryUtil.descriptor2FieldMap(descriptor);

        final MBeanAttributeInfo mBeanAttributeInfo = new MBeanAttributeInfo(
                "name1", "type1", "description1", true, false, false, descriptor);
        final MXBeanAttributeEntry mxBeanAttributeEntry = MXBeanAttributeEntry.from(mBeanAttributeInfo);
        Assertions.assertNotNull(mxBeanAttributeEntry);
        Assertions.assertEquals("name1", mxBeanAttributeEntry.getName());
        Assertions.assertEquals("type1", mxBeanAttributeEntry.getAttributeType());
        Assertions.assertEquals("description1", mxBeanAttributeEntry.getDescription());
        Assertions.assertTrue(mxBeanAttributeEntry.isReadable());
        Assertions.assertFalse(mxBeanAttributeEntry.isWritable());
        Assertions.assertFalse(mxBeanAttributeEntry.isIs());
        Assertions.assertEquals(fieldMap, mxBeanAttributeEntry.getFieldMap());

        Assertions.assertNull(MXBeanAttributeEntry.from(null));
    }
}
