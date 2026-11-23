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

package com.viveworks.appbase.core.service.metric;

import io.micrometer.core.instrument.Tags;
import org.eclipse.microprofile.metrics.Tag;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class MetricServiceTest {
    @Test
    void testToMicroProfileTags() {
        final Tags microMeterTags1 = Tags.of("n1", "v1", "n2", "v2");
        final Tag[] microProfileTags1 = MetricService.toMicroProfileTags(microMeterTags1);
        Assertions.assertNotNull(microProfileTags1);
        Assertions.assertEquals(2, microProfileTags1.length);
        Assertions.assertEquals("n1", microProfileTags1[0].getTagName());
        Assertions.assertEquals("v1", microProfileTags1[0].getTagValue());
        Assertions.assertEquals("n2", microProfileTags1[1].getTagName());
        Assertions.assertEquals("v2", microProfileTags1[1].getTagValue());

        Assertions.assertNull(MetricService.toMicroProfileTags(null));
    }
}
