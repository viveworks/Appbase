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
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanParameterInfo;
import javax.management.modelmbean.DescriptorSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class MXBeanConstructorEntryTest {
    @Test
    void testSerde() throws Exception {
        final MXBeanParameterEntry mxBeanParameterEntry = MXBeanParameterEntry
                .builder()
                .name("paramName1")
                .description("paramDesc1")
                .type("paramType1")
                .fieldMap(Map.of("k11", "v11"))
                .build();
        final MXBeanConstructorEntry mxBeanConstructorEntry = MXBeanConstructorEntry
                .builder()
                .name("name2")
                .description("desc2")
                .fieldMap(Map.of("k1", "v1"))
                .signature(List.of(mxBeanParameterEntry))
                .build();

        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();
        final String yaml = yamlMapper
                .writerFor(MXBeanConstructorEntry.class)
                .writeValueAsString(mxBeanConstructorEntry);
        final MXBeanConstructorEntry mxBeanConstructorEntry1 = yamlMapper
                .readerFor(MXBeanConstructorEntry.class)
                .readValue(yaml);

        Assertions.assertEquals(mxBeanConstructorEntry.getName(), mxBeanConstructorEntry1.getName());
        Assertions.assertEquals(mxBeanConstructorEntry.getDescription(), mxBeanConstructorEntry1.getDescription());
        Assertions.assertEquals(mxBeanConstructorEntry.getFieldMap(), mxBeanConstructorEntry1.getFieldMap());
        Assertions.assertEquals(mxBeanConstructorEntry.getSignature(), mxBeanConstructorEntry1.getSignature());
        Assertions.assertEquals(mxBeanConstructorEntry, mxBeanConstructorEntry1);
        Assertions.assertEquals(mxBeanConstructorEntry.hashCode(), mxBeanConstructorEntry1.hashCode());
        Assertions.assertEquals(mxBeanConstructorEntry.toString(), mxBeanConstructorEntry1.toString());
    }

    @Test
    void testFrom() throws Exception {
        final Descriptor descriptor = new DescriptorSupport();
        descriptor.setField("fk1", "fv1");
        descriptor.setField("fk2", "fv2");
        final Map<String, String> fieldMap = EntryUtil.descriptor2FieldMap(descriptor);

        final MBeanParameterInfo[] mBeanParameterInfos = new MBeanParameterInfo[2];
        final MBeanParameterInfo mBeanParameterInfo1 = new MBeanParameterInfo(
                "paramName1", "paramType1", "paramDesc1", descriptor);
        mBeanParameterInfos[0] = mBeanParameterInfo1;

        final MBeanParameterInfo mBeanParameterInfo2 = new MBeanParameterInfo(
                "paramName2", "paramType2", "paramDesc2", descriptor);
        mBeanParameterInfos[1] = mBeanParameterInfo2;

        final List<MXBeanParameterEntry> mxBeanParameterEntries = new ArrayList<>();
        mxBeanParameterEntries.add(MXBeanParameterEntry.from(mBeanParameterInfo1));
        mxBeanParameterEntries.add(MXBeanParameterEntry.from(mBeanParameterInfo2));

        final MBeanConstructorInfo mBeanConstructorInfo = new MBeanConstructorInfo(
                "constructorName1", "constructorDesc1", mBeanParameterInfos, descriptor);
        final MXBeanConstructorEntry mxBeanConstructorEntry = MXBeanConstructorEntry.from(mBeanConstructorInfo);

        Assertions.assertNotNull(mxBeanConstructorEntry);
        Assertions.assertEquals("constructorDesc1", mxBeanConstructorEntry.getDescription());
        Assertions.assertEquals(fieldMap, mxBeanConstructorEntry.getFieldMap());
        Assertions.assertEquals("constructorName1", mxBeanConstructorEntry.getName());
        Assertions.assertEquals(mxBeanParameterEntries, mxBeanConstructorEntry.getSignature());

        Assertions.assertNull(MXBeanConstructorEntry.from(null));
    }
}
