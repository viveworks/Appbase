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
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
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
class MXBeanEntryTest {
    @Test
    void testSerde() throws Exception {
        final Descriptor descriptor = new DescriptorSupport();
        descriptor.setField("fk1", "fv1");
        descriptor.setField("fk2", "fv2");
        final Map<String, String> fieldMap = EntryUtil.descriptor2FieldMap(descriptor);

        final List<MXBeanAttributeEntry> mxBeanAttributeEntries = new ArrayList<>();
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
        mxBeanAttributeEntries.add(mxBeanAttributeEntry);

        final List<MXBeanConstructorEntry> mxBeanConstructorEntries = new ArrayList<>();
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
        mxBeanConstructorEntries.add(mxBeanConstructorEntry);

        final List<MXBeanNotificationEntry> mxBeanNotificationEntries = new ArrayList<>();
        final MXBeanNotificationEntry mxBeanNotificationEntry = MXBeanNotificationEntry
                .builder()
                .name("nName1")
                .description("nDesc1")
                .fieldMap(fieldMap)
                .types(List.of("type1", "type2"))
                .build();
        mxBeanNotificationEntries.add(mxBeanNotificationEntry);

        final MBeanParameterInfo mBeanParameterInfo1 = new MBeanParameterInfo(
                "paramName1", "paramType1", "paramDesc1", descriptor);
        final MBeanParameterInfo mBeanParameterInfo2 = new MBeanParameterInfo(
                "paramName2", "paramType2", "paramDesc2", descriptor);
        final List<MXBeanParameterEntry> mxBeanParameterEntries = new ArrayList<>();
        mxBeanParameterEntries.add(MXBeanParameterEntry.from(mBeanParameterInfo1));
        mxBeanParameterEntries.add(MXBeanParameterEntry.from(mBeanParameterInfo2));
        final List<MXBeanOperationEntry> mxBeanOperationEntries = new ArrayList<>();
        final MXBeanOperationEntry mxBeanOperationEntry = MXBeanOperationEntry
                .builder()
                .name("oName1")
                .description("oDesc1")
                .fieldMap(fieldMap)
                .impact(1)
                .returnType("rType1")
                .signature(mxBeanParameterEntries)
                .build();
        mxBeanOperationEntries.add(mxBeanOperationEntry);

        final MXBeanEntry mxBeanEntry = MXBeanEntry
                .builder()
                .className(getClass().getName())
                .description("description1")
                .fieldMap(fieldMap)
                .mxBeanAttributeEntries(mxBeanAttributeEntries)
                .mxBeanConstructorEntries(mxBeanConstructorEntries)
                .mxBeanNotificationEntries(mxBeanNotificationEntries)
                .mxBeanOperationEntries(mxBeanOperationEntries)
                .build();

        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();
        final String yaml = yamlMapper
                .writerFor(MXBeanEntry.class)
                .writeValueAsString(mxBeanEntry);
        final MXBeanEntry mxBeanEntry1 = yamlMapper
                .readerFor(MXBeanEntry.class)
                .readValue(yaml);

        Assertions.assertEquals(mxBeanEntry.getClassName(), mxBeanEntry1.getClassName());
        Assertions.assertEquals(mxBeanEntry.getDescription(), mxBeanEntry1.getDescription());
        Assertions.assertEquals(mxBeanEntry.getFieldMap(), mxBeanEntry1.getFieldMap());
        Assertions.assertEquals(mxBeanEntry.getMxBeanAttributeEntries(), mxBeanEntry1.getMxBeanAttributeEntries());
        Assertions.assertEquals(mxBeanEntry.getMxBeanConstructorEntries(), mxBeanEntry1.getMxBeanConstructorEntries());
        Assertions.assertEquals(
                mxBeanEntry.getMxBeanNotificationEntries(), mxBeanEntry1.getMxBeanNotificationEntries());
        Assertions.assertEquals(mxBeanEntry.getMxBeanOperationEntries(), mxBeanEntry1.getMxBeanOperationEntries());
        Assertions.assertEquals(mxBeanEntry, mxBeanEntry1);
        Assertions.assertEquals(mxBeanEntry.hashCode(), mxBeanEntry1.hashCode());
        Assertions.assertEquals(mxBeanEntry.toString(), mxBeanEntry1.toString());
    }

    @Test
    void testFrom() throws Exception {
        final Descriptor descriptor = new DescriptorSupport();
        descriptor.setField("fk1", "fv1");
        descriptor.setField("fk2", "fv2");
        final Map<String, String> fieldMap = EntryUtil.descriptor2FieldMap(descriptor);

        final MBeanAttributeInfo[] mBeanAttributeInfos = new MBeanAttributeInfo[1];
        final MBeanAttributeInfo mBeanAttributeInfo = new MBeanAttributeInfo(
                "name1", "type1", "description1", true, false, false, descriptor);
        mBeanAttributeInfos[0] = mBeanAttributeInfo;
        final List<MXBeanAttributeEntry> mxBeanAttributeEntries = new ArrayList<>();
        mxBeanAttributeEntries.add(MXBeanAttributeEntry.from(mBeanAttributeInfo));

        final MBeanParameterInfo[] mBeanParameterInfos = new MBeanParameterInfo[2];
        final MBeanParameterInfo mBeanParameterInfo1 = new MBeanParameterInfo(
                "paramName1", "paramType1", "paramDesc1", descriptor);
        mBeanParameterInfos[0] = mBeanParameterInfo1;
        final MBeanParameterInfo mBeanParameterInfo2 = new MBeanParameterInfo(
                "paramName2", "paramType2", "paramDesc2", descriptor);
        mBeanParameterInfos[1] = mBeanParameterInfo2;

        final MBeanConstructorInfo[] mBeanConstructorInfos = new MBeanConstructorInfo[1];
        final MBeanConstructorInfo mBeanConstructorInfo = new MBeanConstructorInfo(
                "constructorName1", "constructorDesc1", mBeanParameterInfos, descriptor);
        mBeanConstructorInfos[0] = mBeanConstructorInfo;
        final List<MXBeanConstructorEntry> mxBeanConstructorEntries = new ArrayList<>();
        mxBeanConstructorEntries.add(MXBeanConstructorEntry.from(mBeanConstructorInfo));

        final MBeanNotificationInfo[] mBeanNotificationInfos = new MBeanNotificationInfo[1];
        final MBeanNotificationInfo mBeanNotificationInfo = new MBeanNotificationInfo(
                new String[] {"nt1", "nt2"}, "nName1", "nDesc1", descriptor);
        mBeanNotificationInfos[0] = mBeanNotificationInfo;
        final List<MXBeanNotificationEntry> mxBeanNotificationEntries = new ArrayList<>();
        mxBeanNotificationEntries.add(MXBeanNotificationEntry.from(mBeanNotificationInfo));

        final MBeanOperationInfo[] mBeanOperationInfos = new MBeanOperationInfo[1];
        final MBeanOperationInfo mBeanOperationInfo = new MBeanOperationInfo(
                "oName1", "oDesc1", mBeanParameterInfos, "oType1", 1, descriptor);
        mBeanOperationInfos[0] = mBeanOperationInfo;
        final List<MXBeanOperationEntry> mxBeanOperationEntries = new ArrayList<>();
        mxBeanOperationEntries.add(MXBeanOperationEntry.from(mBeanOperationInfo));

        final MBeanInfo mBeanInfo = new MBeanInfo(
                getClass().getName(),
                "Description",
                mBeanAttributeInfos,
                mBeanConstructorInfos,
                mBeanOperationInfos,
                mBeanNotificationInfos,
                descriptor);
        final MXBeanEntry mxBeanEntry = MXBeanEntry.from(mBeanInfo);

        Assertions.assertNotNull(mxBeanEntry);
        Assertions.assertEquals(getClass().getName(), mxBeanEntry.getClassName());
        Assertions.assertEquals("Description", mBeanInfo.getDescription());
        Assertions.assertEquals(fieldMap, mxBeanEntry.getFieldMap());
        Assertions.assertEquals(mxBeanAttributeEntries, mxBeanEntry.getMxBeanAttributeEntries());
        Assertions.assertEquals(mxBeanConstructorEntries, mxBeanEntry.getMxBeanConstructorEntries());
        Assertions.assertEquals(mxBeanNotificationEntries, mxBeanEntry.getMxBeanNotificationEntries());
        Assertions.assertEquals(mxBeanOperationEntries, mxBeanEntry.getMxBeanOperationEntries());

        Assertions.assertNull(MXBeanEntry.from(null));
    }
}
