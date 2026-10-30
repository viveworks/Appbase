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
import javax.management.MBeanParameterInfo;
import javax.management.modelmbean.DescriptorSupport;
import java.util.Map;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class MXBeanParameterEntryTest {
    @Test
    void testSerde() throws Exception {
        final Descriptor descriptor = new DescriptorSupport();
        descriptor.setField("fk1", "fv1");
        descriptor.setField("fk2", "fv2");
        final Map<String, String> fieldMap = EntryUtil.descriptor2FieldMap(descriptor);

        final MXBeanParameterEntry mxBeanParameterEntry = MXBeanParameterEntry
                .builder()
                .name("pName1")
                .description("pDesc1")
                .fieldMap(fieldMap)
                .type("pType1")
                .build();

        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();
        final String yaml = yamlMapper
                .writerFor(MXBeanParameterEntry.class)
                .writeValueAsString(mxBeanParameterEntry);
        final MXBeanParameterEntry mxBeanParameterEntry1 = yamlMapper
                .readerFor(MXBeanParameterEntry.class)
                .readValue(yaml);

        Assertions.assertEquals(mxBeanParameterEntry.getName(), mxBeanParameterEntry1.getName());
        Assertions.assertEquals(mxBeanParameterEntry.getDescription(), mxBeanParameterEntry1.getDescription());
        Assertions.assertEquals(mxBeanParameterEntry.getFieldMap(), mxBeanParameterEntry1.getFieldMap());
        Assertions.assertEquals(mxBeanParameterEntry.getType(), mxBeanParameterEntry1.getType());
        Assertions.assertEquals(mxBeanParameterEntry, mxBeanParameterEntry1);
        Assertions.assertEquals(mxBeanParameterEntry.hashCode(), mxBeanParameterEntry1.hashCode());
        Assertions.assertEquals(mxBeanParameterEntry.toString(), mxBeanParameterEntry1.toString());
    }

    @Test
    void testFrom() throws Exception {
        final Descriptor descriptor = new DescriptorSupport();
        descriptor.setField("fk1", "fv1");
        descriptor.setField("fk2", "fv2");
        final Map<String, String> fieldMap = EntryUtil.descriptor2FieldMap(descriptor);

        final MBeanParameterInfo mBeanParameterInfo = new MBeanParameterInfo(
                "pName1", "pType1", "pDesc1", descriptor);
        final MXBeanParameterEntry mxBeanParameterEntry = MXBeanParameterEntry.from(mBeanParameterInfo);

        Assertions.assertEquals("pName1", mxBeanParameterEntry.getName());
        Assertions.assertEquals("pDesc1", mxBeanParameterEntry.getDescription());
        Assertions.assertEquals(fieldMap, mxBeanParameterEntry.getFieldMap());
        Assertions.assertEquals("pType1", mxBeanParameterEntry.getType());

        Assertions.assertNull(MXBeanParameterEntry.from(null));
    }
}
