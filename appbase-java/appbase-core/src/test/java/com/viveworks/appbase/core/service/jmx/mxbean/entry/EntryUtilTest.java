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

import com.viveworks.appbase.core.service.configuration.DefaultConfigurationServiceConfiguration;
import com.viveworks.appbase.core.service.log.DefaultLogServiceConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.management.Descriptor;
import javax.management.MBeanParameterInfo;
import javax.management.modelmbean.DescriptorSupport;
import java.util.List;
import java.util.Map;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class EntryUtilTest {
    @Test
    void testDescriptor2FieldMap() {
        final Descriptor descriptor = new DescriptorSupport();
        descriptor.setField("fk1", "fv1");
        descriptor.setField("fk2", "fv2");

        final Map<String, String> fieldMap = EntryUtil.descriptor2FieldMap(descriptor);
        Assertions.assertNotNull(fieldMap);
        Assertions.assertEquals(2, fieldMap.size());
        Assertions.assertEquals("fv1", fieldMap.get("fk1"));
        Assertions.assertEquals("fv2", fieldMap.get("fk2"));
    }

    @Test
    void testSignature2MxBeanParameterEntries() {
        final MBeanParameterInfo[] signature = new MBeanParameterInfo[2];

        final MBeanParameterInfo mBeanParameterInfo1 = new MBeanParameterInfo(
                "defaultConfigurationServiceConfiguration",
                DefaultConfigurationServiceConfiguration.class.getName(),
                "Configuration service configuration");
        signature[0] = mBeanParameterInfo1;

        final MBeanParameterInfo mBeanParameterInfo2 = new MBeanParameterInfo(
                "defaultLogServiceConfiguration",
                DefaultLogServiceConfiguration.class.getName(),
                "Log service configuration");
        signature[1] = mBeanParameterInfo2;

        final List<MXBeanParameterEntry> mxBeanParameterEntries = EntryUtil.signature2MxBeanParameterEntries(signature);
        Assertions.assertNotNull(mxBeanParameterEntries);
        Assertions.assertEquals(2, mxBeanParameterEntries.size());
    }
}
