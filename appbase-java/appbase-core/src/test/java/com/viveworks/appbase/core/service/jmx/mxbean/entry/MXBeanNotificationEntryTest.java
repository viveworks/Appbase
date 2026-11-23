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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.management.Descriptor;
import javax.management.MBeanNotificationInfo;
import javax.management.modelmbean.DescriptorSupport;
import java.util.List;
import java.util.Map;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class MXBeanNotificationEntryTest {
    @Test
    void testSerde() throws Exception {
        final Descriptor descriptor = new DescriptorSupport();
        descriptor.setField("fk1", "fv1");
        descriptor.setField("fk2", "fv2");
        final Map<String, String> fieldMap = EntryUtil.descriptor2FieldMap(descriptor);

        final MXBeanNotificationEntry mxBeanNotificationEntry = MXBeanNotificationEntry
                .builder()
                .name("nName1")
                .description("nDesc1")
                .fieldMap(fieldMap)
                .types(List.of("type1", "type2"))
                .build();

        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();
        final String yaml = yamlMapper
                .writerFor(MXBeanNotificationEntry.class)
                .writeValueAsString(mxBeanNotificationEntry);
        final MXBeanNotificationEntry mxBeanNotificationEntry1 = yamlMapper
                .readerFor(MXBeanNotificationEntry.class)
                .readValue(yaml);

        Assertions.assertEquals(mxBeanNotificationEntry.getName(), mxBeanNotificationEntry1.getName());
        Assertions.assertEquals(mxBeanNotificationEntry.getDescription(), mxBeanNotificationEntry1.getDescription());
        Assertions.assertEquals(mxBeanNotificationEntry.getFieldMap(), mxBeanNotificationEntry1.getFieldMap());
        Assertions.assertEquals(mxBeanNotificationEntry.getTypes(), mxBeanNotificationEntry1.getTypes());
        Assertions.assertEquals(mxBeanNotificationEntry, mxBeanNotificationEntry1);
        Assertions.assertEquals(mxBeanNotificationEntry.hashCode(), mxBeanNotificationEntry1.hashCode());
        Assertions.assertEquals(mxBeanNotificationEntry.toString(), mxBeanNotificationEntry1.toString());
    }

    @Test
    void testFrom() throws Exception {
        final Descriptor descriptor = new DescriptorSupport();
        descriptor.setField("fk1", "fv1");
        descriptor.setField("fk2", "fv2");
        final Map<String, String> fieldMap = EntryUtil.descriptor2FieldMap(descriptor);

        final MBeanNotificationInfo mBeanNotificationInfo = new MBeanNotificationInfo(
                new String[] {"nt1", "nt2"}, "nName1", "nDesc1", descriptor);
        final MXBeanNotificationEntry mxBeanNotificationEntry = MXBeanNotificationEntry.from(mBeanNotificationInfo);

        Assertions.assertEquals("nName1", mxBeanNotificationEntry.getName());
        Assertions.assertEquals("nDesc1", mxBeanNotificationEntry.getDescription());
        Assertions.assertEquals(fieldMap, mxBeanNotificationEntry.getFieldMap());
        Assertions.assertEquals(List.of("nt1", "nt2"), mxBeanNotificationEntry.getTypes());

        Assertions.assertNull(MXBeanNotificationEntry.from(null));
    }
}
