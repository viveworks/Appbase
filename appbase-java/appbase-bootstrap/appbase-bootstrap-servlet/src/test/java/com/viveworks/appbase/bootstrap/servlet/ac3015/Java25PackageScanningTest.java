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

package com.viveworks.appbase.bootstrap.servlet.ac3015;

import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.Test;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves Jersey package scanning accepts classes emitted for the project's Java 25 baseline.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 */
final class Java25PackageScanningTest {

    @Test
    void test_AC3015_1_packageScanProcessesJava25ClassWithoutVersionWarning() throws IOException {
        assertTrue(Runtime.version().feature() >= 25, "AC3015 requires Java 25 or later");
        try (InputStream classStream = Java25Resource.class
                .getResourceAsStream("Java25Resource.class");
                DataInputStream classData = new DataInputStream(classStream))
        {
            assertEquals(0xCAFEBABE, classData.readInt());
            classData.readUnsignedShort();
            assertEquals(69, classData.readUnsignedShort(), "fixture must use Java 25 bytecode");
        }
        final List<String> warnings = new ArrayList<>();
        final Logger logger = Logger.getLogger("jersey.repackaged.org.objectweb.asm.ClassReader");
        final Handler handler = new Handler() {
            @Override
            public void publish(final LogRecord record) {
                warnings.add(record.getMessage());
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        logger.addHandler(handler);
        try {
            final ResourceConfig config =
                new ResourceConfig().packages(getClass().getPackageName());

            assertTrue(config.getClasses().contains(Java25Resource.class));
            assertTrue(warnings.stream()
                .noneMatch(message -> message.contains("Unsupported class file major version")));
        } finally {
            logger.removeHandler(handler);
        }
    }
}
