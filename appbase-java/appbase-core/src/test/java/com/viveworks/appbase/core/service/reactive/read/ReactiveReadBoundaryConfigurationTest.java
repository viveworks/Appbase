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

package com.viveworks.appbase.core.service.reactive.read;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class ReactiveReadBoundaryConfigurationTest {
    @Test
    void zeroConfigYieldsSpecDefaults() {
        final ReactiveReadBoundaryConfiguration c = ReactiveReadBoundaryConfiguration.builder().build();

        assertEquals(1000, c.getMaxPageSize());
        assertEquals(30, c.getOffloadTimeoutSeconds());
        assertEquals(Duration.ofSeconds(30), c.timeout());
        assertNull(c.getOffloadSchedulerId());
    }

    @Test
    void validatorRejectsNonPositiveMaxPageSize() {
        final AppbaseRuntimeException ex = assertThrows(AppbaseRuntimeException.class,
                () -> ReactiveReadBoundaryConfiguration.builder().maxPageSize(0).build());

        assertTrue(ex.getMessage().contains("maxPageSize"));
    }

    @Test
    void validatorRejectsNonPositiveOffloadTimeoutSeconds() {
        final AppbaseRuntimeException ex = assertThrows(AppbaseRuntimeException.class,
                () -> ReactiveReadBoundaryConfiguration.builder().offloadTimeoutSeconds(0).build());

        assertTrue(ex.getMessage().contains("offloadTimeoutSeconds"));
    }

    @Test
    void jsonRoundTripPreservesAllFields() throws Exception {
        final ReactiveReadBoundaryConfiguration in = ReactiveReadBoundaryConfiguration
                .builder()
                .maxPageSize(250)
                .offloadTimeoutSeconds(5)
                .offloadSchedulerId("core-file-io")
                .build();

        final ObjectMapper mapper = new ObjectMapper();
        final ReactiveReadBoundaryConfiguration out =
                mapper.readValue(mapper.writeValueAsString(in), ReactiveReadBoundaryConfiguration.class);

        assertEquals(in, out);
        assertEquals(in.hashCode(), out.hashCode());
        assertEquals(in.toString(), out.toString());
    }
}
