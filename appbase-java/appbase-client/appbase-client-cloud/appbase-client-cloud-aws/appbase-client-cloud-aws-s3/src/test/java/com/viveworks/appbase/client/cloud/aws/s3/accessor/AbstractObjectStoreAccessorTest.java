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

package com.viveworks.appbase.client.cloud.aws.s3.accessor;

import com.viveworks.appbase.client.cloud.aws.s3.S3ClientsService;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.metric.MetricService;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Timer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * AUD-33 (CWE-22 family): {@link AbstractObjectStoreAccessor#readFileDestinationPath(String)}
 * joined the caller-supplied {@code objectId} into {@code readDestinationBasePathname} with no
 * canonicalisation or confinement check. A traversal-shaped {@code objectId} (e.g. {@code
 * "../escaped.bin"}) resolved to a destination OUTSIDE the configured root. No in-repo caller
 * reaches this public accessor today, but it is a public library API, not dead code.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class AbstractObjectStoreAccessorTest {

    private static final class TestObjectStoreAccessor extends AbstractObjectStoreAccessor {
        TestObjectStoreAccessor(final AppbaseCore appbaseCore, final S3ClientsService s3ClientsService,
                                final String bucketName, final String readDestinationBasePathname) {
            super(appbaseCore, s3ClientsService, bucketName, readDestinationBasePathname);
        }

        @Override
        public String readDestinationSubDirectoryName() {
            return "downloads";
        }

        @Override
        public String keyPrefix() {
            return "/";
        }
    }

    private static AppbaseCore mockAppbaseCore() {
        final AppbaseCore appbaseCore = Mockito.mock(AppbaseCore.class);

        final LogService logService = Mockito.mock(LogService.class);
        Mockito.when(appbaseCore.getLogService()).thenReturn(logService);
        Mockito.when(logService.getLogger(Mockito.any(Class.class)))
                .thenReturn(LoggerFactory.getLogger(AbstractObjectStoreAccessorTest.class));

        final MetricService metricService = Mockito.mock(MetricService.class, Mockito.CALLS_REAL_METHODS);
        final MetricRegistry metricRegistry = Mockito.mock(MetricRegistry.class);
        final Timer timer = Mockito.mock(Timer.class);
        Mockito.when(appbaseCore.getMetricService()).thenReturn(metricService);
        Mockito.doReturn(metricRegistry).when(metricService).getMetricRegistry();
        Mockito.when(metricRegistry.timer(Mockito.anyString())).thenReturn(timer);

        return appbaseCore;
    }

    private static TestObjectStoreAccessor accessor(final Path baseDir) {
        final S3ClientsService s3ClientsService = Mockito.mock(S3ClientsService.class);
        return new TestObjectStoreAccessor(mockAppbaseCore(), s3ClientsService, "my-bucket", baseDir.toString());
    }

    @Test
    void readFileDestinationPathRejectsTraversalOutsideConfiguredRoot() throws Exception {
        final Path baseDir = Files.createTempDirectory(AbstractObjectStoreAccessorTest.class.getName());
        try {
            final TestObjectStoreAccessor accessor = accessor(baseDir);

            // One "../" only cancels the "downloads" sub-directory segment and still lands
            // beneath baseDir; a SECOND "../" is what actually escapes the configured root
            // itself, which is the property this fix guards.
            Assertions.assertThrows(RuntimeException.class,
                    () -> accessor.readFileDestinationPath("../../escaped.bin"),
                    "a traversal-shaped objectId must not resolve to a destination outside the "
                            + "configured root");

            final Path escaped = baseDir.getParent().resolve("escaped.bin");
            Assertions.assertFalse(Files.exists(escaped));
        } finally {
            Files.deleteIfExists(baseDir);
        }
    }

    @Test
    void readFileDestinationPathStillResolvesAnOrdinaryObjectIdBeneathTheRoot() throws Exception {
        final Path baseDir = Files.createTempDirectory(AbstractObjectStoreAccessorTest.class.getName());
        try {
            final TestObjectStoreAccessor accessor = accessor(baseDir);

            final Path destination = accessor.readFileDestinationPath("object-1.bin");

            Assertions.assertTrue(destination.normalize().startsWith(baseDir.toAbsolutePath().normalize()),
                    "an ordinary objectId's destination must remain beneath the configured root");
        } finally {
            Files.deleteIfExists(baseDir);
        }
    }
}
