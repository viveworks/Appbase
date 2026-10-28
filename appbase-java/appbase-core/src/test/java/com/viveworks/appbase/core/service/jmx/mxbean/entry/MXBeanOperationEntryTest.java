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
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.modelmbean.DescriptorSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class MXBeanOperationEntryTest {
    @Test
    void testSerde() throws Exception {
        final Descriptor descriptor = new DescriptorSupport();
        descriptor.setField("fk1", "fv1");
        descriptor.setField("fk2", "fv2");
        final Map<String, String> fieldMap = EntryUtil.descriptor2FieldMap(descriptor);

        final MBeanParameterInfo mBeanParameterInfo1 = new MBeanParameterInfo(
                "paramName1", "paramType1", "paramDesc1", descriptor);
        final MBeanParameterInfo mBeanParameterInfo2 = new MBeanParameterInfo(
                "paramName2", "paramType2", "paramDesc2", descriptor);

        final List<MXBeanParameterEntry> mxBeanParameterEntries = new ArrayList<>();
        mxBeanParameterEntries.add(MXBeanParameterEntry.from(mBeanParameterInfo1));
        mxBeanParameterEntries.add(MXBeanParameterEntry.from(mBeanParameterInfo2));

        final MXBeanOperationEntry mxBeanOperationEntry = MXBeanOperationEntry
                .builder()
                .name("oName1")
                .description("oDesc1")
                .fieldMap(fieldMap)
                .impact(1)
                .returnType("rType1")
                .signature(mxBeanParameterEntries)
                .build();

        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();
        final String yaml = yamlMapper
                .writerFor(MXBeanOperationEntry.class)
                .writeValueAsString(mxBeanOperationEntry);
        final MXBeanOperationEntry mxBeanOperationEntry1 = yamlMapper
                .readerFor(MXBeanOperationEntry.class)
                .readValue(yaml);

        Assertions.assertEquals(mxBeanOperationEntry.getName(), mxBeanOperationEntry1.getName());
        Assertions.assertEquals(mxBeanOperationEntry.getDescription(), mxBeanOperationEntry1.getDescription());
        Assertions.assertEquals(mxBeanOperationEntry.getFieldMap(), mxBeanOperationEntry1.getFieldMap());
        Assertions.assertEquals(mxBeanOperationEntry.getImpact(), mxBeanOperationEntry1.getImpact());
        Assertions.assertEquals(mxBeanOperationEntry.getReturnType(), mxBeanOperationEntry1.getReturnType());
        Assertions.assertEquals(mxBeanOperationEntry.getSignature(), mxBeanOperationEntry1.getSignature());
        Assertions.assertEquals(mxBeanOperationEntry, mxBeanOperationEntry1);
        Assertions.assertEquals(mxBeanOperationEntry.hashCode(), mxBeanOperationEntry1.hashCode());
        Assertions.assertEquals(mxBeanOperationEntry.toString(), mxBeanOperationEntry1.toString());
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

        final MBeanOperationInfo mBeanOperationInfo = new MBeanOperationInfo(
                "oName1", "oDesc1", mBeanParameterInfos, "oType1", 1, descriptor);
        final MXBeanOperationEntry mxBeanOperationEntry = MXBeanOperationEntry.from(mBeanOperationInfo);

        Assertions.assertNotNull(mxBeanOperationEntry);
        Assertions.assertEquals("oName1", mxBeanOperationEntry.getName());
        Assertions.assertEquals("oDesc1", mxBeanOperationEntry.getDescription());
        Assertions.assertEquals(fieldMap, mxBeanOperationEntry.getFieldMap());
        Assertions.assertEquals(1, mxBeanOperationEntry.getImpact());
        Assertions.assertEquals("oType1", mxBeanOperationEntry.getReturnType());
        Assertions.assertEquals(mxBeanParameterEntries, mxBeanOperationEntry.getSignature());

        Assertions.assertNull(MXBeanOperationEntry.from(null));
    }
}
